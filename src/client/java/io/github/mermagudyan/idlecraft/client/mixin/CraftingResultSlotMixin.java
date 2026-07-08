package io.github.mermagudyan.idlecraft.client.mixin;

import io.github.mermagudyan.idlecraft.event.CraftingLockHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class CraftingResultSlotMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void idlecraft$blockCrafting(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (self instanceof ResultSlot) {
            int gridSize = player.containerMenu instanceof CraftingMenu ? 9 : 4;
            ItemStack result = self.getItem();
            if (CraftingLockHandler.isCraftingLocked(player, gridSize, result)) {
                cir.setReturnValue(false);
            }
        }
    }
}