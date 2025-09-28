package goldenrose01.enchlib.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.mojang.logging.LogUtils

import goldenrose01.enchlib.Enchlib
import goldenrose01.enchlib.utils.Json5
import goldenrose01.enchlib.utils.EnchLogger

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

import net.minecraft.enchantment.Enchantment
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.component.type.ItemEnchantmentsComponent

import java.nio.file.Path
import java.nio.file.Files
import java.io.File
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets

import kotlin.io.path.exists
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * WorldConfigManager
 *
 * - Opera SOLO nella cartella del mondo: .minecraft/saves/<world>/config/enchlib
 * - Non crea JSON nel mondo.
 * - Fornisce le API usate dai comandi:
 *     - reloadConfigs(server)
 *     - validateAgainstRegistry(server): List<String>
 *     - addOrUpdateEnchantment(server, id, level)
 *     - getCurrentComponent(): wrapper per /plusec list
 */


object WorldConfigManager {

    private const val FILE_AVAILABLE   = "AviableEnch.config"
    private const val FILE_LVL_MAX     = "EnchLVLmax.config"
    private const val FILE_RARITY      = "EnchRarity.config"
    private const val FILE_COMPAT      = "EnchCompatibility.config"
    private const val FILE_CATEGORIES  = "EnchCategories.config"
    private const val FILE_UNCOMPAT    = "EnchUncompatibility.config"

    /** Ricarica le config del mondo (copiando i default se mancano) e aggiorna lo stato in memoria. */
    fun reloadConfigs(server: MinecraftServer) {
        ConfigManager.loadAll(server)
        EnchLogger.info("WorldConfigManager: ricaricate configurazioni da ${worldDir(server)}")
    }

    /**
     * Valida che tutti gli enchantment presenti in config siano nel registry (e viceversa segnala extra).
     * Ritorna una lista di stringhe descrittive dei problemi (vuota se ok).
     */
    fun validateAgainstRegistry(server: MinecraftServer): List<String> {
        val issues = mutableListOf<String>()

        val cfgIds: Set<String> = ConfigManager.availableEnchantments.map { it.id }.toSet()

        val reg = server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        val registryIds: Set<String> = reg.ids.map { it.toString() }.toSet()

        val missingInRegistry = cfgIds - registryIds
        val notConfigured = registryIds - cfgIds

        if (missingInRegistry.isNotEmpty()) {
            issues += "Presenti in config ma non nel registry: ${missingInRegistry.size}"
            missingInRegistry.sorted().forEach { issues += " - $it" }
        }
        if (notConfigured.isNotEmpty()) {
            issues += "Presenti nel registry ma non in config: ${notConfigured.size}"
            notConfigured.sorted().forEach { issues += " - $it" }
        }

        // Controllo bidirezionalità incompatibilità
        ConfigManager.enchantmentUncompatibility.forEach { (a, list) ->
            list.forEach { b ->
                val back = ConfigManager.enchantmentUncompatibility[b]
                if (back?.contains(a) != true) {
                    issues += "Incompatibilità non bidirezionale: $a <-> $b"
                }
            }
        }

        return issues
    }

    /**
     * Aggiunge/aggiorna un incantesimo in config:
     * - in AviableEnch.config mette id=true
     * - in EnchLVLmax.config scrive/aggiorna id=<level>
     */
    fun addOrUpdateEnchantment(server: MinecraftServer, id: String, level: Int) {
        setAvailable(server, id, true)
        setMaxLevelOverride(server, id, level)
        ConfigManager.loadAll(server)
    }

    /**
     * Espone una “vista” della configurazione corrente per /plusec list.
     * Mostra solo le voci abilitate; il livello è l’override se presente, altrimenti 1.
     */
    fun getCurrentComponent(): CurrentComponent {
        val enabled = ConfigManager.availableEnchantments
            .filter { it.sources.firstOrNull()?.equals("enabled", true) == true }
            .map { it.id }
        val levels = ConfigManager.enchantmentLevelMax

        val entries = enabled.map { id ->
            val lvl = levels[id] ?: 1
            EnchantmentEntry(EnchRef(id), lvl)
        }
        return CurrentComponent(entries)
    }

    // ===== setter mirati usati dai comandi =====

