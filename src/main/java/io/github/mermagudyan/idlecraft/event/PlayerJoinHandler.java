package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerJoinHandler {

    private static final Map<UUID, Integer> pendingSyncs = new ConcurrentHashMap<>();

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID id = handler.getPlayer().getUuid();
            pendingSyncs.put(id, 40);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            pendingSyncs.remove(handler.getPlayer().getUuid());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, Integer>> it = pendingSyncs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> entry = it.next();
                int ticks = entry.getValue() - 1;
                if (ticks <= 0) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                    if (player != null) {
                        IdlecraftNetworking.syncPointsToClient(player);
                        IdlecraftNetworking.syncNodesToClient(player);
                        IdlecraftNetworking.syncSacrificeState(player);
                    }
                    it.remove();
                } else {
                    pendingSyncs.put(entry.getKey(), ticks);
                }
            }
        });
    }
}