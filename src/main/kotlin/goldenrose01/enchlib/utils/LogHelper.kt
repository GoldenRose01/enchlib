package goldenrose01.enchlib.utils

import com.mojang.logging.LogUtils
import org.slf4j.Logger

/**
 * Wrapper semplice per logging coerente.
 */
object LogHelper {
    val logger: Logger = LogUtils.getLogger()

    fun info(msg: String) = logger.info("[EnchLib] $msg")
    fun warn(msg: String) = logger.warn("[EnchLib] $msg")
    fun error(msg: String, t: Throwable? = null) = logger.error("[EnchLib] $msg", t)
}
