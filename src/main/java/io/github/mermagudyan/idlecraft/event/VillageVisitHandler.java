package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

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

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkVillageProximity(player, server);
            }
        });
    }

    private static void checkVillageProximity(ServerPlayer player, MinecraftServer server) {
        PlayerData data = PlayerData.getServer(server);
        if (data.hasVisitedVillage(player.getUUID())) return;

        AABB searchBox = new AABB(
                player.getX() - 48, player.getY() - 16, player.getZ() - 48,
                player.getX() + 48, player.getY() + 16, player.getZ() + 48
        );

        List<Villager> villagers = player.level().getEntitiesOfClass(Villager.class, searchBox, v -> true);

        if (!villagers.isEmpty()) {
            data.markVillageVisited(player.getUUID());

            Map<String, Integer> progress = new HashMap<>();
            progress.put("crafting_table_unlock", 1);
            IdlecraftNetworking.syncConditionProgress(player, progress);
        }
    }
}