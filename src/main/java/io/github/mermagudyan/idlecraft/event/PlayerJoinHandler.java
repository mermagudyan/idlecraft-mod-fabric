package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerJoinHandler {

    private static final Map<UUID, Integer> pendingSyncs = new ConcurrentHashMap<>();

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID id = handler.getPlayer().getUUID();
            PlayerData data = PlayerData.getServer(server);
            if (!data.hasDebug(id)) {
                data.setDebug(id, false);
            }
            pendingSyncs.put(id, 40);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            pendingSyncs.remove(handler.getPlayer().getUUID());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, Integer>> it = pendingSyncs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> entry = it.next();
                int ticks = entry.getValue() - 1;
                if (ticks <= 0) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null) {
                        IdlecraftNetworking.syncPointsToClient(player);
                        IdlecraftNetworking.syncNodesToClient(player);
                        IdlecraftNetworking.syncSacrificeState(player);
                        IdlecraftNetworking.syncDebugToClient(player);
                        IdlecraftNetworking.syncRepairState(player);
                    }
                    it.remove();
                } else {
                    pendingSyncs.put(entry.getKey(), ticks);
                }
            }
        });
    }
}