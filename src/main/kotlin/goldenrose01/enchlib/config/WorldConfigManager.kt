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

    private val worldConfigPath: Path by lazy {
        server.getSavePath(server.getWorldSaveHandler().worldDir).resolve("config/enchlib")
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
            EnchLogger.info("Initializing world configurations for: ${server.getSavePath(server.getWorldSaveHandler().worldDir)}")

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

        // Aggiungi nuovi incantesimi non presenti
        enchantmentRegistry.ids.forEach { id ->
            val idString = id.toString()
            if (idString !in existingIds) {
                existingConfig.enchantments.add(
                    EnchantmentStatusEntry(
                        id = idString,
                        enabled = true // Default abilitato
                    )
                )
                EnchLogger.debug("Added new enchantment to available list: $idString")
            }
        }

        // Salva la configurazione aggiornata
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

        // Aggiungi dettagli per nuovi incantesimi
        enchantmentRegistry.entrySet.forEach { entry ->
            val id = entry.key.value.toString()
            val enchantment = entry.value

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

        // Genera livelli di default basati sul tipo di incantesimo
        val levels = mutableListOf<EnchantmentLevelData>()
        for (level in 1..maxLevel) {
            levels.add(createDefaultLevelData(id, level))
        }

        return DetailedEnchantmentEntry(
            id = id,
            name = getEnchantmentDisplayName(id),
            max_level = maxLevel,
            applicable_to = getApplicableItems(id),
            description = getEnchantmentDescription(id),
            enc_category = getEnchantmentCategories(id),
            mob_category = getMobCategories(id),
            rarity = getEnchantmentRarity(id),
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
            val defaultMobCategories = createDefaultMobCategories()
            mobCategoriesFile.writeText(json.encodeToString(defaultMobCategories))
            mobCategoriesCache = defaultMobCategories
            EnchLogger.info("Created default mob categories configuration")
        } else {
            try {
                mobCategoriesCache = json.decodeFromString<MobCategoriesConfig>(mobCategoriesFile.readText())
                EnchLogger.info("Loaded existing mob categories configuration")
            } catch (e: Exception) {
                EnchLogger.error("Failed to parse Mob_category.json", e)
                val defaultConfig = createDefaultMobCategories()
                mobCategoriesFile.writeText(json.encodeToString(defaultConfig))
                mobCategoriesCache = defaultConfig
            }
        }
    }

    private fun initializeIncompatibilityRules() {
        if (!incompatibilityFile.exists()) {
            val defaultRules = createDefaultIncompatibilityRules()
            incompatibilityFile.writeText(json.encodeToString(defaultRules))
            incompatibilityCache = defaultRules
            EnchLogger.info("Created default incompatibility rules")
        } else {
            try {
                incompatibilityCache = json.decodeFromString<IncompatibilityRules>(incompatibilityFile.readText())
                EnchLogger.info("Loaded existing incompatibility rules")
            } catch (e: Exception) {
                EnchLogger.error("Failed to parse Uncompatibility.json", e)
                val defaultRules = createDefaultIncompatibilityRules()
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
            // Fallback al registry
            try {
                val enchantmentRegistry = server.registryManager.get(RegistryKeys.ENCHANTMENT)
                val identifier = Identifier.tryParse(enchantmentId) ?: return 1
                enchantmentRegistry.get(identifier)?.maxLevel ?: 1
            } catch (e: Exception) {
                EnchLogger.warn("Failed to get max level for $enchantmentId", e)
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

    // Metodi di supporto per la creazione dei default
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
        return when {
            id.contains("sharpness") || id.contains("smite") || id.contains("bane") ->
                listOf("sword", "axe")
            id.contains("efficiency") || id.contains("fortune") || id.contains("silk_touch") ->
                listOf("pickaxe", "axe", "shovel", "hoe")
            id.contains("protection") || id.contains("fire_protection") ->
                listOf("helmet", "chestplate", "leggings", "boots")
            id.contains("power") || id.contains("punch") || id.contains("flame") ->
                listOf("bow")
            else -> listOf("all")
        }
    }

    private fun getEnchantmentDescription(id: String): String {
        return "Auto-generated description for $id"
    }

    private fun getEnchantmentCategories(id: String): List<String> {
        return when {
            id.contains("sharpness") || id.contains("smite") || id.contains("bane") ->
                listOf("Damage", "Combat")
            id.contains("efficiency") || id.contains("fortune") || id.contains("silk_touch") ->
                listOf("Mining")
            id.contains("protection") -> listOf("Protection", "Defense")
            id.contains("power") || id.contains("punch") -> listOf("Ranged", "Combat")
            else -> listOf("Utility")
        }
    }

    private fun getMobCategories(id: String): List<String> {
        return when {
            id.contains("smite") -> listOf("undead")
            id.contains("bane_of_arthropods") -> listOf("arthropods")
            else -> listOf("all")
        }
    }

    private fun getEnchantmentRarity(id: String): String {
        return "common" // Default, può essere espanso con logica più complessa
    }

    // Ricarica configurazioni
    fun reloadConfigurations() {
        availableEnchCache = null
        detailsCache = null
        mobCategoriesCache = null
        incompatibilityCache = null

        initializeWorldConfigs()
    }
}

// Aggiungere dentro WorldConfigManager

private fun createDefaultMobCategories(): MobCategoriesConfig {
    val mobs = mutableListOf<MobEntry>()

    // Dati forniti dall'utente
    val defaultMobs = listOf(
        MobEntry("Allay", "Passive", listOf("magik", "flying")),
        MobEntry("Armadillo", "Passive", listOf("animals", "cubic")),
        MobEntry("Axolotl", "Pet", listOf("animals", "water", "arthropods")),
        MobEntry("Bat", "Passive", listOf("animals", "flying")),
        MobEntry("Bee", "Neutral", listOf("flying")),
        MobEntry("Blaze", "Hostile", listOf("magik", "hell", "flying")),
        MobEntry("Bogged", "Hostile", listOf("undead", "fungi")),
        MobEntry("Breeze", "Hostile", listOf("magik", "flying")),
        MobEntry("Camel", "Passive", listOf("animals")),
        MobEntry("Cat", "Pet", listOf("animals")),
        MobEntry("Cave spider", "H-Neutral", listOf("arthropods")),
        MobEntry("Chicken", "Passive", listOf("animals")),
        MobEntry("Cod", "Passive", listOf("water")),
        MobEntry("Copper Golem", "Golem", listOf("magik", "cubic")),
        MobEntry("Cow", "Passive", listOf("animals")),
        MobEntry("Creeper", "Hostile", listOf("fungi", "arthropods")),
        MobEntry("Dog", "Pet", listOf("animals")),
        MobEntry("Dolphin", "Neutral", listOf("animals", "water")),
        MobEntry("Donkey", "Passive", listOf("animals")),
        MobEntry("Drowned", "H-Neutral", listOf("undead", "water")),
        MobEntry("Elder Guardian", "Hostile", listOf("magik", "water")),
        MobEntry("Ender Dragon", "Hostile", listOf("magik", "void", "arthropods", "flying")),
        MobEntry("Enderman", "Neutral", listOf("void")),
        MobEntry("Endermite", "Hostile", listOf("void")),
        MobEntry("Evoker", "Hostile", listOf("magik", "rebel")),
        MobEntry("Fox", "Passive", listOf("animals")),
        MobEntry("Frog", "Passive", listOf("animals")),
        MobEntry("Ghastling", "Pet", listOf("magik", "hell", "arthropods", "cubic", "flying")),
        MobEntry("Ghast", "Hostile", listOf("undead", "hell", "arthropods", "cubic", "flying")),
        MobEntry("Glow Squid", "Passive", listOf("animals", "magik", "water", "arthropods")),
        MobEntry("Goat", "R-Neutral", listOf("animals")),
        MobEntry("Guardian", "Hostile", listOf("water")),
        MobEntry("Hoglin", "Hostile", listOf("animals")),
        MobEntry("Horse", "Passive", listOf("animals")),
        MobEntry("Husk", "Hostile", listOf("undead")),
        MobEntry("Illusioner", "Hostile", listOf("magik", "rebel", "arthropods")),
        MobEntry("Iron Golem", "Golem", listOf("cubic")),
        MobEntry("Llama", "Neutral", listOf("animals")),
        MobEntry("Magma Cube", "Hostile", listOf("hell", "cubic")),
        MobEntry("Mooshroom", "Passive", listOf("animals")),
        MobEntry("Mule", "Passive", listOf("animals")),
        MobEntry("Ocelot", "Passive", listOf("animals")),
        MobEntry("Panda", "Neutral", listOf("animals")),
        MobEntry("Parrot", "Passive", listOf("animals", "flying")),
        MobEntry("Phantom", "Hostile", listOf("undead", "flying")),
        MobEntry("Pig", "Passive", listOf("animals")),
        MobEntry("Piglin", "H-Neutral", listOf("hell")),
        MobEntry("Piglin Brute", "Hostile", listOf("rebel", "hell")),
        MobEntry("Pillager", "Hostile", listOf("rebel")),
        MobEntry("Polar Bear", "Neutral", listOf("animals", "water")),
        MobEntry("Pufferfish", "Passive", listOf("animals", "water")),
        MobEntry("Rabbit", "Passive", listOf("animals")),
        MobEntry("Ravanger", "Hostile", listOf("rebel")),
        MobEntry("Salmon", "Passive", listOf("animals", "water")),
        MobEntry("Sheep", "Passive", listOf("animals")),
        MobEntry("Shulker", "Hostile", listOf("magik", "cubic", "flying")),
        MobEntry("Silverfish", "Hostile", listOf("arthropods")),
        MobEntry("Skeleton", "Hostile", listOf("undead")),
        MobEntry("Skeleton Horse", "Passive", listOf("magik", "undead")),
        MobEntry("Slime", "Hostile", listOf("water", "cubic")),
        MobEntry("Sniffer", "Passive", listOf("animals", "arthropods", "cubic")),
        MobEntry("Snow Golem", "Golem", listOf("cubic")),
        MobEntry("Spider", "H-Neutral", listOf("arthropods")),
        MobEntry("Squid", "Passive", listOf("animals", "water", "arthropods")),
        MobEntry("Stray", "Hostile", listOf("undead")),
        MobEntry("Strider", "Passive", listOf("undead", "hell")),
        MobEntry("Tadpole", "Passive", listOf("animals", "water")),
        MobEntry("Turtle", "Passive", listOf("animals", "water", "flying")),
        MobEntry("Vex", "Hostile", listOf("magik", "undead", "rebel", "flying")),
        MobEntry("Villager", "NPC", listOf()),
        MobEntry("Vindicator", "Hostile", listOf("rebel")),
        MobEntry("Wandering Trader", "NPC", listOf("magik")),
        MobEntry("Warden", "Hostile", listOf("magik", "fungi")),
        MobEntry("Witch", "Hostile", listOf("magik")),
        MobEntry("Wither Boss", "Hostile", listOf("magik", "hell", "void")),
        MobEntry("Wither Skeleton", "Hostile", listOf("undead", "hell")),
        MobEntry("Wolf", "Neutral", listOf("animals")),
        MobEntry("Zoglin", "Hostile", listOf("undead", "hell")),
        MobEntry("Zombie", "Hostile", listOf("undead")),
        MobEntry("Zombie Horse", "Hostile", listOf("undead")),
        MobEntry("Zombified Piglin", "Neutral", listOf("undead", "hell")),
        MobEntry("Zombified villager", "Hostile", listOf("undead"))
    )

    mobs.addAll(defaultMobs)
    return MobCategoriesConfig(mobs)
}

private fun createDefaultIncompatibilityRules(): IncompatibilityRules {
    val incompatiblePairs = mutableListOf<IncompatiblePair>()
    val categoryLimits = mutableMapOf<String, Int>()

    // Incompatibilità classiche di Minecraft
    incompatiblePairs.addAll(listOf(
        IncompatiblePair("minecraft:fortune", "minecraft:silk_touch"),
        IncompatiblePair("minecraft:depth_strider", "minecraft:frost_walker"),
        IncompatiblePair("minecraft:loyalty", "minecraft:riptide"),
        IncompatiblePair("minecraft:channeling", "minecraft:riptide"),
        IncompatiblePair("minecraft:multishot", "minecraft:piercing")
    ))

    // Limiti per categoria
    categoryLimits["Damage"] = 1
    categoryLimits["Protection"] = 4
    categoryLimits["Combat"] = 3
    categoryLimits["Mining"] = 3

    return IncompatibilityRules(incompatiblePairs, categoryLimits)
}

