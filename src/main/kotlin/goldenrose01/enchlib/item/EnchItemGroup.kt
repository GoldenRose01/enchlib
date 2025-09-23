package goldenrose01.enchlib.item

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text
import net.minecraft.registry.Registries
import net.minecraft.enchantment.EnchantmentHelper

/**
 * Definisce e registra la creative tab EnchLib.
 */
object EnchItemGroup {
    lateinit var ENCHLIB_GROUP: ItemGroup
        private set

    /** Registra la tab creativa, da chiamare in fase di inizializzazione. */
    fun register() {
        ENCHLIB_GROUP = FabricItemGroup.builder()
            .icon { ItemStack(Items.ENCHANTED_BOOK) }
            .displayName(Text.literal("EnchLib"))
            .entries { _, stacks ->
                // Aggiunge un libro incantato per ogni incantesimo vanilla (livello 1)
                for (enchant in Registries.ENCHANTMENT) {
                    val stack = ItemStack(Items.ENCHANTED_BOOK)
                    EnchantmentHelper.setEnchantments(mapOf(enchant to 1), stack)
                    stacks.add(stack)
                }
            }
            .build()
    }
}
