package goldenrose01.enchlib.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.enchantment.Enchantment
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import goldenrose01.enchlib.config.ConfigManager
import goldenrose01.enchlib.utils.EnchLogger
import goldenrose01.enchlib.utils.msg
import goldenrose01.enchlib.utils.err

// Helper sicuro per ottenere l'ID "namespace:path" da un RegistryEntry<Enchantment>
private fun RegistryEntry<Enchantment>.idString(): String =
    this.getKey().map { it.value.toString() }.orElse("unknown:enchantment")

object EnchLibCommands {

    fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, _ ->
            val enchArg = RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENCHANTMENT)

            dispatcher.register(
                CommandManager.literal("plusec")
                    .requires { source: ServerCommandSource -> source.hasPermissionLevel(2) }
                    .executes { ctx ->
                        val s = ctx.source
                        s.sendFeedback({ Text.literal("=== EnchLib Commands ===") }, false)
                        s.sendFeedback({ Text.literal("/plusec add <enchantment> [level]  : Aggiungi incantesimo") }, false)
                        s.sendFeedback({ Text.literal("/plusec remove <enchantment>       : Rimuovi incantesimo") }, false)
                        s.sendFeedback({ Text.literal("/plusec list                       : Lista incantesimi item") }, false)
                        s.sendFeedback({ Text.literal("/plusec clear                      : Rimuovi tutti incantesimi") }, false)
                        s.sendFeedback({ Text.literal("/plusec info <enchantment>         : Info incantesimo") }, false)
                        s.sendFeedback({ Text.literal("/plusec-debug [flag]               : Comandi debug") }, false)
                        1
                    }
                    .then(
                        CommandManager.literal("add")
                            .then(
                                CommandManager.argument("enchantment", enchArg)
                                    .then(
                                        CommandManager.argument("level", IntegerArgumentType.integer(1))
                                            .executes { ctx -> executeAdd(ctx) }
                                    )
                                    .executes { ctx -> executeAddDefault(ctx) }
                            )
                    )
                    .then(
                        CommandManager.literal("remove")
                            .then(
                                CommandManager.argument("enchantment", enchArg)
                                    .executes { ctx -> executeRemove(ctx) }
                            )
                    )
                    .then(
                        CommandManager.literal("list")
                            .executes { ctx -> listEnchantments(ctx.source) }
                    )
                    .then(
                        CommandManager.literal("clear")
                            .executes { ctx -> clearEnchantments(ctx.source) }
                    )
                    .then(
                        CommandManager.literal("info")
                            .then(
                                CommandManager.argument("enchantment", enchArg)
                                    .executes { ctx -> showEnchantmentInfo(ctx) }
                            )
                    )
            )
        }
    }

    private fun executeAdd(context: CommandContext<ServerCommandSource>): Int {
        val enchantRef: RegistryEntry.Reference<Enchantment> =
            RegistryEntryReferenceArgumentType.getEnchantment(context, "enchantment")
        val level = IntegerArgumentType.getInteger(context, "level")
        return addEnchantmentToHeld(context.source, enchantRef, level)
    }

    private fun executeAddDefault(context: CommandContext<ServerCommandSource>): Int {
        val enchantRef: RegistryEntry.Reference<Enchantment> =
            RegistryEntryReferenceArgumentType.getEnchantment(context, "enchantment")
        return addEnchantmentToHeld(context.source, enchantRef, 1)
    }

    private fun addEnchantmentToHeld(
        source: ServerCommandSource,
        enchantment: RegistryEntry<Enchantment>,
        level: Int
    ): Int {
        val player = source.player ?: return 0
        val stack: ItemStack = player.mainHandStack

        if (!stack.isEnchantable || stack.item === Items.AIR) {
            source.err("L'oggetto in mano non può essere incantato")
            return 0
        }

        val id = enchantment.idString()

        if (!ConfigManager.isEnchantmentEnabled(id)) {
            source.err("L'incantesimo $id non è abilitato nella configurazione")
            return 0
        }

        val maxLevel = ConfigManager.getMaxLevel(id)
        if (level > maxLevel) {
            source.err("Il livello specificato ($level) supera il limite massimo ($maxLevel) per l'incantesimo $id")
            return 0
        }

        // Scrittura via Data Components (1.21+)
        val current = stack.get(DataComponentTypes.ENCHANTMENTS) ?: ItemEnchantmentsComponent.DEFAULT
        val builder = ItemEnchantmentsComponent.Builder(current)
        builder.set(enchantment, level)
        stack.set(DataComponentTypes.ENCHANTMENTS, builder.build())

        source.msg("✅ Aggiunto l'incantesimo $id livello $level all'oggetto in mano")
        EnchLogger.debug("Applicato incantesimo $id livello $level da ${source.name}")
        return 1
    }

    private fun executeRemove(context: CommandContext<ServerCommandSource>): Int {
        val enchantRef: RegistryEntry.Reference<Enchantment> =
            RegistryEntryReferenceArgumentType.getEnchantment(context, "enchantment")
        val source = context.source
        val player = source.player ?: return 0
        val stack: ItemStack = player.mainHandStack

        val comp = stack.get(DataComponentTypes.ENCHANTMENTS) ?: ItemEnchantmentsComponent.DEFAULT
        val currentLevel = comp.getLevel(enchantRef)

        if (currentLevel > 0) {
            // Ricostruisce senza l’incantesimo target
            val rebuilt = ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT).also { b ->
                comp.getEnchantments().forEach { entry ->
                    if (entry != enchantRef) b.set(entry, comp.getLevel(entry))
                }
            }.build()
            stack.set(DataComponentTypes.ENCHANTMENTS, rebuilt)

            val id = enchantRef.idString()
            source.msg("✅ Rimosso l'incantesimo $id dall'oggetto in mano")
            EnchLogger.debug("Rimosso incantesimo $id da ${source.name}")
            return 1
        } else {
            val id = enchantRef.idString()
            source.err("L'oggetto in mano non ha l'incantesimo $id")
            return 0
        }
    }

    private fun listEnchantments(source: ServerCommandSource): Int {
        val player = source.player ?: return 0
        val stack: ItemStack = player.mainHandStack
        if (stack.item === Items.AIR) {
            source.err("Nessun oggetto in mano")
            return 0
        }

        val comp = stack.get(DataComponentTypes.ENCHANTMENTS) ?: ItemEnchantmentsComponent.DEFAULT
        val all = comp.getEnchantments()
        if (all.isEmpty()) {
            source.msg("L'oggetto in mano non ha incantesimi")
            return 1
        }

        source.sendFeedback({ Text.literal("=== Incantesimi su ${stack.item.name.string} ===") }, false)
        all.forEach { e ->
            val id = e.idString()
            val lvl = comp.getLevel(e)
            val max = ConfigManager.getMaxLevel(id)
            val rarity = ConfigManager.enchantmentRarity[id] ?: "unknown"
            source.sendFeedback({ Text.literal("$id: $lvl/$max ($rarity)") }, false)
        }
        return 1
    }

    private fun clearEnchantments(source: ServerCommandSource): Int {
        val player = source.player ?: return 0
        val stack: ItemStack = player.mainHandStack
        if (stack.item === Items.AIR) {
            source.err("Nessun oggetto in mano")
            return 0
        }

        val comp = stack.get(DataComponentTypes.ENCHANTMENTS) ?: ItemEnchantmentsComponent.DEFAULT
        val count = comp.getEnchantments().size
        if (count == 0) {
            source.msg("L'oggetto in mano non ha incantesimi da rimuovere")
            return 1
        }

        stack.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
        source.msg("✅ Rimossi $count incantesimi dall'oggetto in mano")
        EnchLogger.debug("Rimossi tutti gli incantesimi ($count) da ${source.name}")
        return 1
    }

    private fun showEnchantmentInfo(context: CommandContext<ServerCommandSource>): Int {
        val enchantRef: RegistryEntry.Reference<Enchantment> =
            RegistryEntryReferenceArgumentType.getEnchantment(context, "enchantment")
        val source = context.source
        val id = enchantRef.idString()

        source.sendFeedback({ Text.literal("=== Info Incantesimo: $id ===") }, false)

        val isEnabled = ConfigManager.isEnchantmentEnabled(id)
        val maxLevel = ConfigManager.getMaxLevel(id)
        val rarity = ConfigManager.enchantmentRarity[id] ?: "non configurata"

        source.sendFeedback({ Text.literal("Abilitato: ${if (isEnabled) "Sì" else "No"}") }, false)
        source.sendFeedback({ Text.literal("Livello massimo: $maxLevel") }, false)
        source.sendFeedback({ Text.literal("Rarità: $rarity") }, false)

        val compatibleWith = ConfigManager.enchantmentCompatibility[id]
        if (!compatibleWith.isNullOrEmpty()) {
            source.sendFeedback({ Text.literal("Compatibile con: ${compatibleWith.joinToString(", ")}") }, false)
        }

        val incompatibleWith = ConfigManager.enchantmentUncompatibility[id]
        if (!incompatibleWith.isNullOrEmpty()) {
            source.sendFeedback({ Text.literal("Incompatibile con: ${incompatibleWith.joinToString(", ")}") }, false)
        }

        val categories = ConfigManager.enchantmentCategories[id]
        if (!categories.isNullOrEmpty()) {
            source.sendFeedback({ Text.literal("Categorie: ${categories.joinToString(", ")}") }, false)
        }

        val enchantConfig = ConfigManager.availableEnchantments.find { it.id == id }
        if (enchantConfig != null) {
            source.sendFeedback({ Text.literal("Sorgenti: ${enchantConfig.sources.joinToString(", ")}") }, false)
        }

        return 1
    }
}
