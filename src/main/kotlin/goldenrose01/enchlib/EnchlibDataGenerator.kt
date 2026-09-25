package goldenrose01.enchlib

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.core.registries.Registries
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.HolderLookup
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.resources.Identifier
import java.util.concurrent.CompletableFuture

object EnchlibDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(gen: FabricDataGenerator) {
        val pack = gen.createPack()
        pack.addProvider { output, future -> EnchlibItemTagProvider(output, future) }
    }
}

class EnchlibItemTagProvider(
    output: FabricPackOutput,
    future: CompletableFuture<HolderLookup.Provider>
) : FabricTagsProvider.ItemTagsProvider(output, future) {

    // Wrapper compatibile con 1.21.x: sostituisce il vecchio getOrCreateTagBuilder
    // Restituisce direttamente il builder di ValueLookupTagProvider senza tipizzarlo,
    // così i metodi .add/.addTag/.forceAddTag sono visibili correttamente.
    private fun getOrCreateTagBuilder(tag: TagKey<Item>) = builder(tag)

    private fun itemKey(item: Item) = BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow()

    override fun addTags(wrapper: HolderLookup.Provider) {
        val PICKAXES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "pickaxes"))
        val AXES     = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "axes"))
        val SHOVELS  = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "shovels"))
        val HOES     = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "hoes"))
        val SWORDS   = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "swords"))
        val TOOLS    = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "tools"))
        val ARMOR    = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "armor"))
        val WEAPONS  = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "weapons"))
        val BOWS     = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "bows"))
        val WEIGHTED = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("enchlib", "weighted"))



        // pickaxes
        getOrCreateTagBuilder(PICKAXES)
            .add(
                itemKey(Items.WOODEN_PICKAXE), itemKey(Items.STONE_PICKAXE), itemKey(Items.IRON_PICKAXE),
                itemKey(Items.GOLDEN_PICKAXE), itemKey(Items.DIAMOND_PICKAXE), itemKey(Items.NETHERITE_PICKAXE)
            )

        // axes
        getOrCreateTagBuilder(AXES)
            .add(
                itemKey(Items.WOODEN_AXE), itemKey(Items.STONE_AXE), itemKey(Items.IRON_AXE),
                itemKey(Items.GOLDEN_AXE), itemKey(Items.DIAMOND_AXE), itemKey(Items.NETHERITE_AXE)
            )

        // shovels
        getOrCreateTagBuilder(SHOVELS)
            .add(
                itemKey(Items.WOODEN_SHOVEL), itemKey(Items.STONE_SHOVEL), itemKey(Items.IRON_SHOVEL),
                itemKey(Items.GOLDEN_SHOVEL), itemKey(Items.DIAMOND_SHOVEL), itemKey(Items.NETHERITE_SHOVEL)
            )

        // hoes
        getOrCreateTagBuilder(HOES)
            .add(
                itemKey(Items.WOODEN_HOE), itemKey(Items.STONE_HOE), itemKey(Items.IRON_HOE),
                itemKey(Items.GOLDEN_HOE), itemKey(Items.DIAMOND_HOE), itemKey(Items.NETHERITE_HOE)
            )

        // swords
        getOrCreateTagBuilder(SWORDS)
            .add(
                itemKey(Items.WOODEN_SWORD), itemKey(Items.STONE_SWORD), itemKey(Items.IRON_SWORD),
                itemKey(Items.GOLDEN_SWORD), itemKey(Items.DIAMOND_SWORD), itemKey(Items.NETHERITE_SWORD)
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
            .add(itemKey(Items.ELYTRA))

        // weapons -> swords + archi/balestre/tridente
        getOrCreateTagBuilder(WEAPONS)
            .forceAddTag(SWORDS)
            .add(itemKey(Items.BOW), itemKey(Items.CROSSBOW), itemKey(Items.TRIDENT), itemKey(Items.MACE))

        // bows -> bow + cross
        getOrCreateTagBuilder(BOWS)
            .add(itemKey(Items.BOW), itemKey(Items.CROSSBOW))

        // weighted -> Mace + shovel
        getOrCreateTagBuilder(WEIGHTED)
            .forceAddTag(SHOVELS)
            .add(itemKey(Items.MACE))
    }
}
