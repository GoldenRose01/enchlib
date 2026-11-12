package goldenrose01.enchlib.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import goldenrose01.enchlib.tree.TreeCutConfig
import goldenrose01.enchlib.tree.TreeCutMode
import goldenrose01.enchlib.weapons.AxesAreWeaponsConfig
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permissions

object GameSetCommand {

    private val SUGGEST_MODES = SuggestionProvider<CommandSourceStack> { _, builder ->
        builder.suggest("off")
        builder.suggest("fell")
        builder.suggest("chop")
        builder.buildFuture()
    }

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val root = literal("gameset")
            .then(
                literal("treecut")
                    .executes { ctx ->
                        val current = TreeCutConfig.getMode()
                        ctx.source.sendSuccess(
                            { Component.literal("ℹ Modalità taglio alberi attuale: ${current.id}") },
                            false
                        )
                        1
                    }
                    .then(
                        argument("mode", StringArgumentType.word())
                            .requires { src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) }
                            .suggests(SUGGEST_MODES)
                            .executes { ctx ->
                                val modeRaw = StringArgumentType.getString(ctx, "mode")
                                val mode = TreeCutMode.fromString(modeRaw)
                                TreeCutConfig.setMode(mode)
                                ctx.source.sendSuccess(
                                    { Component.literal("✔ Modalità taglio alberi impostata su: ${mode.id}") },
                                    true
                                )
                                1
                            }
                    )
            )

        // Support both /gameset axesareweapons and /gameset axesarewepons
        for (name in listOf("axesareweapons", "axesarewepons")) {
            root.then(
                literal(name)
                    .executes { ctx ->
                        val current = AxesAreWeaponsConfig.isEnabled()
                        ctx.source.sendSuccess(
                            { Component.literal("ℹ Axes as weapons abilitato: $current") },
                            false
                        )
                        1
                    }
                    .then(
                        argument("enabled", BoolArgumentType.bool())
                            .requires { src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) }
                            .executes { ctx ->
                                val value = BoolArgumentType.getBool(ctx, "enabled")
                                AxesAreWeaponsConfig.setEnabled(value)
                                ctx.source.sendSuccess(
                                    { Component.literal("✔ Axes as weapons impostato su: $value") },
                                    true
                                )
                                1
                            }
                    )
            )
        }

        dispatcher.register(root)
    }
}
