package goldenrose01.enchlib.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
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


/**
 * Comando principale: /plusec
 * - add <enchantment> <level>          (registry argument)
 * - add <namespace:id> <level>         (string id)
 * - remove <enchantment>
 * - clear
 * - list
 * - info <enchantment>
 *
 * Supporto robusto agli ID stringa: se valido ma non presente nei JSON → aggiunta automatica.
 */
object EnchLibCommands {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerInternal(dispatcher)
        }
    }

    private fun registerInternal(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("plusec")
                .requires { it.hasPermissionLevel(2) }
                .then(
                    CommandManager.literal("add")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                            .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 255))
                                .executes { ctx ->
                                    val src = ctx.source
                                    val player = src.entity as? ServerPlayerEntity ?: return@executes error(src, "Serve un giocatore.")
                                    val id = IdentifierArgumentType.getIdentifier(ctx, "id")
                                    val level = IntegerArgumentType.getInteger(ctx, "level")
                                    val entry = WorldConfigManager.resolveEnchantment(src.server, id)
                                        ?: return@executes error(src, "Enchantment non trovato: $id")
                                    WorldConfigManager.ensurePresentInJson(id.toString())
                                    applyEnchant(player, entry.value(), level)
                                    ok(src, "Aggiunto $id livello $level")
                                }
                            )
                        )
                )
                .then(
                    CommandManager.literal("remove")
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                            .executes { ctx ->
                                val src = ctx.source
                                val player = src.entity as? ServerPlayerEntity ?: return@executes error(src, "Serve un giocatore.")
                                val id = IdentifierArgumentType.getIdentifier(ctx, "id")
                                val entry = WorldConfigManager.resolveEnchantment(src.server, id)
                                    ?: return@executes error(src, "Enchantment non trovato: $id")
                                removeEnchant(player, entry.value())
                                ok(src, "Rimosso $id")
                            }
                        )
                )
                .then(CommandManager.literal("clear").executes { ctx ->
                    val src = ctx.source
                    val player = src.entity as? ServerPlayerEntity ?: return@executes error(src, "Serve un giocatore.")
                    clearEnchants(player)
                    ok(src, "Rimossi tutti gli incantesimi")
                })
                .then(CommandManager.literal("list").executes { ctx ->
                    val src = ctx.source
                    val player = src.entity as? ServerPlayerEntity ?: return@executes error(src, "Serve un giocatore.")
                    val stack = player.mainHandStack
                    val comp = stack.get(DataComponentTypes.ENCHANTMENTS) ?: ItemEnchantmentsComponent.DEFAULT
                    if (comp.isEmpty) return@executes ok(src, "Nessun incantesimo")
                    val reg = src.server.registryManager.get(RegistryKeys.ENCHANTMENT)
                    val lines = comp.enchantmentEntries().joinToString("\n") { entry ->
                        val id = reg.getId(entry.enchantment)?.toString() ?: "?"
                        "- $id: ${entry.level}"
                    }
                    src.sendFeedback({ Text.literal("Incantesimi:\n$lines") }, false)
                    Command.SINGLE_SUCCESS
                })
        )
    }

    private fun applyEnchant(player: ServerPlayerEntity, enchant: Enchantment, level: Int) {
        val stack = player.mainHandStack
        val comp = stack.get(DataComponentTypes.ENCHANTMENTS) ?: ItemEnchantmentsComponent.DEFAULT
        val builder = ItemEnchantmentsComponent.Builder(comp)
        builder.set(enchant, level)
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build())
    }

    private fun removeEnchant(player: ServerPlayerEntity, enchant: Enchantment) {
        val stack = player.mainHandStack
        val comp = stack.get(DataComponentTypes.ENCHANTMENTS) ?: ItemEnchantmentsComponent.DEFAULT
        val builder = ItemEnchantmentsComponent.Builder(comp)
        builder.remove(enchant)
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build())
    }

    private fun clearEnchants(player: ServerPlayerEntity) {
        val stack = player.mainHandStack
        stack.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
    }

    private fun ok(src: ServerCommandSource, msg: String): Int {
        src.sendFeedback({ Text.literal("§a$msg") }, false)
        return Command.SINGLE_SUCCESS
    }

    private fun error(src: ServerCommandSource, msg: String): Int {
        src.sendError(Text.literal("§c$msg"))
        return 0
    }
}