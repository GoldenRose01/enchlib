package goldenrose01.enchlib.config

import goldenrose01.enchlib.config.data.*
import goldenrose01.enchlib.utils.EnchLogger

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.exists
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.io.path.readText

import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

// ====== DATA CLASSES ======

@Serializable
data class AviableEnch(val enchantments: MutableList<AvailEntry> = mutableListOf())

@Serializable
data class AvailEntry(val id: String, val enabled: Boolean = true)

@Serializable
data class EnchantmentDetail(
    val id: String,
    val maxLevel: Int = 1,
    val multiplier: Double = 1.0,
    val rarity: String = "common",
    val category: String = "generic"
)

@Serializable
data class EnchantmentDetails(val details: MutableList<EnchantmentDetail> = mutableListOf())

@Serializable
data class UncompatRule(val enchantment: String, val incompatibleWith: List<String> = emptyList())

@Serializable
data class Uncompatibility(val rules: MutableList<UncompatRule> = mutableListOf())

@Serializable
data class MobCategoryRule(val mobId: String, val categories: List<String> = emptyList())

@Serializable
data class MobCategories(val rules: MutableList<MobCategoryRule> = mutableListOf())

data class ValidateResult(val valid: Boolean, val report: String)

data class Stats(val total: Int, val enabled: Int, val disabled: Int, val missing: Int)

// ====== MANAGER ======

object WorldConfigManager {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private const val FOLDER = "config/enchlib"
    private const val FN_AVAILABLE = "AviableEnch.json5"
    private const val FN_DETAILS = "EnchantmentDetails.json5"
    private const val FN_UNCOMP = "Uncompatibility.json5"
    private const val FN_MOBS   = "Mob_category.json5"

    /** Directory world-based: `<world>/config/enchlib` */
    fun getWorldConfigDir(server: MinecraftServer): Path {
        val runDir: Path = server.runDirectory.toAbsolutePath()
        val levelName = server.saveProperties.levelName
        val target = runDir.resolve("saves").resolve(levelName).resolve(FOLDER)
        if (!target.exists()) target.createDirectories()
        return target
    }

    fun initializeWorldConfigs(server: MinecraftServer) {
        val cfg = getWorldConfigDir(server)
        val allIds = allEnchantmentIds()

        // Crea/merge AviableEnch
        val available = loadOrCreateAviable(cfg, allIds)
        saveAviable(cfg, available)

        // Crea/merge EnchantmentDetails
        val details = loadOrCreateDetails(cfg, allIds)
        saveDetails(cfg, details)

        // Crea se mancante Uncompatibility
        if (!cfg.resolve(FN_UNCOMP).exists()) {
            saveUncompat(cfg, Uncompatibility())
        }

        // Crea se mancante Mob_category
        if (!cfg.resolve(FN_MOBS).exists()) {
            saveMobCats(cfg, MobCategories())
        }
    }

    fun reload(server: MinecraftServer) {
        // In caso di cache in memoria, ricaricala. Qui è stateless, quindi no-op.
        // Lasciato per compatibilità con /plusec-debug reload
        getWorldConfigDir(server) // force ensure dir exists
    }

    fun regen(server: MinecraftServer) {
        initializeWorldConfigs(server)
    }

    fun validate(server: MinecraftServer): ValidateResult {
        val cfg = getWorldConfigDir(server)
        val all = allEnchantmentIds().toSet()

        val avail = loadAviableSafely(cfg).enchantments
            .mapNotNull { Identifier.tryParse(it.id) }
            .toSet()
        val missingInJson = all.minus(avail)
        val extraInJson = avail.minus(all)
        val ok = missingInJson.isEmpty() && extraInJson.isEmpty()

        val report = buildString {
            appendLine("MissingInJson=${missingInJson.size}, ExtraInJson=${extraInJson.size}")
            if (missingInJson.isNotEmpty()) appendLine("Missing: ${missingInJson.joinToString()}")
            if (extraInJson.isNotEmpty()) appendLine("Extra: ${extraInJson.joinToString()}")
        }.trim()

        return ValidateResult(ok, report.trim())
    }

