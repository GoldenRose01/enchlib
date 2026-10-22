package goldenrose01.enchlib.network

import goldenrose01.enchlib.config.GlobalConfigIO
import goldenrose01.enchlib.config.MobCategoryManager
import goldenrose01.enchlib.utils.EnchLogger
import goldenrose01.enchlib.network.WorldConfigSyncPayload
import goldenrose01.enchlib.network.WorldConfigUpdatePayload
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.permissions.Permissions

/** Keeps world settings authoritative on the server and mirrors them to connected clients. */
object ConfigSyncNetworking {
    @Volatile private var server: MinecraftServer? = null

    fun register() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(WorldConfigSyncPayload.TYPE, WorldConfigSyncPayload.CODEC, 5_000_000)
        PayloadTypeRegistry.serverboundPlay().registerLarge(WorldConfigUpdatePayload.TYPE, WorldConfigUpdatePayload.CODEC, 3_000_000)

        ServerPlayConnectionEvents.JOIN.register { handler, _, runningServer ->
            sendSnapshot(runningServer, handler.player)
        }
        ServerPlayNetworking.registerGlobalReceiver(WorldConfigUpdatePayload.TYPE) { payload, context ->
            context.server().execute {
                if (!context.player().createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    EnchLogger.warn("Ignorata modifica EnchLib da ${context.player().gameProfile.name}: permessi insufficienti")
                    sendSnapshot(context.server(), context.player())
                    return@execute
                }
                runCatching { GlobalConfigIO.applyServerSnapshot(payload.available, payload.details) }
                    .onSuccess { broadcastCurrent() }
                    .onFailure {
                        EnchLogger.warn("Rifiutato aggiornamento EnchLib non valido da ${context.player().gameProfile.name}: ${it.message}")
                        sendSnapshot(context.server(), context.player())
                    }
            }
        }
    }

    fun serverStarted(server: MinecraftServer) { this.server = server }

    fun serverStopping(server: MinecraftServer) {
        if (this.server === server) this.server = null
    }

    fun broadcastCurrent() {
        val runningServer = server ?: return
        runningServer.execute {
            if (server !== runningServer) return@execute
            runningServer.playerList.players.toList().forEach { sendSnapshot(runningServer, it) }
        }
    }

    fun sendClientUpdate() {
        runCatching {
            Class.forName("goldenrose01.enchlib.EnchlibClient")
                .getMethod("sendConfigUpdate")
                .invoke(null)
        }.onFailure { EnchLogger.debug("Configurazione locale in attesa della connessione al server") }
    }

    fun clientConnected(): Boolean = runCatching {
        Class.forName("goldenrose01.enchlib.network.ClientConfigSync")
            .getMethod("isConnected")
            .invoke(null) as Boolean
    }.getOrDefault(false)

    private fun sendSnapshot(server: MinecraftServer, player: net.minecraft.server.level.ServerPlayer) {
        if (!ServerPlayNetworking.canSend(player, WorldConfigSyncPayload.TYPE)) return
        val (available, details) = GlobalConfigIO.encodeSnapshot()
        ServerPlayNetworking.send(player, WorldConfigSyncPayload(available, details, MobCategoryManager.encodeSnapshot(), GlobalConfigIO.encodePresetSnapshot()))
    }
}
