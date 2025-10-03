package goldenrose01.enchlib.config

import goldenrose01.enchlib.utils.EnchLogger
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer

/**
 * GlobalConfigManager
 *
 * - Usa SOLO ~/.minecraft/config/enchlib (tramite GlobalConfigIO).
 * - API per comandi e runtime:
 *     - reloadConfigs()
 *     - validateAgainstRegistry(server): List<String>
 *     - addOrUpdateEnchantment(id, level)
 *     - getCurrentComponent()
 */
object GlobalConfigManager {

    private const val FILE_AVAILABLE   = "AviableEnch.config"
    private const val FILE_LVL_MAX     = "EnchLVLmax.config"
    private const val FILE_RARITY      = "EnchRarity.config"
    private const val FILE_COMPAT      = "EnchCompatibility.config"
    private const val FILE_CATEGORIES  = "EnchCategories.config"
    private const val FILE_UNCOMPAT    = "EnchUncompatibility.config"

    /** Ricarica (assicurando la presenza dei template). */
    fun reloadConfigs() {
        GlobalConfigBootstrap.ensureDefaultsInstalled()
        EnchLogger.info("GlobalConfigManager: ricaricate configurazioni da ${GlobalConfigIO.baseDir()}")
    }

    /** Confronta config vs registry runtime degli enchant.
     * Valida che tutti gli enchantment presenti in config siano nel registry (e viceversa segnala extra).
     * Ritorna una lista di stringhe descrittive dei problemi (vuota se ok).
     */
    fun validateAgainstRegistry(server: MinecraftServer): List<String> {
        val issues = mutableListOf<String>()

        val cfgIds: Set<String> = GlobalConfigIO.readAvailable()
            .map { it.id }
            .toSet()

        val reg = server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        val registryIds: Set<String> = reg.ids.map { it.toString() }.toSet()

        val missingInRegistry = cfgIds - registryIds
        val notConfigured = registryIds - cfgIds

        if (missingInRegistry.isNotEmpty()) {
            issues += "Presenti in config ma non nel registry: ${missingInRegistry.size}"
            missingInRegistry.sorted().forEach { issues += " - $it" }
        }
        if (notConfigured.isNotEmpty()) {
            issues += "Presenti nel registry ma non in config: ${notConfigured.size}"
            notConfigured.sorted().forEach { issues += " - $it" }
        }

        return issues
    }

    /**
     * Abilita/upserta un incantesimo e imposta il max level (>=1).
     * Scrive entrambe le strutture: AvailableEnch + EnchantmentsDetails.
     */
    fun addOrUpdateEnchantment(id: String, level: Int) {
        // available
        val available = GlobalConfigIO.readAvailable().toMutableList()
        val i = available.indexOfFirst { it.id == id }
        if (i >= 0) available[i] = GlobalConfigIO.AvailableEnch(id, true)
        else available += GlobalConfigIO.AvailableEnch(id, true)
        available.sortBy { it.id }
        GlobalConfigIO.writeAvailable(available)

        // details
        val root = GlobalConfigIO.readDetails()
        val j = root.enchantments.indexOfFirst { it.id == id }
        val lvl = level.coerceAtLeast(1)
        if (j >= 0) root.enchantments[j].max_level = lvl
        else root.enchantments += GlobalConfigIO.EnchantmentDetails(id = id, max_level = lvl)
        root.enchantments.sortBy { it.id }
        GlobalConfigIO.writeDetails(root)

        EnchLogger.info("GlobalConfigManager: aggiornato $id (enabled=true, max_level=$lvl)")
    }

    /** Vista compat per /plusec list.
    * Espone una “vista” della configurazione corrente per /plusec list.
    * Mostra solo le voci abilitate; il livello è l’override se presente, altrimenti 1.
    */
    fun getCurrentComponent(): CurrentComponent {
        val enabledIds = GlobalConfigIO.readAvailable()
            .filter { it.enabled }
            .map { it.id }
            .toSet()

        val levels = GlobalConfigIO.readDetails()
            .enchantments
            .associate { it.id to it.max_level }

        val entries = enabledIds.sorted().map { id ->
            val lvl = levels[id] ?: 1
            EnchantmentEntry(EnchRef(id), lvl)
        }
        return CurrentComponent(entries)
    }

    // ---- tipi wrapper per /plusec list (compat firme esistenti) ----

    data class EnchRef(val idAsString: String)
    data class EnchantmentEntry(val key: EnchRef, val intValue: Int)
    class CurrentComponent(private val entries: List<EnchantmentEntry>) {
        fun getSize(): Int = entries.size
        fun getEnchantmentEntries(): List<EnchantmentEntry> = entries
    }
}
