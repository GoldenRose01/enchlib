package goldenrose01.enchlib.commands

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument

import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import net.minecraft.util.Identifier
import net.minecraft.registry.RegistryKeys
import net.minecraft.component.DataComponentTypes

import goldenrose01.enchlib.Enchlib
import goldenrose01.enchlib.config.WorldConfigManager
import goldenrose01.enchlib.utils.msg
import goldenrose01.enchlib.utils.err
import goldenrose01.enchlib.utils.noop
import goldenrose01.enchlib.utils.ok


object DebugCommands {
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("plusec-debug")
                .then(
                    CommandManager.literal("reload")
                        .executes { ctx ->
                            val server = ctx.source.server
                            try {
                                WorldConfigManager.reloadConfigs(server)
                                ctx.source.sendFeedback(
                                    { Text.literal("Reload complete") },
                                    false
                                )
                                1
                            } catch (e: Exception) {
                                ctx.source.sendFeedback(
                                    { Text.literal("Reload failed: ${e.message}") },
                                    false
                                )
                                0
                            }
                        }
                )
                .then(
                    CommandManager.literal("validate")
                        .executes { ctx ->
                            val server = ctx.source.server
                            val issues = WorldConfigManager.validateAgainstRegistry(server)
                            if (issues.isEmpty()) {
                                ctx.source.sendFeedback(
                                    { Text.literal("No issues found in config") },
                                    false
                                )
                            } else {
                                ctx.source.sendFeedback(
                                    { Text.literal("Found issues:") },
                                    false
                                )
                                issues.forEach { issue ->
                                    ctx.source.sendFeedback({ Text.literal(" - $issue") }, false)
                                }
                            }
                            1
                        }
                )
        )
    }

    private fun showHelp(source: ServerCommandSource) {
        source.sendFeedback({ Text.literal("=== EnchLib Debug Commands ===") }, false)
        source.sendFeedback({ Text.literal("--stats           : Mostra statistiche generali") }, false)
        source.sendFeedback({ Text.literal("--check-configs   : Verifica configurazioni") }, false)
        source.sendFeedback({ Text.literal("--dump-config     : Dump completo configurazioni") }, false)
        source.sendFeedback({ Text.literal("--check-conflicts : Controlla conflitti incantesimi") }, false)
        source.sendFeedback({ Text.literal("--reload-configs  : Ricarica configurazioni") }, false)
        source.sendFeedback({ Text.literal("--registry-info   : Info registry incantesimi") }, false)
        source.sendFeedback({ Text.literal("--debug-toggle    : Attiva/disattiva debug mode") }, false)
    }

    private fun execute(source: ServerCommandSource, flag: String): Int {
        return when (flag) {
            "--stats" -> showStats(source)
            "--check-configs" -> checkConfigs(source)
            "--dump-config" -> dumpConfig(source)
            "--check-conflicts" -> checkConflicts(source)
            "--reload-configs" -> reloadConfigs(source)
            "--registry-info" -> showRegistryInfo(source)
            "--debug-toggle" -> toggleDebugMode(source)
            else -> {
                source.sendFeedback({ Text.literal("Flag sconosciuto: $flag. Usa /plusec-debug per vedere i flag disponibili.") }, false)
                0
            }
        }
    }

    private fun showStats(source: ServerCommandSource): Int {
        val availableEnchants = ConfigManager.availableEnchantments.size
        val maxLevels = ConfigManager.enchantmentLevelMax.size
        val compatibilities = ConfigManager.enchantmentCompatibility.size
        val categories = ConfigManager.enchantmentCategories.size
        val rarities = ConfigManager.enchantmentRarity.size
        val uncompat = ConfigManager.enchantmentUncompatibility.size

        source.sendFeedback({ Text.literal("=== EnchLib Statistics ===") }, false)
        source.sendFeedback({ Text.literal("Incantesimi disponibili: $availableEnchants") }, false)
        source.sendFeedback({ Text.literal("Livelli massimi configurati (override): $maxLevels") }, false)
        source.sendFeedback({ Text.literal("Compatibilità configurate: $compatibilities") }, false)
        source.sendFeedback({ Text.literal("Categorie configurate: $categories") }, false)
        source.sendFeedback({ Text.literal("Rarità configurate: $rarities") }, false)
        source.sendFeedback({ Text.literal("Incompatibilità configurate: $uncompat") }, false)
        source.sendFeedback({ Text.literal("Debug mode: ${if (EnchLogger.debugMode) "ON" else "OFF"}") }, false)
        return 1
    }

    private fun checkConfigs(source: ServerCommandSource): Int {
        source.sendFeedback({ Text.literal("=== Controllo Configurazioni ===") }, false)
        var issues = 0

        val availableIds = ConfigManager.availableEnchantments.map { it.id }.toSet()
        val registryIds = EnchantmentRegistry.all().keys.toSet()

        val missingInRegistry = availableIds - registryIds
        val extraInRegistry = registryIds - availableIds

        if (missingInRegistry.isNotEmpty()) {
            source.sendFeedback({ Text.literal("⚠️ Incantesimi disponibili ma non nel registry: ${missingInRegistry.size}") }, false)
            issues++
        }
        if (extraInRegistry.isNotEmpty()) {
            source.sendFeedback({ Text.literal("ℹ️ Incantesimi nel registry ma non configurati come disponibili: ${extraInRegistry.size}") }, false)
        }

        val incompatibilityIssues = checkIncompatibilityCircularity()
        if (incompatibilityIssues > 0) {
            source.sendFeedback({ Text.literal("⚠️ Problemi di incompatibilità rilevati: $incompatibilityIssues") }, false)
            issues += incompatibilityIssues
        }

        if (issues == 0) {
            source.sendFeedback({ Text.literal("✅ Tutte le configurazioni sembrano valide!") }, false)
        } else {
            source.sendFeedback({ Text.literal("❌ Rilevati $issues problemi di configurazione") }, false)
        }
        return 1
    }

    private fun dumpConfig(source: ServerCommandSource): Int {
        source.sendFeedback({ Text.literal("=== Dump Configurazioni ===") }, false)

        source.sendFeedback({ Text.literal("Incantesimi disponibili (primi 5):") }, false)
        ConfigManager.availableEnchantments.take(5).forEach {
            source.sendFeedback({ Text.literal("  ${it.id} -> ${it.sources.joinToString(",")}") }, false)
        }

        source.sendFeedback({ Text.literal("Livelli massimi (override):") }, false)
        ConfigManager.enchantmentLevelMax.entries.take(5).forEach { (id, level) ->
            source.sendFeedback({ Text.literal("  $id -> $level") }, false)
        }

        source.sendFeedback({ Text.literal("Rarità (primi 5):") }, false)
        ConfigManager.enchantmentRarity.entries.take(5).forEach { (id, rarity) ->
            source.sendFeedback({ Text.literal("  $id -> $rarity") }, false)
        }

        source.sendFeedback({ Text.literal("Compatibilità:") }, false)
        ConfigManager.enchantmentCompatibility.entries.take(5).forEach { (id, groups) ->
            source.sendFeedback({ Text.literal("  $id -> ${groups.joinToString(",")}") }, false)
        }

        source.sendFeedback({ Text.literal("Categorie:") }, false)
        ConfigManager.enchantmentCategories.entries.take(5).forEach { (id, groups) ->
            source.sendFeedback({ Text.literal("  $id -> ${groups.joinToString(",")}") }, false)
        }

        source.sendFeedback({ Text.literal("Incompatibilità:") }, false)
        ConfigManager.enchantmentUncompatibility.entries.take(5).forEach { (id, groups) ->
            source.sendFeedback({ Text.literal("  $id -> ${groups.joinToString(",")}") }, false)
        }

        EnchLogger.debug("=== FULL CONFIG DUMP ===")
        EnchLogger.debug("Available enchantments: ${ConfigManager.availableEnchantments}")
        EnchLogger.debug("Max levels: ${ConfigManager.enchantmentLevelMax}")
        EnchLogger.debug("Compatibilities: ${ConfigManager.enchantmentCompatibility}")
        EnchLogger.debug("Categories: ${ConfigManager.enchantmentCategories}")
        EnchLogger.debug("Incompatibilities: ${ConfigManager.enchantmentUncompatibility}")
        EnchLogger.debug("Rarities: ${ConfigManager.enchantmentRarity}")
        return 1
    }

    private fun checkConflicts(source: ServerCommandSource): Int {
        source.sendFeedback({ Text.literal("=== Controllo Conflitti ===") }, false)
        var conflicts = 0

        ConfigManager.enchantmentUncompatibility.forEach { (enchant, incompatibleList) ->
            incompatibleList.forEach { incompatible ->
                val reverse = ConfigManager.enchantmentUncompatibility[incompatible]
                if (reverse?.contains(enchant) != true) {
                    source.sendFeedback({ Text.literal("⚠️ Incompatibilità non bidirezionale: $enchant vs $incompatible") }, false)
                    conflicts++
                }
            }
        }

        if (conflicts == 0) {
            source.sendFeedback({ Text.literal("✅ Nessun conflitto rilevato!") }, false)
        } else {
            source.sendFeedback({ Text.literal("❌ Rilevati $conflicts conflitti") }, false)
        }
        return 1
    }

    private fun reloadConfigs(source: ServerCommandSource): Int {
        source.sendFeedback({ Text.literal("🔄 Ricaricamento configurazioni...") }, false)
        return try {
            ConfigManager.loadAll()
            source.sendFeedback({ Text.literal("✅ Configurazioni ricaricate con successo!") }, false)
            EnchLogger.info("Configurazioni ricaricate tramite comando debug")
            1
        } catch (e: Exception) {
            source.sendFeedback({ Text.literal("❌ Errore durante il ricaricamento: ${e.message}") }, false)
            EnchLogger.error("Errore ricaricamento configurazioni", e)
            0
        }
    }

    private fun showRegistryInfo(source: ServerCommandSource): Int {
        val all = EnchantmentRegistry.all()
        source.sendFeedback({ Text.literal("=== Registry Incantesimi ===") }, false)
        source.sendFeedback({ Text.literal("Totale incantesimi registrati: ${all.size}") }, false)

        val byNamespace = all.keys.groupBy { it.substringBefore(":") }
        byNamespace.forEach { (ns, list) ->
            source.sendFeedback({ Text.literal("$ns: ${list.size} incantesimi") }, false)
        }

        source.sendFeedback({ Text.literal("Esempi (primi 10):") }, false)
        all.keys.take(10).forEach { id ->
            source.sendFeedback({ Text.literal("  $id") }, false)
        }
        return 1
    }

    private fun toggleDebugMode(source: ServerCommandSource): Int {
        EnchLogger.debugMode = !EnchLogger.debugMode
        val status = if (EnchLogger.debugMode) "attivata" else "disattivata"
        source.sendFeedback({ Text.literal("🔧 Modalità debug $status") }, false)
        EnchLogger.info("Debug mode toggled to: ${EnchLogger.debugMode}")
        return 1
    }

    private fun checkIncompatibilityCircularity(): Int {
        var issues = 0
        ConfigManager.enchantmentUncompatibility.forEach { (enchant, incompatibleList) ->
            incompatibleList.forEach { incompatible ->
                val reverse = ConfigManager.enchantmentUncompatibility[incompatible]
                if (reverse?.contains(enchant) != true) {
                    EnchLogger.warn("Incompatibilità non bidirezionale: $enchant -> $incompatible")
                    issues++
                }
            }
        }
        return issues
    }
}
}
