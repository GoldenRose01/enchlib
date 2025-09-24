package goldenrose01.enchlib.utils

import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

fun ServerCommandSource.msg(message: String, broadcastToOps: Boolean = false) {
    // Firma moderna: Supplier<Text> come primo argomento
    this.sendFeedback({ Text.literal(message) }, broadcastToOps)
}

fun ServerCommandSource.err(message: String) {
    // In 1.21.x è ancora accettato il Text diretto
    this.sendError(Text.literal(message))
}
