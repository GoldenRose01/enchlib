package goldenrose01.enchlib.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@OptIn(ExperimentalSerializationApi::class)
object GlobalConfigIO {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        allowComments = true
        allowTrailingComma = true
        encodeDefaults = true
    }

    @Serializable
    data class AvailableEnch(val id: String, val enabled: Boolean = true)

    @Serializable
    data class EnchantmentsDetailsRoot(
        val enchantments: MutableList<EnchantmentDetails> = mutableListOf()
    )

    @Serializable
    data class EnchantmentDetails(
        val id: String,
        var max_level: Int = 1,
        var enc_category: MutableList<String> = mutableListOf(),
        var mob_category: MutableList<String> = mutableListOf()
    )

    fun baseDir(): Path = FabricLoader.getInstance().configDir.resolve("enchlib")
    private fun availableFile(): Path = baseDir().resolve("AviableEnch.json5")
    private fun detailsFile(): Path = baseDir().resolve("EnchantmentsDetails.json5")

    fun readAvailable(): MutableList<AvailableEnch> {
        val p = availableFile()
        if (!Files.exists(p)) return mutableListOf()
        return json.decodeFromString(Files.readString(p))
    }

    fun writeAvailable(list: List<AvailableEnch>) {
        Files.createDirectories(baseDir())
        Files.writeString(
            availableFile(),
            json.encodeToString(list),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
        )
    }

    fun readDetails(): EnchantmentsDetailsRoot {
        val p = detailsFile()
        if (!Files.exists(p)) return EnchantmentsDetailsRoot()
        return json.decodeFromString(Files.readString(p))
    }

    fun writeDetails(root: EnchantmentsDetailsRoot) {
        Files.createDirectories(baseDir())
        Files.writeString(
            detailsFile(),
            json.encodeToString(root),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
        )
    }
}
