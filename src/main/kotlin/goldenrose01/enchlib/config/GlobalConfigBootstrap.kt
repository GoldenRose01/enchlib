package goldenrose01.enchlib.config

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Installs defaults in each world's `config/enchlib`; migrates the former global settings once. */
object GlobalConfigBootstrap {
    private val legacyDir: Path = FabricLoader.getInstance().configDir.resolve("enchlib")
    private val migrationMarker = legacyDir.resolve("world-config-migration-v1.done")
    private val dataFiles = listOf("AviableEnch.json5", "EnchantmentsDetails.json5")
    val presetFiles = listOf(
        "AviableEnch.config", "EnchLVLmax.config", "EnchRarity.config", "EnchCompatibility.config",
        "EnchCategory.config", "EnchMultiplier.config", "EnchUNcompatibility.config", "Rarity.config",
        "MobCategory.config", "MobCategory.defaults.config"
    )

    fun ensureDefaultsInstalled(server: MinecraftServer) {
        val worldDir = server.getWorldPath(LevelResource.ROOT).resolve("config").resolve("enchlib")
        Files.createDirectories(worldDir)
        val shouldMigrate = !Files.exists(migrationMarker)
        (dataFiles + presetFiles).forEach { name ->
            val destination = worldDir.resolve(name)
            if (Files.exists(destination)) return@forEach
            val legacy = legacyDir.resolve(name)
            if (shouldMigrate && Files.exists(legacy)) {
                Files.copy(legacy, destination, StandardCopyOption.REPLACE_EXISTING)
            } else {
                val resourceName = if (name == "MobCategory.defaults.config") "MobCategory.config" else name
                copyBundled("config/$resourceName", destination)
            }
        }
        if (shouldMigrate) {
            Files.createDirectories(legacyDir)
            Files.writeString(migrationMarker, "Migrated EnchLib settings to a world config directory.\n")
        }
    }

    private fun copyBundled(resourcePath: String, destination: Path) {
        if (Files.exists(destination)) return
        val input = GlobalConfigBootstrap::class.java.classLoader.getResourceAsStream(resourcePath)
        if (input != null) input.use {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING)
        } else {
            val fallback = when (destination.fileName.toString()) {
                "AviableEnch.json5" -> "[]\n"
                "EnchantmentsDetails.json5" -> "{\"enchantments\": []}\n"
                else -> "# Per-world EnchLib preset file.\n"
            }
            Files.writeString(destination, fallback)
        }
    }
}
