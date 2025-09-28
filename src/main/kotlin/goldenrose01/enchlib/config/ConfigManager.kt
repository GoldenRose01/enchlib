package goldenrose01.enchlib.config

import net.fabricmc.loader.api.FabricLoader
import goldenrose01.enchlib.utils.EnchLogger
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.Files

/**
 * ConfigManager è responsabile del caricamento, della validazione e della
 * fornitura di accesso a tutti i file `.config` usati da EnchLib. Carica i file
 * di configurazione dalla directory dell'utente (ad es. `.minecraft/config/enchlib`),
 * li valida e fornisce accessor tipizzati.
 *
 * Se un file di configurazione manca o contiene voci non valide, vengono
 * applicati valori di default e viene loggato un avviso tramite [EnchLogger].
 */

object ConfigManager {

    // ==== Nomi file ====
    private const val FILE_AVAILABLE   = "AviableEnch.config"
    private const val FILE_DISABLED    = "DisabledEnch.config"         // <-- MANCAVA
    private const val FILE_LVL_MAX     = "EnchLVLmax.config"
    private const val FILE_RARITY      = "EnchRarity.config"
    private const val FILE_COMPAT      = "EnchCompatibility.config"
    private const val FILE_CATEGORIES  = "EnchCategories.config"
    private const val FILE_UNCOMPAT    = "EnchUncompatibility.config"

    // ==== Stato in memoria ====
    private val availableSources: MutableMap<String, List<String>> = ConcurrentHashMap()
    private val disabledSet: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val maxLevelMap: MutableMap<String, Int> = ConcurrentHashMap()
    private val rarityMap: MutableMap<String, String> = ConcurrentHashMap()
    private val compatibilityMap: MutableMap<String, List<String>> = ConcurrentHashMap()
    private val categoriesMap: MutableMap<String, List<String>> = ConcurrentHashMap()
    private val uncompatibilityMap: MutableMap<String, List<String>> = ConcurrentHashMap()

    data class AvailableEnchant(val id: String, val sources: List<String>)

    fun configDir(): Path = FabricLoader.getInstance().configDir.resolve("enchlib")

    fun loadAll() {
        ensureCoreFiles()
        loadAvailable()
        loadDisabled()
        loadMaxLevels()
        loadRarity()
        loadCompatibility()
        loadCategories()
        loadUncompatibility()
        EnchLogger.info("Config EnchLib caricati")
    }

    fun reloadCoreFilesIfNeeded() {
        ensureCoreFiles()
        loadAvailable()
        loadDisabled()
    }

    // ==== Accessors read-only per comandi/debug ====
    val availableEnchantments: List<AvailableEnchant>
        get() = availableSources.entries.map { AvailableEnchant(it.key, it.value) }
    val enchantmentLevelMax: Map<String, Int> get() = maxLevelMap.toMap()
    val enchantmentRarity: Map<String, String> get() = rarityMap.toMap()
    val enchantmentCompatibility: Map<String, List<String>> get() = compatibilityMap.toMap()
    val enchantmentCategories: Map<String, List<String>> get() = categoriesMap.toMap()
    val enchantmentUncompatibility: Map<String, List<String>> get() = uncompatibilityMap.toMap()

    fun isEnchantmentEnabled(id: String): Boolean {
        if (disabledSet.contains(id)) return false
        val sources = availableSources[id] ?: return false
        return sources.isNotEmpty() && !(sources.size == 1 && sources[0].equals("disabled", true))
    }

    // Max dinamico: override da file o valore runtime dal registry del server
    fun getMaxLevel(id: String, server: net.minecraft.server.MinecraftServer): Int {
        maxLevelMap[id]?.let { return it }
        return resolveRuntimeMaxLevel(id, server) ?: 1
    }
    @Suppress("DEPRECATION")
    fun getMaxLevel(id: String): Int {
        maxLevelMap[id]?.let { return it }
        val server = FabricLoader.getInstance().gameInstance as? net.minecraft.server.MinecraftServer
        return if (server != null) resolveRuntimeMaxLevel(id, server) ?: 1 else 1
    }

