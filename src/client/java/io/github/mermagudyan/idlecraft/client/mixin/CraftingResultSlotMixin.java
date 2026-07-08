package io.github.mermagudyan.idlecraft.client.mixin;

import io.github.mermagudyan.idlecraft.event.CraftingLockHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class CraftingResultSlotMixin {

    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    private void idlecraft$blockCrafting(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (self instanceof CraftingResultSlot) {
            int gridSize = player.currentScreenHandler instanceof CraftingScreenHandler ? 9 : 4;
            ItemStack result = self.getStack();
            if (CraftingLockHandler.isCraftingLocked(player, gridSize, result)) {
                cir.setReturnValue(false);
            }
        }
    }
}
