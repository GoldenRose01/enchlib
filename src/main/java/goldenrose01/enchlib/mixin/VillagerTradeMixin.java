package goldenrose01.enchlib.mixin;

import goldenrose01.enchlib.trading.VillagerEnchantmentRules;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerTrade.class)
public abstract class VillagerTradeMixin {
    @Inject(method = "getOffer", at = @At("RETURN"), cancellable = true)
    private void enchlib$filterEnchantments(LootContext context, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = cir.getReturnValue();
        // Null is the vanilla mechanism for skipping an ineligible trade.
        if (offer != null && !VillagerEnchantmentRules.allows(offer.getResult())) cir.setReturnValue(null);
    }
}
