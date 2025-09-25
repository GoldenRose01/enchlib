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

    private fun getEnchantments(stack: ItemStack): ItemEnchantmentsComponent =
        stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)

    private fun setEnchantments(stack: ItemStack, comp: ItemEnchantmentsComponent) {
        stack.set(DataComponentTypes.ENCHANTMENTS, comp)
    }

    @Suppress("UNCHECKED_CAST")
    private fun compAsMap(comp: ItemEnchantmentsComponent): Map<Enchantment, Int> {
        // preferisci asMap()
        try {
            return ItemEnchantmentsComponent::class.java
                .getMethod("asMap")
                .invoke(comp) as Map<Enchantment, Int>
        } catch (_: Throwable) { /* fallthrough */ }

        // fallback: campo "enchantments"
        return try {
            val f = comp.javaClass.getDeclaredField("enchantments")
            f.isAccessible = true
            f.get(comp) as Map<Enchantment, Int>
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    private fun idFromEntry(entry: RegistryEntry<Enchantment>): Identifier? {
        // Tentativi robusti per ricavare l'Identifier dal RegistryEntry (mappings variano).
        return try {
            // entry.registryKey().getValue()
            val rk = entry.javaClass.getMethod("registryKey").invoke(entry)
            val v = rk.javaClass.getMethod("getValue").invoke(rk)
            v as? Identifier
        } catch (_: Throwable) {
            try {
                // entry.getKey().get().getValue()
                val k = entry.javaClass.getMethod("getKey").invoke(entry)
                val opt = k as? java.util.Optional<*>
                val key = opt?.orElse(null)
                val v = key?.javaClass?.getMethod("getValue")?.invoke(key)
                v as? Identifier
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun identifierOf(enchantment: Enchantment): String {
        // Best effort: prova a chiamare getTranslationKey o toString come fallback leggibile
        return try {
            // molte mappings hanno toString() => "minecraft:sharpness"
            enchantment.toString()
        } catch (_: Throwable) {
            "unknown:unknown"
        }
    }

    private fun builderNewFrom(current: ItemEnchantmentsComponent): ItemEnchantmentsComponent.Builder {
        // Preferisci costruttore (ItemEnchantmentsComponent.Builder(current))
        return try {
            val ctor = ItemEnchantmentsComponent.Builder::class.java.getConstructor(ItemEnchantmentsComponent::class.java)
            ctor.newInstance(current)
        } catch (_: Throwable) {
            // fallback default ctor + set successivi
            try { ItemEnchantmentsComponent.Builder::class.java.getConstructor().newInstance() }
            catch (_: Throwable) { throw IllegalStateException("No suitable Builder constructor for ItemEnchantmentsComponent") }
        }
    }

    private fun builderSet(builder: Any, entry: RegistryEntry<Enchantment>, level: Int): Boolean {
        // set(RegistryEntry<Enchantment>, int)
        return try {
            val m = builder.javaClass.getMethod("set", RegistryEntry::class.java, Int::class.javaPrimitiveType)
            m.invoke(builder, entry, level)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun builderSet(builder: Any, ench: Enchantment, level: Int): Boolean {
        // set(Enchantment, int)
        return try {
            val m = builder.javaClass.getMethod("set", Enchantment::class.java, Int::class.javaPrimitiveType)
            m.invoke(builder, ench, level)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun builderRemove(builder: Any, entry: RegistryEntry<Enchantment>): Boolean {
        return try {
            val m = builder.javaClass.getMethod("remove", RegistryEntry::class.java)
            m.invoke(builder, entry)
            true
        } catch (_: Throwable) { false }
    }

    private fun builderRemove(builder: Any, ench: Enchantment): Boolean {
        return try {
            val m = builder.javaClass.getMethod("remove", Enchantment::class.java)
            m.invoke(builder, ench)
            true
        } catch (_: Throwable) { false }
    }

    private fun builderBuild(builder: Any): ItemEnchantmentsComponent {
        return try {
            val m = builder.javaClass.getMethod("build")
            m.invoke(builder) as ItemEnchantmentsComponent
        } catch (_: Throwable) {
            ItemEnchantmentsComponent.DEFAULT
        }
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

    // ===== Comandi =====

    private fun addEnchantment(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        val entry: RegistryEntry<Enchantment> =
            RegistryEntryReferenceArgumentType.getRegistryEntry(ctx, "enchantment", RegistryKeys.ENCHANTMENT)
        val ench = entry.value()

        val reqLevel = IntegerArgumentType.getInteger(ctx, "level")
        val maxFromJson = WorldConfigManager.getMaxLevelFor(ench, src.server)
        val level = reqLevel.coerceIn(1, maxFromJson)

        val current = getEnchantments(stack)
        val builder = builderNewFrom(current)

        if (!builderSet(builder, entry, level) && !builderSet(builder, ench, level)) {
            return noop(ctx, "Impossibile applicare l'incantesimo (mappings non compatibili).")
        }

        setEnchantments(stack, builderBuild(builder))

        // auto-popola JSON se non presente
        idFromEntry(entry)?.let { id ->
            WorldConfigManager.ensurePresentInJson(id.toString(), src.server)
        }

        return ok(ctx, "Aggiunto ${idFromEntry(entry) ?: identifierOf(ench)} livello $level all'item in mano.")
    }

    private fun removeEnchantment(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        val entry: RegistryEntry<Enchantment> =
            RegistryEntryReferenceArgumentType.getRegistryEntry(ctx, "enchantment", RegistryKeys.ENCHANTMENT)
        val ench = entry.value()

        val comp = getEnchantments(stack)
        val map = compAsMap(comp)
        if (!map.containsKey(ench)) return noop(ctx, "L'item non ha questo incantesimo.")

        val builder = builderNewFrom(comp)
        if (!builderRemove(builder, entry) && !builderRemove(builder, ench)) {
            return noop(ctx, "Impossibile rimuovere l'incantesimo (mappings non compatibili).")
        }
        setEnchantments(stack, builderBuild(builder))

        return ok(ctx, "Rimosso ${idFromEntry(entry) ?: identifierOf(ench)} dall'item in mano.")
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
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        val comp = getEnchantments(stack)
        val map = compAsMap(comp)
        if (map.isEmpty()) return ok(ctx, "Nessun incantesimo sull'item in mano.")

        val lines = buildString {
            appendLine("Incantesimi sull'item:")
            map.forEach { (e, lvl) ->
                appendLine("- ${identifierOf(e)} lvl $lvl")
            }
        }
        return ok(ctx, lines)
    }

    private fun infoEnchantment(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val entry: RegistryEntry<Enchantment> =
            RegistryEntryReferenceArgumentType.getRegistryEntry(ctx, "enchantment", RegistryKeys.ENCHANTMENT)
        val ench = entry.value()

        val idStr = idFromEntry(entry)?.toString() ?: identifierOf(ench)
        val details = WorldConfigManager.getDetailsFor(ench, src.server)
        val text = buildString {
            appendLine("Info: $idStr")
            appendLine(" - maxLevel: ${details.maxLevel}")
            appendLine(" - rarity: ${details.rarity}")
            appendLine(" - category: ${details.category}")
            appendLine(" - multiplier: ${details.multiplier}")
        }
        return ok(ctx, text)
    }

    // ===== Variante compat: add tramite string id (senza entry) =====

    private fun addEnchantmentById(ctx: CommandContext<ServerCommandSource>): Int {
        val src = ctx.source
        val player = src.player ?: return noop(ctx, "Nessun giocatore.")
        val stack = player.mainHandStack
        if (stack.isEmpty) return noop(ctx, "Item in mano vuoto.")

        val idStr = StringArgumentType.getString(ctx, "enchantment_id")
        val id = Identifier.tryParse(idStr) ?: return noop(ctx, "ID non valido: $idStr")
        // Possiamo solo salvare nei JSON; l'applicazione effettiva richiede un RegistryEntry/Enchantment.
        WorldConfigManager.ensurePresentInJson(id.toString(), src.server)
        return ok(ctx, "Registrato in config l'incantesimo $idStr. Usa `/plusec add` (non addid) per applicarlo.")
    }
}