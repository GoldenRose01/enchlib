package goldenrose01.enchlib.mixin;

import goldenrose01.enchlib.weapons.AxesAreWeaponsConfig;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class AxesAreWeaponsEnchantmentMixin {

    @Unique
    private static final ThreadLocal<Boolean> ENCHLIB$CHECKING = ThreadLocal.withInitial(() -> false);

    @Inject(method = "isSupportedItem", at = @At("RETURN"), cancellable = true)
    private void enchlib$axeSwordSupported(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (ENCHLIB$CHECKING.get()) return;
        if (AxesAreWeaponsConfig.isEnabled() && stack.is(ItemTags.AXES)) {
            try {
                ENCHLIB$CHECKING.set(true);
                Enchantment self = (Enchantment) (Object) this;
                if (self.isSupportedItem(new ItemStack(Items.DIAMOND_SWORD))) {
                    cir.setReturnValue(true);
                }
            } finally {
                ENCHLIB$CHECKING.set(false);
            }
        }
    }

    @Inject(method = "isPrimaryItem", at = @At("RETURN"), cancellable = true)
    private void enchlib$axeSwordPrimary(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (ENCHLIB$CHECKING.get()) return;
        if (AxesAreWeaponsConfig.isEnabled() && stack.is(ItemTags.AXES)) {
            try {
                ENCHLIB$CHECKING.set(true);
                Enchantment self = (Enchantment) (Object) this;
                if (self.isPrimaryItem(new ItemStack(Items.DIAMOND_SWORD))) {
                    cir.setReturnValue(true);
                }
            } finally {
                ENCHLIB$CHECKING.set(false);
            }
        }
    }
}
