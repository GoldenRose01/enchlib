package goldenrose01.enchlib.item

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.ItemEnchantments

object EnchItemGroup {
    private val ENCHLIB_GROUP = FabricCreativeModeTab.builder()
        .icon { ItemStack(Items.ENCHANTED_BOOK) }
        .title(Component.translatable("itemgroup.enchlib.main"))
        .displayItems { parameters, output ->
            val enchantments = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT)
            enchantments.listElements().forEach { enchantment ->
                val contents = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
                contents.set(enchantment, 1)
                val book = ItemStack(Items.ENCHANTED_BOOK)
                book.set(DataComponents.STORED_ENCHANTMENTS, contents.toImmutable())
                output.accept(book)
            }
        }
        .build()

    fun register() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("enchlib", "main"),
            ENCHLIB_GROUP
        )
    }
}
