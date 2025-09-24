package goldenrose01.enchlib.config

import goldenrose01.enchlib.utils.EnchLogger
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

object WorldConfigBootstrap {

    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            try {
                EnchLogger.info("Starting world configuration bootstrap...")

                // Inizializza il WorldConfigManager per questo server/mondo
                val worldConfigManager = WorldConfigManager.getInstance(server)
                worldConfigManager.initializeWorldConfigs()

                EnchLogger.info("World configuration bootstrap completed successfully")

            } catch (e: Exception) {
                EnchLogger.error("Failed to bootstrap world configurations", e)
            }
        }

        ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
            // Pulisci l'istanza quando il server si ferma
            WorldConfigManager.clearInstance()
        }
    }
}
