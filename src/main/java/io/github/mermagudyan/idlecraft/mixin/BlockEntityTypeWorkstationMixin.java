package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.IdlecraftWorkstation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeWorkstationMixin {

    @Inject(method = "isValid", at = @At("TAIL"), cancellable = true)
    private void idlecraft$validForWorkstation(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (state.getBlock() instanceof IdlecraftWorkstation) {
            cir.setReturnValue(true);
        }
    }
}