    // ==== Helper bootstrap ====
    fun currentDisabledIds(): Set<String> = disabledSet
    fun existsInAvailable(id: String)  = fileContainsKey(configDir().resolve(FILE_AVAILABLE), id)
    fun existsInLvlMax(id: String)     = fileContainsKey(configDir().resolve(FILE_LVL_MAX), id)
    fun existsInRarity(id: String)     = fileContainsKey(configDir().resolve(FILE_RARITY), id)
    fun existsInCompat(id: String)     = fileContainsKey(configDir().resolve(FILE_COMPAT), id)
    fun existsInCategories(id: String) = fileContainsKey(configDir().resolve(FILE_CATEGORIES), id)
    fun existsInUncompat(id: String)   = fileContainsKey(configDir().resolve(FILE_UNCOMPAT), id)

    fun ensureAvailable(id: String, enabled: Boolean) {
        val p = configDir().resolve(FILE_AVAILABLE)
        if (!fileContainsKey(p, id)) {
            appendLine(p, "$id=${if (enabled) "true" else "false"}")
        }
    }
    fun ensureLvlMax(id: String, level: Int?) {
        val p = configDir().resolve(FILE_LVL_MAX)
        if (!fileContainsKey(p, id) && level != null) appendLine(p, "$id=$level")
    }
    fun ensureRarity(id: String, rarity: String) {
        val p = configDir().resolve(FILE_RARITY)
        if (!fileContainsKey(p, id)) appendLine(p, "$id=$rarity")
    }
    fun ensureCompat(id: String, groups: List<String>) {
        val p = configDir().resolve(FILE_COMPAT)
        if (!fileContainsKey(p, id)) {
            val value = if (groups.isEmpty()) "" else groups.joinToString(",")
            appendLine(p, "$id=$value")
        }
    }
    fun ensureCategories(id: String, groups: List<String>) {
        val p = configDir().resolve(FILE_CATEGORIES)
        if (!fileContainsKey(p, id)) {
            val value = if (groups.isEmpty()) "" else groups.joinToString(",")
            appendLine(p, "$id=$value")
        }
    }
    fun ensureUncompat(id: String, groups: List<String>) {
        val p = configDir().resolve(FILE_UNCOMPAT)
        if (!fileContainsKey(p, id)) {
            val value = if (groups.isEmpty()) "" else groups.joinToString(",")
            appendLine(p, "$id=$value")
        }
    }

    // ====== Runtime resolver ======
    private fun resolveRuntimeMaxLevel(id: String, server: net.minecraft.server.MinecraftServer): Int? {
        return try {
            val reg = server.registryManager.getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
            val identifier = net.minecraft.util.Identifier.tryParse(id) ?: return null

            // getEntry(Identifier) -> Optional<RegistryEntry.Reference<Enchantment>>
            val opt = reg.getEntry(identifier)
            if (!opt.isPresent) return null
            val ref = opt.get()

            // Prova a ricavare l'istanza Enchantment (value()) con reflection
            val ench = try {
                val mValue = ref.javaClass.getMethod("value")
                mValue.invoke(ref) as net.minecraft.enchantment.Enchantment
            } catch (_: Throwable) {
                return 1
            }

            // getMaxLevel() o maxLevel()
            ench.javaClass.methods.firstOrNull { it.name.equals("getMaxLevel", true) && it.parameterCount == 0 }?.let {
                (it.invoke(ench) as? Int) ?: 1
            } ?: run {
                val m = ench.javaClass.methods.firstOrNull { it.name.equals("maxLevel", true) && it.parameterCount == 0 }
                (m?.invoke(ench) as? Int) ?: 1
            }
        } catch (_: Throwable) {
            null
        }
    }

