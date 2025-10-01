@file:Suppress("UNCHECKED_CAST")

package goldenrose01.enchlib.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.enchantment.Enchantment

import goldenrose01.enchlib.compat.MCCompat

object EnchLibCommands {

    private const val PERM = 2

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("plusec")
                .requires { it.hasPermissionLevel(PERM) }
                .then(
                    literal("add")
                        .then(
                            argument("id", StringArgumentType.string())
                                .suggests(ENCH_SUGGEST)
                                .then(
                                    argument("level", IntegerArgumentType.integer(1))
                                        .executes { ctx ->
                                            val src = ctx.source
                                            val idInput = StringArgumentType.getString(ctx, "id")
                                            val level   = IntegerArgumentType.getInteger(ctx, "level")

                                            val player = try { src.player } catch (_: Throwable) {
                                                try { src.getPlayer() } catch (_: Throwable) { null }
                                            }
                                            if (player == null) {
                                                src.sendError(Text.literal("Devi eseguire il comando come giocatore."))
                                                return@executes 0
                                            }

                                            val stack: ItemStack = try { player.getMainHandStack() } catch (_: Throwable) { ItemStack.EMPTY }
                                            if (stack.isEmpty) {
                                                src.sendError(Text.literal("Tieni un oggetto nella mano principale."))
                                                return@executes 0
                                            }

                                            val normalized = if (idInput.contains(":")) idInput else "minecraft:$idInput"
                                            val id = Identifier.tryParse(normalized)
                                            if (id == null) {
                                                src.sendError(Text.literal("Identifier non valido: $idInput"))
                                                return@executes 0
                                            }

                                            val ench: Enchantment = MCCompat.getEnchantment(src.server, id) ?: run {
                                                src.sendError(Text.literal("Incantesimo non trovato: $normalized"))
                                                return@executes 0
                                            }

                                            if (!applyByNbt(stack, ench, id, level)) {
                                                src.sendError(Text.literal("Impossibile applicare l'incantesimo all'oggetto."))
                                                return@executes 0
                                            }

                                            try { player.inventory.markDirty() } catch (_: Throwable) {}
                                            try { player.playerScreenHandler.syncState() } catch (_: Throwable) {}

                                            src.sendFeedback({ Text.literal("Added/Updated enchantment ${id} -> $level") }, false)
                                            1
                                        }
                                )
                        )
                )
        )
    }

    // ------- SUGGESTIONS -------
    private val ENCH_SUGGEST: SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { ctx, builder: SuggestionsBuilder ->
            try {
                val list = MCCompat.suggestStringsForEnchantments(ctx.source.server)
                for (s in list) builder.suggest(s)
            } catch (_: Throwable) { /* ignore */ }
            builder.buildFuture()
        }

    // ------- APPLY VIA NBT (no compat checks) -------
    private fun applyByNbt(stack: ItemStack, ench: Enchantment, enchId: Identifier, level: Int): Boolean {
        return try {
            val key = MCCompat.storedOrRegularKey(stack)
            val nbt: NbtCompound = MCCompat.getOrCreateNbt(stack)
            val list: NbtList = MCCompat.getOrCreateList(nbt, key)

            val targetId = enchId.toString()

            // aggiorna se esiste
            val size = MCCompat.listSize(list)
            for (i in 0 until size) {
                val cmp = MCCompat.listGetCompound(list, i) ?: continue
                val curId = MCCompat.getString(cmp, "id")
                if (curId == targetId) {
                    MCCompat.putShort(cmp, "lvl", level.toShort())
                    MCCompat.nbtPut(nbt, key, list)
                    MCCompat.setNbt(stack, nbt)
                    return true
                }
            }

            // altrimenti aggiungi
            val tag = NbtCompound()
            MCCompat.putString(tag, "id", targetId)
            MCCompat.putShort(tag, "lvl", level.toShort())
            MCCompat.listAdd(list, tag)

            MCCompat.nbtPut(nbt, key, list)
            MCCompat.setNbt(stack, nbt)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
