package goldenrose01.enchlib.network;

import goldenrose01.enchlib.config.GlobalConfigIO;
import goldenrose01.enchlib.config.MobCategoryManager;
import goldenrose01.enchlib.utils.EnchLogger;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Client-only packet handlers, loaded reflectively by the common entrypoint. */
public final class ClientConfigSync {
    private ClientConfigSync() { }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(WorldConfigSyncPayload.Companion.getTYPE(), (payload, context) ->
                context.client().execute(() -> {
                    try {
                        GlobalConfigIO.INSTANCE.applyClientSnapshot(payload.getAvailable(), payload.getDetails());
                        MobCategoryManager.INSTANCE.applyClientSnapshot(payload.getMobs());
                        GlobalConfigIO.INSTANCE.applyClientPresetsSnapshot(payload.getPresets());
                    } catch (RuntimeException exception) {
                        EnchLogger.INSTANCE.warn("Ricevuta configurazione EnchLib non valida dal server: " + exception.getMessage());
                    }
                }));
    }

    public static void sendUpdate() {
        if (Minecraft.getInstance().getConnection() == null) return;
        var snapshot = GlobalConfigIO.INSTANCE.encodeSnapshot();
        ClientPlayNetworking.send(new WorldConfigUpdatePayload(snapshot.getFirst(), snapshot.getSecond()));
    }

    public static boolean isConnected() {
        return Minecraft.getInstance().getConnection() != null;
    }
}
