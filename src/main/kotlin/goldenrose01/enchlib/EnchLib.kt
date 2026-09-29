package goldenrose01.enchlib

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import net.minecraft.commands.CommandSourceStack

import com.mojang.brigadier.CommandDispatcher

import goldenrose01.enchlib.commands.DebugCommands
import goldenrose01.enchlib.commands.EnchLibCommands
import goldenrose01.enchlib.config.GlobalConfigBootstrap
import goldenrose01.enchlib.config.MobCategoryManager
import goldenrose01.enchlib.config.GlobalConfigIO
import goldenrose01.enchlib.network.ConfigSyncNetworking
import goldenrose01.enchlib.api.EnchantLibAPI
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

import org.slf4j.LoggerFactory

object Enchlib : ModInitializer {
    const val MOD_ID = "enchlib"
    private val LOGGER = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        LOGGER.info("[$MOD_ID] Initializing...")

        // Default registrations are staged here, then applied to the selected world's files.
        EnchantLibAPI.registerMaxLevelDefaults(loadIntegerDefaults("config/EnchLVLmax.config"))
        EnchantLibAPI.registerRarityDefaults(loadRarityDefaults())
        ConfigSyncNetworking.register()
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            GlobalConfigIO.openWorld(server)
            MobCategoryManager.reload(server)
            ConfigSyncNetworking.serverStarted(server)
        }
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            ConfigSyncNetworking.serverStopping(server)
            GlobalConfigIO.closeWorld(server)
            MobCategoryManager.clearLoadedWorld()
        }

        // Registrazione comandi (Fabric API v2)
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            EnchLibCommands.register(dispatcher)
            DebugCommands.register(dispatcher)
        }
    }

    private fun loadRarityDefaults(): Map<String, String> {
        val input = Enchlib::class.java.classLoader.getResourceAsStream("config/EnchRarity.config") ?: return emptyMap()
        return input.bufferedReader().useLines { lines ->
            lines.map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val split = line.indexOf('=')
                    if (split <= 0) null else line.substring(0, split).trim() to line.substring(split + 1).trim()
                }.toMap()
        }
    }

    private fun loadIntegerDefaults(resource: String): Map<String, Int> {
        val input = Enchlib::class.java.classLoader.getResourceAsStream(resource) ?: return emptyMap()
        return input.bufferedReader().useLines { lines ->
            lines.map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val split = line.indexOf('=')
                    if (split <= 0) null else line.substring(0, split).trim() to line.substring(split + 1).trim().toIntOrNull()
                }.filter { it.second != null }.associate { it.first to it.second!! }
        }
    }
}
