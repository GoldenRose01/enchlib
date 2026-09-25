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

import net.minecraft.commands.Commands.literal
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.resources.Identifier
import net.minecraft.core.registries.Registries
import net.minecraft.core.component.DataComponents

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

    private val GENERIC_ERROR = SimpleCommandExceptionType(Component.literal("Operazione fallita."))

    // Provider suggerimenti ID incantesimi dal registry runtime (inclusi modded)
    private val SUGGEST_ENCHANTMENTS: SuggestionProvider<CommandSourceStack> =
        SuggestionProvider { ctx, builder -> suggestEnchantments(ctx, builder) }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        // Costruisco builders separati per evitare ambiguità Kotlin su .then/argument

        val cmdReload: LiteralArgumentBuilder<CommandSourceStack> =
            literal("reload").executes { reload(it) }

        val cmdValidate: LiteralArgumentBuilder<CommandSourceStack> =
            literal("validate").executes { validate(it) }

        val cmdShowPath: LiteralArgumentBuilder<CommandSourceStack> =
            literal("show-path").executes { showPath(it) }

        val cmdListEnabled: LiteralArgumentBuilder<CommandSourceStack> =
            literal("list-enabled").executes { listEnabled(it) }

        // toggle <enchantment> <enabled>
        @Suppress("UNCHECKED_CAST")
        val argEnchToggle: RequiredArgumentBuilder<CommandSourceStack, String> =
            argument("enchantment", StringArgumentType.string()) as RequiredArgumentBuilder<CommandSourceStack, String>
        argEnchToggle.suggests(SUGGEST_ENCHANTMENTS)

        val argEnabled: RequiredArgumentBuilder<CommandSourceStack, Boolean> =
            argument("enabled", BoolArgumentType.bool())
                .executes { setToggle(it) }

        val cmdToggle: LiteralArgumentBuilder<CommandSourceStack> =
            literal("toggle").then(argEnchToggle.then(argEnabled))

        // setmax <enchantment> <level>
        @Suppress("UNCHECKED_CAST")
        val argEnchMax: RequiredArgumentBuilder<CommandSourceStack, String> =
            argument("enchantment", StringArgumentType.string()) as RequiredArgumentBuilder<CommandSourceStack, String>
        argEnchMax.suggests(SUGGEST_ENCHANTMENTS)

        val argLevel: RequiredArgumentBuilder<CommandSourceStack, Int> =
            argument("level", IntegerArgumentType.integer(1))
                .executes { setMax(it) }

        val cmdSetMax: LiteralArgumentBuilder<CommandSourceStack> =
            literal("setmax").then(argEnchMax.then(argLevel))

        // config read <id>: legge i valori effettivi dai file JSON5 globali.
        val configReadId = argument("enchantment", StringArgumentType.string())
            .suggests(SUGGEST_ENCHANTMENTS)
            .executes { readConfigEntry(it) }
        val configRead = literal("read").then(configReadId)

        // config write enabled <id> <true|false>
        val configWriteEnabledId = argument("enchantment", StringArgumentType.string())
            .suggests(SUGGEST_ENCHANTMENTS)
        val configWriteEnabledValue = argument("enabled", BoolArgumentType.bool())
            .executes { writeConfigEnabled(it) }
        val configWriteEnabled = literal("enabled").then(configWriteEnabledId.then(configWriteEnabledValue))

        // config write max-level <id> <level>
        val configWriteMaxId = argument("enchantment", StringArgumentType.string())
            .suggests(SUGGEST_ENCHANTMENTS)
        val configWriteMaxValue = argument("level", IntegerArgumentType.integer(1))
            .executes { writeConfigMaxLevel(it) }
        val configWriteMax = literal("max-level").then(configWriteMaxId.then(configWriteMaxValue))

        val config = literal("config")
            .then(configRead)
            .then(literal("write").then(configWriteEnabled).then(configWriteMax))

        // root
        val root: LiteralArgumentBuilder<CommandSourceStack> =
            literal("plusec-debug")
                .requires { it.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER) }

        // attach children
        root.then(cmdReload)
        root.then(cmdValidate)
        root.then(cmdShowPath)
        root.then(cmdListEnabled)
        root.then(cmdToggle)
        root.then(cmdSetMax)
        root.then(config)

        dispatcher.register(root)
    }

    // ----- subcommands -----

    // NB: in base al tuo errore più recente, assumo reloadConfigs() SENZA parametri.
    private fun reload(ctx: CommandContext<CommandSourceStack>): Int {
        return try {
            GlobalConfigManager.reloadConfigs()
            ctx.source.sendSuccess({ Component.literal("Config globale ricaricata da ${GlobalConfigIO.baseDir()}") }, true)
            Command.SINGLE_SUCCESS
        } catch (_: Throwable) {
            throw GENERIC_ERROR.create()
        }
    }

    private fun validate(ctx: CommandContext<CommandSourceStack>): Int {
        val server = ctx.source.server
        val issues = GlobalConfigManager.validateAgainstRegistry(server)
        if (issues.isEmpty()) {
            ctx.source.sendSuccess({ Component.literal("Validazione OK: tutti gli ID in config sono presenti nel registry.") }, false)
        } else {
            ctx.source.sendSuccess({ Component.literal("Problemi di validazione: ${issues.size}") }, false)
            issues.forEach { line -> ctx.source.sendSuccess({ Component.literal(line) }, false) }
        }
        return Command.SINGLE_SUCCESS
    }

    private fun showPath(ctx: CommandContext<CommandSourceStack>): Int {
        ctx.source.sendSuccess({ Component.literal("Percorso config globale: ${GlobalConfigIO.baseDir()}") }, false)
        return Command.SINGLE_SUCCESS
    }

    private fun listEnabled(ctx: CommandContext<CommandSourceStack>): Int {
        val list = EnchantLibAPI.getEnabledEnchantments()
        if (list.isEmpty()) {
            ctx.source.sendSuccess({ Component.literal("Nessun incantesimo abilitato in config globale.") }, false)
        } else {
            ctx.source.sendSuccess({ Component.literal("Incantesimi abilitati (${list.size}):") }, false)
            list.forEach { id -> ctx.source.sendSuccess({ Component.literal(" - $id") }, false) }
        }
        return Command.SINGLE_SUCCESS
    }

    // toggle handler
    private fun setToggle(ctx: CommandContext<CommandSourceStack>): Int {
        val idRaw = StringArgumentType.getString(ctx, "enchantment")
        val id = normalizeId(idRaw) ?: run {
            ctx.source.sendFailure(Component.literal("ID incantesimo non valido: '$idRaw'"))
            return 0
        }
        val enabled = BoolArgumentType.getBool(ctx, "enabled")
        EnchantLibAPI.setEnabled(id, enabled)
        ctx.source.sendSuccess({ Component.literal("Impostato $id -> enabled=$enabled (config globale)") }, true)
        return Command.SINGLE_SUCCESS
    }

    // setmax handler
    private fun setMax(ctx: CommandContext<CommandSourceStack>): Int {
        val idRaw = StringArgumentType.getString(ctx, "enchantment")
        val id = normalizeId(idRaw) ?: run {
            ctx.source.sendFailure(Component.literal("ID incantesimo non valido: '$idRaw'"))
            return 0
        }
        val lvl = IntegerArgumentType.getInteger(ctx, "level")
        EnchantLibAPI.setMaxLevel(id, lvl)
        ctx.source.sendSuccess({ Component.literal("Impostato $id -> max_level=$lvl (config globale)") }, true)
        return Command.SINGLE_SUCCESS
    }

    private fun readConfigEntry(ctx: CommandContext<CommandSourceStack>): Int {
        val raw = StringArgumentType.getString(ctx, "enchantment")
        val id = normalizeId(raw) ?: return commandError(ctx, "ID incantesimo non valido: '$raw'")
        val enabled = EnchantLibAPI.isEnabled(id)
        val maxLevel = EnchantLibAPI.getMaxLevel(id)
        ctx.source.sendSuccess({
            Component.literal(
                "$id: enabled=$enabled, max_level=${maxLevel ?: "non impostato"} " +
                    "(file: ${GlobalConfigIO.baseDir()})"
            )
        }, false)
        return Command.SINGLE_SUCCESS
    }

    private fun writeConfigEnabled(ctx: CommandContext<CommandSourceStack>): Int {
        val raw = StringArgumentType.getString(ctx, "enchantment")
        val id = normalizeId(raw) ?: return commandError(ctx, "ID incantesimo non valido: '$raw'")
        val enabled = BoolArgumentType.getBool(ctx, "enabled")
        return runCatching {
            EnchantLibAPI.setEnabled(id, enabled)
            ctx.source.sendSuccess({ Component.literal("Salvato $id: enabled=$enabled") }, true)
            Command.SINGLE_SUCCESS
        }.getOrElse { commandError(ctx, "Impossibile scrivere la configurazione: ${it.message ?: "errore I/O"}") }
    }

    private fun writeConfigMaxLevel(ctx: CommandContext<CommandSourceStack>): Int {
        val raw = StringArgumentType.getString(ctx, "enchantment")
        val id = normalizeId(raw) ?: return commandError(ctx, "ID incantesimo non valido: '$raw'")
        val level = IntegerArgumentType.getInteger(ctx, "level")
        return runCatching {
            EnchantLibAPI.setMaxLevel(id, level)
            ctx.source.sendSuccess({ Component.literal("Salvato $id: max_level=$level") }, true)
            Command.SINGLE_SUCCESS
        }.getOrElse { commandError(ctx, "Impossibile scrivere la configurazione: ${it.message ?: "errore I/O"}") }
    }

    private fun commandError(ctx: CommandContext<CommandSourceStack>, message: String): Int {
        ctx.source.sendFailure(Component.literal(message))
        return 0
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
        ctx: CommandContext<CommandSourceStack>,
        builder: com.mojang.brigadier.suggestion.SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val server = ctx.source.server
        val ids = MCCompat.listEnchantmentIds(server)
        ids.forEach { id -> builder.suggest(id.toString()) }
        return builder.buildFuture()
    }
}
