package goldenrose01.enchlib.item

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.EnchantedBookItem
import net.minecraft.enchantment.EnchantmentLevelEntry
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import goldenrose01.enchlib.utils.EnchLogger
import goldenrose01.enchlib.compat.MCCompat

object EnchItemGroup {
    // Inizializza la tab
    private val ENCHLIB_GROUP: ItemGroup = FabricItemGroup.builder()
        .icon { ItemStack(Items.ENCHANTED_BOOK) }
        .displayName(Text.translatable("itemgroup.enchlib.main"))
        .entries { context, entries ->
            // Ottieni il server o il registry manager dal contesto se disponibile,
            // altrimenti usa i registri client-side per la visualizzazione.

            // Nota: In un contesto ItemGroup puramente client side 1.21+,
            // l'accesso ai registry dinamici (come gli Enchantments) è cambiato.
            // Qui iteriamo sul registry di base se possibile.

            val registry = context.lookup().getOptionalWrapper(net.minecraft.registry.RegistryKeys.ENCHANTMENT)

            if (registry.isPresent) {
                registry.get().streamEntries().forEach { ref ->
                    val enchant = ref.value()
                    // Aggiunge il libro al livello 1
                    entries.add(EnchantedBookItem.forEnchantment(EnchantmentLevelEntry(ref, 1)))

                    // Opzionale: Se vuoi aggiungere anche il livello massimo:
                    // val maxLevel = enchant.maxLevel
                    // if (maxLevel > 1) {
                    //    entries.add(EnchantedBookItem.forEnchantment(EnchantmentLevelEntry(ref, maxLevel)))
                    // }
                }
            }
        }.build()

    fun register() {
        Registry.register(Registries.ITEM_GROUP, Identifier.of("enchlib", "main"), ENCHLIB_GROUP)
        EnchLogger.info("📋 Tab creativa EnchLib registrata")
    }
}
