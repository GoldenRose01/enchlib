package goldenrose01.enchlib

import net.fabricmc.api.ClientModInitializer
import goldenrose01.enchlib.utils.EnchLogger

object EnchlibClient : ClientModInitializer {
    override fun onInitializeClient() {
        EnchLogger.info("🖥️ Inizializzazione client EnchLib")
        runCatching {
            Class.forName("goldenrose01.enchlib.network.ClientConfigSync")
                .getMethod("register").invoke(null)
        }.onFailure { EnchLogger.error("Impossibile inizializzare la sincronizzazione client EnchLib", it) }
    }

    @JvmStatic
    fun sendConfigUpdate() {
        runCatching {
            Class.forName("goldenrose01.enchlib.network.ClientConfigSync")
                .getMethod("sendUpdate").invoke(null)
        }.onFailure { EnchLogger.debug("Modifica EnchLib in attesa della connessione al server") }
    }
}
