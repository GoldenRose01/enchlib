package goldenrose01.enchlib.config

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import goldenrose01.enchlib.utils.EnchLogger

object ConfigBootstrap {

    // Chiave del registry enchantment costruita a runtime
    private val ENCH_REGISTRY_KEY: RegistryKey<Registry<Enchantment>> =
        RegistryKey.ofRegistry(Identifier.of("minecraft", "enchantment")) // [docs: RegistryKey.ofRegistry][web:145]

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
        // ricarica i file base (preserva commenti)
        ConfigManager.reloadCoreFilesIfNeeded()

        val registry = server.registryManager.get(ENCH_REGISTRY_KEY)
        // Alcuni mapping espongono getIds() anziché una property; usa il getter
        val allIds: List<Identifier> = registry.getIds().toList()

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

            // Default: attivo con fonti standard; se l’utente lo ha messo in DisabledEnch, scrivi "=disabled"
            val defaultSources = "enchanting_table,chest_loot,villager_trade"
            val lineAvailable = if (disabled.contains(idStr)) "$idStr=disabled" else "$idStr=$defaultSources"

            // Assicura presenza in ogni file, senza toccare commenti esistenti
            ConfigManager.ensureAvailable(idStr, lineAvailable)
            // Non imponiamo un livello max nel file: il runtime fornirà il valore
            ConfigManager.ensureLvlMax(idStr, null)
            ConfigManager.ensureRarity(idStr, "common")
            ConfigManager.ensureCompat(idStr, emptyList())
            ConfigManager.ensureCategories(idStr, emptyList())
            ConfigManager.ensureUncompat(idStr, emptyList())

            added++
        }

        if (added > 0) {
            EnchLogger.info("🔧 Aggiunte/aggiornate $added voci di incantesimi nei .config")
        }

        // ricarica stato completo
        ConfigManager.loadAll()
    }
}
