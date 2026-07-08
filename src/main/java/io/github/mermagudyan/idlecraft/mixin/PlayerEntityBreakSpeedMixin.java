package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.event.StickToolHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityBreakSpeedMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void slowBreak(BlockState block, CallbackInfoReturnable<Float> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        boolean isSurvival = !self.isCreative() && !self.isSpectator();
        if (!isSurvival) return;

        float hardness = block.getHardness(self.getEntityWorld(), self.getBlockPos());
        ItemStack mainHand = self.getMainHandStack();
        boolean isTool = StickToolHandler.isToolOrStick(mainHand);

        if (mainHand.getItem() == Items.STICK && StickToolHandler.isWoodBlock(block)) {
            cir.setReturnValue(0.30f);
            return;
        }

        if (hardness >= 1.5f && !isTool) {
            cir.setReturnValue(0.0f);
            return;
        }

        if (!isTool) {
            if (isDirtLikeBlock(block)) {
                cir.setReturnValue(cir.getReturnValue() / 6.0f);
            } else if (hardness >= 0.1f && hardness <= 0.6f) {
                cir.setReturnValue(cir.getReturnValue() / 5.0f);
            } else if (hardness > 0.0f) {
                cir.setReturnValue(cir.getReturnValue() / 3.0f);
            }
        }
    }

    private boolean isDirtLikeBlock(BlockState state) {
        return state.isIn(net.minecraft.registry.tag.BlockTags.DIRT) ||
                state.isOf(net.minecraft.block.Blocks.GRASS_BLOCK) ||
                state.isOf(net.minecraft.block.Blocks.MYCELIUM) ||
                state.isOf(net.minecraft.block.Blocks.PODZOL) ||
                state.isOf(net.minecraft.block.Blocks.COARSE_DIRT) ||
                state.isOf(net.minecraft.block.Blocks.ROOTED_DIRT) ||
                state.isOf(net.minecraft.block.Blocks.MOSS_BLOCK) ||
                state.isOf(net.minecraft.block.Blocks.FARMLAND) ||
                state.isOf(net.minecraft.block.Blocks.DIRT_PATH);
    }
}