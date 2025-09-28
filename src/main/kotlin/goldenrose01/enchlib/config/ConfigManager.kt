package goldenrose01.enchlib.config

import net.fabricmc.loader.api.FabricLoader

import goldenrose01.enchlib.utils.EnchLogger

import net.minecraft.server.MinecraftServer
import net.minecraft.util.WorldSavePath

import java.util.concurrent.ConcurrentHashMap
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.Files
import java.nio.charset.StandardCharsets

import kotlin.io.use
/**
 * ConfigManager è responsabile del caricamento, della validazione e della
 * fornitura di accesso a tutti i file `.config` usati da EnchLib. Carica i file
 * di configurazione dalla directory dell'utente (ad es. `.minecraft/config/enchlib`),
 * li valida e fornisce accessor tipizzati.
 * - AviableEnch.config            -> <id>=true|false  (abilitazione)
 * - EnchLVLmax.config            -> <id>=<int>       (override opzionale)
 * - EnchRarity.config            -> <id>=<rarity>
 * - EnchCompatibility.config     -> <id>=g1,g2
 * - EnchCategories.config        -> <id>=c1,c2
 * - EnchUncompatibility.config   -> <id>=id1,id2
 *
 * Gestione file .config (formato properties) per EnchLib.
 * Percorso:
 *   - per-mondo:  <.minecraft>/saves/<world>/config/enchlib   (preferito)
 *   - fallback:   <.minecraft>/config/enchlib                  (se non inizializzato con server)
 *
 * Se un file di configurazione manca o contiene voci non valide, vengono
 * applicati valori di default e viene loggato un avviso tramite [EnchLogger].
 */

object ConfigManager {

    // ====== Nomi file ======
    private const val FILE_AVAILABLE   = "AviableEnch.config"
    private const val FILE_LVL_MAX     = "EnchLVLmax.config"
    private const val FILE_RARITY      = "EnchRarity.config"
    private const val FILE_COMPAT      = "EnchCompatibility.config"
    private const val FILE_CATEGORIES  = "EnchCategories.config"
    private const val FILE_UNCOMPAT    = "EnchUncompatibility.config"

    // ====== Stato in memoria ======
    private val availableMap:    MutableMap<String, Boolean>        = ConcurrentHashMap()
    private val maxLevelMap:     MutableMap<String, Int>            = ConcurrentHashMap()
    private val rarityMap:       MutableMap<String, String>         = ConcurrentHashMap()
    private val compatMap:       MutableMap<String, List<String>>   = ConcurrentHashMap()
    private val catMap:          MutableMap<String, List<String>>   = ConcurrentHashMap()
    private val uncompatMap:     MutableMap<String, List<String>>   = ConcurrentHashMap()

    // ==== DTO compat per vecchi debug (/plusec-debug) ====
    data class AvailableEnchant(val id: String, val sources: List<String>)
    val availableEnchantments: List<AvailableEnchant>
        get() = availableMap.map { (id, enabled) ->
            AvailableEnchant(id, listOf(if (enabled) "enabled" else "disabled"))
        }

    val enchantmentLevelMax: Map<String, Int>            get() = maxLevelMap.toMap()
    val enchantmentRarity: Map<String, String>           get() = rarityMap.toMap()
    val enchantmentCompatibility: Map<String, List<String>> get() = compatMap.toMap()
    val enchantmentCategories: Map<String, List<String>> get() = catMap.toMap()
    val enchantmentUncompatibility: Map<String, List<String>> get() = uncompatMap.toMap()

    // ==== Percorso cartella mondo ====
    fun worldConfigDir(server: MinecraftServer): Path =
        server.getSavePath(WorldSavePath.ROOT).resolve("config").resolve("enchlib")

    // ==== API ====
    fun loadAll(server: MinecraftServer) {
        ensureCoreFiles(server)
        loadAvailable(server)
        loadMaxLevels(server)
        loadRarity(server)
        loadCompatibility(server)
        loadCategories(server)
        loadUncompatibility(server)
        EnchLogger.info("EnchLib: config caricati da ${worldConfigDir(server)}")
    }

    fun reloadCoreFilesIfNeeded(server: MinecraftServer) {
        ensureCoreFiles(server)
    }

