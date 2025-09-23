package goldenrose01.enchlib.config

import goldenrose01.enchlib.utils.EnchLogger
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.charset.StandardCharsets

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
    /**
     * Posizione della cartella di configurazione. Tutti i file `.config` devono
     * trovarsi in questa cartella. Di default è `config/enchlib`.
     */
    private val configDir: Path = Paths.get("config", "enchlib")

    // Data class per ogni categoria di config
    data class AviableEnchantment(val id: String, val sources: List<String>)
    data class EnchMultiplier(val level: Int, val primary: Double, val secondary: Double)
    data class EnchLevelMax(val id: String, val maxLevel: Int)
    data class EnchCompatibility(val id: String, val toolGroups: List<String>)
    data class MobCategory(val mobId: String, val categories: List<String>)
    data class EnchCategory(val id: String, val categories: List<String>)
    data class EnchUncompatibility(val enchantment: String, val incompatibleWith: List<String>)
    data class EnchRarity(val id: String, val rarity: String)
    data class Rarity(val rarity: String, val dropChance: Double, val weight: Int, val specialMethods: List<String>)

    // Mappe popolate dopo loadAll()
    var availableEnchantments: List<AviableEnchantment> = emptyList()
        private set
    var enchantmentMultipliers: Map<String, List<EnchMultiplier>> = emptyMap()
        private set
    var enchantmentLevelMax: Map<String, Int> = emptyMap()
        private set
    var enchantmentCompatibility: Map<String, List<String>> = emptyMap()
        private set
    var mobCategories: Map<String, List<String>> = emptyMap()
        private set
    var enchantmentCategories: Map<String, List<String>> = emptyMap()
        private set
    var enchantmentUncompatibility: Map<String, List<String>> = emptyMap()
        private set
    var enchantmentRarity: Map<String, String> = emptyMap()
        private set
    var rarities: Map<String, Rarity> = emptyMap()
        private set

    /**
     * Carica tutti i file di configurazione da disco. Va chiamato durante
     * l’inizializzazione della mod.
     */
    fun loadAll() {
        availableEnchantments = loadAvailableEnchantments()
        enchantmentMultipliers = loadEnchMultipliers()
        enchantmentLevelMax = loadEnchLevelMax()
        enchantmentCompatibility = loadEnchCompatibility()
        mobCategories = loadMobCategories()
        enchantmentCategories = loadEnchCategories()
        enchantmentUncompatibility = loadEnchUncompatibility()
        enchantmentRarity = loadEnchRarity()
        rarities = loadRarities()
    }

    /**
     * Indica se l’incantesimo specificato è abilitato.
     */
    fun isEnchantmentEnabled(enchantId: String): Boolean {
        return availableEnchantments.any { it.id == enchantId }
    }

    /**
     * Restituisce il livello massimo consentito per l’incantesimo specificato.
     * Se non è definito in `EnchLVLmax.config`, ritorna 1.
     */
    fun getMaxLevel(enchantId: String): Int {
        return enchantmentLevelMax[enchantId] ?: 1
    }

    /**
     * Carica AviableEnch.config. Formato: enchantmentId=sources1,sources2
     */
    private fun loadAvailableEnchantments(): List<AviableEnchantment> {
        val file = configDir.resolve("AviableEnch.config")
        val result = mutableListOf<AviableEnchantment>()
        if (!Files.exists(file)) {
            EnchLogger.warn("Missing AviableEnch.config, using empty list")
            return result
        }
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = trimmed.split("=")
                if (parts.size != 2) {
                    EnchLogger.warn("Invalid line in AviableEnch.config: '$line'")
                    return@forEachLine
                }
                val id = parts[0].trim()
                val sources = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (id.isNotEmpty()) {
                    result.add(AviableEnchantment(id, sources))
                }
            }
        }
        return result
    }

    /**
     * Carica EnchMultiplierCSV.config. Formato CSV:
     * enchantmentId, level, primaryMultiplier, secondaryMultiplier
     */
    private fun loadEnchMultipliers(): Map<String, List<EnchMultiplier>> {
        val file = configDir.resolve("EnchMultiplierCSV.config")
        val map = mutableMapOf<String, MutableList<EnchMultiplier>>()
        if (!Files.exists(file)) {
            EnchLogger.warn("Missing EnchMultiplierCSV.config, using empty map")
            return map
        }
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = trimmed.split(",")
                if (parts.size != 4) {
                    EnchLogger.warn("Invalid CSV line in EnchMultiplierCSV.config: '$line'")
                    return@forEachLine
                }
                val id = parts[0].trim()
                val level = parts[1].trim().toIntOrNull()
                val primary = parts[2].trim().toDoubleOrNull()
                val secondary = parts[3].trim().toDoubleOrNull()
                if (id.isNotEmpty() && level != null && primary != null && secondary != null) {
                    val list = map.getOrPut(id) { mutableListOf() }
                    list.add(EnchMultiplier(level, primary, secondary))
                } else {
                    EnchLogger.warn("Invalid values in EnchMultiplierCSV.config: '$line'")
                }
            }
        }
        return map
    }

    /**
     * Carica EnchLVLmax.config. Formato: enchantmentId=maxLevel
     */
    private fun loadEnchLevelMax(): Map<String, Int> {
        val file = configDir.resolve("EnchLVLmax.config")
        val map = mutableMapOf<String, Int>()
        if (!Files.exists(file)) {
            EnchLogger.warn("Missing EnchLVLmax.config, using empty map")
            return map
        }
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = trimmed.split("=")
                if (parts.size != 2) {
                    EnchLogger.warn("Invalid line in EnchLVLmax.config: '$line'")
                    return@forEachLine
                }
                val id = parts[0].trim()
                val level = parts[1].trim().toIntOrNull()
                if (id.isNotEmpty() && level != null) {
                    map[id] = level
                } else {
                    EnchLogger.warn("Invalid values in EnchLVLmax.config: '$line'")
                }
            }
        }
        return map
    }

    /**
     * Carica EnchCompatibility.config. Formato: enchantmentId=group1,group2
     */
    private fun loadEnchCompatibility(): Map<String, List<String>> {
        val file = configDir.resolve("EnchCompatibility.config")
        val map = mutableMapOf<String, MutableList<String>>()
        if (!Files.exists(file)) {
            EnchLogger.warn("Missing EnchCompatibility.config, using empty map")
            return map
        }
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = trimmed.split("=")
                if (parts.size != 2) {
                    EnchLogger.warn("Invalid line in EnchCompatibility.config: '$line'")
                    return@forEachLine
                }
                val id = parts[0].trim()
                val groups = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (id.isNotEmpty()) {
                    map[id] = groups.toMutableList()
                }
            }
        }
        return map
    }

    /**
     * Carica MobCategory.config. Formato: mobId=category1,category2
     */
    private fun loadMobCategories(): Map<String, List<String>> {
        val file = configDir.resolve("MobCategory.config")
        val map = mutableMapOf<String, MutableList<String>>()
        if (!Files.exists(file)) {
            EnchLogger.warn("Missing MobCategory.config, using empty map")
            return map
        }
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = trimmed.split("=")
                if (parts.size != 2) {
                    EnchLogger.warn("Invalid line in MobCategory.config: '$line'")
                    return@forEachLine
                }
                val mobId = parts[0].trim()
                val categories = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (mobId.isNotEmpty()) {
                    map[mobId] = categories.toMutableList()
                }
            }
        }
        return map
    }

    /**
     * Carica EnchCategory.config. Formato: enchantmentId=category1,category2
     */
    private fun loadEnchCategories(): Map<String, List<String>> {
        val file = configDir.resolve("EnchCategory.config")
        val map = mutableMapOf<String, MutableList<String>>()
        if (!Files.exists(file)) {
            EnchLogger.warn("Missing EnchCategory.config, using empty map")
            return map
        }
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = trimmed.split("=")
                if (parts.size != 2) {
                    EnchLogger.warn("Invalid line in EnchCategory.config: '$line'")
                    return@forEachLine
                }
                val id = parts[0].trim()
                val categories = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (id.isNotEmpty()) {
                    map[id] = categories.toMutableList()
                }
            }
        }
        return map
    }

    /**
     * Carica EnchUNcompatibility.config. Formato: enchantmentId=incompatible1,incompatible2
     */
    private fun loadEnchUncompatibility(): Map<String, List<String>> {
        val file = configDir.resolve("EnchUNcompatibility.config")
        val map = mutableMapOf<String, MutableList<String>>()
        if (!Files.exists(file)) {
            EnchLogger.warn("Missing EnchUNcompatibility.config, using empty map")
            return map
        }
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = trimmed.split("=")
                if (parts.size != 2) {
                    EnchLogger.warn("Invalid line in EnchUNcompatibility.config: '$line'")
                    return@forEachLine
                }
                val id = parts[0].trim()
                val incompatibleWith = parts[1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (id.isNotEmpty()) {
                    map[id] = incompatibleWith.toMutableList()
                }
            }
        }
        return map
    }

    /**
     * Carica EnchRarity.config. Formato: enchantmentId=rarity
     */
    private fun loadEnchRarity(): Map<String, String> {
        val file = configDir.resolve("EnchRarity.config")
        val map = mutableMapOf<String, String>()
        if (!Files.exists(file)) {
            EnchLogger.warn("Missing EnchRarity.config, using empty map")
            return map
        }
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = trimmed.split("=")
                if (parts.size != 2) {
                    EnchLogger.warn("Invalid line in EnchRarity.config: '$line'")
                    return@forEachLine
                }
                val id = parts[0].trim()
                val rarity = parts[1].trim()
                if (id.isNotEmpty() && rarity.isNotEmpty()) {
                    map[id] = rarity
                } else {
                    EnchLogger.warn("Invalid values in EnchRarity.config: '$line'")
                }
            }
        }
        return map
    }

    /**
     * Carica Rarity.config. Formato:
     * rarity=dropChance,weight,method1|method2
     */
    private fun loadRarities(): Map<String, Rarity> {
        val file = configDir.resolve("Rarity.config")
        val map = mutableMapOf<String, Rarity>()
        if (!Files.exists(file)) {
            EnchLogger.warn("Missing Rarity.config, using empty map")
            return map
        }
        Files.newBufferedReader(file, StandardCharsets.UTF_8).use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                val parts = trimmed.split("=")
                if (parts.size != 2) {
                    EnchLogger.warn("Invalid line in Rarity.config: '$line'")
                    return@forEachLine
                }
                val rarityId = parts[0].trim()
                val values = parts[1].split(",")
                if (values.size < 3) {
                    EnchLogger.warn("Invalid values in Rarity.config: '$line'")
                    return@forEachLine
                }
                val dropChance = values[0].trim().toDoubleOrNull()
                val weight = values[1].trim().toIntOrNull()
                val specialMethods = values[2].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (dropChance != null && weight != null) {
                    map[rarityId] = Rarity(rarityId, dropChance, weight, specialMethods)
                } else {
                    EnchLogger.warn("Invalid numeric values in Rarity.config: '$line'")
                }
            }
        }
        return map
    }
}