    fun stats(server: MinecraftServer): Stats {
        val cfg = getWorldConfigDir(server)
        val avail = loadAviableSafely(cfg)
        val total = avail.enchantments.size
        val enabled = avail.enchantments.count { it.enabled }
        val disabled = total - enabled
        val missing = allEnchantmentIds().size - total
        return Stats(total, enabled, disabled, missing)
    }

    // ====== Accessors usati dai comandi ======

    fun getMaxLevelFor(ench: Enchantment, server: MinecraftServer): Int {
        val id = Registries.ENCHANTMENT.getId(ench) ?: return 1
        val detail = getDetailsFor(ench, server)
        return detail.maxLevel
    }

    fun getDetailsFor(ench: Enchantment, server: MinecraftServer): EnchantmentDetail {
        val cfg = getWorldConfigDir(server)
        val id = Registries.ENCHANTMENT.getId(ench)?.toString() ?: "minecraft:unknown"
        val details = loadDetailsSafely(cfg)
        return details.details.firstOrNull { it.id == id } ?: EnchantmentDetail(id)
    }

    // ====== Helpers JSON ======

    private fun loadOrCreateAviable(cfg: Path, allIds: List<Identifier>): AviableEnch {
        val file = cfg.resolve(FN_AVAILABLE)
        val current = if (file.exists()) {
            runCatching { json.decodeFromString<AviableEnch>(file.readText()) }.getOrElse { AviableEnch() }
        } else AviableEnch()

        val known = current.enchantments.mapTo(mutableSetOf()) { it.id }
        // merge non distruttivo: aggiungi solo i mancanti (enabled default true)
        allIds.forEach { id ->
            val s = id.toString()
            if (!known.contains(s)) {
                current.enchantments.add(AvailEntry(s, true))
            }
        }
        return current
    }

    private fun saveAviable(cfg: Path, data: AviableEnch) {
        cfg.resolve(FN_AVAILABLE).writeText(json.encodeToString(data))
    }

    private fun loadAviableSafely(cfg: Path): AviableEnch {
        val f = cfg.resolve(FN_AVAILABLE)
        if (!f.exists()) return AviableEnch()
        return runCatching { json.decodeFromString<AviableEnch>(f.readText()) }.getOrElse { AviableEnch() }
    }

    private fun loadOrCreateDetails(cfg: Path, allIds: List<Identifier>): EnchantmentDetails {
        val file = cfg.resolve(FN_DETAILS)
        val current = if (file.exists()) {
            runCatching { json.decodeFromString<EnchantmentDetails>(file.readText()) }.getOrElse { EnchantmentDetails() }
        } else EnchantmentDetails()

        val idx = current.details.associateBy { it.id }.toMutableMap()
        allIds.forEach { id ->
            val s = id.toString()
            if (!idx.containsKey(s)) {
                idx[s] = EnchantmentDetail(s)
            }
        }
        return EnchantmentDetails(idx.values.sortedBy { it.id }.toMutableList())
    }

    private fun saveDetails(cfg: Path, data: EnchantmentDetails) {
        cfg.resolve(FN_DETAILS).writeText(json.encodeToString(data))
    }

    private fun loadDetailsSafely(cfg: Path): EnchantmentDetails {
        val f = cfg.resolve(FN_DETAILS)
        if (!f.exists()) return EnchantmentDetails()
        return runCatching { json.decodeFromString<EnchantmentDetails>(f.readText()) }.getOrElse { EnchantmentDetails() }
    }

    private fun saveUncompat(cfg: Path, data: Uncompatibility) {
        cfg.resolve(FN_UNCOMP).writeText(json.encodeToString(data))
    }

    private fun saveMobCats(cfg: Path, data: MobCategories) {
        cfg.resolve(FN_MOBS).writeText(json.encodeToString(data))
    }

    // ====== Registry helpers ======

    /** Lista ordinata e stabile di tutti gli Identifier degli enchant (vanilla + mod). */
    private fun allEnchantmentIds(): List<Identifier> {
        val reg = Registries.ENCHANTMENT
        // entrySet(): Set<Map.Entry<RegistryKey<Enchantment>, Enchantment>>
        return reg.entrySet()
            .map { it.key.value() }
            .sortedWith(compareBy({ it.namespace }, { it.path }))
    }
}