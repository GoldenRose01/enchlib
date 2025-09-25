package goldenrose01.enchlib.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.enchantment.Enchantment
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.enchantment.Enchantments
import net.minecraft.server.command.CommandManager.*
import net.minecraft.server.command.ServerCommandSource
import goldenrose01.enchlib.config.WorldConfigManager
import goldenrose01.enchlib.utils.msg
import goldenrose01.enchlib.utils.err

// Helper sicuro per ottenere l'ID "namespace:path" da un RegistryEntry<Enchantment>
private fun RegistryEntry<Enchantment>.idString(): String =
    this.getKey().map { it.value.toString() }.orElse("unknown:enchantment")

object EnchLibCommands {

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->

            val enchantmentArg = RegistryEntryReferenceArgumentType.registryEntry(
                registryAccess,
                RegistryKeys.ENCHANTMENT
            )

            dispatcher.register(
                literal("plusec")
                    .then(literal("add")
                        .then(argument("enchantment", enchantmentArg)
                            .executes { addEnchantment(it, 1) }
                            .then(argument("level", IntegerArgumentType.integer(1))
                                .executes { addEnchantment(it, IntegerArgumentType.getInteger(it, "level")) }
                            )
                        )
                    )
                    .then(literal("remove")
                        .then(argument("enchantment", enchantmentArg)
                            .executes { removeEnchantment(it) }
                        )
                    )
                    .then(literal("list")
                        .executes { listEnchantments(it) }
                    )
                    .then(literal("clear")
                        .executes { clearEnchantments(it) }
                    )
                    .then(literal("info")
                        .then(argument("enchantment", enchantmentArg)
                            .executes { showEnchantmentInfo(it) }
                        )
                    )
            )
        }
    }

    private fun addEnchantment(context: CommandContext<ServerCommandSource>, level: Int): Int {
        val source = context.source
        val player = source.playerOrThrow
        val enchantmentEntry = RegistryEntryReferenceArgumentType.getRegistryEntry(
            context, "enchantment", RegistryKeys.ENCHANTMENT
        )

        if (!WorldConfigManager.hasInstance()) {
            return source.err("World configuration not available")
        }

        val configManager = WorldConfigManager.getInstance(source.server)
        val enchantmentId = configManager.getEnchantmentId(enchantmentEntry)

        if (!configManager.isEnchantmentEnabled(enchantmentId)) {
            return source.err("Enchantment $enchantmentId is disabled in this world")
        }

        val maxLevel = configManager.getMaxLevel(enchantmentId)
        if (level > maxLevel) {
            return source.err("Level $level exceeds maximum level $maxLevel for $enchantmentId")
        }

        val heldItem = player.mainHandStack
        if (heldItem.isEmpty) {
            return source.err("You must hold an item to enchant")
        }

        // Applica l'incantesimo
        val currentEnchantments = heldItem.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
        val builder = ItemEnchantmentsComponent.Builder(currentEnchantments)
        builder.add(enchantmentEntry, level)
        val newEnchantments = builder.build()

        heldItem.set(DataComponentTypes.ENCHANTMENTS, newEnchantments)

        return source.msg("Added $enchantmentId level $level to your item")
    }

    private fun removeEnchantment(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.playerOrThrow
        val enchantmentEntry = RegistryEntryReferenceArgumentType.getRegistryEntry(
            context, "enchantment", RegistryKeys.ENCHANTMENT
        )

        if (!WorldConfigManager.hasInstance()) {
            return source.err("World configuration not available")
        }

        val configManager = WorldConfigManager.getInstance(source.server)
        val enchantmentId = configManager.getEnchantmentId(enchantmentEntry)

        val heldItem = player.mainHandStack
        if (heldItem.isEmpty) {
            return source.err("You must hold an item")
        }

        val currentEnchantments = heldItem.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
        if (!currentEnchantments.enchantments.containsKey(enchantmentEntry)) {
            return source.err("Item doesn't have $enchantmentId")
        }

        // Rimozione dell'incantesimo
        val builder = ItemEnchantmentsComponent.Builder(currentEnchantments)
        builder.remove { entry -> entry == enchantmentEntry }
        val newEnchantments = builder.build()

        heldItem.set(DataComponentTypes.ENCHANTMENTS, newEnchantments)

        return source.msg("Removed $enchantmentId from your item")
    }

    private fun listEnchantments(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.playerOrThrow
        val heldItem = player.mainHandStack

        if (heldItem.isEmpty) {
            return source.err("You must hold an item")
        }

        val enchantments = heldItem.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
        if (enchantments.isEmpty) {
            return source.msg("This item has no enchantments")
        }

        val configManager = if (WorldConfigManager.hasInstance()) {
            WorldConfigManager.getInstance(source.server)
        } else null

        source.msg("Enchantments on this item:")

        // Iterazione corretta sulle enchantments
        enchantments.enchantments.forEach { (enchantmentEntry: RegistryEntry<Enchantment>, level: Int) ->
            val id = configManager?.getEnchantmentId(enchantmentEntry) ?: "unknown"
            val maxLevel = configManager?.getMaxLevel(id) ?: enchantmentEntry.value().maxLevel
            val details = configManager?.getEnchantmentDetails(id)

            val rarity = details?.rarity ?: "unknown"
            source.msg("- $id: Level $level/$maxLevel (Rarity: $rarity)")
        }

        return 1
    }

    private fun clearEnchantments(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.playerOrThrow
        val heldItem = player.mainHandStack

        if (heldItem.isEmpty) {
            return source.err("You must hold an item")
        }

        heldItem.remove(DataComponentTypes.ENCHANTMENTS)
        return source.msg("Cleared all enchantments from your item")
    }

    private fun showEnchantmentInfo(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val enchantmentEntry = RegistryEntryReferenceArgumentType.getRegistryEntry(
            context, "enchantment", RegistryKeys.ENCHANTMENT
        )

        if (!WorldConfigManager.hasInstance()) {
            return source.err("World configuration not available")
        }

        val configManager = WorldConfigManager.getInstance(source.server)
        val enchantmentId = configManager.getEnchantmentId(enchantmentEntry)
        val details = configManager.getEnchantmentDetails(enchantmentId)
        val isEnabled = configManager.isEnchantmentEnabled(enchantmentId)

        source.msg("=== Enchantment Info: $enchantmentId ===")
        source.msg("Name: ${details?.name ?: "Unknown"}")
        source.msg("Enabled: $isEnabled")
        source.msg("Max Level: ${details?.max_level ?: "Unknown"}")
        source.msg("Rarity: ${details?.rarity ?: "Unknown"}")
        source.msg("Applicable to: ${details?.applicable_to?.joinToString(", ") ?: "Unknown"}")
        source.msg("Categories: ${details?.enc_category?.joinToString(", ") ?: "Unknown"}")
        source.msg("Mob Categories: ${details?.mob_category?.joinToString(", ") ?: "Unknown"}")
        source.msg("Description: ${details?.description ?: "No description available"}")

        return 1
    }
}