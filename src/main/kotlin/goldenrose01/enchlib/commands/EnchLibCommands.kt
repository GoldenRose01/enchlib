package goldenrose01.enchlib.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.enchantment.Enchantment
import net.minecraft.component.DataComponentTypes
import net.minecraft.command.argument.ItemStackArgumentType
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.Registries
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource

import goldenrose01.enchlib.config.WorldConfigManager
import goldenrose01.enchlib.utils.msg
import goldenrose01.enchlib.utils.err
import goldenrose01.enchlib.utils.noop
import goldenrose01.enchlib.utils.ok


object EnchLibCommands {

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("plusec")
                .requires { it.hasPermissionLevel(2) }
                .then(literal("add")
                    .then(
                        argument("enchantment_id", StringArgumentType.string())
                        .then(
                            argument("level", IntegerArgumentType.integer(1))
                            .executes(::addEnchantment)
                        )
                    )
                )
                .then(literal("remove")
                    .then(
                        argument("enchantment_id", StringArgumentType.string())
                        .executes(::removeEnchantment)
                    )
                )
                .then(literal("list").executes(::listEnchantmentsOnHeldItem))
                .then(literal("clear").executes(::clearEnchantmentsOnHeldItem))
                .then(literal("info")
                    .then(
                        argument("enchantment_id", StringArgumentType.string())
                        .executes(::infoEnchantment)
                    )
                )
        )
    }

    private fun findEnchantment(idStr: String): Enchantment? {
        val id = Identifier.tryParse(idStr) ?: return null
        return Registries.ENCHANTMENT.get(id)
    }

    private fun addEnchantment(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        val enchId = StringArgumentType.getString(ctx, "enchantment_id")
        val level = IntegerArgumentType.getInteger(ctx, "level")
        val ench = findEnchantment(enchId) ?: return noop(ctx, "Incantesimo non trovato: $enchId")

        // In 1.21.x addEnchantment richiede RegistryEntry<Enchantment>
        val entry: RegistryEntry<Enchantment> =
            Registries.ENCHANTMENT.getEntry(ench).orElse(null) ?: return noop(ctx, "Entry non trovata per: $enchId")

        // Legge max level dai JSON (no hardcoded)
        val maxFromJson = WorldConfigManager.getMaxLevelFor(ench, src.server)
        val clamped = level.coerceIn(1, maxFromJson)

        stack.addEnchantment(entry, clamped)
        return ok(ctx, "Aggiunto $enchId livello $clamped all'item in mano.")
    }

    private fun removeEnchantment(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        val enchId = StringArgumentType.getString(ctx, "enchantment_id")
        val ench = findEnchantment(enchId) ?: return noop(ctx, "Incantesimo non trovato: $enchId")

        val nbt = net.minecraft.enchantment.EnchantmentHelper.get(stack)
        if (!nbt.containsKey(ench)) return noop(ctx, "L'item non ha $enchId.")

        // Rimuovi: ricostruisci mappa senza l'incantesimo
        val newMap = nbt.toMutableMap().apply { remove(ench) }
        net.minecraft.enchantment.EnchantmentHelper.set(newMap, stack)
        return ok(ctx, "Rimosso $enchId dall'item in mano.")
    }

    private fun clearEnchantmentsOnHeldItem(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")
        net.minecraft.enchantment.EnchantmentHelper.set(emptyMap(), stack)
        return ok(ctx, "Tutti gli incantesimi rimossi dall'item in mano.")
    }

    private fun listEnchantmentsOnHeldItem(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")
        val map = net.minecraft.enchantment.EnchantmentHelper.get(stack)
        if (map.isEmpty()) return ok(ctx, "Nessun incantesimo sull'item in mano.")

        val lines = buildString {
            appendLine("Incantesimi sull'item:")
            map.forEach { (ench, lvl) ->
                val id = Registries.ENCHANTMENT.getId(ench) ?: Identifier.of("unknown", "unknown")
                appendLine("- ${id.toString()} lvl $lvl")
            }
        }
        return ok(ctx, lines)
    }

    private fun infoEnchantment(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val enchId = StringArgumentType.getString(ctx, "enchantment_id")
        val ench = findEnchantment(enchId) ?: return noop(ctx, "Incantesimo non trovato: $enchId")

        val details = WorldConfigManager.getDetailsFor(ench, src.server)
        val id = Registries.ENCHANTMENT.getId(ench) ?: Identifier.of("unknown", "unknown")
        val text = buildString {
            appendLine("Info: $id")
            appendLine(" - maxLevel: ${details.maxLevel}")
            appendLine(" - rarity: ${details.rarity}")
            appendLine(" - category: ${details.category}")
            appendLine(" - multiplier: ${details.multiplier}")
        }
        return ok(ctx, text)
    }

    /** Utility: tutti gli ID enchant (se serve altrove). */
    fun allEnchantmentIds(): List<Identifier> {
        val reg = Registries.ENCHANTMENT
        return reg.entrySet().map { it.key.value() }
    }
}


