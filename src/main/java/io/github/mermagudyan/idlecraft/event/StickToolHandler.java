package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.registry.tag.ItemTags;

public class StickToolHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return true;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;

            if (serverPlayer.isCreative()) return true;

            MinecraftServer server = serverPlayer.getEntityWorld().getServer();
            if (server == null) return true;
            PlayerData data = PlayerData.getServer(server);
            boolean hasStart = data.getUnlockedNodes(serverPlayer.getUuid()).contains("start");

            ItemStack mainHand = serverPlayer.getMainHandStack();

            if (mainHand.getItem() == Items.STICK) {
                boolean isWood = isWoodBlock(state);
                if (isWood && hasStart) {
                    mainHand.decrement(1);
                    if (mainHand.isEmpty()) {
                        serverPlayer.sendMessage(
                                Text.literal("Your stick broke!").formatted(Formatting.GRAY),
                                true
                        );
                    }
                }
            }

            return true;
        });
    }

    public static boolean isToolOrStick(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.SHOVELS)
                || stack.isIn(ItemTags.HOES)
                || stack.isIn(ItemTags.SWORDS)
                || stack.getItem() == Items.STICK;
    }

    public static boolean isWoodBlock(BlockState state) {
        return state.isIn(net.minecraft.registry.tag.BlockTags.LOGS) ||
                state.isOf(Blocks.MUSHROOM_STEM) ||
                state.isOf(Blocks.CRIMSON_STEM) ||
                state.isOf(Blocks.WARPED_STEM);
    }
}