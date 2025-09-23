package goldenrose01.enchlib

import goldenrose01.enchlib.config.ConfigManager
import goldenrose01.enchlib.registry.EnchantmentRegistry
import goldenrose01.enchlib.commands.EnchLibCommands
import goldenrose01.enchlib.item.EnchItemGroup
import goldenrose01.enchlib.api.EnchLibAPI
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

/**
 * Entry point della mod EnchLib. Gestisce il caricamento di configurazioni,
 * registri, tab creativa, API e comandi.
 */
object Enchlib : ModInitializer {
    private val LOGGER = LoggerFactory.getLogger("EnchLib")

    override fun onInitialize() {
        LOGGER.info("📦 Inizializzazione EnchLib - Libreria avanzata per incantesimi")

        // Caricamento configurazioni
        ConfigManager.loadAll()

        // Inizializzazione registry di tutti gli incantesimi vanilla
        EnchantmentRegistry.initialize()

        // Registrazione della tab creativa
        EnchItemGroup.register()

        // Inizializza l'API pubblica
        EnchLibAPI.initialize()

        // Registra i comandi /plusec
        EnchLibCommands.registerCommands()

        LOGGER.info("✅ EnchLib inizializzata con successo!")
    }
}
