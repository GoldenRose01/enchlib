package goldenrose01.enchlib.config

import goldenrose01.enchlib.utils.EnchLogger
import goldenrose01.enchlib.network.ConfigSyncNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Loads category assignments for vanilla and modded entity types from MobCategory.config. */
object MobCategoryManager {
    private const val FILE_NAME = "MobCategory.config"
    private const val JSON_FILE_NAME = "MobCategory.json"

    @Serializable
    private data class JsonDocument(
        val schemaVersion: Int = 1,
        val minecraftVersion: String? = null,
        val mobs: List<JsonMob> = emptyList()
    )

    @Serializable
    private data class JsonMob(
        val id: String,
        val name: String = "",
        val type: String = "",
        val tameable: Boolean = false,
        val rideable: Boolean = false,
        val breedable: Boolean = false,
        val categories: List<String> = emptyList()
    )

    private val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Volatile
    private var categoriesByMob: Map<String, List<String>> = emptyMap()
    /** Defaults contributed by optional integrations. World-specific choices always take precedence. */
    private val registeredDefaults = ConcurrentHashMap<String, List<String>>()

    fun registerHooks() {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            runCatching { reload(server) }
                .onFailure { EnchLogger.error("Impossibile caricare le categorie dei mob", it) }
        }
        ServerLifecycleEvents.SERVER_STOPPING.register {
            categoriesByMob = emptyMap()
        }
    }

    /** Reloads world config and resolves legacy unqualified names as minecraft entity IDs. */
    fun reload(server: MinecraftServer) {
        val jsonFile = jsonConfigFile(server)
        val propertiesFile = configFile(server)
        Files.createDirectories(propertiesFile.parent)
        val file = if (Files.exists(jsonFile)) jsonFile else propertiesFile
        if (file == propertiesFile) {
            installDefaultIfMissing(file)
            mergeBundledDefaults(file)
        }

        val loaded = if (file == jsonFile) {
            jsonFormat.decodeFromString<JsonDocument>(Files.readString(file, StandardCharsets.UTF_8))
                .mobs.associateTo(linkedMapOf()) { mob ->
                    val mobId = normalizeMobId(mob.id)
                        ?: throw IllegalArgumentException("ID mob non valido nel file $JSON_FILE_NAME: ${mob.id}")
                    mobId to normalizeCategories(mob.categories)
                }
        } else {
            linkedMapOf<String, List<String>>().also { parsed ->
                Files.readAllLines(file, StandardCharsets.UTF_8).forEachIndexed { index, raw ->
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
                    val separator = line.indexOf('=')
                    if (separator <= 0) {
                        EnchLogger.warn("Riga ${index + 1} non valida in $FILE_NAME")
                        return@forEachIndexed
                    }
                    val mobId = normalizeMobId(line.substring(0, separator))
                    if (mobId == null) {
                        EnchLogger.warn("ID mob non valido alla riga ${index + 1} di $FILE_NAME")
                        return@forEachIndexed
                    }
                    val categories = line.substring(separator + 1)
                        .split(',')
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                        .distinct()
                    parsed[mobId] = categories
                }
            }
        }
        val missingAssignments = registeredDefaults.filterKeys { it !in loaded }
        registeredDefaults.forEach { (id, categories) -> loaded.putIfAbsent(id, categories) }
        categoriesByMob = loaded.toMap()
        persistRegisteredPresets(server)
        missingAssignments.forEach { (id, categories) -> setCategories(server, id, categories) }
        EnchLogger.info("Caricate categorie per ${loaded.size} tipi di entità da $file")
    }

    /** Adds mod-provided mob defaults without replacing user assignments in a world config. */
    fun registerDefaults(defaults: Map<String, List<String>>): Int {
        var registered = 0
        defaults.toSortedMap().forEach { (rawId, categories) ->
            val id = normalizeMobId(rawId)
            if (id == null) {
                EnchLogger.warn("API: ignorato ID mob non valido: $rawId")
                return@forEach
            }
            registeredDefaults.putIfAbsent(id, normalizeCategories(categories))
            registered++
        }
        // Makes registrations visible immediately for the active world while preserving its overrides.
        val merged = categoriesByMob.toMutableMap()
        registeredDefaults.forEach { (id, categories) -> merged.putIfAbsent(id, categories) }
        categoriesByMob = merged.toMap()
        return registered
    }

    /** Categories assigned to a namespaced entity ID, e.g. `othermod:crystal_beast`. */
    fun getCategories(entityId: String): List<String> =
        normalizeMobId(entityId)?.let { categoriesByMob[it] }.orEmpty()

    /** Category keys currently present in the loaded mob catalog. */
    fun categoryNames(): List<String> = (categoriesByMob.values.flatten() + registeredDefaults.values.flatten()).distinct().sorted()

    /** Encodes this world's category catalog for clients joined to the authoritative server. */
    fun encodeSnapshot(): String = jsonFormat.encodeToString(
        JsonDocument(mobs = categoriesByMob.toSortedMap().map { (id, categories) -> JsonMob(id = id, categories = categories) })
    )

    /** Applies a server-sent world snapshot on a client without touching client disk. */
    fun applyClientSnapshot(snapshot: String) {
        require(snapshot.length <= 1_000_000) { "Mob category snapshot is too large" }
        val document = jsonFormat.decodeFromString<JsonDocument>(snapshot)
        categoriesByMob = document.mobs.associate { mob ->
            val id = normalizeMobId(mob.id) ?: throw IllegalArgumentException("Invalid mob ID: ${mob.id}")
            id to normalizeCategories(mob.categories)
        }
    }

    fun clearLoadedWorld() {
        categoriesByMob = emptyMap()
    }

    /** Categories assigned to an entity instance, including entity types from other mods. */
    fun getCategories(server: MinecraftServer, entity: Entity): List<String> =
        getCategories(server, entity.type)

    /** Categories assigned to an entity type, including types registered by other mods. */
    fun getCategories(server: MinecraftServer, entityType: EntityType<*>): List<String> {
        val entityId = entityRegistry(server).getKey(entityType)?.toString() ?: return emptyList()
        return getCategories(entityId)
    }

    fun hasCategory(server: MinecraftServer, entity: Entity, category: String): Boolean =
        category.equals("all", ignoreCase = true) ||
                getCategories(server, entity).any { it.equals(category, ignoreCase = true) }

    /** Returns registry IDs known by this server, optionally filtered by namespace. */
    fun registeredEntityIds(server: MinecraftServer, namespace: String? = null): List<String> =
        entityRegistry(server).keySet()
            .asSequence()
            .map(Identifier::toString)
            .filter { namespace == null || it.substringBefore(':') == namespace }
            .sorted()
            .toList()

    /** Saves assignments in the world config and makes them available immediately. */
    fun setCategories(server: MinecraftServer, entityId: String, categories: List<String>) {
        val normalizedId = normalizeMobId(entityId)
            ?: throw IllegalArgumentException("ID entità non valido: $entityId")
        val normalizedCategories = normalizeCategories(categories)
        val jsonFile = jsonConfigFile(server)
        if (Files.exists(jsonFile)) {
            val document = jsonFormat.decodeFromString<JsonDocument>(Files.readString(jsonFile, StandardCharsets.UTF_8))
            val updatedMob = document.mobs.find { normalizeMobId(it.id) == normalizedId }
                ?.copy(categories = normalizedCategories)
                ?: JsonMob(id = normalizedId, categories = normalizedCategories)
            val updatedDocument = document.copy(mobs = document.mobs.filterNot { normalizeMobId(it.id) == normalizedId } + updatedMob)
            Files.writeString(
                jsonFile,
                jsonFormat.encodeToString(updatedDocument) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )
        } else {
            val file = configFile(server)
            Files.createDirectories(file.parent)
            installDefaultIfMissing(file)
            val lines = Files.readAllLines(file, StandardCharsets.UTF_8)
            val retained = lines.filterNot { raw ->
                val line = raw.trim()
                val separator = line.indexOf('=')
                separator > 0 && normalizeMobId(line.substring(0, separator)) == normalizedId
            }.toMutableList()
            retained += "$normalizedId=${normalizedCategories.joinToString(",")}"
            Files.writeString(
                file,
                retained.joinToString(System.lineSeparator()) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )
        }

        categoriesByMob = categoriesByMob + (normalizedId to normalizedCategories)
        ConfigSyncNetworking.broadcastCurrent()
    }

    private fun entityRegistry(server: MinecraftServer) =
        server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE)

    private fun configFile(server: MinecraftServer): Path =
        ConfigManager.worldConfigDir(server).resolve(FILE_NAME)

    private fun jsonConfigFile(server: MinecraftServer): Path =
        ConfigManager.worldConfigDir(server).resolve(JSON_FILE_NAME)

    private fun persistRegisteredPresets(server: MinecraftServer) {
        val file = ConfigManager.worldConfigDir(server).resolve("MobCategory.defaults.config")
        if (!Files.exists(file)) return
        val lines = Files.readAllLines(file, StandardCharsets.UTF_8).toMutableList()
        val existing = lines.mapNotNull { raw ->
            val line = raw.trim()
            val separator = line.indexOf('=')
            if (line.isEmpty() || line.startsWith("#") || separator <= 0) null else normalizeMobId(line.substring(0, separator))
        }.toMutableSet()
        registeredDefaults.toSortedMap().forEach { (id, categories) ->
            if (existing.add(id)) lines += "$id=${categories.joinToString(",")}"
        }
        Files.writeString(file, lines.joinToString(System.lineSeparator()) + System.lineSeparator(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
    }

    private fun normalizeCategories(categories: List<String>): List<String> {
        val normalized = categories.map { it.trim().lowercase(Locale.ROOT) }
            .filter(String::isNotEmpty).distinct()
        require(normalized.all { it.matches(Regex("^[a-z0-9_.:-]+$")) }) {
            "Le categorie possono contenere solo lettere, numeri, _, -, . e :"
        }
        return normalized
    }

    private fun installDefaultIfMissing(file: Path) {
        if (Files.exists(file)) return
        val resource = MobCategoryManager::class.java.classLoader
            .getResourceAsStream("config/$FILE_NAME")
        if (resource != null) {
            resource.use { Files.copy(it, file) }
        } else {
            Files.writeString(
                file,
                "# Entity registry ID=category1,category2${System.lineSeparator()}# othermod:crystal_beast=magical,boss${System.lineSeparator()}",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW
            )
        }
    }

    /** Adds newly shipped defaults to existing world configs without overwriting user choices. */
    private fun mergeBundledDefaults(file: Path) {
        val resource = MobCategoryManager::class.java.classLoader
            .getResourceAsStream("config/$FILE_NAME") ?: return
        val existingIds = Files.readAllLines(file, StandardCharsets.UTF_8)
            .mapNotNull { raw ->
                val line = raw.trim()
                val separator = line.indexOf('=')
                if (line.isEmpty() || line.startsWith("#") || separator <= 0) null
                else normalizeMobId(line.substring(0, separator))
            }
            .toMutableSet()
        val missingDefaults = resource.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.filter { raw ->
                val line = raw.trim()
                val separator = line.indexOf('=')
                if (line.isEmpty() || line.startsWith("#") || separator <= 0) return@filter false
                val id = normalizeMobId(line.substring(0, separator)) ?: return@filter false
                existingIds.add(id)
            }.toList()
        }
        if (missingDefaults.isNotEmpty()) {
            Files.writeString(
                file,
                System.lineSeparator() + missingDefaults.joinToString(System.lineSeparator()) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND
            )
        }
    }

    private fun normalizeMobId(raw: String): String? {
        val value = raw.trim()
        val path = if (':' in value) value else {
            val legacyPath = when (value.lowercase(Locale.ROOT)) {
                "dog" -> "wolf"
                "ghastling" -> "happy_ghast"
                "ravanger" -> "ravager"
                "wither boss" -> "wither"
                "zombified villager" -> "zombie_villager"
                else -> value.lowercase(Locale.ROOT).replace(' ', '_')
            }
            "minecraft:$legacyPath"
        }
        return Identifier.tryParse(path)?.toString()
    }
}
