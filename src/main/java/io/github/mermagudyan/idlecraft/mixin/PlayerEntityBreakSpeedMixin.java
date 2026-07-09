package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.BreakSpeedRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityBreakSpeedMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void slowBreak(BlockState block, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        float modified = BreakSpeedRules.apply(self, block, self.level(), cir.getReturnValue());
        if (modified != cir.getReturnValue()) {
            cir.setReturnValue(modified);
        }
    }
}
