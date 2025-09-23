package goldenrose01.enchlib.config

import goldenrose01.enchlib.EnchLib
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.BufferedReader
import java.io.FileReader
import java.util.Properties

object ConfigManager {
    private val configDir = FabricLoader.getInstance().configDir
    private val aviableEnchConfig = Properties()
    private val enchLevelMaxConfig = Properties()
    private val enchCompatibilityConfig = Properties()
    private val mobCategoryConfig = Properties()
    private val enchCategoryConfig = Properties()
    private val enchUncompatibilityConfig = Properties()
    private val enchRarityConfig = Properties()
    private val rarityConfig = Properties()

    private val enchMultipliers = mutableMapOf<String, MutableMap<Int, Double>>()

    private val enchantmentCategories = mutableMapOf<String, MutableSet<String>>()
    private val categoryIncompatibilities = mutableMapOf<String, MutableSet<String>>()
    private val categoryLimits = mutableMapOf<String, Int>()

    fun initialize() {
        createConfigDirectory()
        loadAllConfigurations()
        buildCaches()
    }

    private fun createConfigDirectory() {
        val dir = configDir.toFile()
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    // Load all configs
    private fun loadAllConfigurations() {
        loadAvailableEnchConfig()
        loadEnchLevelMaxConfig()
        loadEnchCompatibilityConfig()
        loadMobCategoryConfig()
        loadEnchCategoryConfig()
        loadEnchUncompatibilityConfig()
        loadEnchRarityConfig()
        loadRarityConfig()
        loadEnchMultiplierConfig()
    }

    private fun loadAvailableEnchConfig() {
        val file = File(configDir.toFile(), "AviableEnch.config")
        if (!file.exists()) {
            createDefaultAvailableEnchConfig(file)
        }
        FileInputStream(file).use { aviableEnchConfig.load(it) }
    }

    private fun createDefaultAvailableEnchConfig(file: File) {
        val defaultConfig = Properties()
        defaultConfig.setProperty("enchantment.minecraft.sharpness.enabled", "true")
        defaultConfig.setProperty("enchantment.minecraft.sharpness.sources", "enchanting_table,chest_loot,fishing")
        FileOutputStream(file).use { defaultConfig.store(it, "Configurazione enchantments disponibili e fonti") }
    }

    private fun loadEnchLevelMaxConfig() {
        val file = File(configDir.toFile(), "EnchLVLmax.config")
        if (!file.exists()) {
            createDefaultLevelMaxConfig(file)
        }
        FileInputStream(file).use { enchLevelMaxConfig.load(it) }
    }

    private fun createDefaultLevelMaxConfig(file: File) {
        val defaultConfig = Properties()
        defaultConfig.setProperty("enchantment.minecraft.sharpness.max_level", "10")
        defaultConfig.setProperty("enchantment.minecraft.protection.max_level", "8")
        FileOutputStream(file).use { defaultConfig.store(it, "Configurazione livelli massimi degli enchantments") }
    }

    private fun loadEnchCompatibilityConfig() {
        val file = File(configDir.toFile(), "EnchCompatibility.config")
        if (!file.exists()) {
            createDefaultCompatibilityConfig(file)
        }
        FileInputStream(file).use { enchCompatibilityConfig.load(it) }
    }

    private fun createDefaultCompatibilityConfig(file: File) {
        val defaultConfig = Properties()
        // simplified example group and compatible_items definitions
        defaultConfig.setProperty("enchantment.minecraft.sharpness.compatible_items", "swords,axes")
        defaultConfig.setProperty("item_group.swords", "minecraft:wooden_sword,minecraft:stone_sword,minecraft:iron_sword,minecraft:golden_sword,minecraft:diamond_sword,minecraft:netherite_sword")
        FileOutputStream(file).use { defaultConfig.store(it, "Configurazione compatibilità enchantments con strumenti e gruppi") }
    }

    fun isEnchantmentEnabled(enchantId: String): Boolean =
        aviableEnchConfig.getProperty("$enchantId.enabled", "false").toBoolean()

    // More loader functions and business logic methods would continue here to fully match previous Java examples
    // ...
}
