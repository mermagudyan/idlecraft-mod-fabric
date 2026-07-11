package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.world.BlockConverter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.server.level.ServerLevel.class)
public abstract class ServerLevelTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void idlecraft$drainConversions(java.util.function.BooleanSupplier hasTime, CallbackInfo ci) {
        BlockConverter.drain();
    }
}
