package goldenrose01.enchlib.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/** Server-authoritative enchantment settings stored inside each world's `config/enchlib` directory. */
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
    data class EnchantmentsDetailsRoot(val enchantments: MutableList<EnchantmentDetails> = mutableListOf())

    @Serializable
    data class EnchantmentDetails(
        val id: String,
        var max_level: Int = 1,
        var enc_category: MutableList<String> = mutableListOf(),
        var mob_category: MutableList<String> = mutableListOf(),
        var rarity: String? = null,
        var incompatible_enchantments: MutableList<String> = mutableListOf(),
        var table_rules: MutableMap<String, EnchantingTableRule> = mutableMapOf(),
        var preset_max_level: Int? = null,
        var preset_enc_category: MutableList<String>? = null,
        var preset_rarity: String? = null
    )

    @Serializable
    data class EnchantingTableRule(var enabled: Boolean = true, var chance: Int = 100)

    @Volatile private var activeWorldDir: Path? = null
    @Volatile private var clientAvailable: MutableList<AvailableEnch>? = null
    @Volatile private var clientDetails: EnchantmentsDetailsRoot? = null
    private val stagedAvailable = linkedMapOf<String, AvailableEnch>()
    private val stagedDetails = linkedMapOf<String, EnchantmentDetails>()

    /** Active world's settings path; falls back to the legacy location before a world is selected. */
    fun baseDir(): Path = activeWorldDir ?: FabricLoader.getInstance().configDir.resolve("enchlib")

    fun openWorld(server: MinecraftServer) {
        val dir = server.getWorldPath(LevelResource.ROOT).resolve("config").resolve("enchlib")
        activeWorldDir = dir
        GlobalConfigBootstrap.ensureDefaultsInstalled(server)
        val available = readAvailableFromDisk().associateBy { it.id }.toMutableMap()
        synchronized(stagedAvailable) {
            stagedAvailable.forEach { (id, value) -> available.putIfAbsent(id, value) }
            stagedAvailable.clear()
        }
        val details = readDetailsFromDisk()
        synchronized(stagedDetails) {
            stagedDetails.values.forEach { staged ->
                val existing = details.enchantments.firstOrNull { it.id == staged.id }
                if (existing == null) details.enchantments += staged
                else mergeMissingPresetValues(existing, staged)
            }
            stagedDetails.clear()
        }
        writeAvailableToDisk(available.values.sortedBy { it.id })
        writeDetailsToDisk(details)
        clientAvailable = null
        clientDetails = null
    }

    fun closeWorld(server: MinecraftServer) {
        val dir = server.getWorldPath(LevelResource.ROOT).resolve("config").resolve("enchlib")
        if (activeWorldDir == dir) activeWorldDir = null
        clientAvailable = null
        clientDetails = null
    }

    fun readAvailable(): MutableList<AvailableEnch> {
        clientAvailable?.let { return it.toMutableList() }
        if (activeWorldDir == null) synchronized(stagedAvailable) {
            if (stagedAvailable.isNotEmpty()) return stagedAvailable.values.toMutableList()
        }
        return readAvailableFromDisk()
    }

    fun writeAvailable(list: List<AvailableEnch>) {
        val normalized = list.distinctBy { it.id }.sortedBy { it.id }
        when {
            activeWorldDir != null -> {
                writeAvailableToDisk(normalized)
                ConfigSyncNetworking.broadcastCurrent()
            }
            FabricLoader.getInstance().environmentType == EnvType.CLIENT -> {
                clientAvailable = normalized.toMutableList()
                ConfigSyncNetworking.sendClientUpdate()
            }
            else -> synchronized(stagedAvailable) { stagedAvailable.putAll(normalized.associateBy { it.id }) }
        }
    }

    fun readDetails(): EnchantmentsDetailsRoot {
        clientDetails?.let { return copyDetails(it) }
        if (activeWorldDir == null) synchronized(stagedDetails) {
            if (stagedDetails.isNotEmpty()) return EnchantmentsDetailsRoot(stagedDetails.values.map(::copyDetail).toMutableList())
        }
        return readDetailsFromDisk()
    }

    fun writeDetails(root: EnchantmentsDetailsRoot) {
        val normalized = copyDetails(root)
        when {
            activeWorldDir != null -> {
                writeDetailsToDisk(normalized)
                ConfigSyncNetworking.broadcastCurrent()
            }
            FabricLoader.getInstance().environmentType == EnvType.CLIENT -> {
                clientDetails = normalized
                ConfigSyncNetworking.sendClientUpdate()
            }
            else -> synchronized(stagedDetails) {
                normalized.enchantments.forEach { stagedDetails[it.id] = copyDetail(it) }
            }
        }
    }

    fun encodeSnapshot(): Pair<String, String> = json.encodeToString(readAvailable()) to json.encodeToString(readDetails())

    fun applyServerSnapshot(availableJson: String, detailsJson: String) {
        require(availableJson.length <= 1_000_000 && detailsJson.length <= 2_000_000) { "EnchLib configuration packet is too large" }
        val available = json.decodeFromString<List<AvailableEnch>>(availableJson)
        val details = json.decodeFromString<EnchantmentsDetailsRoot>(detailsJson)
        validateSnapshot(available, details)
        writeAvailableToDisk(available)
        writeDetailsToDisk(details)
    }

    fun applyClientSnapshot(availableJson: String, detailsJson: String) {
        require(availableJson.length <= 1_000_000 && detailsJson.length <= 2_000_000) { "EnchLib configuration packet is too large" }
        clientAvailable = json.decodeFromString<List<AvailableEnch>>(availableJson).toMutableList()
        clientDetails = json.decodeFromString(detailsJson)
    }

    private fun readAvailableFromDisk(): MutableList<AvailableEnch> {
        val path = baseDir().resolve("AviableEnch.json5")
        if (!Files.exists(path)) return mutableListOf()
        return json.decodeFromString(Files.readString(path))
    }

    private fun writeAvailableToDisk(list: List<AvailableEnch>) {
        Files.createDirectories(baseDir())
        Files.writeString(baseDir().resolve("AviableEnch.json5"), json.encodeToString(list), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    }

    private fun readDetailsFromDisk(): EnchantmentsDetailsRoot {
        val path = baseDir().resolve("EnchantmentsDetails.json5")
        if (!Files.exists(path)) return EnchantmentsDetailsRoot()
        return json.decodeFromString(Files.readString(path))
    }

    private fun writeDetailsToDisk(root: EnchantmentsDetailsRoot) {
        Files.createDirectories(baseDir())
        Files.writeString(baseDir().resolve("EnchantmentsDetails.json5"), json.encodeToString(root), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    }

    private fun validateSnapshot(available: List<AvailableEnch>, details: EnchantmentsDetailsRoot) {
        require(available.size <= 20_000 && details.enchantments.size <= 20_000) { "Too many EnchLib entries" }
        require(available.all { it.id.matches(Regex("^[a-z0-9_.-]+:[a-z0-9_./-]+$")) }) { "Invalid enchantment ID" }
        require(details.enchantments.all { it.id.matches(Regex("^[a-z0-9_.-]+:[a-z0-9_./-]+$")) && it.max_level in 1..255 }) { "Invalid enchantment details" }
    }

    private fun mergeMissingPresetValues(target: EnchantmentDetails, source: EnchantmentDetails) {
        if (target.preset_max_level == null) target.preset_max_level = source.preset_max_level
        if (target.preset_enc_category == null) target.preset_enc_category = source.preset_enc_category?.toMutableList()
        if (target.preset_rarity == null) target.preset_rarity = source.preset_rarity
        if (target.enc_category.isEmpty() && source.enc_category.isNotEmpty()) target.enc_category = source.enc_category.toMutableList()
        if (target.mob_category.isEmpty() && source.mob_category.isNotEmpty()) target.mob_category = source.mob_category.toMutableList()
    }

    private fun copyDetails(source: EnchantmentsDetailsRoot) = EnchantmentsDetailsRoot(source.enchantments.map(::copyDetail).toMutableList())
    private fun copyDetail(source: EnchantmentDetails) = source.copy(
        enc_category = source.enc_category.toMutableList(), mob_category = source.mob_category.toMutableList(),
        incompatible_enchantments = source.incompatible_enchantments.toMutableList(),
        table_rules = source.table_rules.mapValuesTo(mutableMapOf()) { (_, value) -> value.copy() },
        preset_enc_category = source.preset_enc_category?.toMutableList()
    )
}
