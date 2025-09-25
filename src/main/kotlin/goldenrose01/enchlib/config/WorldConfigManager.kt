package goldenrose01.enchlib.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

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
    private const val FN_DETAILS   = "EnchantmentDetails.json5"
    private const val FN_UNCOMP    = "Uncompatibility.json5"
    private const val FN_MOBS      = "Mob_category.json5"

    private fun enchRegistry(server: MinecraftServer): Registry<Enchantment> =
        server.registryManager.get(RegistryKeys.ENCHANTMENT)

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
        val allIds = allEnchantmentIds(server)

        val available = loadOrCreateAvailable(cfg, allIds)
        saveAvailable(cfg, available)

        val details = loadOrCreateDetails(cfg, allIds)
        saveDetails(cfg, details)

        if (!cfg.resolve(FN_UNCOMP).exists()) saveUncompat(cfg, Uncompatibility())
        if (!cfg.resolve(FN_MOBS).exists())   saveMobCats(cfg, MobCategories())
    }

    fun reload(server: MinecraftServer) { getWorldConfigDir(server) }
    fun regen(server: MinecraftServer) { initializeWorldConfigs(server) }

    fun validate(server: MinecraftServer): ValidateResult {
        val cfg = getWorldConfigDir(server)
        val all = allEnchantmentIds(server).toSet()

        val avail = loadAvailableSafely(cfg).enchantments
            .mapNotNull { Identifier.tryParse(it.id) }
            .toSet()

        val missingInJson = all - avail
        val extraInJson = avail - all
        val ok = missingInJson.isEmpty() && extraInJson.isEmpty()

        val report = buildString {
            appendLine("MissingInJson=${missingInJson.size}, ExtraInJson=${extraInJson.size}")
            if (missingInJson.isNotEmpty()) appendLine("Missing: ${missingInJson.joinToString()}")
            if (extraInJson.isNotEmpty()) appendLine("Extra: ${extraInJson.joinToString()}")
        }.trim()

        return ValidateResult(ok, report)
    }

    fun stats(server: MinecraftServer): Stats {
        val cfg = getWorldConfigDir(server)
        val avail = loadAvailableSafely(cfg)
        val total = avail.enchantments.size
        val enabled = avail.enchantments.count { it.enabled }
        val disabled = total - enabled
        val missing = allEnchantmentIds(server).size - total
        return Stats(total, enabled, disabled, missing)
    }

    // ====== Accessors per comandi ======

    fun getMaxLevelFor(ench: Enchantment, server: MinecraftServer): Int {
        val detail = getDetailsFor(ench, server)
        return detail.maxLevel
    }

    fun getDetailsFor(ench: Enchantment, server: MinecraftServer): EnchantmentDetail {
        val cfg = getWorldConfigDir(server)
        val id = enchRegistry(server).getId(ench).toString()
        val details = loadDetailsSafely(cfg)
        return details.details.firstOrNull { it.id == id } ?: EnchantmentDetail(id)
    }

    // ====== Helpers JSON (.json5 → sanificati a JSON) ======

    private fun loadOrCreateAvailable(cfg: Path, allIds: List<Identifier>): AviableEnch {
        val file = cfg.resolve(FN_AVAILABLE)
        val current = if (file.exists()) {
            runCatching {
                json.decodeFromString<AviableEnch>(sanitizeJson5(file.readText()))
            }.getOrElse { AviableEnch() }
        } else AviableEnch()

        val known = current.enchantments.mapTo(mutableSetOf()) { it.id }
        allIds.forEach { id ->
            val s = id.toString()
            if (!known.contains(s)) current.enchantments.add(AvailEntry(s, true))
        }
        return current
    }

    // alias compatibilità (se altrove usavi A**i**able)
    @Suppress("unused")
    private fun loadOrCreateAviable(cfg: Path, allIds: List<Identifier>): AviableEnch =
        loadOrCreateAvailable(cfg, allIds)

    private fun saveAvailable(cfg: Path, data: AviableEnch) {
        cfg.resolve(FN_AVAILABLE).writeText(json.encodeToString(data))
    }

    @Suppress("unused")
    private fun saveAviable(cfg: Path, data: AviableEnch) = saveAvailable(cfg, data)

    private fun loadAvailableSafely(cfg: Path): AviableEnch {
        val f = cfg.resolve(FN_AVAILABLE)
        if (!f.exists()) return AviableEnch()
        return runCatching {
            json.decodeFromString<AviableEnch>(sanitizeJson5(f.readText()))
        }.getOrElse { AviableEnch() }
    }

    private fun loadOrCreateDetails(cfg: Path, allIds: List<Identifier>): EnchantmentDetails {
        val file = cfg.resolve(FN_DETAILS)
        val current = if (file.exists()) {
            runCatching {
                json.decodeFromString<EnchantmentDetails>(sanitizeJson5(file.readText()))
            }.getOrElse { EnchantmentDetails() }
        } else EnchantmentDetails()

        val idx = current.details.associateBy { it.id }.toMutableMap()
        allIds.forEach { id ->
            val s = id.toString()
            if (!idx.containsKey(s)) idx[s] = EnchantmentDetail(s)
        }
        return EnchantmentDetails(idx.values.sortedBy { it.id }.toMutableList())
    }

    private fun saveDetails(cfg: Path, data: EnchantmentDetails) {
        cfg.resolve(FN_DETAILS).writeText(json.encodeToString(data))
    }

    private fun loadDetailsSafely(cfg: Path): EnchantmentDetails {
        val f = cfg.resolve(FN_DETAILS)
        if (!f.exists()) return EnchantmentDetails()
        return runCatching {
            json.decodeFromString<EnchantmentDetails>(sanitizeJson5(f.readText()))
        }.getOrElse { EnchantmentDetails() }
    }

    private fun saveUncompat(cfg: Path, data: Uncompatibility) {
        cfg.resolve(FN_UNCOMP).writeText(json.encodeToString(data))
    }

    private fun saveMobCats(cfg: Path, data: MobCategories) {
        cfg.resolve(FN_MOBS).writeText(json.encodeToString(data))
    }

    // ====== Registry ======

    private fun allEnchantmentIds(server: MinecraftServer): List<Identifier> =
        enchRegistry(server).ids.sortedWith(compareBy({ it.namespace }, { it.path }))

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

            if (c == '"' || c == '\'') {
                inString = true
                stringDelim = c
                out.append(c)
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