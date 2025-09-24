package goldenrose01.enchlib.utils

import org.slf4j.LoggerFactory

/**
 * Logger centralizzato per EnchLib con livelli di debug configurabili.
 * Evita clash JVM tra setter della property e metodo omonimo.
 */
object EnchLogger {
    private val LOGGER = LoggerFactory.getLogger("EnchLib")

    /**
     * Flag per abilitare output di debug più verboso.
     * Setter personalizzato per loggare l’attivazione/disattivazione.
     */
    var debugMode: Boolean = false
        set(value) {
            field = value
            if (value) {
                LOGGER.info("🔧 Debug mode abilitato - Output verboso attivo")
            } else {
                LOGGER.info("🔧 Debug mode disabilitato")
            }
        }

    fun info(message: String) = LOGGER.info(message)
    fun warn(message: String) = LOGGER.warn(message)
    fun error(message: String) = LOGGER.error(message)
    fun error(message: String, throwable: Throwable) = LOGGER.error(message, throwable)

    /** Log di debug (stampato solo se debugMode è true). */
    fun debug(message: String) {
        if (debugMode) {
            LOGGER.info("[DEBUG] $message")
        }
    }

    /** Helper con nomi diversi dal setter JVM per evitare clash. */
    fun enableDebug() { debugMode = true }
    fun disableDebug() { debugMode = false }
    fun toggleDebug() { debugMode = !debugMode }
}
