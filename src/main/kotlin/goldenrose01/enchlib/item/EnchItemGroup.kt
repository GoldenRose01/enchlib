package goldenrose01.enchlib.item

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import goldenrose01.enchlib.utils.EnchLogger

object EnchItemGroup {
    private val ENCHLIB_GROUP: ItemGroup = FabricItemGroup.builder()
        .icon { ItemStack(Items.ENCHANTED_BOOK) }
        .displayName(Text.translatable("itemgroup.enchlib.main"))
        .entries { _, entries ->
            //qui saranno impostati dinamicamente i libri degli incantesimi
        }.build()
    fun register() {
        Registry.register(Registries.ITEM_GROUP, Identifier.of("enchlib", "main"), ENCHLIB_GROUP)
        EnchLogger.info("📋 Tab creativa EnchLib registrata")
    }
}
