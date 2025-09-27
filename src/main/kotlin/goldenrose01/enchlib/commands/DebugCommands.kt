package goldenrose01.enchlib.commands

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument

import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text


import goldenrose01.enchlib.Enchlib
import goldenrose01.enchlib.config.WorldConfigManager
import goldenrose01.enchlib.utils.msg
import goldenrose01.enchlib.utils.err
import goldenrose01.enchlib.utils.noop
import goldenrose01.enchlib.utils.ok
import net.minecraft.util.WorldSavePath

/**
 * /plusec-debug
 *  - reload     : ricarica i JSON dal disco senza riavvio
 *  - validate   : cross-check tra registry runtime e JSON
 *  - path       : mostra il path dei file
 *  - stats      : conti base (wrapper di validate.totals)
 */
object DebugCommands {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerInternal(dispatcher)
        }
    }

    private fun registerInternal(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("plusec-debug")
                .requires { it.hasPermissionLevel(2) }
                .then(CommandManager.literal("reload").executes { ctx ->
                    val src = ctx.source
                    val report = WorldConfigManager.reload(src.server)
                    src.sendFeedback({ Text.literal("§aReload OK. Aviable=${report.aviableCount}, Details=${report.detailsCount}") }, false)
                    Command.SINGLE_SUCCESS
                })
                .then(CommandManager.literal("validate").executes { ctx ->
                    val src = ctx.source
                    val rep = WorldConfigManager.validate(src.server)
                    val sb = StringBuilder()
                    sb.appendLine("§eValidate report:")
                    sb.appendLine("Totals: ${rep.totals}")
                    if (rep.missingInJson.isNotEmpty()) {
                        sb.appendLine("Missing in JSON (${rep.missingInJson.size}):")
                        rep.missingInJson.forEach { sb.appendLine("- $it") }
                    }
                    if (rep.extraInJson.isNotEmpty()) {
                        sb.appendLine("Extra in JSON (${rep.extraInJson.size}):")
                        rep.extraInJson.forEach { sb.appendLine("- $it") }
                    }
                    src.sendFeedback({ Text.literal(sb.toString().trim()) }, false)
                    Command.SINGLE_SUCCESS
                })
                .then(CommandManager.literal("path").executes { ctx ->
                    val src = ctx.source
                    val root = src.server.getSavePath(WorldSavePath.ROOT).toFile()
                    val cfg = root.resolve("config/${Enchlib.MOD_ID}").absolutePath
                    src.sendFeedback({ Text.literal("Config dir: $cfg") }, false)
                    Command.SINGLE_SUCCESS
                })
                .then(CommandManager.literal("stats").executes { ctx ->
                    val src = ctx.source
                    val rep = WorldConfigManager.validate(src.server)
                    src.sendFeedback({ Text.literal("Totals: ${rep.totals}") }, false)
                    Command.SINGLE_SUCCESS
                })
        )
    }
}