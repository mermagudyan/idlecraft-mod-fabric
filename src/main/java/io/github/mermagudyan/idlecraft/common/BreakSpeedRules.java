package io.github.mermagudyan.idlecraft.common;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.event.StickToolHandler;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BreakSpeedRules {

    private BreakSpeedRules() {
    }

    /**
     * Applies the idlecraft break-speed gating (stick-on-wood, node unlocks, tool requirement
     * and non-tool slowdowns) to the given base destroy speed. Returns the modified speed.
     * A value of {@code 0.0f} means the block cannot be broken.
     */
    public static float apply(Player player, BlockState state, Level level, float baseSpeed) {
        boolean isSurvival = !player.isCreative() && !player.isSpectator();
        if (!isSurvival) {
            return baseSpeed;
        }

        float hardness = state.getDestroySpeed(level, player.blockPosition());
        ItemStack mainHand = player.getMainHandItem();
        boolean isTool = StickToolHandler.isToolOrStick(mainHand);

        if (mainHand.getItem() == Items.STICK && StickToolHandler.isWoodBlock(state)) {
            return 0.30f;
        }

        if (isStoneBlock(state) && !isUnlocked(player, "stone_1")) {
            return 0.0f;
        }

        if (isCobblestoneBlock(state) && !isUnlocked(player, "cobblestone")) {
            return 0.0f;
        }

        if (isCoalOreBlock(state) && !isUnlocked(player, "coal_knowledge")) {
            return 0.0f;
        }

        if (isDecorativeStoneBlock(state) && isUnlocked(player, "decorative")) {
            return baseSpeed / 2.0f;
        }

        if (hardness >= 1.5f && !isTool) {
            return 0.0f;
        }

        if (!isTool) {
            if (isDirtLikeBlock(state)) {
                return baseSpeed / 6.0f;
            } else if (hardness >= 0.1f && hardness <= 0.6f) {
                return baseSpeed / 5.0f;
            } else if (hardness > 0.0f) {
                return baseSpeed / 3.0f;
            }
        }

        return baseSpeed / 4.0f;
    }

    public static boolean isUnlocked(Player player, String nodeId) {
        if (player.level().isClientSide()) {
            return ClientState.getUnlockedNodes().contains(nodeId);
        }
        var server = player.level().getServer();
        if (server == null) return false;
        return PlayerData.getServer(server)
                .getUnlockedNodes(player.getUUID()).contains(nodeId);
    }

    public static boolean isStoneBlock(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE);
    }

    public static boolean isCobblestoneBlock(BlockState state) {
        return state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE);
    }

    public static boolean isCoalOreBlock(BlockState state) {
        return state.is(Blocks.COAL_ORE)
                || state.is(Blocks.DEEPSLATE_COAL_ORE);
    }

    public static boolean isDecorativeStoneBlock(BlockState state) {
        return state.is(Blocks.GRANITE)
                || state.is(Blocks.POLISHED_GRANITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.POLISHED_ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.POLISHED_DIORITE);
    }

    public static boolean isDirtLikeBlock(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.DIRT_PATH);
    }
}
