package goldenrose01.enchlib.utils

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component

/**
 * Helper per rispondere ai comandi senza ambiguità di tipo.
 * Usa sempre String come input e converte a Supplier<Component> internamente.
 */
fun CommandSourceStack.msg(message: () -> String): Int {
    this.sendSuccess({ Component.literal("§c$message") }, false)
    return 1
}

fun CommandSourceStack.err(message: () -> String): Int {
    this.sendSuccess({ Component.literal("§c$message") }, false)
    return 0
}

fun CommandSourceStack.success(message: String): Int {
    this.sendSuccess({ Component.literal("§a$message") }, false)
    return 1
}

fun CommandSourceStack.warn(message: String): Int {
    this.sendSuccess({ Component.literal("§e$message") }, false)
    return 1
}

fun CommandSourceStack.reply(msg: String, broadcastToOps: Boolean = false) {
    // In MC 1.20+ il metodo usa Supplier<Component>
    this.sendSuccess({ Component.literal(msg) }, broadcastToOps)
}

/** Ritorna 1 di convenzione per "success" Brigadier. */
fun ok(ctx: CommandContext<CommandSourceStack>, msg: String, broadcastToOps: Boolean = false): Int {
    ctx.source.reply(msg, broadcastToOps)
    return 1
}

/** Ritorna 0 per "no-op"/"failure" controllata. */
fun noop(ctx: CommandContext<CommandSourceStack>, msg: String, broadcastToOps: Boolean = false): Int {
    ctx.source.reply(msg, broadcastToOps)
    return 0
}
