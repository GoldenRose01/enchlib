package goldenrose01.enchlib.api

import goldenrose01.enchlib.config.GlobalConfigIO
import goldenrose01.enchlib.config.MobCategoryManager
import goldenrose01.enchlib.utils.EnchLogger
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Entity
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap

/**
 * EnchantLibAPI
 *
 * API minimale e stabile per accedere/modificare la configurazione del mondo attivo
 * (cartella: <mondo>/config/enchlib).
 *
 * - getEnabledEnchantments(): List<String>
 * - isEnabled(id): Boolean
 * - getMaxLevel(id): Int?
 * - setEnabled(id, enabled)
 * - setMaxLevel(id, level)   (level >= 1)
 * - save()                    (flush esplicito opzionale)
 *
 * Nota: le funzioni scrivono subito su disco per semplicità e atomicità.
 */
object EnchantLibAPI {

    private data class TableInfo(val id: String, val name: String)
    private val enchantingTables = ConcurrentHashMap<String, TableInfo>()

    /** Registers an enchanting table exposed by this mod (for example a modded table). */
    @JvmStatic
    fun registerEnchantingTable(id: String, displayName: String) {
        require(id.matches(Regex("^[a-z0-9_.-]+:[a-z0-9_./-]+$"))) { "Invalid table ID: $id" }
        enchantingTables[id] = TableInfo(id, displayName)
    }

    /** Seeds a table's initial values without replacing choices already made in EnchLib. */
    @JvmStatic
    fun registerEnchantingTableDefaults(
        id: String,
        displayName: String,
        enabledDefaults: Map<String, Boolean>,
        chanceDefaults: Map<String, Int>
    ) {
        registerEnchantingTable(id, displayName)
        val root = GlobalConfigIO.readDetails()
        var changed = false
        for (enchantmentId in (enabledDefaults.keys + chanceDefaults.keys).distinct()) {
            val detail = upsertDetail(root, enchantmentId)
            if (id !in detail.table_rules) {
                detail.table_rules[id] = GlobalConfigIO.EnchantingTableRule(
                    enabled = enabledDefaults[enchantmentId] ?: true,
                    chance = (chanceDefaults[enchantmentId] ?: 100).coerceIn(0, 100)
                )
                changed = true
            }
        }
        if (changed) GlobalConfigIO.writeDetails(root)
    }

    /** Registered table IDs in stable display order. */
    @JvmStatic
    fun getEnchantingTables(): Map<String, String> =
        (mapOf("minecraft:enchanting_table" to "Enchanting Table") +
                enchantingTables.mapValues { it.value.name }).toSortedMap()

    /** Nullable when no EnchLib override exists, allowing the registering mod to use its own preset. */
    @JvmStatic
    fun getTableEnabled(enchantmentId: String, tableId: String): Boolean? =
        detail(enchantmentId)?.table_rules?.get(tableId)?.enabled

    @JvmStatic
    fun setTableEnabled(enchantmentId: String, tableId: String, enabled: Boolean) {
        val root = GlobalConfigIO.readDetails()
        val d = upsertDetail(root, enchantmentId)
        val rule = d.table_rules.getOrPut(tableId) { GlobalConfigIO.EnchantingTableRule() }
        rule.enabled = enabled
        GlobalConfigIO.writeDetails(root)
    }

    @JvmStatic
    fun getTableChance(enchantmentId: String, tableId: String): Int? =
        detail(enchantmentId)?.table_rules?.get(tableId)?.chance

    @JvmStatic
    fun setTableChance(enchantmentId: String, tableId: String, chance: Int) {
        val root = GlobalConfigIO.readDetails()
        val d = upsertDetail(root, enchantmentId)
        val rule = d.table_rules.getOrPut(tableId) { GlobalConfigIO.EnchantingTableRule() }
        rule.chance = chance.coerceIn(0, 100)
        GlobalConfigIO.writeDetails(root)
    }

