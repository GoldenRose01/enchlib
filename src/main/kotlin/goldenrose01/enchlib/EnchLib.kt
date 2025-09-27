package goldenrose01.enchlib

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import net.minecraft.server.command.ServerCommandSource

import com.mojang.brigadier.CommandDispatcher

import goldenrose01.enchlib.commands.DebugCommands
import goldenrose01.enchlib.commands.EnchLibCommands
import goldenrose01.enchlib.config.WorldConfigBootstrap

import org.slf4j.LoggerFactory

object Enchlib : ModInitializer {
    const val MOD_ID = "enchlib"
    private val LOGGER = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        LOGGER.info("[$MOD_ID] Initializing...")

        //world Configuration
        WorldConfigBootstrap.register()
        // Comandi
        CommandRegistrationCallback.EVENT.register(
            CommandRegistrationCallback { dispatcher: CommandDispatcher<ServerCommandSource>, _, _ ->
                EnchLibCommands.register(dispatcher)
                DebugCommands.register(dispatcher)
            }
        )


    }
}