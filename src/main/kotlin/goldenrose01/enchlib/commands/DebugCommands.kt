package goldenrose01.enchlib.commands

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType

import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import net.minecraft.util.Identifier
import net.minecraft.registry.RegistryKeys
import net.minecraft.component.DataComponentTypes

import goldenrose01.enchlib.Enchlib
import goldenrose01.enchlib.api.EnchantLibAPI
import goldenrose01.enchlib.config.GlobalConfigManager
import goldenrose01.enchlib.config.GlobalConfigIO
import goldenrose01.enchlib.config.ConfigManager
import goldenrose01.enchlib.utils.msg
import goldenrose01.enchlib.utils.err
import goldenrose01.enchlib.utils.noop
import goldenrose01.enchlib.utils.ok
import goldenrose01.enchlib.compat.MCCompat
import goldenrose01.enchlib.utils.EnchLogger
import goldenrose01.enchlib.registry.EnchantmentRegistry

import java.util.concurrent.CompletableFuture
/**
 * /plusec-debug
 *
 * Comandi di diagnostica e gestione config **globale**.
 * - /plusec-debug reload
 * - /plusec-debug validate
 * - /plusec-debug show-path
 * - /plusec-debug toggle <enchantmentId> <enabled>
 * - /plusec-debug setmax <enchantmentId> <level>
 * - /plusec-debug list-enabled
 */

object DebugCommands {

    private val GENERIC_ERROR = SimpleCommandExceptionType(Text.literal("Operazione fallita."))

    // Provider suggerimenti ID incantesimi dal registry runtime (inclusi modded)
    private val SUGGEST_ENCHANTMENTS: SuggestionProvider<ServerCommandSource> =
        SuggestionProvider { ctx, builder -> suggestEnchantments(ctx, builder) }

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        // Costruisco builders separati per evitare ambiguità Kotlin su .then/argument

        val cmdReload: LiteralArgumentBuilder<ServerCommandSource> =
            literal("reload").executes { reload(it) }

        val cmdValidate: LiteralArgumentBuilder<ServerCommandSource> =
            literal("validate").executes { validate(it) }

        val cmdShowPath: LiteralArgumentBuilder<ServerCommandSource> =
            literal("show-path").executes { showPath(it) }

        val cmdListEnabled: LiteralArgumentBuilder<ServerCommandSource> =
            literal("list-enabled").executes { listEnabled(it) }

        // toggle <enchantment> <enabled>
        @Suppress("UNCHECKED_CAST")
        val argEnchToggle: RequiredArgumentBuilder<ServerCommandSource, String> =
            argument("enchantment", StringArgumentType.string()) as RequiredArgumentBuilder<ServerCommandSource, String>
        argEnchToggle.suggests(SUGGEST_ENCHANTMENTS)

        val argEnabled: RequiredArgumentBuilder<ServerCommandSource, Boolean> =
            argument("enabled", BoolArgumentType.bool())
                .executes { setToggle(it) }

        val cmdToggle: LiteralArgumentBuilder<ServerCommandSource> =
            literal("toggle").then(argEnchToggle.then(argEnabled))

        // setmax <enchantment> <level>
        @Suppress("UNCHECKED_CAST")
        val argEnchMax: RequiredArgumentBuilder<ServerCommandSource, String> =
            argument("enchantment", StringArgumentType.string()) as RequiredArgumentBuilder<ServerCommandSource, String>
        argEnchMax.suggests(SUGGEST_ENCHANTMENTS)

        val argLevel: RequiredArgumentBuilder<ServerCommandSource, Int> =
            argument("level", IntegerArgumentType.integer(1))
                .executes { setMax(it) }

        val cmdSetMax: LiteralArgumentBuilder<ServerCommandSource> =
            literal("setmax").then(argEnchMax.then(argLevel))

        // root
        val root: LiteralArgumentBuilder<ServerCommandSource> =
            literal("plusec-debug")
                .requires { it.hasPermissionLevel(2) }

        // attach children
        root.then(cmdReload)
        root.then(cmdValidate)
        root.then(cmdShowPath)
        root.then(cmdListEnabled)
        root.then(cmdToggle)
        root.then(cmdSetMax)

