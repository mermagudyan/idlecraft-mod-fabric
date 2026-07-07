package io.github.mermagudyan.idlecraft.client.network;

import io.github.mermagudyan.idlecraft.network.ClientState;
import io.github.mermagudyan.idlecraft.network.NodesSyncPayload;
import io.github.mermagudyan.idlecraft.network.PointsSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworkInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Синхронизация очков
        ClientPlayNetworking.registerGlobalReceiver(PointsSyncPayload.ID,
                (payload, ctx) -> {
                    ctx.client().execute(() -> ClientState.setPoints(payload.points()));
                });

        // Синхронизация разблокированных нод
        ClientPlayNetworking.registerGlobalReceiver(NodesSyncPayload.ID,
                (payload, ctx) -> {
                    ctx.client().execute(() -> ClientState.setUnlockedNodes(payload.nodes()));
                });
    }
}