    fun isEnchantmentEnabled(id: String): Boolean = availableMap[id] == true

    /** Overload compat: senza server → usa solo override file, default 1 */
    fun getMaxLevel(id: String): Int = maxLevelMap[id] ?: 1

    /** Con server: prova registry se non c’è override */
    fun getMaxLevel(id: String, server: MinecraftServer): Int {
        maxLevelMap[id]?.let { return it }
        return resolveRuntimeMaxLevel(id, server) ?: 1
    }

    // ==== exists/ensure (sui file del mondo) ====
    fun existsInAvailable(server: MinecraftServer, id: String) = fileContainsKey(worldConfigDir(server).resolve(FILE_AVAILABLE), id)
    fun existsInLvlMax(server: MinecraftServer, id: String)   = fileContainsKey(worldConfigDir(server).resolve(FILE_LVL_MAX), id)
    fun existsInRarity(server: MinecraftServer, id: String)   = fileContainsKey(worldConfigDir(server).resolve(FILE_RARITY), id)
    fun existsInCompat(server: MinecraftServer, id: String)   = fileContainsKey(worldConfigDir(server).resolve(FILE_COMPAT), id)
    fun existsInCategories(server: MinecraftServer, id: String)= fileContainsKey(worldConfigDir(server).resolve(FILE_CATEGORIES), id)
    fun existsInUncompat(server: MinecraftServer, id: String) = fileContainsKey(worldConfigDir(server).resolve(FILE_UNCOMPAT), id)

    fun ensureAvailable(server: MinecraftServer, id: String, enabled: Boolean) {
        val p = worldConfigDir(server).resolve(FILE_AVAILABLE)
        if (!fileContainsKey(p, id)) appendLine(p, "$id=${enabled}")
    }

    fun ensureLvlMax(server: MinecraftServer, id: String, level: Int?) {
        val p = worldConfigDir(server).resolve(FILE_LVL_MAX)
        if (!fileContainsKey(p, id) && level != null) appendLine(p, "$id=$level")
    }

    fun ensureRarity(server: MinecraftServer, id: String, rarity: String) {
        val p = worldConfigDir(server).resolve(FILE_RARITY)
        if (!fileContainsKey(p, id)) appendLine(p, "$id=$rarity")
    }

    fun ensureCompat(server: MinecraftServer, id: String, groups: List<String>) {
        val p = worldConfigDir(server).resolve(FILE_COMPAT)
        if (!fileContainsKey(p, id)) {
            val value = if (groups.isEmpty()) "" else groups.joinToString(",")
            appendLine(p, "$id=$value")
        }
    }

    fun ensureCategories(server: MinecraftServer, id: String, groups: List<String>) {
        val p = worldConfigDir(server).resolve(FILE_CATEGORIES)
        if (!fileContainsKey(p, id)) {
            val value = if (groups.isEmpty()) "" else groups.joinToString(",")
            appendLine(p, "$id=$value")
        }
    }

    fun ensureUncompat(server: MinecraftServer, id: String, groups: List<String>) {
        val p = worldConfigDir(server).resolve(FILE_UNCOMPAT)
        if (!fileContainsKey(p, id)) {
            val value = if (groups.isEmpty()) "" else groups.joinToString(",")
            appendLine(p, "$id=$value")
        }
    }

