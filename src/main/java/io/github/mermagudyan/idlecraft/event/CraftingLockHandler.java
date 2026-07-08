package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

public class CraftingLockHandler {

    private static final int INVENTORY_GRID = 4;
    private static final int TABLE_GRID = 9;

    public static boolean isCraftingLocked(Player player, int gridSize, ItemStack result) {
        if (result.isEmpty()) return false;

        if (gridSize == INVENTORY_GRID) {
            if (isCraftingTableAllowed(player, result)) return false;
            sendMessage(player, player.level().isClientSide());
            return true;
        }

        if (gridSize == TABLE_GRID) {
            if (isUnlocked(player, "crafting_table_unlock")) return false;
            sendMessage(player, player.level().isClientSide());
            return true;
        }

        return false;
    }

    public static boolean isResultSlotLocked(Player player, int gridSize, ItemStack result) {
        if (gridSize == INVENTORY_GRID) {
            return !isCraftingTableAllowed(player, result);
        }

        if (gridSize == TABLE_GRID) {
            return !isUnlocked(player, "crafting_table_unlock");
        }

        return false;
    }

    private static boolean isCraftingTableAllowed(Player player, ItemStack result) {
        return isUnlocked(player, "crafting_table_unlock")
                && !result.isEmpty()
                && result.getItem() == Items.CRAFTING_TABLE;
    }

    private static boolean isUnlocked(Player player, String id) {
        if (player.level().isClientSide()) {
            return ClientState.getUnlockedNodes().contains(id);
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        return PlayerData.getServer(server).getUnlockedNodes(player.getUUID()).contains(id);
    }

    private static void sendMessage(Player player, boolean isClient) {
        if (isClient) return;
        player.sendSystemMessage(
                Component.literal("Crafting is locked! Use a crafting table or unlock the required node.")
                        .withStyle(ChatFormatting.RED)
        );
    }
}
