package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VillageVisitHandler {

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 40) return;
            tickCounter = 0;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkVillageProximity(player, server);
            }
        });
    }

    private static void checkVillageProximity(ServerPlayerEntity player, MinecraftServer server) {
        PlayerData data = PlayerData.getServer(server);
        if (data.hasVisitedVillage(player.getUuid())) return;

        Box searchBox = new Box(
                player.getX() - 48, player.getY() - 16, player.getZ() - 48,
                player.getX() + 48, player.getY() + 16, player.getZ() + 48
        );

        List<VillagerEntity> villagers = player.getEntityWorld().getEntitiesByClass(VillagerEntity.class, searchBox, v -> true);

        if (!villagers.isEmpty()) {
            data.markVillageVisited(player.getUuid());

            Map<String, Integer> progress = new HashMap<>();
            progress.put("crafting_table_unlock", 1);
            IdlecraftNetworking.syncConditionProgress(player, progress);
        }
    }
}