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

import java.util.HashMap;
import java.util.Map;

public class StatTracker {

    // Какие статы отслеживаем для каких нод
    private static final Map<String, String> NODE_STAT_KEYS = Map.of(
            "wood_1", "wood_mined"
    );

    // Целевые значения
    private static final Map<String, Integer> NODE_TARGETS = Map.of(
            "wood_1", 5
    );

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 20) return; // раз в секунду
            tickCounter = 0;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                trackPlayer(player, server);
            }
        });
    }

    private static void trackPlayer(ServerPlayerEntity player, MinecraftServer server) {
        PlayerData data = PlayerData.getServer(server);
        String woodKey = "wood_mined";

        Map<String, Integer> progress = new HashMap<>();

        // Инициализируем базу при первом заходе (через hasStatBase, не == 0)
        if (!data.hasStatBase(player.getUuid(), woodKey)) {
            data.setStatBase(player.getUuid(), "wood_mined", StatTracker.getWoodMined(player));
            System.out.println("[IDLECRAFT] Initialized wood_mined base for "
                    + player.getName().getString() + " = " + StatTracker.getWoodMined(player));
        }

        int base = data.getStatBase(player.getUuid(), woodKey);
        int current = StatTracker.getWoodMined(player);
        int delta = Math.max(0, current - base);
        int target = NODE_TARGETS.getOrDefault("wood_1", 5);
        progress.put("wood_1", Math.min(delta, target));

        IdlecraftNetworking.syncConditionProgress(player, progress);
    }

    public static int getWoodMined(ServerPlayerEntity player) {
        int total = 0;
        Block[] logs = {
                Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG,
                Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG,
                Blocks.CHERRY_LOG, Blocks.MANGROVE_LOG, Blocks.PALE_OAK_LOG
        };
        for (Block log : logs) {
            Stat<Block> stat = Stats.MINED.getOrCreateStat(log);
            total += player.getStatHandler().getStat(stat);
        }
        return total;
    }
}