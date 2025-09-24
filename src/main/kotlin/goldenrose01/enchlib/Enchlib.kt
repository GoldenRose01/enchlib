package goldenrose01.enchlib

import goldenrose01.enchlib.commands.DebugCommands
import goldenrose01.enchlib.commands.EnchLibCommands
import goldenrose01.enchlib.config.WorldConfigBootstrap
import goldenrose01.enchlib.item.EnchItemGroup
import goldenrose01.enchlib.utils.EnchLogger
import net.fabricmc.api.ModInitializer

class Enchlib : ModInitializer {

    companion object {
        const val MOD_ID = "enchlib"
    }

    override fun onInitialize() {
        EnchLogger.info("Initializing EnchLib...")

        // Registra il bootstrap per le configurazioni world-based
        WorldConfigBootstrap.register()

        // Registra creative tab
        EnchItemGroup.register()

        // Registra comandi
        EnchLibCommands.register()
        DebugCommands.register()

        EnchLogger.info("EnchLib initialization completed!")
    }
}