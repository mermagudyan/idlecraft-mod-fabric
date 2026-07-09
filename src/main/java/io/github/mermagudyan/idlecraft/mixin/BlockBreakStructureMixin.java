package io.github.mermagudyan.idlecraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import io.github.mermagudyan.idlecraft.event.PlayerPlacedTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBreakStructureMixin {

    @Inject(method = "getDestroyProgress(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F", at = @At("RETURN"), cancellable = true)
    private void idlecraft$structureDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (player == null || player.isCreative() || player.isSpectator()) return;
        if (!(level instanceof ServerLevel lvl)) return;

        StructureStart start = lvl.structureManager().getStructureWithPieceAt(pos, s -> true);
        if (start.isValid() && !PlayerPlacedTracker.isPlayerPlaced(lvl, pos)) {
            cir.setReturnValue(0.0F);
        }
    }
}
