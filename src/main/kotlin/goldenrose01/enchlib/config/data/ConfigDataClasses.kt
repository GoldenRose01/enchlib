package goldenrose01.enchlib.config.data

import kotlinx.serialization.Serializable

@Serializable
data class AvailableEnchantmentsConfig(
    val enchantments: List<EnchantmentStatusEntry>
)

@Serializable
data class EnchantmentStatusEntry(
    val id: String,
    val enabled: Boolean
)

@Serializable
data class EnchantmentDetailsConfig(
    val enchantments: List<DetailedEnchantmentEntry>
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
    val extra_damage_vs_category: Double? = null
)

@Serializable
data class IncompatibilityRules(
    val incompatible_pairs: List<IncompatiblePair>,
    val category_limits: Map<String, Int>
)

@Serializable
data class IncompatiblePair(
    val enchantment1: String,
    val enchantment2: String
)

@Serializable
data class MobCategoriesConfig(
    val mobs: List<MobEntry>
)

@Serializable
data class MobEntry(
    val name: String,
    val type: String,
    val categories: List<String>
)
