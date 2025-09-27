package goldenrose01.enchlib.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.enchantment.Enchantment
import net.minecraft.component.DataComponentTypes
import net.minecraft.command.argument.ItemStackArgumentType
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.util.Identifier
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

import goldenrose01.enchlib.Enchlib
import goldenrose01.enchlib.config.WorldConfigManager
import goldenrose01.enchlib.utils.msg
import goldenrose01.enchlib.utils.err
import goldenrose01.enchlib.utils.noop
import goldenrose01.enchlib.utils.ok

object EnchLibCommands {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("plusec")
                .then(
                    CommandManager.literal("add")
                        .then(
                            CommandManager.argument("id", StringArgumentType.string())
                                .then(
                                    CommandManager.argument("level", IntegerArgumentType.integer(1, 255))
                                        .executes { ctx ->
                                            val id = StringArgumentType.getString(ctx, "id")
                                            val level = IntegerArgumentType.getInteger(ctx, "level")
                                            val server = ctx.source.server

                                            WorldConfigManager.addOrUpdateEnchantment(server, id, level)
                                            ctx.source.sendFeedback(
                                                { Text.literal("Added/Updated enchantment $id -> $level") },
                                                false
                                            )
                                            1
                                        }
                                )
                        )
                )
                .then(
                    CommandManager.literal("list")
                        .executes { ctx ->
                            val component = WorldConfigManager.getCurrentComponent()
                            ctx.source.sendFeedback(
                                { Text.literal("Configured enchantments (${component.getSize()}):") },
                                false
                            )
                            for (entry in component.getEnchantmentEntries()) {
                                val ench = entry.key
                                val lvl = entry.intValue
                                ctx.source.sendFeedback(
                                    { Text.literal(" - ${ench.idAsString}: $lvl") },
                                    false
                                )
                            }
                            1
                        }
                )
        )
    }
}