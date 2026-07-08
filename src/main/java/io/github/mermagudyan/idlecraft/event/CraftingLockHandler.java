package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class CraftingLockHandler {

    private static final int INVENTORY_GRID = 4;
    private static final int TABLE_GRID = 9;

    public static boolean isCraftingLocked(PlayerEntity player, int gridSize, ItemStack result) {
        if (result.isEmpty()) return false;

        boolean isClient = player.getEntityWorld().isClient();
        List<String> unlocked;

        if (isClient) {
            unlocked = ClientState.getUnlockedNodes();
        } else {
            MinecraftServer server = player.getEntityWorld().getServer();
            if (server == null) return false;
            unlocked = PlayerData.getServer(server).getUnlockedNodes(player.getUuid());
        }

        if (gridSize == 4) {
            if (result.getItem() == Items.CRAFTING_TABLE) {
                return !unlocked.contains("village_visit");
            }
            return true;
        }

        if (gridSize == 9) {
            if (!unlocked.contains("crafting_table_unlock")) {
                return true;
            }

            if (result.getItem() == Items.WOODEN_PICKAXE
                    || result.getItem() == Items.WOODEN_SHOVEL
                    || result.getItem() == Items.WOODEN_AXE
                    || result.getItem() == Items.WOODEN_SWORD) {
                return !unlocked.contains("wooden_tools");
            }

            if (result.isIn(ItemTags.PLANKS) && !unlocked.contains("sticky")) {
                return true;
            }
            if (result.getItem() == Items.STICK && !unlocked.contains("sticky")) {
                return true;
            }
        }

        return false;
    }

    private static void sendMessage(PlayerEntity player, boolean isClient) {
        if (isClient) return;
        player.sendMessage(
                Text.literal("Crafting is locked! Unlock the required node first.")
                        .formatted(Formatting.RED),
                true
        );
    }
}