package goldenrose01.enchlib.config

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import goldenrose01.enchlib.utils.EnchLogger

object ConfigBootstrap {

    // Chiave del registry enchantment costruita a runtime
    private val ENCH_REGISTRY_KEY: RegistryKey<Registry<Enchantment>> = RegistryKeys.ENCHANTMENT

    fun registerServerHooks() {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            try {
                ensureAndMergeFromRegistry(server)
            } catch (t: Throwable) {
                EnchLogger.error("Errore durante generazione/merge config incantesimi", t)
            }
        }
    }

    private fun ensureAndMergeFromRegistry(server: MinecraftServer) {
        // Assicura intestazioni + carica available/disabled per sapere cosa è off
        ConfigManager.reloadCoreFilesIfNeeded()

        // 1.21: usare getOrThrow(...)
        val registry = server.registryManager.getOrThrow(ENCH_REGISTRY_KEY)

        // 1.21: niente getIds(); iteriamo le entry e prendiamo l'Identifier dalla chiave
        val allIds: List<Identifier> = registry.entrySet.map { it.key.value }.toList()

        val disabled = ConfigManager.currentDisabledIds()
        var added = 0

        for (id in allIds) {
            val idStr = id.toString()

            val presentEverywhere =
                ConfigManager.existsInAvailable(idStr) &&
                        ConfigManager.existsInLvlMax(idStr) &&
                        ConfigManager.existsInRarity(idStr) &&
                        ConfigManager.existsInCompat(idStr) &&
                        ConfigManager.existsInCategories(idStr) &&
                        ConfigManager.existsInUncompat(idStr)

            if (presentEverywhere) continue

            // Available booleana (true/false) in AviableEnch.config
            ConfigManager.ensureAvailable(idStr, enabled = !disabled.contains(idStr))

            // Opzionali/di default
            ConfigManager.ensureLvlMax(idStr, null)              // lascia runtime
            ConfigManager.ensureRarity(idStr, "common")
            ConfigManager.ensureCompat(idStr, emptyList())
            ConfigManager.ensureCategories(idStr, emptyList())
            ConfigManager.ensureUncompat(idStr, emptyList())

            added++
        }

        if (added > 0) {
            EnchLogger.info("🔧 Aggiunte/aggiornate $added voci nei .config (available=boolean)")
        }

        // Ricarica tutto lo stato in memoria
        ConfigManager.loadAll()
    }
}
