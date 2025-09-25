package goldenrose01.enchlib.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

import net.minecraft.enchantment.Enchantment
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

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

object WorldConfigManager {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private const val FOLDER = "config/enchlib"
    private const val FN_AVAILABLE = "AviableEnch.json5"
    private const val FN_DETAILS   = "EnchantmentDetails.json5"
    private const val FN_UNCOMP    = "Uncompatibility.json5"
    private const val FN_MOBS      = "Mob_category.json5"

    /** Directory world-based: `<runDir>/saves/<levelName>/config/enchlib` */
    fun getWorldConfigDir(server: MinecraftServer): Path {
        val runDir: Path = server.runDirectory.toAbsolutePath()
        val levelName = server.saveProperties.levelName
        val target = runDir.resolve("saves").resolve(levelName).resolve(FOLDER)
        if (!target.exists()) target.createDirectories()
        return target
    }

    fun initializeWorldConfigs(server: MinecraftServer) {
        val cfg = getWorldConfigDir(server)
        if (!cfg.resolve(FN_AVAILABLE).exists()) saveAvailable(cfg, AviableEnch())
        if (!cfg.resolve(FN_DETAILS).exists())   saveDetails(cfg, EnchantmentDetails())
        if (!cfg.resolve(FN_UNCOMP).exists())    cfg.resolve(FN_UNCOMP).writeText(json.encodeToString(Uncompatibility()))
        if (!cfg.resolve(FN_MOBS).exists())      cfg.resolve(FN_MOBS).writeText(json.encodeToString(MobCategories()))
    }

    fun reload(server: MinecraftServer) { getWorldConfigDir(server) }
    fun regen(server: MinecraftServer)  { initializeWorldConfigs(server) }

    fun validate(server: MinecraftServer): ValidateResult {
        val cfg = getWorldConfigDir(server)
        val avail = loadAvailableSafely(cfg)
        // senza accesso all’elenco globale runtime, la validazione è “interna”
        val report = "Available=${avail.enchantments.size} (validazione cross-registry disabilitata in questa build)"
        return ValidateResult(true, report)
    }

    fun stats(server: MinecraftServer): Stats {
        val cfg = getWorldConfigDir(server)
        val avail = loadAvailableSafely(cfg)
        val total = avail.enchantments.size
        val enabled = avail.enchantments.count { it.enabled }
        val disabled = total - enabled
        val missing = 0 // senza enumerazione globale
        return Stats(total, enabled, disabled, missing)
    }

    // ====== Accessors per comandi ======

    fun getMaxLevelFor(ench: Enchantment, server: MinecraftServer): Int {
        val detail = getDetailsFor(ench, server)
        return detail.maxLevel
    }

    fun getDetailsFor(ench: Enchantment, server: MinecraftServer): EnchantmentDetail {
        val cfg = getWorldConfigDir(server)
        val idGuess = try { ench.toString() } catch (_: Throwable) { "unknown:unknown" }
        val details = loadDetailsSafely(cfg)
        return details.details.firstOrNull { it.id == idGuess } ?: EnchantmentDetail(idGuess)
    }

    /** Assicura che un id sia presente in Available/Details, senza distruggere valori esistenti. */
    fun ensurePresentInJson(id: String, server: MinecraftServer) {
        val cfg = getWorldConfigDir(server)

        val avail = loadAvailableSafely(cfg)
        if (avail.enchantments.none { it.id == id }) {
            avail.enchantments.add(AvailEntry(id, true))
            saveAvailable(cfg, avail)
        }

        val dets = loadDetailsSafely(cfg)
        if (dets.details.none { it.id == id }) {
            dets.details.add(EnchantmentDetail(id))
            dets.details.sortBy { it.id }
            saveDetails(cfg, dets)
        }
    }

    // ====== Helpers JSON (.json5 → sanificati a JSON) ======

    private fun loadAvailableSafely(cfg: Path): AviableEnch {
        val f = cfg.resolve(FN_AVAILABLE)
        if (!f.exists()) return AviableEnch()
        return runCatching {
            json.decodeFromString<AviableEnch>(sanitizeJson5(f.readText()))
        }.getOrElse { AviableEnch() }
    }

    private fun saveAvailable(cfg: Path, data: AviableEnch) {
        cfg.resolve(FN_AVAILABLE).writeText(json.encodeToString(data))
    }

    private fun loadDetailsSafely(cfg: Path): EnchantmentDetails {
        val f = cfg.resolve(FN_DETAILS)
        if (!f.exists()) return EnchantmentDetails()
        return runCatching {
            json.decodeFromString<EnchantmentDetails>(sanitizeJson5(f.readText()))
        }.getOrElse { EnchantmentDetails() }
    }

    private fun saveDetails(cfg: Path, data: EnchantmentDetails) {
        cfg.resolve(FN_DETAILS).writeText(json.encodeToString(data))
    }

    // ====== JSON5 → JSON sanitizer ======

    private fun sanitizeJson5(input: String): String {
        val noComments = removeComments(input)
        return removeTrailingCommas(noComments)
    }

    private fun removeComments(s: String): String {
        val out = StringBuilder(s.length)
        var i = 0
        var inString = false
        var stringDelim = '\u0000'
        var escape = false

        while (i < s.length) {
            val c = s[i]

            if (inString) {
                out.append(c)
                if (escape) {
                    escape = false
                } else {
                    if (c == '\\') {
                        escape = true
                    } else if (c == stringDelim) {
                        inString = false
                    }
                }
                i++
                continue
            }

            if (c == '"' || c == '\'') {
                inString = true
                stringDelim = c
                out.append(c)
                i++
                continue
            }

            if (c == '/' && i + 1 < s.length) {
                val n = s[i + 1]
                if (n == '/') {
                    i += 2
                    while (i < s.length && s[i] != '\n' && s[i] != '\r') i++
                    continue
                }
                if (n == '*') {
                    i += 2
                    while (i + 1 < s.length && !(s[i] == '*' && s[i + 1] == '/')) i++
                    i += 2.coerceAtMost(s.length - i)
                    continue
                }
            }

            out.append(c)
            i++
        }
        return out.toString()
    }

    private fun removeTrailingCommas(s: String): String {
        val out = StringBuilder(s.length)
        var i = 0
        var inString = false
        var stringDelim = '\u0000'
        var escape = false

        while (i < s.length) {
            val c = s[i]

            if (inString) {
                out.append(c)
                if (escape) {
                    escape = false
                } else {
                    if (c == '\\') {
                        escape = true
                    } else if (c == stringDelim) {
                        inString = false
                    }
                }
                i++
                continue
            }

            if (c == ',') {
                var j = i + 1
                while (j < s.length && s[j].isWhitespace()) j++
                if (j < s.length && (s[j] == '}' || s[j] == ']')) {
                    i++
                    continue
                }
            }

            out.append(c)
            i++
        }
        return out.toString()
    }
}
