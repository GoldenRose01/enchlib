package goldenrose01.enchlib.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import java.util.concurrent.CompletableFuture

import goldenrose01.enchlib.compat.MCCompat
import goldenrose01.enchlib.config.GlobalConfigIO

object EnchLibCommands {

    private const val PERM = 2

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
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

                                            val ok = try { MCCompat.upsertEnchantment(stack, id, level, source.server) } catch (_: Throwable) { false }
                                            if (!ok) return@executes err(source, "Impossibile applicare l'incantesimo (NBT/API).")

                                            try { player.inventory.setChanged() } catch (_: Throwable) {}

                                            source.sendSuccess({ Component.literal("✔ Aggiunto/Aggiornato $id → livello $level") }, false)
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

                                    try { player.inventory.setChanged() } catch (_: Throwable) {}

                                    source.sendSuccess({ Component.literal("✔ Rimosso $id dall'oggetto in mano") }, false)
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
                                source.sendSuccess({ Component.literal("ℹ Nessun incantesimo presente sull'oggetto.") }, false)
                            } else {
                                val lines = list.joinToString(", ") { (id, lvl) -> "$id $lvl" }
                                source.sendSuccess({ Component.literal("Incantesimi: $lines") }, false)
                            }
                            1
                        }
                )

                // /plusec clear
                .then(
                    literal("clear")
                        .executes { ctx ->
                            val source = ctx.source
                            val player = currentPlayer(source)
                                ?: return@executes err(source, "Devi essere un giocatore.")

                            val stack = mainHand(player)
                                ?: return@executes err(source, "Mano vuota.")

                            val componentType = if (stack.item == net.minecraft.world.item.Items.ENCHANTED_BOOK) {
                                DataComponents.STORED_ENCHANTMENTS
                            } else {
                                DataComponents.ENCHANTMENTS
                            }
                            val hadEnchantments = stack.get(componentType) != null
                            if (!hadEnchantments) {
                                return@executes err(source, "L'oggetto non ha incantesimi.")
                            }

                            stack.remove(componentType)

                            try { player.inventory.setChanged() } catch (_: Throwable) {}
                            source.sendSuccess({ Component.literal("✨ Rimossi tutti gli incantesimi.") }, false)
                            1
                        }
                )

                // /plusec repair: ripristina tutta la durabilità
                .then(
                    literal("repair")
                        .executes { ctx ->
                            val source = ctx.source
                            val player = currentPlayer(source)
                                ?: return@executes err(source, "Devi essere un giocatore.")
                            val stack = mainHand(player)
                                ?: return@executes err(source, "Tieni un oggetto nella mano principale.")
                            if (!stack.isDamageableItem()) {
                                return@executes err(source, "L'oggetto non ha durabilità.")
                            }

                            stack.setDamageValue(0)
                            player.inventory.setChanged()
                            source.sendSuccess({ Component.literal("✔ Oggetto riparato completamente.") }, false)
                            1
                        }
                )

                // /plusec setdurability <percentuale residua>
                .then(
                    literal("setdurability")
                        .then(
                            argument("value", IntegerArgumentType.integer())
                                .executes { ctx ->
                                    val source = ctx.source
                                    val player = currentPlayer(source)
                                        ?: return@executes err(source, "Devi essere un giocatore.")
                                    val stack = mainHand(player)
                                        ?: return@executes err(source, "Tieni un oggetto nella mano principale.")
                                    if (!stack.isDamageableItem()) {
                                        return@executes err(source, "L'oggetto non ha durabilità.")
                                    }

                                    val percent = IntegerArgumentType.getInteger(ctx, "value")
                                    if (percent !in 0..100) {
                                        return@executes err(source, "La percentuale deve essere compresa tra 0 e 100.")
                                    }
                                    val maxDamage = stack.maxDamage
                                    val remaining = (maxDamage.toDouble() * percent / 100.0).toInt()
                                    val damage = (maxDamage - remaining).coerceAtMost(maxDamage - 1).coerceAtLeast(0)
                                    stack.setDamageValue(damage)
                                    player.inventory.setChanged()
                                    source.sendSuccess({ Component.literal("✔ Durabilità impostata al $percent%.") }, false)
                                    1
                                }
                        )
                )

                // /plusec damage +/-<punti>: meno danneggia, più ripara
                .then(
                    literal("damage")
                        .then(
                            argument("delta", StringArgumentType.word())
                                .executes { ctx ->
                                    val source = ctx.source
                                    val player = currentPlayer(source)
                                        ?: return@executes err(source, "Devi essere un giocatore.")
                                    val stack = mainHand(player)
                                        ?: return@executes err(source, "Tieni un oggetto nella mano principale.")
                                    if (!stack.isDamageableItem()) {
                                        return@executes err(source, "L'oggetto non ha durabilità.")
                                    }

                                    val rawDelta = StringArgumentType.getString(ctx, "delta")
                                    if (!rawDelta.matches(Regex("^[+-]\\d+$"))) {
                                        return@executes err(source, "Indica un valore con segno, per esempio -20 o +20.")
                                    }
                                    val delta = rawDelta.toIntOrNull()
                                        ?: return@executes err(source, "Valore fuori intervallo.")
                                    if (delta == 0) {
                                        return@executes err(source, "Il valore deve essere diverso da zero.")
                                    }

                                    val maxDamage = stack.maxDamage
                                    val targetDamage = (stack.damageValue.toLong() - delta.toLong())
                                        .coerceIn(0L, (maxDamage - 1).toLong())
                                        .toInt()
                                    stack.setDamageValue(targetDamage)
                                    player.inventory.setChanged()
                                    val action = if (delta < 0) "danneggiato" else "riparato"
                                    source.sendSuccess({ Component.literal("✔ Oggetto $action; durabilità residua ${maxDamage - targetDamage}/$maxDamage.") }, false)
                                    1
                                }
                        )
                )

                // /plusec info <id>: stato runtime e dettagli salvati in config.
                .then(
                    literal("info")
                        .then(
                            argument("id", StringArgumentType.string())
                                .suggests(ENCH_SUGGEST)
                                .executes { ctx ->
                                    val source = ctx.source
                                    val rawId = StringArgumentType.getString(ctx, "id")
                                    val id = MCCompat.parseEnchantmentId(rawId)
                                        ?: return@executes err(source, "ID incantesimo non valido: $rawId")
                                    val runtimeEnchantment = MCCompat.getEnchantment(source.server, id)
                                    val configured = try {
                                        val idString = id.toString()
                                        val available = GlobalConfigIO.readAvailable()
                                            .firstOrNull { it.id == idString }
                                        val details = GlobalConfigIO.readDetails()
                                            .enchantments.firstOrNull { it.id == idString }
                                        available to details
                                    } catch (_: Throwable) { null to null }
                                    val enabled = configured.first?.enabled ?: false
                                    val configuredDetails = configured.second

                                    source.sendSuccess({
                                        Component.literal(
                                            "$id — runtime=${if (runtimeEnchantment != null) "presente" else "assente"}, " +
                                        "config=${if (configured.first != null || configuredDetails != null) "presente" else "assente"}, " +
                                                "abilitato=$enabled, max_level=${configuredDetails?.max_level ?: "non impostato"}, " +
                                                "categorie=${configuredDetails?.enc_category?.joinToString().orEmpty().ifBlank { "nessuna" }}, " +
                                                "mob=${configuredDetails?.mob_category?.joinToString().orEmpty().ifBlank { "nessuna" }}"
                                        )
                                    }, false)
                                    1
                                }
                        )
                )
        )
    }

    // ------- Suggerimenti dinamici ----------
    private val ENCH_SUGGEST: SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { ctx, builder: SuggestionsBuilder ->
            val server = ctx.source.server
            val items = MCCompat.suggestStringsForEnchantments(server)
            for (s in items) builder.suggest(s)
            builder.buildFuture()
        }

    // ------- Helper locali -------
    private fun hasLevel(src: CommandSourceStack, level: Int): Boolean =
        try { src.permissions().hasPermission(if (level >= 4) net.minecraft.server.permissions.Permissions.COMMANDS_OWNER else if (level >= 3) net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN else if (level >= 2) net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER else net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR) } catch (_: Throwable) { true }

    private fun currentPlayer(src: CommandSourceStack): Player? =
        try { src.player } catch (_: Throwable) {
            try { src.getPlayer() } catch (_: Throwable) { null }
        }

    /**
     * Restituisce l'ItemStack nella mano principale del giocatore, o null se vuoto.
     * Usa API certe per la tua versione (getMainHandStack) con fallback a inventory.getSelectedStack().
     */
    private fun mainHand(player: Player): ItemStack? {
        return try {
            val s = player.getMainHandItem()
            if (!s.isEmpty) s else null
        } catch (_: Throwable) {
            try {
                val s2 = player.inventory.getSelectedItem()
                if (!s2.isEmpty) s2 else null
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun err(src: CommandSourceStack, msg: String): Int {
        try { src.sendFailure(Component.literal("❌ $msg")) } catch (_: Throwable) {}
        return 0
    }
}
