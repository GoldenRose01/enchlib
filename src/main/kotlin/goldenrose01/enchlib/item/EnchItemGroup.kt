package goldenrose01.enchlib.item

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.text.Text

// In Yarn 1.21 ItemStack e Items si trovano in net.minecraft.item.
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

// Anche Enchantment è sotto net.minecraft.enchantment.
import net.minecraft.enchantment.Enchantment

// Per ottenere la lista di incantesimi usiamo Registries, non BuiltInRegistries.
import net.minecraft.registry.Registries

/**
 * Registra la creative tab “EnchLib”, che mostra libri incantati con tutti
 * gli incantesimi vanilla. Utile per test e debug.
 */
object EnchItemGroup {
    lateinit var ENCHLIB_GROUP: ItemGroup
        private set

    fun register() {
        ENCHLIB_GROUP = FabricItemGroup.builder()
            .icon { ItemStack(Items.ENCHANTED_BOOK) }
            .displayName(Text.literal("EnchLib"))
            .entries { _, stacks ->
                // Scorri tutti gli incantesimi vanilla, crea un libro incantato
                // per ciascuno (a livello 1) e aggiungilo alla tab.
                for (enchant in Registries.ENCHANTMENT as Iterable<Enchantment>) {
                    val stack = ItemStack(Items.ENCHANTED_BOOK)
                    stack.enchant(enchant, 1)
                    stacks.add(stack)
                }
            }
            .build()
    }
}
