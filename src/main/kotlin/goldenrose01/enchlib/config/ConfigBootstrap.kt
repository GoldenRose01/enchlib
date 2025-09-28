package goldenrose01.enchlib.config

import goldenrose01.enchlib.utils.EnchLogger
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier

/**
 * Aggancia il bootstrap di configurazione al ciclo vita del server.
 * Si limita a garantire che i file di config esistano nella cartella del mondo
 * e ad effettuare un primo merge/ensure.
 */

object ConfigBootstrap {

    // Chiave del registry enchantments
    private val ENCH_REGISTRY_KEY: RegistryKey<Registry<Enchantment>> = RegistryKeys.ENCHANTMENT

    /** Registra l’hook SERVER_STARTED una sola volta. */
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
        // Assicura che i file nel mondo esistano (copiandoli da /resources/config se mancano)
        ConfigManager.reloadCoreFilesIfNeeded(server)

        val registry = server.registryManager.getOrThrow(ENCH_REGISTRY_KEY)
        val allIds: List<Identifier> = registry.ids.toList()

        var added = 0
        for (id in allIds) {
            val idStr = id.toString()

            val presentEverywhere =
                ConfigManager.existsInAvailable(server, idStr) &&
                        ConfigManager.existsInLvlMax(server, idStr) &&
                        ConfigManager.existsInRarity(server, idStr) &&
                        ConfigManager.existsInCompat(server, idStr) &&
                        ConfigManager.existsInCategories(server, idStr) &&
                        ConfigManager.existsInUncompat(server, idStr)

            if (presentEverywhere) continue

            // Available booleano default=true
            ConfigManager.ensureAvailable(server, idStr, enabled = true)
            // Non forziamo livello max: lascia vuoto, utente può override
            ConfigManager.ensureLvlMax(server, idStr, null)
            ConfigManager.ensureRarity(server, idStr, "common")
            ConfigManager.ensureCompat(server, idStr, emptyList())
            ConfigManager.ensureCategories(server, idStr, emptyList())
            ConfigManager.ensureUncompat(server, idStr, emptyList())
            added++
        }

        if (added > 0) {
            EnchLogger.info("🔧 Aggiunte/aggiornate $added voci nei .config del mondo")
        }

        // Carica in memoria lo stato corrente
        ConfigManager.loadAll(server)
    }
}
