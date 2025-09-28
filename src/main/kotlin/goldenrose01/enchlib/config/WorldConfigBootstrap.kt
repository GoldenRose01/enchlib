package goldenrose01.enchlib.config

import goldenrose01.enchlib.utils.EnchLogger
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer

object WorldConfigBootstrap {

    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register { server: MinecraftServer ->
            try {
                EnchLogger.info("Starting world configuration bootstrap...")

                WorldConfigManager.reloadConfigs(server)

                EnchLogger.info("World configuration bootstrap completed successfully")

            } catch (e: Exception) {
                EnchLogger.error("Failed to bootstrap world configurations", e)
            }
        }
    }
}
