package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

public class CraftingLockHandler {

    private static final int INVENTORY_GRID = 4;
    private static final int TABLE_GRID = 9;

    private static final Item[] WOODEN_TOOLS = {
            Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_AXE,
            Items.WOODEN_SHOVEL, Items.WOODEN_HOE
    };

    public static boolean isCraftingLocked(Player player, int gridSize, ItemStack result) {
        
        
        if (!QualityComponent.isEligible(result)) return false;
        return !isUnlocked(player, "crafting_table_unlock");
    }

    public static boolean isResultSlotLocked(Player player, int gridSize, ItemStack result) {
        if (!QualityComponent.isEligible(result)) return false;
        return !isUnlocked(player, "crafting_table_unlock");
    }

    private static boolean isWoodenTool(ItemStack result) {
        Item item = result.getItem();
        for (Item t : WOODEN_TOOLS) {
            if (item == t) return true;
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
