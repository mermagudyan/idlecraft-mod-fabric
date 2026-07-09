package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.event.PlayerPlacedTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerPlaceMixin {

    @Shadow
    protected ServerPlayer player;

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void idlecraft$capturePlace(ServerPlayer player, Level level, net.minecraft.world.item.ItemStack itemStack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        this.lastPlacePos = hitResult.getBlockPos().relative(hitResult.getDirection());
        this.lastBefore = level.getBlockState(this.lastPlacePos);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void idlecraft$trackPlace(ServerPlayer player, Level level, net.minecraft.world.item.ItemStack itemStack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (this.lastPlacePos == null || this.lastBefore == null) return;
        BlockState after = level.getBlockState(this.lastPlacePos);
        if (!this.lastBefore.equals(after) && !after.isAir()) {
            PlayerPlacedTracker.add((ServerLevel) level, this.lastPlacePos);
        }
        this.lastPlacePos = null;
        this.lastBefore = null;
    }

    private BlockPos lastPlacePos;
    private BlockState lastBefore;
}