    @JvmStatic
    fun resetTableRule(enchantmentId: String, tableId: String) {
        val root = GlobalConfigIO.readDetails()
        root.enchantments.find { it.id == enchantmentId }?.table_rules?.remove(tableId)
        GlobalConfigIO.writeDetails(root)
    }

    @JvmStatic
    fun getRarity(enchantmentId: String): String = detail(enchantmentId)?.rarity ?: "common"

    @JvmStatic
    fun setRarity(enchantmentId: String, rarity: String) {
        require(rarity in listOf("common", "uncommon", "rare", "very_rare"))
        val root = GlobalConfigIO.readDetails()
        upsertDetail(root, enchantmentId).rarity = rarity
        GlobalConfigIO.writeDetails(root)
    }

    @JvmStatic
    fun getIncompatibleEnchantments(enchantmentId: String): List<String> =
        detail(enchantmentId)?.incompatible_enchantments.orEmpty()

    /** Stores symmetric incompatibility so either enchantment blocks the other. */
    @JvmStatic
    fun setIncompatibleEnchantments(enchantmentId: String, incompatible: List<String>) {
        val root = GlobalConfigIO.readDetails()
        val selected = root.enchantments.find { it.id == enchantmentId }
            ?: GlobalConfigIO.EnchantmentDetails(enchantmentId).also(root.enchantments::add)
        val old = selected.incompatible_enchantments.toSet()
        val next = incompatible.distinct().filter { it != enchantmentId }.toSet()
        for (otherId in old - next) {
            root.enchantments.find { it.id == otherId }?.incompatible_enchantments?.remove(enchantmentId)
        }
        for (otherId in next) {
            val other = root.enchantments.find { it.id == otherId }
                ?: GlobalConfigIO.EnchantmentDetails(otherId).also(root.enchantments::add)
            if (enchantmentId !in other.incompatible_enchantments) other.incompatible_enchantments.add(enchantmentId)
        }
        selected.incompatible_enchantments = next.sorted().toMutableList()
        GlobalConfigIO.writeDetails(root)
    }

    @JvmStatic
    fun resetEnchantmentDetails(enchantmentId: String) {
        val root = GlobalConfigIO.readDetails()
        root.enchantments.removeIf { it.id == enchantmentId }
        root.enchantments.forEach { it.incompatible_enchantments.remove(enchantmentId) }
        GlobalConfigIO.writeDetails(root)
    }

    @JvmStatic
    fun getItemCategories(enchantmentId: String): List<String> = detail(enchantmentId)?.enc_category.orEmpty()

    @JvmStatic
    fun getPresetItemCategories(enchantmentId: String): List<String> = detail(enchantmentId)?.preset_enc_category.orEmpty()

    @JvmStatic
    fun getPresetMaxLevel(enchantmentId: String): Int? = detail(enchantmentId)?.preset_max_level

    @JvmStatic
    fun getPresetRarity(enchantmentId: String): String? = detail(enchantmentId)?.preset_rarity

    @JvmStatic
    fun registerMaxLevelDefaults(defaults: Map<String, Int>): Int {
        val root = GlobalConfigIO.readDetails()
        var count = 0
        defaults.toSortedMap().forEach { (id, maxLevel) ->
            val level = maxLevel.coerceAtLeast(1)
            val detail = root.enchantments.firstOrNull { it.id == id }
                ?: GlobalConfigIO.EnchantmentDetails(id = id, max_level = level).also(root.enchantments::add)
            if (detail.preset_max_level == null) {
                // First registration establishes both the reset value and the world's initial value.
                detail.preset_max_level = level
                detail.max_level = level
            }
            count++
        }
        GlobalConfigIO.writeDetails(root)
        return count
    }

