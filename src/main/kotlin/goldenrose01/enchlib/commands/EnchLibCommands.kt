package goldenrose01.enchlib.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EnchantmentArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import goldenrose01.enchlib.config.ConfigManager

/**
 * Registra il comando /plusec. Uso:
 * /plusec add <incantesimo> <livello> – aggiunge un incantesimo all'oggetto in mano.
 * /plusec remove <incantesimo> – rimuove un incantesimo dall'oggetto in mano.
 */
object EnchLibCommands {
    fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("plusec")
                    .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) }
                    .then(
                        CommandManager.literal("add")
                            .then(
                                CommandManager.argument(
                                    "enchantment",
                                    EnchantmentArgumentType.enchantment()
                                ).then(
                                    CommandManager.argument(
                                        "level",
                                        IntegerArgumentType.integer(1)
                                    ).executes { context ->
                                        val enchantment =
                                            EnchantmentArgumentType.getEnchantment(context, "enchantment")
                                        val level = IntegerArgumentType.getInteger(context, "level")
                                        val player = context.source.player ?: return@executes 0
                                        val stack: ItemStack = player.mainHandStack
                                        // Verifica che l’oggetto sia incantabile
                                        if (!stack.isEnchantable || stack.item === Items.AIR) {
                                            context.source.sendError(
                                                Text.literal("L'oggetto in mano non può essere incantato")
                                            )
                                            return@executes 0
                                        }
                                        val id = enchantment.registry.asString()
                                        // Controlla se l'incantesimo è abilitato
                                        if (!ConfigManager.isEnchantmentEnabled(id)) {
                                            context.source.sendError(
                                                Text.literal("L'incantesimo $id non è abilitato nella configurazione")
                                            )
                                            return@executes 0
                                        }
                                        // Controlla il livello massimo
                                        val maxLevel = ConfigManager.getMaxLevel(id)
                                        if (level > maxLevel) {
                                            context.source.sendError(
                                                Text.literal(
                                                    "Il livello specificato ($level) supera il limite massimo ($maxLevel) per l'incantesimo $id"
                                                )
                                            )
                                            return@executes 0
                                        }
                                        // Applica l'incantesimo
                                        stack.addEnchantment(enchantment, level)
                                        context.source.sendFeedback(
                                            Text.literal(
                                                "Aggiunto l'incantesimo $id livello $level all'oggetto in mano"
                                            ),
                                            false
                                        )
                                        1
                                    }
                                )
                            )
                    )
                    .then(
                        CommandManager.literal("remove")
                            .then(
                                CommandManager.argument(
                                    "enchantment",
                                    EnchantmentArgumentType.enchantment()
                                ).executes { context ->
                                    val enchantment =
                                        EnchantmentArgumentType.getEnchantment(context, "enchantment")
                                    val player = context.source.player ?: return@executes 0
                                    val stack: ItemStack = player.mainHandStack
                                    val enchantments = EnchantmentHelper.get(stack)
                                    if (enchantments.containsKey(enchantment)) {
                                        enchantments.remove(enchantment)
                                        EnchantmentHelper.set(enchantments, stack)
                                        context.source.sendFeedback(
                                            Text.literal(
                                                "Rimosso l'incantesimo ${enchantment.registry.asString()} dall'oggetto in mano"
                                            ),
                                            false
                                        )
                                        1
                                    } else {
                                        context.source.sendError(
                                            Text.literal(
                                                "L'oggetto in mano non ha l'incantesimo ${enchantment.registry.asString()}"
                                            )
                                        )
                                        0
                                    }
                                }
                            )
                    )
            )
        }
    }
}
