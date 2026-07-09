package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FurnaceUsageTracker {

    public static final String FURNACE_OPENED = "furnace_opened";
    public static final String FOOD_COOKED = "food_cooked";
    public static final String FURNACE_TAKES = "furnace_takes";
    public static final String STONE_LOCK = "stone_lock";

    private static int tickCounter = 0;
    private static final Map<UUID, Boolean> inFurnace = new HashMap<>();
    private static final Map<UUID, Integer> prevOutputCount = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 20) return;
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                trackPlayer(player, server);
            }
        });
    }

    private static void trackPlayer(ServerPlayer player, MinecraftServer server) {
        PlayerData data = PlayerData.getServer(server);
        UUID id = player.getUUID();

        Map<String, Integer> progress = new HashMap<>();

        boolean open = player.containerMenu instanceof AbstractFurnaceMenu;
        boolean wasOpen = inFurnace.getOrDefault(id, false);

        if (open) {
            AbstractFurnaceMenu menu = (AbstractFurnaceMenu) player.containerMenu;
            ItemStack output = menu.getSlot(2).getItem();
            int outputCount = output.getCount();

            if (!wasOpen) {
                int cur = data.getFurnaceCounter(id, FURNACE_OPENED);
                if (cur < 1) data.setFurnaceCounter(id, FURNACE_OPENED, 1);
                progress.put(FURNACE_OPENED, 1);
            }

            int prev = prevOutputCount.getOrDefault(id, 0);
            if (prev > 0 && outputCount < prev) {
                data.setFurnaceCounter(id, FURNACE_TAKES, data.getFurnaceCounter(id, FURNACE_TAKES) + 1);
                if (!output.isEmpty() && output.get(DataComponents.FOOD) != null) {
                    int cur = data.getFurnaceCounter(id, FOOD_COOKED);
                    if (cur < 1) data.setFurnaceCounter(id, FOOD_COOKED, 1);
                }
            }

            prevOutputCount.put(id, outputCount);
        } else {
            prevOutputCount.remove(id);
        }
        inFurnace.put(id, open);

        progress.put(FURNACE_OPENED, data.getFurnaceCounter(id, FURNACE_OPENED));
        progress.put(FOOD_COOKED, data.getFurnaceCounter(id, FOOD_COOKED));
        progress.put(FURNACE_TAKES, data.getFurnaceCounter(id, FURNACE_TAKES));

        long lockUntil = data.getBranchLockUntil(id);
        long remaining = lockUntil > System.currentTimeMillis()
                ? (lockUntil - System.currentTimeMillis()) / 1000 : 0;
        progress.put(STONE_LOCK, (int) remaining);

        IdlecraftNetworking.syncConditionProgress(player, progress);
    }
}
