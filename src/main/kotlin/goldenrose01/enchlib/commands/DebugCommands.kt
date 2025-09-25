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
                    .then(argument("msg", StringArgumentType.greedyString())
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
/*
    private fun showWorldConfigPath(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source

        if (!WorldConfigManager.hasInstance()) {
            return source.err { "World configuration not available" }
        }

        val configManager = WorldConfigManager.getInstance(source.server)
        return source.msg({ "World config path: ${configManager.worldConfigPath}" })
    }

    private fun reloadWorldConfigs(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source

        if (!WorldConfigManager.hasInstance()) {
            return source.err { "World configuration not available" }
        }

        try {
            val configManager = WorldConfigManager.getInstance(source.server)
            configManager.reloadConfigurations()
            return source.msg({ "World configurations reloaded successfully" })
        } catch (e: Exception) {
            return source.err { "Failed to reload configurations: ${e.message}" }
        }
    }

    private fun regenerateConfigs(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source

        if (!WorldConfigManager.hasInstance()) {
            return source.err { "World configuration not available" }
        }

        try {
            val configManager = WorldConfigManager.getInstance(source.server)
            configManager.initializeWorldConfigs()
            return source.msg({ "World configurations regenerated successfully" })
        } catch (e: Exception) {
            return source.err { "Failed to regenerate configurations: ${e.message}" }
        }
    }

    private fun validateConfigs(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source

        if (!WorldConfigManager.hasInstance()) {
            return source.err { "World configuration not available" }
        }

        return source.msg({ "Configuration validation completed (detailed implementation needed)" })
    }

    private fun showStats(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source

        if (!WorldConfigManager.hasInstance()) {
            return source.err { "World configuration not available" }
        }

        source.msg({ "=== EnchLib World Configuration Stats ===" })
        source.msg({ "Available enchantments loaded" })
        source.msg({ "Enchantment details loaded" })
        source.msg({ "Mob categories loaded" })
        source.msg({ "Incompatibility rules loaded" })

        return 1
    }
}
*/