    /** Imports the bundled rarity preset once while preserving explicit user choices. */
    @JvmStatic
    fun registerRarityDefaults(defaults: Map<String, String>): Int {
        val root = GlobalConfigIO.readDetails()
        var count = 0
        defaults.toSortedMap().forEach { (id, rarity) ->
            if (rarity !in listOf("common", "uncommon", "rare", "very_rare")) return@forEach
            val detail = upsertDetail(root, id)
            if (detail.preset_rarity == null) detail.preset_rarity = rarity
            if (detail.rarity == null) detail.rarity = rarity
            count++
        }
        GlobalConfigIO.writeDetails(root)
        return count
    }

    @JvmStatic
    fun setItemCategories(enchantmentId: String, categories: List<String>) {
        val root = GlobalConfigIO.readDetails()
        upsertDetail(root, enchantmentId).enc_category = categories.distinct().sorted().toMutableList()
        GlobalConfigIO.writeDetails(root)
    }

    @JvmStatic
    fun getMobCategories(): List<String> = MobCategoryManager.categoryNames()

    /** Registers default categories for modded mob types; existing per-world assignments win. */
    @JvmStatic
    fun registerMobCategories(defaults: Map<String, List<String>>): Int =
        MobCategoryManager.registerDefaults(defaults)

    /** Returns the categories configured for a mob registry ID, e.g. `othermod:crystal_beast`. */
    @JvmStatic
    fun getMobCategories(entityId: String): List<String> = MobCategoryManager.getCategories(entityId)

    /** Returns the categories configured for an entity instance in this world. */
    @JvmStatic
    fun getMobCategories(server: MinecraftServer, entity: Entity): List<String> =
        MobCategoryManager.getCategories(server, entity)

    /** Writes a per-world mob assignment and applies it immediately. */
    @JvmStatic
    fun setMobCategories(server: MinecraftServer, entityId: String, categories: List<String>) =
        MobCategoryManager.setCategories(server, entityId, categories)

    private fun detail(id: String) = GlobalConfigIO.readDetails().enchantments.firstOrNull { it.id == id }

    private fun upsertDetail(root: GlobalConfigIO.EnchantmentsDetailsRoot, id: String): GlobalConfigIO.EnchantmentDetails {
        return root.enchantments.firstOrNull { it.id == id }
            ?: GlobalConfigIO.EnchantmentDetails(id).also(root.enchantments::add)
    }

    /** Checks whether an entity has a world-configured mob category. */
    @JvmStatic
    fun hasMobCategory(server: MinecraftServer, entity: Entity, category: String): Boolean =
        MobCategoryManager.hasCategory(server, entity, category)

    /**
     * Importa in batch i metadati predefiniti forniti da un'altra mod.
     * Gli ID devono essere completi (namespace:path). Le voci già configurate
     * mantengono abilitazione, livello massimo e categorie scelti dall'utente.
     */
    @JvmStatic
    fun registerEnchantments(maxLevels: Map<String, Int>, enchantmentCategories: Map<String, List<String>>): Int =
        registerEnchantments(maxLevels, enchantmentCategories, emptyMap())

