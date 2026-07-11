package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.StructureProtection;
import io.github.mermagudyan.idlecraft.network.StructureBlockedPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow
    protected ServerPlayer player;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void idlecraft$notifyStructureBreak(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int maxY, int sequence, CallbackInfo ci) {
        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) return;
        if (player == null || player.isCreative() || player.isSpectator()) return;

        ServerLevel level = (ServerLevel) player.level();
        if (!StructureProtection.isProtected(level, player, pos)) return;

        ServerPlayNetworking.send(player, new StructureBlockedPayload(pos));
    }
}
