package goldenrose01.enchlib.mixin;

import goldenrose01.enchlib.weapons.AxesAreWeaponsConfig;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemStack.class)
public abstract class AxesAreWeaponsItemStackMixin {

    @ModifyArg(
            method = "postHurtEnemy",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
            )
    )
    private int enchlib$reduceAxeDurabilityLoss(int amount) {
        ItemStack self = (ItemStack) (Object) this;
        if (AxesAreWeaponsConfig.isEnabled() && self.is(ItemTags.AXES)) {
            return 1;
        }
        return amount;
    }
}
