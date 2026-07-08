package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

public class CraftingLockHandler {

    private static final int INVENTORY_GRID = 4;
    private static final int TABLE_GRID = 9;

    public static boolean isCraftingLocked(Player player, int gridSize, ItemStack result) {
        if (result.isEmpty()) return false;

        if (gridSize == INVENTORY_GRID) {
            sendMessage(player, player.level().isClientSide());
            return true;
        }

        if (gridSize == TABLE_GRID) {
            boolean isClient = player.level().isClientSide();
            List<String> unlocked;

            if (isClient) {
                unlocked = ClientState.getUnlockedNodes();
            } else {
                MinecraftServer server = player.level().getServer();
                if (server == null) return false;
                unlocked = PlayerData.getServer(server).getUnlockedNodes(player.getUUID());
            }

            if (!unlocked.contains("crafting_table_unlock")) {
                sendMessage(player, isClient);
                return true;
            }
        }

        return false;
    }

    private static void sendMessage(Player player, boolean isClient) {
        if (isClient) return;
        player.sendSystemMessage(
                Component.literal("Crafting is locked! Use a crafting table or unlock the required node.")
                        .withStyle(ChatFormatting.RED)
        );
    }
}