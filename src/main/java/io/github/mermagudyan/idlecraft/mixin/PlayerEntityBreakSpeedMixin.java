package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.event.StickToolHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        boolean isSurvival = !self.isCreative() && !self.isSpectator();
        if (!isSurvival) return;

        float hardness = block.getDestroySpeed(self.level(), self.blockPosition());
        ItemStack mainHand = self.getMainHandItem();
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
        return state.is(net.minecraft.tags.BlockTags.DIRT) ||
                state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ||
                state.is(net.minecraft.world.level.block.Blocks.MYCELIUM) ||
                state.is(net.minecraft.world.level.block.Blocks.PODZOL) ||
                state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT) ||
                state.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT) ||
                state.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK) ||
                state.is(net.minecraft.world.level.block.Blocks.FARMLAND) ||
                state.is(net.minecraft.world.level.block.Blocks.DIRT_PATH);
    }
}