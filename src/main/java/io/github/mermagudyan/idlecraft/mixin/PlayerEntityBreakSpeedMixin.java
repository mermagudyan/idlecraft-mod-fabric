package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.event.StickToolHandler;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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

        if (isStoneBlock(block) && !isUnlocked(self, "stone_1")) {
            cir.setReturnValue(0.0f);
            return;
        }

        if (isCobblestoneBlock(block) && !isUnlocked(self, "cobblestone")) {
            cir.setReturnValue(0.0f);
            return;
        }

        if (isCoalOreBlock(block) && !isUnlocked(self, "coal_knowledge")) {
            cir.setReturnValue(0.0f);
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

    private static boolean isUnlocked(Player player, String nodeId) {
        if (player.level().isClientSide()) {
            return ClientState.getUnlockedNodes().contains(nodeId);
        }
        var server = player.level().getServer();
        if (server == null) return false;
        return io.github.mermagudyan.idlecraft.data.PlayerData.getServer(server)
                .getUnlockedNodes(player.getUUID()).contains(nodeId);
    }

    private static boolean isStoneBlock(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE);
    }

    private static boolean isCobblestoneBlock(BlockState state) {
        return state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE);
    }

    private static boolean isCoalOreBlock(BlockState state) {
        return state.is(Blocks.COAL_ORE)
                || state.is(Blocks.DEEPSLATE_COAL_ORE);
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