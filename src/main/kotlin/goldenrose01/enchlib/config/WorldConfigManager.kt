package goldenrose01.enchlib.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.mojang.logging.LogUtils

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import goldenrose01.enchlib.Enchlib
import goldenrose01.enchlib.utils.Json5
import goldenrose01.enchlib.utils.EnchLogger

import net.minecraft.enchantment.Enchantment
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.component.type.ItemEnchantmentsComponent

import java.nio.file.Path
import java.nio.file.Files
import java.io.File
import java.nio.file.StandardCopyOption

import kotlin.io.path.exists
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

// Formato JSON semplice su disco: lista di id -> level
data class EnchantmentEntryDto(val id: String, val level: Int)
data class EnchantmentConfigDto(val enchantments: List<EnchantmentEntryDto> = emptyList())

object WorldConfigManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // Stato attuale in memoria
    private var current: ItemEnchantmentsComponent = ItemEnchantmentsComponent.DEFAULT

    private fun worldRoot(server: MinecraftServer): File {
        return server.getSavePath(WorldSavePath.ROOT).toFile()
    }

    private fun configDir(server: MinecraftServer): File {
        val dir = File(worldRoot(server), "config/enchlib")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun configFile(server: MinecraftServer): File {
        return File(configDir(server), "AviableEnch.json5")
    }

    fun getCurrentComponent(): ItemEnchantmentsComponent = current

    fun reloadConfigs(server: MinecraftServer) {
        val file = configFile(server)
        // se non esiste, prova a copiare dai resources; se ancora non c'è, autopopola
        if (!file.exists() || file.readText().isBlank()) {
            EnchLogger.info("No config found for this world, generating default...")
            copyDefaultIfPresent(server)
            if (!file.exists() || file.readText().isBlank()) {
                autopopulate(server)
                return
            }
        }

        try {
            val text = file.readText()
            val dto = gson.fromJson(text, EnchantmentConfigDto::class.java) ?: EnchantmentConfigDto()
            current = buildComponentFromDto(server, dto)
            EnchLogger.info("World enchantment config reloaded with ${current.getSize()} entries")
        } catch (e: Exception) {
            EnchLogger.error("Failed to parse config, regenerating defaults", e)
            autopopulate(server)
        }
    }

    private fun copyDefaultIfPresent(server: MinecraftServer) {
        val inPath = "/config/enchlib/AviableEnch.json5"
        javaClass.getResourceAsStream(inPath)?.use { ins ->
            val out = configFile(server).toPath()
            Files.createDirectories(out.parent)
            Files.copy(ins, out, StandardCopyOption.REPLACE_EXISTING)
            EnchLogger.info("Copied default config from resources: $inPath")
        }
    }

    private fun autopopulate(server: MinecraftServer) {
        val registry = server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)

        val list = mutableListOf<EnchantmentEntryDto>()
        for (ench in registry) {
            val id = registry.getId(ench) ?: continue
            list.add(EnchantmentEntryDto(id.toString(), 1))
        }
        val dto = EnchantmentConfigDto(list)

        // salva DTO su file
        saveDto(server, dto)

        // costruisci il componente in memoria
        current = buildComponentFromDto(server, dto)

        EnchLogger.info("Default enchantment config generated with ${current.getSize()} entries")
    }

    private fun buildComponentFromDto(server: MinecraftServer, dto: EnchantmentConfigDto): ItemEnchantmentsComponent {
        val registry = server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        val builder = ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT)

        for (e in dto.enchantments) {
            val parsed = Identifier.tryParse(e.id) ?: continue
            val key: RegistryKey<Enchantment> = RegistryKey.of(RegistryKeys.ENCHANTMENT, parsed)
            val refOpt = registry.getOptional(key) // Optional<RegistryEntry.Reference<Enchantment>>
            refOpt.ifPresent { entry: RegistryEntry.Reference<Enchantment> ->
                builder.set(entry, e.level.coerceIn(1, 255))
            }
        }
        return builder.build()
    }

    private fun saveDto(server: MinecraftServer, dto: EnchantmentConfigDto) {
        val file = configFile(server)
        try {
            Files.createDirectories(file.toPath().parent)
            Files.writeString(file.toPath(), gson.toJson(dto))
            EnchLogger.info("Config saved at ${file.absolutePath}")
        } catch (e: Exception) {
            EnchLogger.error("Failed to save config", e)
        }
    }

    private fun saveFromCurrent(server: MinecraftServer) {
        val registry = server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        val list = mutableListOf<EnchantmentEntryDto>()
        for (entry in current.getEnchantmentEntries()) {
            val enchEntry: RegistryEntry<Enchantment> = entry.key
            val lvl = entry.intValue
            val idStr = registry.getId(enchEntry.value())?.toString() ?: continue
            list.add(EnchantmentEntryDto(idStr, lvl))
        }
        saveDto(server, EnchantmentConfigDto(list))
    }

    fun addOrUpdateEnchantment(server: MinecraftServer, id: String, level: Int) {
        val registry = server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        val identifier = Identifier.tryParse(id) ?: return
        val key: RegistryKey<Enchantment> = RegistryKey.of(RegistryKeys.ENCHANTMENT, identifier)
        val refOpt = registry.getOptional(key)

        refOpt.ifPresent { ref ->
            val builder = ItemEnchantmentsComponent.Builder(current)
            builder.set(ref, level.coerceIn(1, 255))
            current = builder.build()
            saveFromCurrent(server)
        }
    }

    fun validateAgainstRegistry(server: MinecraftServer): List<String> {
        val registry = server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        val existingIds: Set<String> = buildSet {
            for (ench in registry) {
                val id = registry.getId(ench)?.toString() ?: continue
                add(id)
            }
        }

        val issues = mutableListOf<String>()
        for (entry in current.getEnchantmentEntries()) {
            val enchEntry: RegistryEntry<Enchantment> = entry.key
            val lvl = entry.intValue
            val idStr = registry.getId(enchEntry.value())?.toString() ?: "unknown"
            if (idStr !in existingIds) {
                issues.add("Unknown enchantment: $idStr")
            }
            if (lvl !in 1..255) {
                issues.add("Invalid level for $idStr: $lvl")
            }
        }
        return issues
    }
}