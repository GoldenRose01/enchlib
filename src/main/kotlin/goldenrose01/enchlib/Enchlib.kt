package goldenrose01.enchlib

import net.fabricmc.api.ModInitializer
import goldenrose01.enchlib.commands.EnchLibCommands
import goldenrose01.enchlib.item.EnchItemGroup
import goldenrose01.enchlib.utils.EnchLogger

object Enchlib : ModInitializer {
    const val MOD_ID = "enchlib"
    override fun onInitialize() {
        EnchLogger.info("🚀 EnchLib inizializzazione (main)")
        // Tab creativa opzionale
        EnchItemGroup.register()
        // Comandi
        EnchLibCommands.registerCommands()
        EnchLogger.info("✅ EnchLib caricato")
    }
}