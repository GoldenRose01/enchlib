package goldenrose01.enchlib.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

import goldenrose01.enchlib.compat.MCCompat

object EnchLibCommands {

    private const val PERM = 2

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("plusec")
                .requires { src -> hasLevel(src, PERM) }

                // /plusec add <id> <level>
                .then(
                    literal("add")
                        .then(
                            argument("id", StringArgumentType.string())
                                .suggests(ENCH_SUGGEST)
                                .then(
                                    argument("level", IntegerArgumentType.integer(1))
                                        .executes { ctx ->
                                            val source = ctx.source
                                            val idRaw = StringArgumentType.getString(ctx, "id")
                                            val level = IntegerArgumentType.getInteger(ctx, "level")

                                            val player = currentPlayer(source)
                                                ?: return@executes err(source, "Devi essere un giocatore per usare questo comando.")

                                            val stack = mainHand(player)
                                                ?: return@executes err(source, "Tieni un oggetto nella mano principale.")

                                            val id: Identifier = MCCompat.parseEnchantmentId(idRaw)
                                                ?: return@executes err(source, "ID incantesimo non valido: $idRaw")

                                            val ok = try { MCCompat.upsertEnchantment(stack, id, level) } catch (_: Throwable) { false }
                                            if (!ok) return@executes err(source, "Impossibile applicare l'incantesimo (NBT/API).")

                                            try { player.inventory.markDirty() } catch (_: Throwable) {}

                                            source.sendFeedback({ Text.literal("✔ Aggiunto/Aggiornato $id → livello $level") }, false)
                                            1
                                        }
                                )
                        )
                )

                // /plusec remove <id>
                .then(
                    literal("remove")
                        .then(
                            argument("id", StringArgumentType.string())
                                .suggests(ENCH_SUGGEST)
                                .executes { ctx ->
                                    val source = ctx.source
                                    val idRaw = StringArgumentType.getString(ctx, "id")

                                    val player = currentPlayer(source)
                                        ?: return@executes err(source, "Devi essere un giocatore per usare questo comando.")

                                    val stack = mainHand(player)
                                        ?: return@executes err(source, "Tieni un oggetto nella mano principale.")

                                    val id: Identifier = MCCompat.parseEnchantmentId(idRaw)
                                        ?: return@executes err(source, "ID incantesimo non valido: $idRaw")

                                    val ok = try { MCCompat.removeEnchantment(stack, id) } catch (_: Throwable) { false }
                                    if (!ok) return@executes err(source, "Incantesimo non presente: $id")

                                    try { player.inventory.markDirty() } catch (_: Throwable) {}

                                    source.sendFeedback({ Text.literal("✔ Rimosso $id dall'oggetto in mano") }, false)
                                    1
                                }
                        )
                )

                // /plusec list
                .then(
                    literal("list")
                        .executes { ctx ->
                            val source = ctx.source
                            val player = currentPlayer(source)
                                ?: return@executes err(source, "Devi essere un giocatore per usare questo comando.")

                            val stack = mainHand(player)
                                ?: return@executes err(source, "Tieni un oggetto nella mano principale.")

                            val list = try { MCCompat.readEnchantments(stack) } catch (_: Throwable) { emptyList() }
                            if (list.isEmpty()) {
                                source.sendFeedback({ Text.literal("ℹ Nessun incantesimo presente sull'oggetto.") }, false)
                            } else {
                                val lines = list.joinToString(", ") { (id, lvl) -> "$id $lvl" }
                                source.sendFeedback({ Text.literal("Incantesimi: $lines") }, false)
                            }
                            1
                        }
                )
        )
    }

    // ------- Suggerimenti dinamici ----------
    private val ENCH_SUGGEST: SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { ctx, builder: SuggestionsBuilder ->
            val server = ctx.source.server
            val items = MCCompat.suggestStringsForEnchantments(server)
            for (s in items) builder.suggest(s)
            builder.buildFuture()
        }

    // ------- Helper locali -------
    private fun hasLevel(src: ServerCommandSource, level: Int): Boolean =
        try { src.hasPermissionLevel(level) } catch (_: Throwable) { true }

    private fun currentPlayer(src: ServerCommandSource): PlayerEntity? =
        try { src.player } catch (_: Throwable) {
            try { src.getPlayer() } catch (_: Throwable) { null }
        }

    /**
     * Restituisce l'ItemStack nella mano principale del giocatore, o null se vuoto.
     * Usa API certe per la tua versione (getMainHandStack) con fallback a inventory.getSelectedStack().
     */
    private fun mainHand(player: PlayerEntity): ItemStack? {
        return try {
            val s = player.getMainHandStack()
            if (!s.isEmpty) s else null
        } catch (_: Throwable) {
            try {
                val s2 = player.inventory.getSelectedStack()
                if (!s2.isEmpty) s2 else null
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun err(src: ServerCommandSource, msg: String): Int {
        try { src.sendError(Text.literal("❌ $msg")) } catch (_: Throwable) {}
        return 0
    }
}
