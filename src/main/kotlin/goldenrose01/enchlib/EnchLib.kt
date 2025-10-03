package goldenrose01.enchlib

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import net.minecraft.server.command.ServerCommandSource

import com.mojang.brigadier.CommandDispatcher

import goldenrose01.enchlib.commands.DebugCommands
import goldenrose01.enchlib.commands.EnchLibCommands
import goldenrose01.enchlib.config.GlobalConfigBootstrap

import org.slf4j.LoggerFactory

object Enchlib : ModInitializer {
    const val MOD_ID = "enchlib"
    private val LOGGER = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        LOGGER.info("[$MOD_ID] Initializing...")

        // World configuration
        GlobalConfigBootstrap.register()

        // Comandi (solo dispatcher: le versioni attuali di EnchLibCommands/DebugCommands
        // espongono register(dispatcher) senza registryAccess)
        CommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<ServerCommandSource>, _, _ ->
            EnchLibCommands.register(dispatcher)
            DebugCommands.register(dispatcher)
        }
    }
}