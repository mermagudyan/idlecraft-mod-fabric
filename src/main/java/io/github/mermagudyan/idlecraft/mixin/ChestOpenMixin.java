package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class ChestOpenMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void idlecraft$gateChestOpen(BlockState state, Level level, BlockPos pos, Player player,
                                         BlockHitResult hitResult,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        boolean unlocked;
        if (level.isClientSide()) {
            unlocked = ClientState.getUnlockedNodes().contains("guardian");
        } else {
            MinecraftServer server = level.getServer();
            unlocked = server != null
                    && PlayerData.getServer(server).getUnlockedNodes(player.getUUID()).contains("guardian");
        }
        if (!unlocked) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(
                        Component.literal("Chests are locked! Unlock the Guardian node.").withStyle(ChatFormatting.RED));
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
