package goldenrose01.enchlib.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.mojang.logging.LogUtils

import goldenrose01.enchlib.Enchlib
import goldenrose01.enchlib.util.Json5

import net.minecraft.enchantment.Enchantment
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.entry.RegistryEntry

import java.nio.file.Path
import java.io.File
import kotlin.io.path.exists
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText



/**
 * Gestisce i file JSON5 per-mondo:
 * - saves/<world>/config/enchlib/AviableEnch.json5
 * - saves/<world>/config/enchlib/EnchantmentDetails.json5
 *
 * Autopopulate all’avvio: aggiunge tutte le entry del registry runtime.
 * Merge non distruttivo: mantiene ciò che esiste già.
 * Cache in memoria + reload live.
 */

object WorldConfigManager {
    private val logger = LogUtils.getLogger()
    private var configDir: File? = null
    private var aviable = AviableEnch()
    private var details = EnchantmentDetails()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun onServerStarted(server: MinecraftServer) {
        val root = server.getSavePath(WorldSavePath.ROOT).toFile()
        configDir = File(root, "config/${EnchLib.MOD_ID}").apply { mkdirs() }
        loadOrCreate()
        autopopulate(server.registryManager)
        saveAll()
        logger.info("[${EnchLib.MOD_ID}] Config loaded.")
    }

    fun onServerStopping() {
        saveAll()
    }

    fun ensurePresentInJson(id: String) {
        if (aviable.enchantments.none { it.id == id })
            aviable.enchantments.add(AviableEntry(id))
        if (details.entries.none { it.id == id })
            details.entries.add(EnchDetail(id))
    }

    fun resolveEnchantment(server: MinecraftServer, id: Identifier): RegistryEntry<Enchantment>? {
        val reg = server.registryManager.get(RegistryKeys.ENCHANTMENT)
        return reg.get(id).orElse(null)
    }

    fun reload(server: MinecraftServer): ReloadReport {
        loadOrCreate()
        return ReloadReport(aviable.enchantments.size, details.entries.size)
    }

    fun validate(server: MinecraftServer): ValidateReport {
        val reg = server.registryManager.get(RegistryKeys.ENCHANTMENT)
        val regIds = reg.keySet().map { it.value.toString() }
        val jsonIds = (aviable.enchantments.map { it.id } + details.entries.map { it.id }).toSet()
        return ValidateReport(
            regIds.filter { it !in jsonIds },
            jsonIds.filter { it !in regIds },
            mapOf(
                "registry" to regIds.size,
                "aviable" to aviable.enchantments.size,
                "details" to details.entries.size
            )
        )
    }

    private fun autopopulate(manager: DynamicRegistryManager) {
        val reg = manager.get(RegistryKeys.ENCHANTMENT)
        reg.keySet().forEach { key ->
            val id = key.value.toString()
            if (aviable.enchantments.none { it.id == id })
                aviable.enchantments.add(AviableEntry(id))
            if (details.entries.none { it.id == id })
                details.entries.add(EnchDetail(id))
        }
    }

    private fun loadOrCreate() {
        val fA = File(configDir, "AviableEnch.json5")
        val fD = File(configDir, "EnchantmentDetails.json5")
        aviable = if (fA.exists()) readAviable(fA) else AviableEnch().also { fA.writeText(json.encodeToString(it)) }
        details = if (fD.exists()) readDetails(fD) else EnchantmentDetails().also { fD.writeText(json.encodeToString(it)) }
    }

    private fun saveAll() {
        File(configDir, "AviableEnch.json5").writeText(json.encodeToString(aviable))
        File(configDir, "EnchantmentDetails.json5").writeText(json.encodeToString(details))
    }

    private fun readAviable(file: File): AviableEnch {
        val s = Json5.sanitize(file.readText())
        return json.decodeFromString(s.ifEmpty { """{"enchantments":[]}""" })
    }

    private fun readDetails(file: File): EnchantmentDetails {
        val s = Json5.sanitize(file.readText())
        return json.decodeFromString(s.ifEmpty { """{"entries":[]}""" })
    }
}