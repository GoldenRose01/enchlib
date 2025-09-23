package goldenrose01.enchlib.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EnchantmentArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

// In Yarn 1.21 ItemStack e Items risiedono in net.minecraft.item.
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

// EnchantmentHelper è in net.minecraft.enchantment.
import net.minecraft.enchantment.EnchantmentHelper

// Usiamo Registries per recuperare gli ID degli incantesimi.
import net.minecraft.registry.Registries

import goldenrose01.enchlib.config.ConfigManager

/**
 * Comando /plusec con sottocomandi add e remove per gestire gli incantesimi
 * sugli oggetti in mano, rispettando i limiti definiti nei file di configurazione.
 */
object EnchLibCommands {
    fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("plusec")
                    .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) }
                    .then(
                        // /plusec add <enchantment> <level>
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
                                        val enchantment = EnchantmentArgumentType.getEnchantment(context, "enchantment")
                                        val level = IntegerArgumentType.getInteger(context, "level")
                                        val player = context.source.player ?: return@executes 0
                                        val stack: ItemStack = player.mainHandStack
                                        if (!stack.isEnchantable || stack.item === Items.AIR) {
                                            context.source.sendError(Text.literal("L'oggetto in mano non può essere incantato"))
                                            return@executes 0
                                        }
                                        // Ottieni l'ID dell'incantesimo come stringa
                                        val id: String = Registries.ENCHANTMENT.getId(enchantment.value()).toString()
                                        if (!ConfigManager.isEnchantmentEnabled(id)) {
                                            context.source.sendError(Text.literal("L'incantesimo $id non è abilitato nella configurazione"))
                                            return@executes 0
                                        }
                                        val maxLevel = ConfigManager.getMaxLevel(id)
                                        if (level > maxLevel) {
                                            context.source.sendError(
                                                Text.literal("Il livello specificato ($level) supera il limite massimo ($maxLevel) per l'incantesimo $id")
                                            )
                                            return@executes 0
                                        }
                                        // Applica l'incantesimo al livello richiesto
                                        stack.enchant(enchantment, level)
                                        context.source.sendFeedback(
                                            Text.literal("Aggiunto l'incantesimo $id livello $level all'oggetto in mano"),
                                            false
                                        )
                                        1
                                    }
                                )
                            )
                    )
                    .then(
                        // /plusec remove <enchantment>
                        CommandManager.literal("remove")
                            .then(
                                CommandManager.argument(
                                    "enchantment",
                                    EnchantmentArgumentType.enchantment()
                                ).executes { context ->
                                    val enchantment = EnchantmentArgumentType.getEnchantment(context, "enchantment")
                                    val player = context.source.player ?: return@executes 0
                                    val stack: ItemStack = player.mainHandStack
                                    // Controlla se l'incantesimo è presente sullo stack
                                    val currentLevel = EnchantmentHelper.getLevel(enchantment, stack)
                                    if (currentLevel > 0) {
                                        // Rimuove l'incantesimo aggiornando i componenti dati
                                        EnchantmentHelper.updateEnchantments(stack) { mutable ->
                                            mutable.removeIf { holder -> holder.value() == enchantment.value() }
                                        }
                                        val idToString = Registries.ENCHANTMENT.getId(enchantment.value()).toString()
                                        context.source.sendFeedback(
                                            Text.literal("Rimosso l'incantesimo $idToString dall'oggetto in mano"),
                                            false
                                        )
                                        1
                                    } else {
                                        val idToString = Registries.ENCHANTMENT.getId(enchantment.value()).toString()
                                        context.source.sendError(
                                            Text.literal("L'oggetto in mano non ha l'incantesimo $idToString")
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
