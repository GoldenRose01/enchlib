package goldenrose01.enchlib

import net.fabricmc.api.ClientModInitializer
import goldenrose01.enchlib.utils.EnchLogger

object EnchlibClient : ClientModInitializer {
    override fun onInitializeClient() {
        EnchLogger.info("🖥️ Inizializzazione client EnchLib")
    }
}
