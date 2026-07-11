package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.world.BlockConverter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class ChunkLoadConversionMixin {

    @Inject(method = "runPostLoad", at = @At("TAIL"))
    private void idlecraft$queueConversion(CallbackInfo ci) {
        LevelChunk self = (LevelChunk) (Object) this;
        Level level = self.getLevel();
        if (level instanceof ServerLevel) {
            BlockConverter.queue(self);
        }
    }
}
