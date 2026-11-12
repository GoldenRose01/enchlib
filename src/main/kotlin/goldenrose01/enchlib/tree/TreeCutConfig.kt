package goldenrose01.enchlib.tree

import goldenrose01.enchlib.config.GlobalConfigIO
import java.nio.file.Files
import java.util.Properties

object TreeCutConfig {
    @Volatile
    private var mode: TreeCutMode = TreeCutMode.FELL
    private const val CONFIG_NAME = "treecut.properties"

    fun getMode(): TreeCutMode = mode

    fun setMode(newMode: TreeCutMode) {
        mode = newMode
        save()
    }

    fun load() {
        try {
            val file = GlobalConfigIO.baseDir().resolve(CONFIG_NAME)
            if (Files.exists(file)) {
                val props = Properties()
                Files.newInputStream(file).use { props.load(it) }
                val raw = props.getProperty("mode", "fell")
                mode = TreeCutMode.fromString(raw)
            } else {
                mode = TreeCutMode.FELL
            }
        } catch (_: Throwable) {
            mode = TreeCutMode.FELL
        }
    }

    fun save() {
        try {
            val dir = GlobalConfigIO.baseDir()
            Files.createDirectories(dir)
            val file = dir.resolve(CONFIG_NAME)
            val props = Properties()
            props.setProperty("mode", mode.id)
            Files.newOutputStream(file).use { props.store(it, "EnchLib TreeCut Configuration") }
        } catch (_: Throwable) {
        }
    }
}
