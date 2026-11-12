package goldenrose01.enchlib.weapons

import goldenrose01.enchlib.config.GlobalConfigIO
import java.nio.file.Files
import java.util.Properties

object AxesAreWeaponsConfig {
    @Volatile
    private var enabled: Boolean = true
    private const val CONFIG_NAME = "axesareweapons.properties"

    @JvmStatic
    fun isEnabled(): Boolean = enabled

    @JvmStatic
    fun setEnabled(value: Boolean) {
        enabled = value
        save()
    }

    fun load() {
        try {
            val file = GlobalConfigIO.baseDir().resolve(CONFIG_NAME)
            if (Files.exists(file)) {
                val props = Properties()
                Files.newInputStream(file).use { props.load(it) }
                val raw = props.getProperty("enabled", "true")
                enabled = raw.toBooleanStrictOrNull() ?: true
            } else {
                enabled = true
            }
        } catch (_: Throwable) {
            enabled = true
        }
    }

    fun save() {
        try {
            val dir = GlobalConfigIO.baseDir()
            Files.createDirectories(dir)
            val file = dir.resolve(CONFIG_NAME)
            val props = Properties()
            props.setProperty("enabled", enabled.toString())
            Files.newOutputStream(file).use { props.store(it, "EnchLib AxesAreWeapons Configuration") }
        } catch (_: Throwable) {
        }
    }
}
