package goldenrose01.enchlib

import net.fabricmc.api.ClientModInitializer
import goldenrose01.enchlib.utils.EnchLogger
import goldenrose01.enchlib.config.GlobalConfigIO
import goldenrose01.enchlib.config.MobCategoryManager
import goldenrose01.enchlib.network.ConfigSyncNetworking
import goldenrose01.enchlib.network.WorldConfigSyncPayload
import goldenrose01.enchlib.network.WorldConfigUpdatePayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object EnchlibClient : ClientModInitializer {
    override fun onInitializeClient() {
        EnchLogger.info("🖥️ Inizializzazione client EnchLib")
        ClientPlayNetworking.registerGlobalReceiver(WorldConfigSyncPayload.TYPE) { payload, context ->
            context.client().execute {
                runCatching {
                    GlobalConfigIO.applyClientSnapshot(payload.available, payload.details)
                    MobCategoryManager.applyClientSnapshot(payload.mobs)
                }.onFailure { EnchLogger.warn("Ricevuta configurazione EnchLib non valida dal server: ${it.message}") }
            }
        }
    }

    @JvmStatic
    fun sendConfigUpdate() {
        val connection = net.minecraft.client.Minecraft.getInstance().connection ?: return
        val (available, details) = GlobalConfigIO.encodeSnapshot()
        ClientPlayNetworking.send(WorldConfigUpdatePayload(available, details))
    }
}
