package goldenrose01.enchlib.trading;

import goldenrose01.enchlib.config.GlobalConfigIO;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashMap;

/** Applies world settings to newly generated trade results, without changing loot elsewhere. */
public final class VillagerEnchantmentRules {
    private VillagerEnchantmentRules() {}

    public static boolean defaultEnabled(Holder<Enchantment> enchantment) {
        return !enchantment.is(EnchantmentTags.CURSE);
    }

    public static boolean allows(ItemStack result) {
        var applied = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var stored = result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (applied.isEmpty() && stored.isEmpty()) return true;
        boolean books = result.is(Items.ENCHANTED_BOOK) || result.is(Items.BOOK);
        var overrides = new HashMap<String, Boolean>();
        for (var detail : GlobalConfigIO.INSTANCE.readDetails().getEnchantments()) {
            Boolean enabled = books ? detail.getVillager_books() : detail.getVillager_items();
            if (enabled != null) overrides.put(detail.getId(), enabled);
        }
        var availability = new HashMap<String, Boolean>();
        for (var entry : GlobalConfigIO.INSTANCE.readAvailable()) availability.put(entry.getId(), entry.getEnabled());
        for (var enchantments : new ItemEnchantments[]{applied, stored}) {
            for (var enchantment : enchantments.keySet()) {
                String id = enchantment.unwrapKey().map(key -> key.identifier().toString()).orElse("");
                if (!availability.getOrDefault(id, true)
                        || !overrides.getOrDefault(id, defaultEnabled(enchantment))) return false;
            }
        }
        return true;
    }
}
