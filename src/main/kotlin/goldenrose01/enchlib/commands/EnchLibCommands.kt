package goldenrose01.enchlib.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.enchantment.Enchantment
import net.minecraft.component.DataComponentTypes
import net.minecraft.command.argument.ItemStackArgumentType
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.util.Identifier
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer

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
                .then(
                    literal("add").then(
                        argument<ServerCommandSource, String>("enchantment_id", StringArgumentType.string()).then(
                                argument<ServerCommandSource, Int>("level", IntegerArgumentType.integer(1))
                                    .executes(::addEnchantment)
                        )
                    )
                )
                .then(
                    literal("remove").then(
                        argument<ServerCommandSource, String>("enchantment_id", StringArgumentType.string())
                            .executes(::removeEnchantment)
                    )
                )
                .then(literal("list").executes(::listEnchantmentsOnHeldItem))
                .then(literal("clear").executes(::clearEnchantmentsOnHeldItem))
                .then(literal("info")
                    .then(
                        argument<ServerCommandSource, String>("enchantment_id", StringArgumentType.string())
                            .executes(::infoEnchantment)
                    )
                )
        )
    }

    // ======= Helpers =======

    private fun enchRegistry(server: MinecraftServer): Registry<Enchantment> =
        server.registryManager.get(RegistryKeys.ENCHANTMENT)

    private fun findEnchantment(server: MinecraftServer, idStr: String): Enchantment? {
        val id = Identifier.tryParse(idStr) ?: return null
        return enchRegistry(server).get(id)
    }

    private fun getId(server: MinecraftServer, ench: Enchantment): Identifier =
        enchRegistry(server).getId(ench)

    private fun getEnchantments(stack: ItemStack): ItemEnchantmentsComponent =
        stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)

    private fun setEnchantments(stack: ItemStack, comp: ItemEnchantmentsComponent) {
        stack.set(DataComponentTypes.ENCHANTMENTS, comp)
    }

    private fun compAsMap(comp: ItemEnchantmentsComponent, server: MinecraftServer): Map<Enchantment, Int> {
        // Proviamo asMap(); in alcune mappings può cambiare nome → fallback reflection
        try {
            @Suppress("UNCHECKED_CAST")
            return ItemEnchantmentsComponent::class.java
                .getMethod("asMap")
                .invoke(comp) as Map<Enchantment, Int>
        } catch (_: Throwable) {
            // Fallback: campo "enchantments"
            return try {
                val f = comp.javaClass.getDeclaredField("enchantments")
                f.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                f.get(comp) as Map<Enchantment, Int>
            } catch (_: Throwable) {
                emptyMap()
            }
        }
    }

    private fun buildComponentFromMap(
        map: Map<Enchantment, Int>,
        server: MinecraftServer
    ): ItemEnchantmentsComponent {
        val builderCtor = try {
            ItemEnchantmentsComponent.Builder::class.java.getConstructor()
        } catch (_: Throwable) { null }

        val builder = builderCtor?.newInstance() as? ItemEnchantmentsComponent.Builder
            ?: ItemEnchantmentsComponent.Builder(getEnchantments(ItemStack.EMPTY)) // fallback “inutile”, ma evita NPE

        // Il set(...) può richiedere RegistryEntry<Enchantment> o Enchantment a seconda delle mappings.
        val reg = enchRegistry(server)
        for ((e, lvl) in map) {
            val setViaEntry = try {
                val m = ItemEnchantmentsComponent.Builder::class.java.getMethod(
                    "set",
                    RegistryEntry::class.java,
                    Int::class.javaPrimitiveType
                )
                val entryOpt = reg.getEntry(e)
                val entry = entryOpt.orElse(null) ?: continue
                m.invoke(builder, entry, lvl)
                true
            } catch (_: Throwable) { false }

            if (!setViaEntry) {
                try {
                    val m2 = ItemEnchantmentsComponent.Builder::class.java.getMethod(
                        "set",
                        Enchantment::class.java,
                        Int::class.javaPrimitiveType
                    )
                    m2.invoke(builder, e, lvl)
                } catch (_: Throwable) {
                    // ultima spiaggia: salta
                }
            }
        }
        return try {
            ItemEnchantmentsComponent.Builder::class.java
                .getMethod("build")
                .invoke(builder) as ItemEnchantmentsComponent
        } catch (_: Throwable) {
            ItemEnchantmentsComponent.DEFAULT
        }
    }
    // ===== Comandi =====

    private fun addEnchantment(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val server = src.server
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        val enchId = StringArgumentType.getString(ctx, "enchantment_id")
        val reqLevel = IntegerArgumentType.getInteger(ctx, "level")
        val ench = findEnchantment(server, enchId) ?: return noop(ctx, "Incantesimo non trovato: $enchId")

        val maxFromJson = WorldConfigManager.getMaxLevelFor(ench, server)
        val level = reqLevel.coerceIn(1, maxFromJson)

        val current = getEnchantments(stack)
        val currentMap = compAsMap(current, server).toMutableMap()
        currentMap[ench] = level

        val updated = buildComponentFromMap(currentMap, server)
        setEnchantments(stack, updated)

        return ok(ctx, "Aggiunto $enchId livello $level all'item in mano.")
    }

    private fun removeEnchantment(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val server = src.server
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        val enchId = StringArgumentType.getString(ctx, "enchantment_id")
        val ench = findEnchantment(server, enchId) ?: return noop(ctx, "Incantesimo non trovato: $enchId")

        val current = getEnchantments(stack)
        val map = compAsMap(current, server)
        if (!map.containsKey(ench)) return noop(ctx, "L'item non ha $enchId.")

        val newMap = map.toMutableMap()
        newMap.remove(ench)

        val updated = buildComponentFromMap(newMap, server)
        setEnchantments(stack, updated)

        return ok(ctx, "Rimosso $enchId dall'item in mano.")
    }

    private fun clearEnchantmentsOnHeldItem(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        setEnchantments(stack, ItemEnchantmentsComponent.DEFAULT)
        return ok(ctx, "Tutti gli incantesimi rimossi dall'item in mano.")
    }

    private fun listEnchantmentsOnHeldItem(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val server = src.server
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        val comp = getEnchantments(stack)
        val map = compAsMap(comp, server)
        if (map.isEmpty()) return ok(ctx, "Nessun incantesimo sull'item in mano.")

        val lines = buildString {
            appendLine("Incantesimi sull'item:")
            map.forEach { (e, lvl) ->
                val id = getId(server, e)
                appendLine("- ${id} lvl $lvl")
            }
        }
        return ok(ctx, lines)
    }

    private fun infoEnchantment(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val server = src.server
        val enchId = StringArgumentType.getString(ctx, "enchantment_id")
        val ench = findEnchantment(server, enchId) ?: return noop(ctx, "Incantesimo non trovato: $enchId")

        val details = WorldConfigManager.getDetailsFor(ench, server)
        val id = getId(server, ench)
        val text = buildString {
            appendLine("Info: $id")
            appendLine(" - maxLevel: ${details.maxLevel}")
            appendLine(" - rarity: ${details.rarity}")
            appendLine(" - category: ${details.category}")
            appendLine(" - multiplier: ${details.multiplier}")
        }
        return ok(ctx, text)
    }
}

