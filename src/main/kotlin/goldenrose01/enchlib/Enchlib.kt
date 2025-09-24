package goldenrose01.enchlib

import net.fabricmc.api.ModInitializer
import goldenrose01.enchlib.commands.EnchLibCommands
import goldenrose01.enchlib.item.EnchItemGroup
import goldenrose01.enchlib.utils.EnchLogger
import goldenrose01.enchlib.config.ConfigManager
import goldenrose01.enchlib.config.ConfigBootstrap

object Enchlib : ModInitializer {
    const val MOD_ID = "enchlib"

    override fun onInitialize() {
        EnchLogger.info("EnchLib inizializzazione (main)") // Carica o crea i file base in config/enchlib/
        ConfigManager.loadAll() // Hook server per enumerare incantesimi (vanilla + mod) e garantirne la presenza in tutti i .config
        ConfigBootstrap.registerServerHooks() // merge e/o generazione dinamica a server start
        // UI e comandi
        EnchItemGroup.register() // Menu creativa
        EnchLibCommands.registerCommands() //Gestione Comandi
        EnchLogger.info("✅ EnchLib caricato")
    }
}