    // ==== Loader singoli ====
    private fun loadAvailable(server: MinecraftServer) {
        availableMap.clear()
        val p = worldConfigDir(server).resolve(FILE_AVAILABLE)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val v  = parts[1].trim().lowercase()
            val enabled = v == "true" || v == "enabled"
            availableMap[id] = enabled
        }
    }

    private fun loadMaxLevels(server: MinecraftServer) {
        maxLevelMap.clear()
        val p = worldConfigDir(server).resolve(FILE_LVL_MAX)
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

    private fun loadRarity(server: MinecraftServer) {
        rarityMap.clear()
        val p = worldConfigDir(server).resolve(FILE_RARITY)
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

    private fun loadCompatibility(server: MinecraftServer) {
        compatMap.clear()
        val p = worldConfigDir(server).resolve(FILE_COMPAT)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val groups = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            compatMap[id] = groups
        }
    }

    private fun loadCategories(server: MinecraftServer) {
        catMap.clear()
        val p = worldConfigDir(server).resolve(FILE_CATEGORIES)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val groups = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            catMap[id] = groups
        }
    }

    private fun loadUncompatibility(server: MinecraftServer) {
        uncompatMap.clear()
        val p = worldConfigDir(server).resolve(FILE_UNCOMPAT)
        if (!Files.exists(p)) return
        Files.readAllLines(p, StandardCharsets.UTF_8).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEach
            val id = parts[0].trim()
            val groups = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            uncompatMap[id] = groups
        }
    }

    // ==== ensure core: copia da resources se mancano ====
    private fun ensureCoreFiles(server: MinecraftServer) {
        val dir = worldConfigDir(server)
        Files.createDirectories(dir)
        copyFromResourcesIfMissing(dir.resolve(FILE_AVAILABLE),   "/config/$FILE_AVAILABLE")
        copyFromResourcesIfMissing(dir.resolve(FILE_LVL_MAX),     "/config/$FILE_LVL_MAX")
        copyFromResourcesIfMissing(dir.resolve(FILE_RARITY),      "/config/$FILE_RARITY")
        copyFromResourcesIfMissing(dir.resolve(FILE_COMPAT),      "/config/$FILE_COMPAT")
        copyFromResourcesIfMissing(dir.resolve(FILE_CATEGORIES),  "/config/$FILE_CATEGORIES")
        copyFromResourcesIfMissing(dir.resolve(FILE_UNCOMPAT),    "/config/$FILE_UNCOMPAT")
    }

    private fun copyFromResourcesIfMissing(target: Path, resourcePath: String) {
        if (Files.exists(target)) return
        val stream: InputStream? = javaClass.getResourceAsStream(resourcePath)
        if (stream != null) {
            stream.use { input ->
                Files.copy(input, target)
            }
        } else {
            // Se nel jar non c'è un default, crea file con header minimo
            val header = when (target.fileName.toString()) {
                FILE_AVAILABLE   -> listOf("# EnchLib - AviableEnch.config (properties)", "# <id>=true|false", "")
                FILE_LVL_MAX     -> listOf("# EnchLib - EnchLVLmax.config", "# <id>=level", "")
                FILE_RARITY      -> listOf("# EnchLib - EnchRarity.config", "# <id>=rarity", "")
                FILE_COMPAT      -> listOf("# EnchLib - EnchCompatibility.config", "# <id>=group1,group2,...", "")
                FILE_CATEGORIES  -> listOf("# EnchLib - EnchCategories.config", "# <id>=cat1,cat2,...", "")
                FILE_UNCOMPAT    -> listOf("# EnchLib - EnchUncompatibility.config", "# <id>=otherId1,otherId2,...", "")
                else             -> listOf("# EnchLib", "")
            }
            Files.writeString(
                target,
                header.joinToString("\n") + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE
            )
        }
    }

    // ==== Utility file ====
    private fun appendLine(path: Path, line: String) {
        Files.writeString(
            path,
            line + System.lineSeparator(),
            StandardCharsets.UTF_8,
            if (Files.exists(path)) StandardOpenOption.APPEND else StandardOpenOption.CREATE
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

    // ==== Max level runtime dal registry ====
    private fun resolveRuntimeMaxLevel(id: String, server: MinecraftServer): Int? {
        return try {
            val reg = server.registryManager.getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
            val identifier = net.minecraft.util.Identifier.tryParse(id) ?: return null
            val opt = reg.getEntry(identifier)
            if (!opt.isPresent) return null
            val ref = opt.get()
            val ench = try {
                val mValue = ref.javaClass.getMethod("value")
                mValue.invoke(ref) as net.minecraft.enchantment.Enchantment
            } catch (_: Throwable) {
                return 1
            }
            val mGetMax = ench.javaClass.methods.firstOrNull { it.name.equals("getMaxLevel", true) && it.parameterCount == 0 }
            if (mGetMax != null) {
                (mGetMax.invoke(ench) as? Int) ?: 1
            } else {
                val mMax = ench.javaClass.methods.firstOrNull { it.name.equals("maxLevel", true) && it.parameterCount == 0 }
                (mMax?.invoke(ench) as? Int) ?: 1
            }
        } catch (_: Throwable) {
            null
        }
    }
}