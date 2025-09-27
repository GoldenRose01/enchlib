package goldenrose01.enchlib

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

import goldenrose01.enchlib.commands.DebugCommands
import goldenrose01.enchlib.commands.EnchLibCommands
import goldenrose01.enchlib.config.WorldConfigBootstrap
import goldenrose01.enchlib.config.WorldConfigManager
import goldenrose01.enchlib.item.EnchItemGroup
import goldenrose01.enchlib.utils.EnchLogger

import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

object EnchLib : ModInitializer {
    const val MOD_ID = "enchlib"

    override fun onInitialize() {
        EnchLibCommands.register()
        DebugCommands.register()

        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted { server: MinecraftServer ->
            WorldConfigManager.onServerStarted(server)
        })
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerLifecycleEvents.ServerStopping { _: MinecraftServer ->
            WorldConfigManager.onServerStopping()
        })
    }
}