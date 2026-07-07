package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.item.Items;
import java.util.HashMap;
import java.util.Map;

public class StatTracker {

    private static final Map<String, String> NODE_STAT_KEYS = Map.of(
            "sticky", "sticks_picked"
    );
    private static final Map<String, Integer> NODE_TARGETS = Map.of(
            "sticky", 5
    );

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 20) return;
            tickCounter = 0;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                trackPlayer(player, server);
            }
        });
    }

    private static void trackPlayer(ServerPlayerEntity player, MinecraftServer server) {
        PlayerData data = PlayerData.getServer(server);
        String stickKey = "sticks_picked";

        Map<String, Integer> progress = new HashMap<>();

        if (!data.hasStatBase(player.getUuid(), stickKey)) {
            int current = StatTracker.getSticksPicked(player);
            data.setStatBase(player.getUuid(), stickKey, current);
            System.out.println("[IDLECRAFT] StatTracker: initialized sticks_picked base for "
                    + player.getName().getString() + " = " + current);
        } else {
            int base = data.getStatBase(player.getUuid(), stickKey);
            int current = StatTracker.getSticksPicked(player);
            System.out.println("[IDLECRAFT] StatTracker: " + player.getName().getString()
                    + " base=" + base + " current=" + current + " delta=" + (current - base));
        }

        int base = data.getStatBase(player.getUuid(), stickKey);
        int current = StatTracker.getSticksPicked(player);
        int delta = Math.max(0, current - base);
        int target = NODE_TARGETS.getOrDefault("sticky", 5);
        progress.put("sticky", Math.min(delta, target));

        IdlecraftNetworking.syncConditionProgress(player, progress);
    }

    public static int getSticksPicked(ServerPlayerEntity player) {
        return player.getStatHandler().getStat(
                net.minecraft.stat.Stats.PICKED_UP.getOrCreateStat(Items.STICK)
        );
    }

}