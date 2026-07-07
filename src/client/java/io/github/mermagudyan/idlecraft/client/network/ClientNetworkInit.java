package io.github.mermagudyan.idlecraft.client.network;

import io.github.mermagudyan.idlecraft.network.ClientState;
import io.github.mermagudyan.idlecraft.network.ConditionProgressPayload;
import io.github.mermagudyan.idlecraft.network.NodesSyncPayload;
import io.github.mermagudyan.idlecraft.network.PointsSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworkInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PointsSyncPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> ClientState.setPoints(payload.points())));

        ClientPlayNetworking.registerGlobalReceiver(NodesSyncPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> ClientState.setUnlockedNodes(payload.nodes())));

        ClientPlayNetworking.registerGlobalReceiver(ConditionProgressPayload.ID,
                (payload, ctx) -> ctx.client().execute(() -> ClientState.setConditionProgress(payload.progress())));
    }
}