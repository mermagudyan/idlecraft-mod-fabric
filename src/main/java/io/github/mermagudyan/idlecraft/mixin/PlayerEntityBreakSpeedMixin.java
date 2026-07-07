package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.event.StickToolHandler;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityBreakSpeedMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void slowBreakWithoutStart(BlockState block, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        boolean isClient = self.getEntityWorld().isClient();

        boolean hasStart = false;
        if (isClient) {
            hasStart = ClientState.getUnlockedNodes().contains("start");
        } else {
            MinecraftServer server = self.getEntityWorld().getServer();
            if (server != null) {
                hasStart = PlayerData.getServer(server).getUnlockedNodes(self.getUuid()).contains("start");
            }
        }

        float hardness = block.getHardness(self.getEntityWorld(), self.getBlockPos());

        ItemStack mainHand = self.getMainHandStack();
        boolean isTool = StickToolHandler.isToolOrStick(mainHand);

        boolean isSurvival = !self.isCreative() && !self.isSpectator();

        if (isSurvival && hardness >= 1.5f) {
            System.out.println("[IDLECRAFT Mixin] Block: " + block.getBlock().getName().getString()
                    + " | Hardness: " + hardness
                    + " | Tool: " + isTool
                    + " | Side: " + (isClient ? "CLIENT" : "SERVER")
                    + " | OriginalSpeed: " + cir.getReturnValue());
        }

        if (isSurvival && hardness >= 1.5f && !isTool) {
            System.out.println("[IDLECRAFT Mixin] Setting speed to 0!");
            cir.setReturnValue(0.0f);
            return;
        }

        if (!hasStart && hardness > 0.0f) {
            cir.setReturnValue(cir.getReturnValue() / 3.0f);
        }
    }
}