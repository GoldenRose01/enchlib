package goldenrose01.enchlib.utils

import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

/**
 * Helper per rispondere ai comandi senza ambiguità di tipo.
 * Usa sempre String come input e converte a Supplier<Text> internamente.
 */
fun ServerCommandSource.msg(message: () -> String): Int {
    this.sendFeedback({ Text.literal("§c$message") }, false)
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

fun ServerCommandSource.reply(msg: String, broadcastToOps: Boolean = false) {
    // In MC 1.20+ il metodo usa Supplier<Text>
    this.sendFeedback({ Text.literal(msg) }, broadcastToOps)
}

/** Ritorna 1 di convenzione per "success" Brigadier. */
fun ok(ctx: CommandContext<ServerCommandSource>, msg: String, broadcastToOps: Boolean = false): Int {
    ctx.source.reply(msg, broadcastToOps)
    return 1
}

/** Ritorna 0 per "no-op"/"failure" controllata. */
fun noop(ctx: CommandContext<ServerCommandSource>, msg: String, broadcastToOps: Boolean = false): Int {
    ctx.source.reply(msg, broadcastToOps)
    return 0
}