    private fun setAvailable(server: MinecraftServer, id: String, enabled: Boolean) {
        val path = worldDir(server).resolve(FILE_AVAILABLE)
        ensureFileExists(server, path, FILE_AVAILABLE)
        upsertKeyValue(path, id, if (enabled) "true" else "false", headerFor(FILE_AVAILABLE))
    }

    private fun setMaxLevelOverride(server: MinecraftServer, id: String, level: Int?) {
        val path = worldDir(server).resolve(FILE_LVL_MAX)
        ensureFileExists(server, path, FILE_LVL_MAX)
        if (level == null) {
            removeKey(path, id, headerFor(FILE_LVL_MAX))
        } else {
            upsertKeyValue(path, id, level.toString(), headerFor(FILE_LVL_MAX))
        }
    }

    // ===== util I/O su file properties =====

    private fun worldDir(server: MinecraftServer): Path =
        ConfigManager.worldConfigDir(server)

    /**
     * Se il file non esiste nel mondo, lascia che ConfigManager copi il default da resources.
     * Se ancora assente, crea un file con header minimo.
     */
    private fun ensureFileExists(server: MinecraftServer, file: Path, logicalName: String) {
        if (Files.exists(file)) return
        ConfigManager.reloadCoreFilesIfNeeded(server)
        if (!Files.exists(file)) {
            Files.createDirectories(file.parent)
            Files.writeString(file, headerFor(logicalName).joinToString("\n") + "\n", StandardCharsets.UTF_8)
        }
    }

    private fun headerFor(fileName: String): List<String> = when (fileName) {
        FILE_AVAILABLE -> listOf(
            "# EnchLib - AviableEnch.config (properties)",
            "# <id>=true|false",
            ""
        )
        FILE_LVL_MAX -> listOf(
            "# EnchLib - Max levels (id=level)",
            ""
        )
        FILE_RARITY -> listOf(
            "# EnchLib - Rarity per enchantment (id=rarity)",
            ""
        )
        FILE_COMPAT -> listOf(
            "# EnchLib - Compatibility per enchantment (id=group1,group2,...)",
            ""
        )
        FILE_CATEGORIES -> listOf(
            "# EnchLib - Categories per enchantment (id=cat1,cat2,...)",
            ""
        )
        FILE_UNCOMPAT -> listOf(
            "# EnchLib - Uncompatibility pairs (id=incompat1,incompat2,...)",
            ""
        )
        else -> listOf("# EnchLib", "")
    }

    private fun readAll(path: Path): List<String> =
        if (!Files.exists(path)) emptyList() else Files.readAllLines(path, StandardCharsets.UTF_8)

    private fun writeAll(path: Path, lines: List<String>, header: List<String>) {
        val body = lines
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .sortedBy { it.substringBefore("=").trim() }
        val content = (header + body).joinToString("\n") + "\n"
        Files.writeString(path, content, StandardCharsets.UTF_8)
    }

    private fun indexOfKey(lines: List<String>, key: String): Int {
        for (i in lines.indices) {
            val raw = lines[i].trim()
            if (raw.isEmpty() || raw.startsWith("#")) continue
            val k = raw.substringBefore("=").trim()
            if (k == key) return i
        }
        return -1
    }

    private fun upsertKeyValue(path: Path, key: String, value: String, header: List<String>) {
        val lines = readAll(path).toMutableList()
        val idx = indexOfKey(lines, key)
        val newLine = "$key=$value"
        if (idx >= 0) lines[idx] = newLine else lines.add(newLine)
        writeAll(path, lines, header)
    }

    private fun removeKey(path: Path, key: String, header: List<String>) {
        val lines = readAll(path).toMutableList()
        val idx = indexOfKey(lines, key)
        if (idx >= 0) {
            lines.removeAt(idx)
            writeAll(path, lines, header)
        }
    }

    // ===== tipi wrapper per /plusec list =====

    data class EnchRef(val idAsString: String)
    data class EnchantmentEntry(val key: EnchRef, val intValue: Int)
    class CurrentComponent(private val entries: List<EnchantmentEntry>) {
        fun getSize(): Int = entries.size
        fun getEnchantmentEntries(): List<EnchantmentEntry> = entries
    }
}