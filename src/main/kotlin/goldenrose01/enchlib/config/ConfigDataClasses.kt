package goldenrose01.enchlib.config.data

import kotlinx.serialization.Serializable

@Serializable
data class AvailableEnchantmentsConfig(
    val enchantments: MutableList<EnchantmentStatusEntry> = mutableListOf()
)

@Serializable
data class EnchantmentStatusEntry(
    val id: String,
    val enabled: Boolean
)

@Serializable
data class EnchantmentDetailsConfig(
    val enchantments: MutableList<DetailedEnchantmentEntry> = mutableListOf()
)

@Serializable
data class DetailedEnchantmentEntry(
    val id: String,
    val name: String,
    val max_level: Int,
    val applicable_to: List<String>,
    val description: String,
    val enc_category: List<String>,
    val mob_category: List<String>,
    val rarity: String,
    val levels: List<EnchantmentLevelData>
)

@Serializable
data class EnchantmentLevelData(
    val level: Int,
    val extra_damage: Double? = null,
    val speed_multiplier: Double? = null,
    val extra_damage_vs_category: Double? = null,
    // Aggiungi altri parametri specifici qui
    val durability_bonus: Int? = null,
    val efficiency_multiplier: Double? = null
)

@Serializable
data class MobCategoriesConfig(
    val mobs: MutableList<MobEntry> = mutableListOf()
)

@Serializable
data class MobEntry(
    val name: String,
    val type: String,
    val categories: List<String>
)

@Serializable
data class IncompatibilityRules(
    val incompatible_pairs: MutableList<IncompatiblePair> = mutableListOf(),
    val category_limits: MutableMap<String, Int> = mutableMapOf()
)

@Serializable
data class IncompatiblePair(
    val enchantment1: String,
    val enchantment2: String
)
