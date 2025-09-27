package goldenrose01.enchlib.config

import kotlinx.serialization.Serializable

@Serializable
data class AviableEnch(
    var enchantments: MutableList<AviableEntry> = mutableListOf()
)

@Serializable
data class AviableEntry(
    val id: String,
    val enabled: Boolean = true
)

@Serializable
data class EnchantmentDetails(
    var entries: MutableList<EnchDetail> = mutableListOf()
)

@Serializable
data class EnchDetail(
    val id: String,
    val maxLevel: Int = 1,
    val rarity: String = "common",
    val category: String = "generic",
    val multiplier: Double = 1.0
)

data class ValidateReport(
    val missingInJson: List<String>,
    val extraInJson: List<String>,
    val totals: Map<String, Int>
)

data class ReloadReport(
    val aviableCount: Int,
    val detailsCount: Int
)
