package goldenrose01.enchlib.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Logger di utilità per EnchLib. */
object EnchLogger {
    private val logger: Logger = LoggerFactory.getLogger("EnchLib")

    fun info(message: String) {
        logger.info(message)
    }

    fun warn(message: String) {
        logger.warn(message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            logger.error(message, throwable)
        } else {
            logger.error(message)
        }
    }
}
