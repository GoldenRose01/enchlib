package goldenrose01.enchlib.config

import goldenrose01.enchlib.config.data.*
import goldenrose01.enchlib.utils.EnchLogger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText

class WorldConfigManager(private val server: MinecraftServer) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private var instance: WorldConfigManager? = null

        fun getInstance(server: MinecraftServer): WorldConfigManager {
            if (instance?.server != server) {
                instance = WorldConfigManager(server)
            }
            return instance!!
        }
    }

    // Config folder path inside the current world save (config/enchlib/)
    private val configFolder: Path by lazy {
        val session = server.session.directoryName
        Path.of("saves", session, "config", "enchlib")
    }

    private val availableEnchFile get() = configFolder.resolve("AviableEnch.json").toFile()
    private val enchantmentDetailsFile get() = configFolder.resolve("EnchantmentDetails.json").toFile()
    private val incompatibilityFile get() = configFolder.resolve("Uncompatibility.json").toFile()
    private val mobCategoriesFile get() = configFolder.resolve("Mob_category.json").toFile()

    // In-memory caches (optional)
    private var availableEnchantments: AvailableEnchantmentsConfig? = null
    private var enchantmentDetails: EnchantmentDetailsConfig? = null
    private var incompatibilities: IncompatibilityRules? = null
    private var mobCategories: MobCategoriesConfig? = null

    fun initializeWorldConfigs() {
        try {
            if (!configFolder.exists()) {
                Files.createDirectories(configFolder)
                EnchLogger.info("Created config folder at $configFolder")
            }

            loadOrCreateAvailableEnchantments()
            loadOrCreateEnchantmentDetails()
            loadOrCreateMobCategories()
            loadOrCreateIncompatibility()

            EnchLogger.info("World configurations initialized successfully in $configFolder")
        } catch (ex: Exception) {
            EnchLogger.error("Failed to initialize world configs", ex)
        }
    }

    private fun loadOrCreateAvailableEnchantments() {
        // Read existing or create new with all registry enchantments active true
        val existing = if (availableEnchFile.exists()) {
            json.decodeFromString<AvailableEnchantmentsConfig>(availableEnchFile.readText())
        } else {
            AvailableEnchantmentsConfig(emptyList())
        }

        val reg = server.registryManager.get(RegistryKeys.ENCHANTMENT)
        val regIds = reg.entrySet.map { it.key.value.toString() }.toSet()
        val existingIds = existing.enchantments.map { it.id }.toSet()

        // Aggiungi eventuali incantesimi nuovi al file senza cancellare la configurazione utente
        val newEntries = regIds
            .filter { it !in existingIds }
            .map { EnchantmentStatusEntry(it, enabled = true) }

        val merged = existing.enchantments + newEntries

        availableEnchantments = AvailableEnchantmentsConfig(merged)
        availableEnchFile.writeText(json.encodeToString(availableEnchantments!!))
    }

    private fun loadOrCreateEnchantmentDetails() {
        val existing = if (enchantmentDetailsFile.exists()) {
            json.decodeFromString<EnchantmentDetailsConfig>(enchantmentDetailsFile.readText())
        } else {
            EnchantmentDetailsConfig(emptyList())
        }

        val reg = server.registryManager.get(RegistryKeys.ENCHANTMENT)
        val regIds = reg.entrySet.map { it.key.value.toString() }.toSet()
        val existingIds = existing.enchantments.map { it.id }.toSet()

        val newDetails = reg.entrySet.filter { it.key.value.toString() !in existingIds }
            .map {
                createEnchantmentDetailsFromRegistryEntry(it.key.value.toString(), it.value)
            }

        val merged = existing.enchantments + newDetails

        enchantmentDetails = EnchantmentDetailsConfig(merged)
        enchantmentDetailsFile.writeText(json.encodeToString(enchantmentDetails!!))
    }

    // Crea un dettaglio minimale da un enchantment di registry (puoi estendere per info più ricche)
    private fun createEnchantmentDetailsFromRegistryEntry(id: String, enchantment: Enchantment): DetailedEnchantmentEntry {
        val maxLevel = enchantment.maxLevel
        val levels = (1..maxLevel).map { level ->
            EnchantmentLevelData(level = level) // senza moltiplicatori per default, puoi estendere
        }
        return DetailedEnchantmentEntry(
            id = id,
            name = getTranslationKeyName(enchantment.translationKey),
            max_level = maxLevel,
            applicable_to = emptyList(), // da estendere con dati custom in futuro
            description = "",
            enc_category = emptyList(),
            mob_category = emptyList(),
            rarity = "common",
            levels = levels
        )
    }

    private fun getTranslationKeyName(translationKey: String): String {
        // Usa una traduzione raw o fallback
        return translationKey.substringAfterLast('.').replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun loadOrCreateMobCategories() {
        if (mobCategoriesFile.exists()) {
            mobCategories = json.decodeFromString<MobCategoriesConfig>(mobCategoriesFile.readText())
        } else {
            // crea vuoto o usa il dump di default in resources/dumpench/Mob_category.json (leggi e scrivi)
            mobCategories = MobCategoriesConfig(emptyList())
            mobCategoriesFile.writeText(json.encodeToString(mobCategories!!))
        }
    }

    private fun loadOrCreateIncompatibility() {
        if (incompatibilityFile.exists()) {
            incompatibilities = json.decodeFromString<IncompatibilityRules>(incompatibilityFile.readText())
        } else {
            incompatibilities = IncompatibilityRules(emptyList(), emptyMap())
            incompatibilityFile.writeText(json.encodeToString(incompatibilities!!))
        }
    }

    // API di lettura configurazione runtime (esempio)
    fun isEnchantmentEnabled(id: String): Boolean {
        return availableEnchantments?.enchantments?.find { it.id == id }?.enabled ?: true
    }

    fun getMaxLevel(id: String): Int {
        return enchantmentDetails?.enchantments?.find { it.id == id }?.max_level ?: 1
    }

    // Utility getter per dati
    fun getEnchantmentDetail(id: String): DetailedEnchantmentEntry? {
        return enchantmentDetails?.enchantments?.find { it.id == id }
    }
}
