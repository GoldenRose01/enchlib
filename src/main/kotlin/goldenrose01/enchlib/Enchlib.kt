package goldenrose01.enchlib

import net.fabricmc.api.ModInitializer
import goldenrose01.enchlib.config.ConfigManager
import goldenrose01.enchlib.registry.EnchantmentRegistry
import goldenrose01.enchlib.api.EnchLibAPI
import goldenrose01.enchlib.commands.EnchLibCommands
import org.slf4j.LoggerFactory

object EnchLib : ModInitializer {
    val LOGGER = LoggerFactory.getLogger("EnchLib")

    override fun onInitialize() {
        LOGGER.info("🔮 Inizializzazione EnchLib - Libreria Avanzata per Enchantments Personalizzati")

        try {
            LOGGER.info("📝 Caricamento configurazioni...")
            ConfigManager.initialize()

            LOGGER.info("📚 Inizializzazione registry enchantments...")
            EnchantmentRegistry.initialize()

            LOGGER.info("🔌 Inizializzazione API pubblica...")
            EnchLibAPI.initialize()

            LOGGER.info("⌨️ Registrazione comandi...")
            EnchLibCommands.registerCommands()

            LOGGER.info("✅ EnchLib caricata con successo!")
        } catch (e: Exception) {
            LOGGER.error("❌ Errore durante l'inizializzazione di EnchLib", e)
            throw RuntimeException("Impossibile inizializzare EnchLib", e)
        }
    }
}
