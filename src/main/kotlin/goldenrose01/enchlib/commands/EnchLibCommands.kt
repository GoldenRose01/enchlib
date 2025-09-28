package goldenrose01.enchlib.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.registry.Registries
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.entity.player.PlayerEntity

object EnchLibCommands {

    private const val PERM = 2

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val root: LiteralArgumentBuilder<ServerCommandSource> = literal("plusec")
            .requires { src -> src.hasPermissionLevel(PERM) }

        val addNode =
            literal("add")
                .then(
                    argument("id", StringArgumentType.string())
                        .suggests(ENCH_SUGGEST)
                        .then(
                            argument("level", IntegerArgumentType.integer(1))
                                .executes { ctx ->
                                    val source = ctx.source
                                    val idInput = StringArgumentType.getString(ctx, "id")
                                    val level = IntegerArgumentType.getInteger(ctx, "level")

                                    val player: PlayerEntity? = try {
                                        source.player
                                    } catch (_: Throwable) {
                                        try { source.getPlayer() } catch (_: Throwable) { null }
                                    }

                                    if (player == null) {
                                        source.sendError(Text.literal("Devi essere un giocatore per usare questo comando."))
                                        return@executes 0
                                    }

                                    val stack: ItemStack = try {
                                        player.mainHandStack
                                    } catch (_: Throwable) {
                                        player.getStackInHand(Hand.MAIN_HAND)
                                    }

                                    if (stack.isEmpty) {
                                        source.sendError(Text.literal("Tieni un oggetto in mano (mano principale)."))
                                        return@executes 0
                                    }

                                    val resolved = resolveEnchantment(idInput)
                                    if (resolved == null) {
                                        source.sendError(Text.literal("Incantesimo non trovato: $idInput"))
                                        return@executes 0
                                    }
                                    val (_, normalizedId) = resolved

                                    val ok = forceApplyEnchantmentNbt(stack, normalizedId, level)
                                    if (ok) {
                                        source.sendFeedback({ Text.literal("Added/Updated enchantment $normalizedId -> $level") }, false)
                                        try { player.currentScreenHandler.sendContentUpdates() } catch (_: Throwable) {}
                                        1
                                    } else {
                                        source.sendError(Text.literal("Impossibile applicare l'incantesimo (API/NBT non disponibili)."))
                                        0
                                    }
                                }
                        )
                )

        dispatcher.register(root.then(addNode))
    }

    /** Suggerimenti: mostra sia "namespace:path" sia "path". */
    private val ENCH_SUGGEST: SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { _, builder: SuggestionsBuilder ->
            for (id in Registries.ENCHANTMENT.ids) {
                builder.suggest(id.toString())
                builder.suggest(id.path)
            }
            builder.buildFuture()
        }

    /** Risolve un Enchantment accettando "ns:path" o solo "path" (default "minecraft:path"). */
    private fun resolveEnchantment(input: String): Pair<Enchantment, String>? {
        val id: Identifier? = if (input.contains(":")) {
            Identifier.tryParse(input)
        } else {
            Identifier.tryParse("minecraft:$input")
        }
        if (id == null) return null
        val ench = Registries.ENCHANTMENT.get(id) ?: return null
        return ench to id.toString()
    }

    /**
     * Applica forzatamente l’enchant tramite NBT:
     *  - lista "Enchantments" di compound {id:"<ns:id>", lvl:<short>}
     *  - rimuove eventuali duplicati, poi inserisce/aggiorna
     *
     * Stabile tra versioni e replica /enchant senza controlli.
     */
    private fun forceApplyEnchantmentNbt(stack: ItemStack, enchantId: String, level: Int): Boolean {
        return try {
            val nbt = getOrCreateNbtCompat(stack)
            val listKey = "Enchantments"

            val list: NbtList = if (nbt.contains(listKey, 9)) {
                nbt.getList(listKey, 10) as NbtList // 9=list, 10=compound
            } else {
                NbtList()
            }

            // rimuovi voci esistenti con lo stesso id
            val toRemove = ArrayList<Int>()
            for (i in 0 until list.size) {
                val c = list.getCompound(i)
                val existingId = runCatching { c.getString("id") }.getOrNull() ?: ""
                if (existingId == enchantId) toRemove.add(i)
            }
            for (i in toRemove.asReversed()) list.removeAt(i)

            val comp = NbtCompound()
            comp.putString("id", enchantId)
            comp.putShort("lvl", level.toShort())
            list.add(comp)

            nbt.put(listKey, list)
            setNbtCompat(stack, nbt)
            true
        } catch (_: Throwable) {
            false
        }
    }

    // === Helpers compatibilità NBT tra mapping/versioni ===

    private fun getOrCreateNbtCompat(stack: ItemStack): NbtCompound {
        // 1) Metodo classico getOrCreateNbt()
        runCatching {
            val m = ItemStack::class.java.getMethod("getOrCreateNbt")
            val r = m.invoke(stack)
            if (r is NbtCompound) return r
        }

        // 2) Property Kotlin "orCreateNbt" (se esiste)
        runCatching {
            val f = ItemStack::class.java.getDeclaredField("nbt")
            f.isAccessible = true
            val current = f.get(stack) as? NbtCompound
            if (current != null) return current
            val nc = NbtCompound()
            f.set(stack, nc)
            return nc
        }

        // 3) Coppia getNbt()/setNbt(NbtCompound)
        runCatching {
            val getM = ItemStack::class.java.methods.firstOrNull { it.name == "getNbt" && it.parameterCount == 0 }
            val setM = ItemStack::class.java.methods.firstOrNull { it.name == "setNbt" && it.parameterCount == 1 }
            val cur = getM?.invoke(stack) as? NbtCompound
            if (cur != null) return cur
            val nc = NbtCompound()
            setM?.invoke(stack, nc)
            return nc
        }

        // Fallback: nuovo Nbt senza collegarlo (meglio di crash)
        return NbtCompound()
    }

    private fun setNbtCompat(stack: ItemStack, nbt: NbtCompound) {
        // prova setNbt(NbtCompound)
        runCatching {
            val setM = ItemStack::class.java.methods.firstOrNull { it.name == "setNbt" && it.parameterCount == 1 }
            if (setM != null) {
                setM.invoke(stack, nbt)
                return
            }
        }
        // prova campo "nbt"
        runCatching {
            val f = ItemStack::class.java.getDeclaredField("nbt")
            f.isAccessible = true
            f.set(stack, nbt)
            return
        }
        // se nessuna delle due, non facciamo nulla (già messo nell’oggetto in getOrCreate)
    }
}
