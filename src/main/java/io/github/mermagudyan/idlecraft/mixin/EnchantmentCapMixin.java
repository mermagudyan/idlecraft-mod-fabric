package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentCapMixin {

    private static final ThreadLocal<Boolean> IDLECRAFT_ENCH_REENTER = ThreadLocal.withInitial(() -> false);

    @Inject(method = "setEnchantments(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/ItemEnchantments;)V",
            at = @At("HEAD"), cancellable = true)
    private static void idlecraft$capEnchantments(ItemStack stack, ItemEnchantments enchantments, CallbackInfo ci) {
        if (IDLECRAFT_ENCH_REENTER.get()) return;
        if (stack == null || enchantments == null || enchantments.isEmpty()) return;
        if (!QualityComponent.isEligible(stack)) return;

        int quality = QualityComponent.getQuality(stack);
        
        if (quality == QualityComponent.CORRUPTED) {
            ci.cancel();
            return;
        }
        int cap = QualityComponent.enchantCap(quality);
        if (cap == Integer.MAX_VALUE) return;

        boolean needsClamp = false;
        for (Object2IntMap.Entry<Holder<Enchantment>> e : enchantments.entrySet()) {
            if (e.getIntValue() > cap) {
                needsClamp = true;
                break;
            }
        }
        if (!needsClamp) return;

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> e : enchantments.entrySet()) {
            int lvl = Math.min(e.getIntValue(), cap);
            if (lvl > 0) mutable.set(e.getKey(), lvl);
        }
        ItemEnchantments clamped = mutable.toImmutable();

        IDLECRAFT_ENCH_REENTER.set(true);
        try {
            EnchantmentHelper.setEnchantments(stack, clamped);
        } finally {
            IDLECRAFT_ENCH_REENTER.set(false);
        }
        ci.cancel();
    }
}
