package goldenrose01.enchlib.commands

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument

import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.command.CommandManager.literal

import goldenrose01.enchlib.config.WorldConfigManager
import goldenrose01.enchlib.utils.msg
import goldenrose01.enchlib.utils.err
import goldenrose01.enchlib.utils.noop
import goldenrose01.enchlib.utils.ok

object DebugCommands {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("plusec-debug")
                .requires { it.hasPermissionLevel(2) }
                .then(literal("path").executes(::path))
                .then(literal("reload").executes(::reload))
                .then(literal("regen").executes(::regen))
                .then(literal("validate").executes(::validate))
                .then(literal("stats").executes(::stats))
                .then(literal("echo")
                    .then(argument<ServerCommandSource, String>("msg", StringArgumentType.greedyString())
                        .executes(::echo)
                    )
                )
        )
    }

    private fun path(ctx: CommandContext<ServerCommandSource>): Int {
        val server = ctx.source.server
        val worldPath = WorldConfigManager.getWorldConfigDir(server)
        return ok(ctx, "EnchLib config path: $worldPath")
    }

    private fun reload(ctx: CommandContext<ServerCommandSource>): Int {
        val server = ctx.source.server
        WorldConfigManager.reload(server)
        return ok(ctx, "EnchLib: configurazioni ricaricate dal disco.")
    }

    private fun regen(ctx: CommandContext<ServerCommandSource>): Int {
        val server = ctx.source.server
        WorldConfigManager.regen(server)
        return ok(ctx, "EnchLib: rigenerazione file completata (merge non distruttivo).")
    }

    private fun validate(ctx: CommandContext<ServerCommandSource>): Int {
        val server = ctx.source.server
        val result = WorldConfigManager.validate(server)
        return if (result.valid) {
            ok(ctx, "Validate OK. Vanilla+Mod in sync con JSON (${result.report}).")
        } else {
            noop(ctx, "Validate FAILED: ${result.report}")
        }
    }

    private fun stats(ctx: CommandContext<ServerCommandSource>): Int {
        val server = ctx.source.server
        val s = WorldConfigManager.stats(server)
        return ok(ctx, "Stats — total:${s.total} enabled:${s.enabled} disabled:${s.disabled} missing:${s.missing}")
    }

    private fun echo(ctx: CommandContext<ServerCommandSource>): Int {
        val msg = StringArgumentType.getString(ctx, "msg")
        return ok(ctx, "[plusec-debug] $msg")
    }
}