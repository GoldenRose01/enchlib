package goldenrose01.enchlib

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

object EnchlibDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(gen: FabricDataGenerator) {
        val pack = gen.createPack()
        pack.addProvider { output, future -> EnchlibItemTagProvider(output, future) }
    }
}

class EnchlibItemTagProvider(
    output: FabricDataOutput,
    future: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider.ItemTagProvider(output, future) {

    // Wrapper compatibile con 1.21.x: sostituisce il vecchio getOrCreateTagBuilder
    // Restituisce direttamente il builder di ValueLookupTagProvider senza tipizzarlo,
    // così i metodi .add/.addTag/.forceAddTag sono visibili correttamente.
    private fun getOrCreateTagBuilder(tag: TagKey<Item>) = valueLookupBuilder(tag)

    override fun configure(wrapper: RegistryWrapper.WrapperLookup) {
        val PICKAXES = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "pickaxes"))
        val AXES     = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "axes"))
        val SHOVELS  = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "shovels"))
        val HOES     = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "hoes"))
        val SWORDS   = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "swords"))
        val TOOLS    = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "tools"))
        val ARMOR    = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "armor"))
        val WEAPONS  = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "weapons"))
        val BOWS     = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "bows"))
        val WEIGHTED = TagKey.of(RegistryKeys.ITEM, Identifier.of("enchlib", "weighted"))



        // pickaxes
        getOrCreateTagBuilder(PICKAXES)
            .add(
                Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE,
                Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE
            )

        // axes
        getOrCreateTagBuilder(AXES)
            .add(
                Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
                Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE
            )

        // shovels
        getOrCreateTagBuilder(SHOVELS)
            .add(
                Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL,
                Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL
            )

        // hoes
        getOrCreateTagBuilder(HOES)
            .add(
                Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE,
                Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE
            )

        // swords
        getOrCreateTagBuilder(SWORDS)
            .add(
                Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD,
                Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD
            )

        // tools -> include gruppi figli
        getOrCreateTagBuilder(TOOLS)
            .forceAddTag(PICKAXES)
            .forceAddTag(AXES)
            .forceAddTag(SHOVELS)
            .forceAddTag(HOES)


        // armor -> usa tag vanilla armatura
        getOrCreateTagBuilder(ARMOR)
            .forceAddTag(ItemTags.HEAD_ARMOR)
            .forceAddTag(ItemTags.CHEST_ARMOR)
            .forceAddTag(ItemTags.LEG_ARMOR)
            .forceAddTag(ItemTags.FOOT_ARMOR)
            .add(Items.ELYTRA)

        // weapons -> swords + archi/balestre/tridente
        getOrCreateTagBuilder(WEAPONS)
            .forceAddTag(SWORDS)
            .add(Items.BOW, Items.CROSSBOW, Items.TRIDENT, Items.MACE)

        // bows -> bow + cross
        getOrCreateTagBuilder(BOWS)
            .add(Items.BOW, Items.CROSSBOW)

        // weighted -> Mace + shovel
        getOrCreateTagBuilder(WEIGHTED)
            .forceAddTag(SHOVELS)
            .add(Items.MACE)
    }
}
