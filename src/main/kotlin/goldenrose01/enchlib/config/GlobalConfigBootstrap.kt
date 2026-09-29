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
    private val configFiles = listOf("AviableEnch.json5", "EnchantmentsDetails.json5")

    fun ensureDefaultsInstalled(server: MinecraftServer) {
        val worldDir = server.getWorldPath(LevelResource.ROOT).resolve("config").resolve("enchlib")
        Files.createDirectories(worldDir)
        val shouldMigrate = !Files.exists(migrationMarker)
        configFiles.forEach { name ->
            val destination = worldDir.resolve(name)
            if (Files.exists(destination)) return@forEach
            val legacy = legacyDir.resolve(name)
            if (shouldMigrate && Files.exists(legacy)) {
                Files.copy(legacy, destination, StandardCopyOption.REPLACE_EXISTING)
            } else {
                copyBundled("config/$name", destination)
            }
        }
        if (shouldMigrate) {
            Files.createDirectories(legacyDir)
            Files.writeString(migrationMarker, "Migrated EnchLib settings to a world config directory.\n")
        }
    }

    private fun copyBundled(resourcePath: String, destination: Path) {
        if (Files.exists(destination)) return
        GlobalConfigBootstrap::class.java.classLoader.getResourceAsStream(resourcePath)?.use { input ->
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING)
        } ?: Files.createFile(destination)
    }
}