    /** Imports item compatibility and target mob categories for a batch of enchantments. */
    @JvmStatic
    fun registerEnchantments(
        maxLevels: Map<String, Int>,
        enchantmentCategories: Map<String, List<String>>,
        mobCategories: Map<String, List<String>>
    ): Int {
        if (maxLevels.isEmpty()) return 0

        val available = GlobalConfigIO.readAvailable().toMutableList()
        val details = GlobalConfigIO.readDetails()
        val availableIds = available.map { it.id }.toMutableSet()
        val detailsById = details.enchantments.associateBy { it.id }
        var registered = 0

        for ((id, maxLevel) in TreeMap(maxLevels)) {
            if (!id.matches(Regex("^[a-z0-9_.-]+:[a-z0-9_./-]+$"))) {
                EnchLogger.warn("API: ignorato ID incantesimo non valido: $id")
                continue
            }

            if (availableIds.add(id)) {
                // Nuova voce visibile per impostazione predefinita; le scelte esistenti restano intatte.
                available += GlobalConfigIO.AvailableEnch(id = id, enabled = true)
            }

            val categories = enchantmentCategories[id].orEmpty().distinct().sorted().toMutableList()
            val targets = mobCategories[id].orEmpty().distinct().sorted().toMutableList()
            val existing = detailsById[id]
            if (existing == null) {
                details.enchantments += GlobalConfigIO.EnchantmentDetails(
                    id = id,
                    max_level = maxLevel.coerceAtLeast(1),
                    enc_category = categories,
                    mob_category = targets,
                    preset_max_level = maxLevel.coerceAtLeast(1),
                    preset_enc_category = categories.toMutableList()
                )
            } else {
                if (existing.preset_max_level == null) {
                    val presetLevel = maxLevel.coerceAtLeast(1)
                    existing.preset_max_level = presetLevel
                    existing.max_level = presetLevel
                }
                if (existing.preset_enc_category == null) existing.preset_enc_category = categories.toMutableList()
                if (existing.enc_category.isEmpty() && categories.isNotEmpty()) {
                    existing.enc_category = categories
                }
                if (existing.mob_category.isEmpty() && targets.isNotEmpty()) {
                    existing.mob_category = targets
                }
            }
            registered++
        }

        available.sortBy { it.id }
        details.enchantments.sortBy { it.id }
        GlobalConfigIO.writeAvailable(available)
        GlobalConfigIO.writeDetails(details)
        EnchLogger.info("API: sincronizzati $registered incantesimi registrati dalle mod dipendenti.")
        return registered
    }

    /** Ritorna gli ID abilitati (ordinati). */
    @JvmStatic
    fun getEnabledEnchantments(): List<String> =
        GlobalConfigIO.readAvailable()
            .filter { it.enabled }
            .map { it.id }
            .sorted()

    /** true se l'id è presente e abilitato. */
    @JvmStatic
    fun isEnabled(id: String): Boolean =
        GlobalConfigIO.readAvailable().firstOrNull { it.id == id }?.enabled ?: true

    /** Max level configurato per l'id, se presente. */
    @JvmStatic
    fun getMaxLevel(id: String): Int? =
        GlobalConfigIO.readDetails()
            .enchantments
            .firstOrNull { it.id == id }
            ?.max_level

    /** Abilita/disabilita un incantesimo in AviableEnch.json5 (upsert). */
    @JvmStatic
    fun setEnabled(id: String, enabled: Boolean) {
        val list = GlobalConfigIO.readAvailable().toMutableList()
        val i = list.indexOfFirst { it.id == id }
        if (i >= 0) list[i] = GlobalConfigIO.AvailableEnch(id, enabled)
        else list += GlobalConfigIO.AvailableEnch(id, enabled)
        list.sortBy { it.id }
        GlobalConfigIO.writeAvailable(list)
        EnchLogger.info("EnchantLibAPI: setEnabled($id, $enabled)")
    }

    /** Imposta il max level (>=1) in EnchantmentsDetails.json5 (upsert). */
    @JvmStatic
    fun setMaxLevel(id: String, level: Int) {
        val lvl = level.coerceAtLeast(1)
        val root = GlobalConfigIO.readDetails()
        val list = root.enchantments
        val i = list.indexOfFirst { it.id == id }
        if (i >= 0) list[i].max_level = lvl
        else list += GlobalConfigIO.EnchantmentDetails(id = id, max_level = lvl)
        list.sortBy { it.id }
        GlobalConfigIO.writeDetails(root)
        EnchLogger.info("EnchantLibAPI: setMaxLevel($id, $lvl)")
    }

    /** Scrive eventuali buffer (qui i metodi scrivono già; funzione lasciata per simmetria). */
    fun save() {
        // No-op: l’API scrive immediatamente
    }
}
