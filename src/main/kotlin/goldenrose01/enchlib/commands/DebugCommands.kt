package goldenrose01.enchlib.commands

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument

import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import net.minecraft.util.Identifier
import net.minecraft.registry.RegistryKeys
import net.minecraft.component.DataComponentTypes

import goldenrose01.enchlib.Enchlib
import goldenrose01.enchlib.config.WorldConfigManager
import goldenrose01.enchlib.utils.msg
import goldenrose01.enchlib.utils.err
import goldenrose01.enchlib.utils.noop
import goldenrose01.enchlib.utils.ok


object DebugCommands {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("plusec-debug")
                .then(
                    CommandManager.literal("reload")
                        .executes { ctx ->
                            val server = ctx.source.server
                            try {
                                WorldConfigManager.reloadConfigs(server)
                                ctx.source.sendFeedback(
                                    { Text.literal("Reload complete") },
                                    false
                                )
                                1
                            } catch (e: Exception) {
                                ctx.source.sendFeedback(
                                    { Text.literal("Reload failed: ${e.message}") },
                                    false
                                )
                                0
                            }
                        }
                )
                .then(
                    CommandManager.literal("validate")
                        .executes { ctx ->
                            val server = ctx.source.server
                            val issues = WorldConfigManager.validateAgainstRegistry(server)
                            if (issues.isEmpty()) {
                                ctx.source.sendFeedback(
                                    { Text.literal("No issues found in config") },
                                    false
                                )
                            } else {
                                ctx.source.sendFeedback(
                                    { Text.literal("Found issues:") },
                                    false
                                )
                                issues.forEach { issue ->
                                    ctx.source.sendFeedback({ Text.literal(" - $issue") }, false)
                                }
                            }
                            1
                        }
                )
        )
    }
}