        dispatcher.register(root)
    }

    // ----- subcommands -----

    // NB: in base al tuo errore più recente, assumo reloadConfigs() SENZA parametri.
    private fun reload(ctx: CommandContext<ServerCommandSource>): Int {
        return try {
            GlobalConfigManager.reloadConfigs()
            ctx.source.sendFeedback({ Text.literal("Config globale ricaricata da ${GlobalConfigIO.baseDir()}") }, true)
            Command.SINGLE_SUCCESS
        } catch (_: Throwable) {
            throw GENERIC_ERROR.create()
        }
    }

    private fun validate(ctx: CommandContext<ServerCommandSource>): Int {
        val server = ctx.source.server
        val issues = GlobalConfigManager.validateAgainstRegistry(server)
        if (issues.isEmpty()) {
            ctx.source.sendFeedback({ Text.literal("Validazione OK: tutti gli ID in config sono presenti nel registry.") }, false)
        } else {
            ctx.source.sendFeedback({ Text.literal("Problemi di validazione: ${issues.size}") }, false)
            issues.forEach { line -> ctx.source.sendFeedback({ Text.literal(line) }, false) }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun showPath(ctx: CommandContext<ServerCommandSource>): Int {
        ctx.source.sendFeedback({ Text.literal("Percorso config globale: ${GlobalConfigIO.baseDir()}") }, false)
        return Command.SINGLE_SUCCESS
    }

    private fun listEnabled(ctx: CommandContext<ServerCommandSource>): Int {
        val list = EnchantLibAPI.getEnabledEnchantments()
        if (list.isEmpty()) {
            ctx.source.sendFeedback({ Text.literal("Nessun incantesimo abilitato in config globale.") }, false)
        } else {
            ctx.source.sendFeedback({ Text.literal("Incantesimi abilitati (${list.size}):") }, false)
            list.forEach { id -> ctx.source.sendFeedback({ Text.literal(" - $id") }, false) }
        }
        return Command.SINGLE_SUCCESS
    }

    // toggle handler
    private fun setToggle(ctx: CommandContext<ServerCommandSource>): Int {
        val idRaw = StringArgumentType.getString(ctx, "enchantment")
        val id = normalizeId(idRaw) ?: run {
            ctx.source.sendError(Text.literal("ID incantesimo non valido: '$idRaw'"))
            return 0
        }
        val enabled = BoolArgumentType.getBool(ctx, "enabled")
        EnchantLibAPI.setEnabled(id, enabled)
        ctx.source.sendFeedback({ Text.literal("Impostato $id -> enabled=$enabled (config globale)") }, true)
        return Command.SINGLE_SUCCESS
    }

    // setmax handler
    private fun setMax(ctx: CommandContext<ServerCommandSource>): Int {
        val idRaw = StringArgumentType.getString(ctx, "enchantment")
        val id = normalizeId(idRaw) ?: run {
            ctx.source.sendError(Text.literal("ID incantesimo non valido: '$idRaw'"))
            return 0
        }
        val lvl = IntegerArgumentType.getInteger(ctx, "level")
        EnchantLibAPI.setMaxLevel(id, lvl)
        ctx.source.sendFeedback({ Text.literal("Impostato $id -> max_level=$lvl (config globale)") }, true)
        return Command.SINGLE_SUCCESS
    }

    // ----- helpers -----

    /**
     * Normalizza l’ID:
     * - accetta "sharpness" → "minecraft:sharpness";
     * - verifica contro il registry; se non valido, ritorna null.
     */
    private fun normalizeId(input: String): String? {
        val parsed = MCCompat.parseEnchantmentId(input) ?: return null
        return parsed.toString()
    }

    /** Suggerimenti dinamici dagli enchant registrati a runtime (anche di altre mod). */
    private fun suggestEnchantments(
        ctx: CommandContext<ServerCommandSource>,
        builder: com.mojang.brigadier.suggestion.SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val server = ctx.source.server
        val ids = MCCompat.listEnchantmentIds(server)
        ids.forEach { id -> builder.suggest(id.toString()) }
        return builder.buildFuture()
    }
}