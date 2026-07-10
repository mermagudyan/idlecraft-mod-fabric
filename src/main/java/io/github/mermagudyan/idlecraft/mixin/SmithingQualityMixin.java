package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingQualityMixin {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void idlecraft$transferQuality(CallbackInfo ci) {
        ItemCombinerMenuAccessor acc = (ItemCombinerMenuAccessor) (Object) this;
        Container inputSlots = acc.idlecraft$inputSlots();
        ResultContainer resultSlots = acc.idlecraft$resultSlots();

        ItemStack base = inputSlots.getItem(SmithingMenu.BASE_SLOT);
        ItemStack result = resultSlots.getItem(0);
        if (result.isEmpty()) return;

        Integer q = base.get(QualityComponent.QUALITY);
        if (q == null) return;
        if (!QualityComponent.isEligible(result)) return;

        ItemStack out = result.copy();
        QualityComponent.applyQuality(out, q);
        resultSlots.setItem(0, out);
    }
}
