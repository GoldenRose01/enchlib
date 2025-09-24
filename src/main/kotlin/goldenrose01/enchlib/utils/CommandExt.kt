package goldenrose01.enchlib.utils

import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

fun ServerCommandSource.msg(message: String): Int {
    this.sendFeedback({ Text.literal(message) }, false)
    return 1
}

fun ServerCommandSource.err(message: () -> String): Int {
    this.sendFeedback({ Text.literal("§c$message") }, false)
    return 0
}

fun ServerCommandSource.success(message: String): Int {
    this.sendFeedback({ Text.literal("§a$message") }, false)
    return 1
}

fun ServerCommandSource.warn(message: String): Int {
    this.sendFeedback({ Text.literal("§e$message") }, false)
    return 1
}
