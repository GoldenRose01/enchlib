package goldenrose01.enchlib.config

import goldenrose01.enchlib.config.data.*
import goldenrose01.enchlib.utils.EnchLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registry
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class WorldConfigManager(private val server: MinecraftServer) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Rendi public la proprietà worldConfigPath
    val worldConfigPath: Path by lazy {
        val worldDir = server.runDirectory.toAbsolutePath()
            .resolve("saves")
            .resolve(server.saveProperties.levelName)
            .resolve("config")
            .resolve("enchlib")
        worldDir
    }

    private val availableEnchFile: File by lazy {
        worldConfigPath.resolve("AviableEnch.json").toFile()
    }
    private val detailsFile: File by lazy {
        worldConfigPath.resolve("EnchantmentDetails.json").toFile()
    }
    private val mobCategoriesFile: File by lazy {
        worldConfigPath.resolve("Mob_category.json").toFile()
    }
    private val incompatibilityFile: File by lazy {
        worldConfigPath.resolve("Uncompatibility.json").toFile()
    }

    // Cache per le configurazioni
    private var availableEnchCache: AvailableEnchantmentsConfig? = null
    private var detailsCache: EnchantmentDetailsConfig? = null
    private var mobCategoriesCache: MobCategoriesConfig? = null
    private var incompatibilityCache: IncompatibilityRules? = null

    companion object {
        private var instance: WorldConfigManager? = null

        fun getInstance(server: MinecraftServer): WorldConfigManager {
            if (instance?.server != server) {
                instance = WorldConfigManager(server)
            }
            return instance!!
        }

        fun hasInstance(): Boolean = instance != null

        fun clearInstance() {
            instance = null
        }
    }

    fun initializeWorldConfigs() {
        try {
            EnchLogger.info("Initializing world configurations for: $worldConfigPath")

            // Crea directory se non esiste
            if (!worldConfigPath.exists()) {
                worldConfigPath.createDirectories()
                EnchLogger.info("Created config directory: $worldConfigPath")
            }

            // Inizializza tutti i file di configurazione
            initializeAvailableEnchantments()
            initializeEnchantmentDetails()
            initializeMobCategories()
            initializeIncompatibilityRules()

            EnchLogger.info("World configurations initialized successfully")

        } catch (e: Exception) {
            EnchLogger.error("Failed to initialize world configurations", e)
        }
    }

    private fun initializeAvailableEnchantments() {
        val existingConfig = if (availableEnchFile.exists()) {
            try {
                json.decodeFromString<AvailableEnchantmentsConfig>(availableEnchFile.readText())
            } catch (e: Exception) {
                EnchLogger.error("Failed to parse existing AviableEnch.json, creating new one", e)
                AvailableEnchantmentsConfig()
            }
        } else {
            AvailableEnchantmentsConfig()
        }

        val enchantmentRegistry = server.registryManager.get(RegistryKeys.ENCHANTMENT)
        val existingIds = existingConfig.enchantments.map { it.id }.toSet()

        // FIX: Iterazione corretta sul registry
        enchantmentRegistry.forEach { enchantment ->
            val id = enchantmentRegistry.getId(enchantment).toString()
            if (id !in existingIds) {
                existingConfig.enchantments.add(
                    EnchantmentStatusEntry(
                        id = id,
                        enabled = true
                    )
                )
                EnchLogger.debug("Added new enchantment to available list: $id")
            }
        }

        availableEnchFile.writeText(json.encodeToString(existingConfig))
        availableEnchCache = existingConfig

        EnchLogger.info("Available enchantments config updated with ${existingConfig.enchantments.size} enchantments")
    }

    private fun initializeEnchantmentDetails() {
        val existingConfig = if (detailsFile.exists()) {
            try {
                json.decodeFromString<EnchantmentDetailsConfig>(detailsFile.readText())
            } catch (e: Exception) {
                EnchLogger.error("Failed to parse existing EnchantmentDetails.json, creating new one", e)
                EnchantmentDetailsConfig()
            }
        } else {
            EnchantmentDetailsConfig()
        }

        val enchantmentRegistry = server.registryManager.get(RegistryKeys.ENCHANTMENT)
        val existingIds = existingConfig.enchantments.map { it.id }.toSet()

        // Aggiungi dettagli per nuovi incantesimi - FIX: usa streamEntries()
        enchantmentRegistry.forEach { enchantment ->
            val id = enchantmentRegistry.getId(enchantment).toString()

            if (id !in existingIds) {
                val detailedEntry = createDefaultEnchantmentDetails(id, enchantment)
                existingConfig.enchantments.add(detailedEntry)
                EnchLogger.debug("Added detailed entry for enchantment: $id")
            }
        }

        detailsFile.writeText(json.encodeToString(existingConfig))
        detailsCache = existingConfig

        EnchLogger.info("Enchantment details config updated with ${existingConfig.enchantments.size} enchantments")
    }

    private fun createDefaultEnchantmentDetails(id: String, enchantment: Enchantment): DetailedEnchantmentEntry {
        val maxLevel = try {
            enchantment.maxLevel
        } catch (e: Exception) {
            EnchLogger.warn("Failed to get max level for $id, using default 1")
            1
        }

        // Carica da file precompilati o usa default
        val precompiledData = DefaultDataLoader.getEnchantmentData(id)

        val levels = mutableListOf<EnchantmentLevelData>()
        for (level in 1..maxLevel) {
            levels.add(precompiledData?.levels?.find { it.level == level }
                ?: createDefaultLevelData(id, level))
        }

        return DetailedEnchantmentEntry(
            id = id,
            name = precompiledData?.name ?: getEnchantmentDisplayName(id),
            max_level = maxLevel,
            applicable_to = precompiledData?.applicable_to ?: getApplicableItems(id),
            description = precompiledData?.description ?: getEnchantmentDescription(id),
            enc_category = precompiledData?.enc_category ?: getEnchantmentCategories(id),
            mob_category = precompiledData?.mob_category ?: getEnchantmentMobCategories(id),
            rarity = precompiledData?.rarity ?: getEnchantmentRarity(id),
            levels = levels
        )
    }

    private fun createDefaultLevelData(enchantmentId: String, level: Int): EnchantmentLevelData {
        // Logica per creare dati di default basati sul tipo di incantesimo
        return when {
            enchantmentId.contains("sharpness") -> EnchantmentLevelData(
                level = level,
                extra_damage = 0.5 * level
            )
            enchantmentId.contains("efficiency") -> EnchantmentLevelData(
                level = level,
                speed_multiplier = 1.0 + (0.3 * level)
            )
            enchantmentId.contains("smite") || enchantmentId.contains("bane") -> EnchantmentLevelData(
                level = level,
                extra_damage_vs_category = 2.5 * level
            )
            else -> EnchantmentLevelData(level = level)
        }
    }

    private fun initializeMobCategories() {
        if (!mobCategoriesFile.exists()) {
            // Carica da file precompilato
            val defaultMobCategories = DefaultDataLoader.getDefaultMobCategories()
            mobCategoriesFile.writeText(json.encodeToString(defaultMobCategories))
            mobCategoriesCache = defaultMobCategories
            EnchLogger.info("Created default mob categories configuration")
        } else {
            try {
                mobCategoriesCache = json.decodeFromString<MobCategoriesConfig>(mobCategoriesFile.readText())
                EnchLogger.info("Loaded existing mob categories configuration")
            } catch (e: Exception) {
                EnchLogger.error("Failed to parse Mob_category.json", e)
                val defaultConfig = DefaultDataLoader.getDefaultMobCategories()
                mobCategoriesFile.writeText(json.encodeToString(defaultConfig))
                mobCategoriesCache = defaultConfig
            }
        }
    }

    private fun initializeIncompatibilityRules() {
        if (!incompatibilityFile.exists()) {
            // Carica da file precompilato
            val defaultRules = DefaultDataLoader.getDefaultIncompatibilityRules()
            incompatibilityFile.writeText(json.encodeToString(defaultRules))
            incompatibilityCache = defaultRules
            EnchLogger.info("Created default incompatibility rules")
        } else {
            try {
                incompatibilityCache = json.decodeFromString<IncompatibilityRules>(incompatibilityFile.readText())
                EnchLogger.info("Loaded existing incompatibility rules")
            } catch (e: Exception) {
                EnchLogger.error("Failed to parse Uncompatibility.json", e)
                val defaultRules = DefaultDataLoader.getDefaultIncompatibilityRules()
                incompatibilityFile.writeText(json.encodeToString(defaultRules))
                incompatibilityCache = defaultRules
            }
        }
    }

    // API pubbliche per accesso alle configurazioni
    fun isEnchantmentEnabled(enchantmentId: String): Boolean {
        val config = availableEnchCache ?: return true
        return config.enchantments.find { it.id == enchantmentId }?.enabled ?: true
    }

    fun getEnchantmentDetails(enchantmentId: String): DetailedEnchantmentEntry? {
        val config = detailsCache ?: return null
        return config.enchantments.find { it.id == enchantmentId }
    }

    fun getMaxLevel(enchantmentId: String): Int {
        val details = getEnchantmentDetails(enchantmentId)
        return details?.max_level ?: run {
            try {
                val enchantmentRegistry = server.registryManager.get(RegistryKeys.ENCHANTMENT)
                val identifier = Identifier.tryParse(enchantmentId) ?: return 1
                enchantmentRegistry.get(identifier)?.maxLevel ?: 1
            } catch (e: Exception) {
                EnchLogger.warn("Failed to get max level for $enchantmentId")
                1
            }
        }
    }

    fun getMobCategories(mobName: String): List<String> {
        val config = mobCategoriesCache ?: return emptyList()
        return config.mobs.find { it.name.equals(mobName, ignoreCase = true) }?.categories ?: emptyList()
    }

    fun areEnchantmentsIncompatible(ench1: String, ench2: String): Boolean {
        val config = incompatibilityCache ?: return false
        return config.incompatible_pairs.any {
            (it.enchantment1 == ench1 && it.enchantment2 == ench2) ||
                    (it.enchantment1 == ench2 && it.enchantment2 == ench1)
        }
    }

    fun getCategoryLimit(category: String): Int {
        val config = incompatibilityCache ?: return Int.MAX_VALUE
        return config.category_limits[category] ?: Int.MAX_VALUE
    }

    // Metodi di supporto semplificati
    private fun getEnchantmentDisplayName(id: String): String {
        return try {
            val enchantmentRegistry = server.registryManager.get(RegistryKeys.ENCHANTMENT)
            val identifier = Identifier.tryParse(id) ?: return id.substringAfter(':').replaceFirstChar { it.uppercase() }
            val enchantment = enchantmentRegistry.get(identifier)
            Text.translatable(enchantment?.translationKey ?: id).string
        } catch (e: Exception) {
            id.substringAfter(':').replaceFirstChar { it.uppercase() }
        }
    }

    private fun getApplicableItems(id: String): List<String> {
        return listOf("all") // Semplificato, ora caricato da file
    }

    private fun getEnchantmentDescription(id: String): String {
        return "Auto-generated description for $id"
    }

    private fun getEnchantmentCategories(id: String): List<String> {
        return listOf("Utility") // Semplificato, ora caricato da file
    }

    private fun getEnchantmentMobCategories(id: String): List<String> {
        return listOf("all") // Semplificato, ora caricato da file
    }

    private fun getEnchantmentRarity(id: String): String {
        return "common"
    }

    fun reloadConfigurations() {
        availableEnchCache = null
        detailsCache = null
        mobCategoriesCache = null
        incompatibilityCache = null
        initializeWorldConfigs()
    }
}

