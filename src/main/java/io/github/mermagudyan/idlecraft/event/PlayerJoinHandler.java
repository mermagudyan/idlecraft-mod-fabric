package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerJoinHandler {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            // Синхронизация после входа (через 2 тика, чтобы клиент был готов)
            server.execute(() -> {
                IdlecraftNetworking.syncPointsToClient(player);
                IdlecraftNetworking.syncNodesToClient(player);
            });
        });
    }
}