package goldenrose01.enchlib.config

import net.fabricmc.loader.api.FabricLoader
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object GlobalConfigBootstrap {

    private val targetDir: Path = FabricLoader.getInstance().configDir.resolve("enchlib")

    /**
     * Chiama questa in Enchlib.onInitialize().
     * Copia i template se non esistono già in ~/.minecraft/config/enchlib/
     */
    fun ensureDefaultsInstalled() {
        Files.createDirectories(targetDir)
        copyIfAbsent("config/AviableEnch.json5", targetDir.resolve("AviableEnch.json5"))
        copyIfAbsent("config/EnchantmentsDetails.json5", targetDir.resolve("EnchantmentsDetails.json5"))
    }

    private fun copyIfAbsent(resourcePath: String, dst: Path) {
        if (Files.exists(dst)) return
        val ins: InputStream? = GlobalConfigBootstrap::class.java.classLoader.getResourceAsStream(resourcePath)
        ins?.use {
            Files.copy(it, dst, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
