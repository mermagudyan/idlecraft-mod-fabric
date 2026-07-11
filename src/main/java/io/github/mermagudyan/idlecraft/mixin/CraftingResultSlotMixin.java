package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.IdlecraftWorkstation;
import io.github.mermagudyan.idlecraft.event.CraftingLockHandler;
import io.github.mermagudyan.idlecraft.mixin.CraftingTableAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class CraftingResultSlotMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void idlecraft$blockCrafting(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().isClientSide()) return;
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) return;
        if (!(player.containerMenu instanceof CraftingMenu menu)) return;

        
        
        boolean atModTable = ((CraftingTableAccessor) menu).idlecraft$access()
                .evaluate((Level level, BlockPos pos) -> level.getBlockState(pos).getBlock() instanceof IdlecraftWorkstation)
                .orElse(false);
        if (!atModTable) return;

        int gridSize = 9;
        ItemStack result = self.getItem();
        if (CraftingLockHandler.isCraftingLocked(player, gridSize, result)) {
            cir.setReturnValue(false);
        }
    }
}
