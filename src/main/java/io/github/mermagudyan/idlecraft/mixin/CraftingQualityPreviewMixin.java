package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.IdleMod;
import io.github.mermagudyan.idlecraft.common.QualityComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingQualityPreviewMixin {

    @Shadow @Final private Player player;

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void idlecraft$previewQuality(Container container, CallbackInfo ci) {
        if (player.level().isClientSide()) return;

        ItemStack result = ((CraftingMenu) (Object) this).getResultSlot().getItem();

        StringBuilder grid = new StringBuilder();
        for (int i = 1; i <= 9; i++) {
            ItemStack s = ((CraftingMenu) (Object) this).getSlot(i).getItem();
            if (!s.isEmpty()) grid.append(s.getItem().toString()).append("(q=").append(QualityComponent.getQuality(s)).append(") ");
        }
        IdleMod.LOGGER.info("[IDLECRAFT][Crafting] grid=[{}] result={}", grid, result.getItem());

        if (result.isEmpty() || !QualityComponent.isEligible(result)) return;

        
        
        
        IdleMod.LOGGER.info("[IDLECRAFT][Crafting] preview result={} (quality assigned on take)", result.getItem());
    }
}
