package io.github.mermagudyan.idlecraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.mermagudyan.idlecraft.event.StickToolHandler;

@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class InteractionManagerAccessor {
    @Shadow protected ServerLevel level;
    @Shadow protected ServerPlayer player;

    private static final float HARDNESS_THRESHOLD = 1.5f;
    private static final int DAMAGE_INTERVAL = 10;

    private BlockPos idlecraftMiningPos = null;
    private int idlecraftMiningTicks = 0;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void idlecraft$onAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int i, int j, CallbackInfo ci) {
        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            idlecraftMiningPos = pos;
            idlecraftMiningTicks = 0;
        } else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
                || action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
            idlecraftMiningPos = null;
            idlecraftMiningTicks = 0;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void idlecraft$onTick(CallbackInfo ci) {
        if (idlecraftMiningPos == null) return;
        ServerPlayer player = this.player;
        if (player == null) return;
        if (player.isCreative() || player.isSpectator()) {
            idlecraftMiningPos = null;
            return;
        }
        BlockState state = this.level.getBlockState(idlecraftMiningPos);
        float hardness = state.getDestroySpeed(this.level, idlecraftMiningPos);
        if (hardness < HARDNESS_THRESHOLD) {
            idlecraftMiningPos = null;
            return;
        }
        ItemStack mainHand = player.getMainHandItem();
        if (StickToolHandler.isToolOrStick(mainHand)) return;

        idlecraftMiningTicks++;
        if (idlecraftMiningTicks >= DAMAGE_INTERVAL) {
            player.hurt(this.level.damageSources().generic(), 2.0f);
            idlecraftMiningTicks = 0;
        }
    }
}
