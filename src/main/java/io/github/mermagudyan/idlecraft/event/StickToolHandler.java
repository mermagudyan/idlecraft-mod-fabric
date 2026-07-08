package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class StickToolHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) return true;
            if (!(player instanceof ServerPlayer serverPlayer)) return true;

            if (serverPlayer.isCreative()) return true;

            MinecraftServer server = serverPlayer.level().getServer();
            if (server == null) return true;
            PlayerData data = PlayerData.getServer(server);
            boolean hasStart = data.getUnlockedNodes(serverPlayer.getUUID()).contains("start");

            ItemStack mainHand = serverPlayer.getMainHandItem();

            if (mainHand.getItem() == Items.STICK) {
                boolean isWood = isWoodBlock(state);
                if (isWood && hasStart) {
                    mainHand.shrink(1);
                    if (mainHand.isEmpty()) {
                        serverPlayer.sendSystemMessage(
                                Component.literal("Your stick broke!").withStyle(ChatFormatting.GRAY),
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
        return stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES)
                || stack.is(ItemTags.SWORDS)
                || stack.getItem() == Items.STICK;
    }

    public static boolean isWoodBlock(BlockState state) {
        return state.is(BlockTags.LOGS) ||
                state.is(Blocks.MUSHROOM_STEM) ||
                state.is(Blocks.CRIMSON_STEM) ||
                state.is(Blocks.WARPED_STEM);
    }
}