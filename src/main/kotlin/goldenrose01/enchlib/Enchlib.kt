package goldenrose01.enchlib

import goldenrose01.enchlib.commands.DebugCommands
import goldenrose01.enchlib.commands.EnchLibCommands
import goldenrose01.enchlib.config.WorldConfigBootstrap
import goldenrose01.enchlib.config.WorldConfigManager
import goldenrose01.enchlib.item.EnchItemGroup
import goldenrose01.enchlib.utils.EnchLogger
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

class Enchlib : ModInitializer {
    private val logger = LoggerFactory.getLogger("EnchLib")

    companion object {
        const val MOD_ID = "enchlib"
    }

    override fun onInitialize() {
        logger.info("EnchLib initializing...")
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted)

        // Registra il bootstrap per le configurazioni world-based
        WorldConfigBootstrap.register()

        // Registra creative tab
        EnchItemGroup.register()

        // Registra comandi
        EnchLibCommands.register()
        DebugCommands.register()

        EnchLogger.info("EnchLib initialization completed!")
    }

    private fun onServerStarted(server: MinecraftServer) {
        logger.info("Server started, initializing world configs...")
        WorldConfigManager.getInstance(server).initializeWorldConfigs()

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