    // ====== I/O config ======   <-- **TUTTO QUESTO ORA È DENTRO L'OBJECT**
    private fun appendLine(path: Path, line: String) {
        Files.writeString(
            path,
            line + System.lineSeparator(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,         // crea se manca
            StandardOpenOption.APPEND
        )
    }

    private fun fileContainsKey(path: Path, key: String): Boolean {
        if (!Files.exists(path)) return false
        Files.readAllLines(path, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val k = line.substringBefore("=").trim()
            if (k == key) return true
        }
        return false
    }

    private fun ensureCoreFiles() {
        val dir = configDir()
        Files.createDirectories(dir)
        createIfMissing(dir.resolve(FILE_AVAILABLE),   headerAvailable())
        createIfMissing(dir.resolve(FILE_DISABLED),    headerGeneric("Disabled enchantments - one id per line"))
        createIfMissing(dir.resolve(FILE_LVL_MAX),     headerGeneric("Max levels (id=level), leave empty to use runtime value"))
        createIfMissing(dir.resolve(FILE_RARITY),      headerGeneric("Rarity per enchantment (id=rarity)"))
        createIfMissing(dir.resolve(FILE_COMPAT),      headerGeneric("Compatibility per enchantment (id=group1,group2)"))
        createIfMissing(dir.resolve(FILE_CATEGORIES),  headerGeneric("Categories per enchantment (id=cat1,cat2)"))
        createIfMissing(dir.resolve(FILE_UNCOMPAT),    headerGeneric("Uncompatibility pairs (id=incompat1,incompat2)"))
    }

    private fun createIfMissing(path: Path, header: String) {
        if (!Files.exists(path)) {
            Files.writeString(path, header, StandardCharsets.UTF_8)
        }
    }

    private fun headerAvailable(): String = buildString {
        appendLine("# EnchLib - AviableEnch.config (properties)")
        appendLine("# Formato: <id>=true|false   (true = abilitato, false = disabilitato)")
        appendLine("# Esempio: minecraft:sharpness=true")
        appendLine()
    }

    private fun headerGeneric(title: String): String = buildString {
        appendLine("# EnchLib - $title")
        appendLine()
    }

    private fun loadAvailable() {
        availableSources.clear()
        val p = configDir().resolve(FILE_AVAILABLE)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val v  = parts[1].trim()

            when (v.lowercase()) {
                "true", "enabled" -> {
                    availableSources[id] = listOf("enabled")
                    disabledSet.remove(id)
                }
                "false", "disabled" -> {
                    availableSources[id] = listOf("disabled")
                    disabledSet.add(id)
                }
                else -> {
                    // Back-compat (vecchio formato sorgenti CSV)
                    val sources = v.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (sources.size == 1 && sources[0].equals("disabled", true)) {
                        availableSources[id] = listOf("disabled")
                        disabledSet.add(id)
                    } else {
                        availableSources[id] = sources
                        disabledSet.remove(id)
                    }
                }
            }
        }
    }

    private fun loadDisabled() {
        disabledSet.clear()
        val p = configDir().resolve(FILE_DISABLED)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            disabledSet.add(line)
        }
    }

    private fun loadMaxLevels() {
        maxLevelMap.clear()
        val p = configDir().resolve(FILE_LVL_MAX)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val lvl = parts[1].trim().toIntOrNull()
            if (lvl != null) maxLevelMap[id] = lvl
        }
    }

    private fun loadRarity() {
        rarityMap.clear()
        val p = configDir().resolve(FILE_RARITY)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val rarity = parts[1].trim()
            rarityMap[id] = rarity
        }
    }

    private fun loadCompatibility() {
        compatibilityMap.clear()
        val p = configDir().resolve(FILE_COMPAT)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val groups = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            compatibilityMap[id] = groups
        }
    }

    private fun loadCategories() {
        categoriesMap.clear()
        val p = configDir().resolve(FILE_CATEGORIES)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val groups = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            categoriesMap[id] = groups
        }
    }

    private fun loadUncompatibility() {
        uncompatibilityMap.clear()
        val p = configDir().resolve(FILE_UNCOMPAT)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val groups = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            uncompatibilityMap[id] = groups
        }
    }
}