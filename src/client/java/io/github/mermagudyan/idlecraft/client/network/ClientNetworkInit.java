package io.github.mermagudyan.idlecraft.client.network;

import io.github.mermagudyan.idlecraft.network.ClientState;
import io.github.mermagudyan.idlecraft.network.ConditionProgressPayload;
import io.github.mermagudyan.idlecraft.network.NodesSyncPayload;
import io.github.mermagudyan.idlecraft.network.PointsSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import io.github.mermagudyan.idlecraft.network.SacrificeStatePayload;
import io.github.mermagudyan.idlecraft.network.DebugStatePayload;
import io.github.mermagudyan.idlecraft.network.RepairStatePayload;
import io.github.mermagudyan.idlecraft.network.ClearConfirmPayload;
import io.github.mermagudyan.idlecraft.network.StructureBlockedPayload;
import io.github.mermagudyan.idlecraft.network.StructureRegionPayload;
import io.github.mermagudyan.idlecraft.client.StructureBreakBlocker;
import io.github.mermagudyan.idlecraft.client.screen.ClearConfirmScreen;

public class ClientNetworkInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PointsSyncPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> ClientState.setPoints(payload.points())));

        ClientPlayNetworking.registerGlobalReceiver(NodesSyncPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> ClientState.setUnlockedNodes(payload.nodes())));

        ClientPlayNetworking.registerGlobalReceiver(ConditionProgressPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> ClientState.setConditionProgress(payload.progress())));

        ClientPlayNetworking.registerGlobalReceiver(SacrificeStatePayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> ClientState.setSacrificeProgress(payload.progress()))
        );

        ClientPlayNetworking.registerGlobalReceiver(DebugStatePayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> ClientState.setDebug(payload.debug()))
        );

        ClientPlayNetworking.registerGlobalReceiver(RepairStatePayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() ->
                        ClientState.setRepairState(payload.nodeId(), payload.startMs(), payload.succeeded()))
        );

        ClientPlayNetworking.registerGlobalReceiver(ClearConfirmPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> {
                    if (ctx.client().player == null) return;
                    ctx.client().setScreenAndShow(new ClearConfirmScreen(
                            io.github.mermagudyan.idlecraft.client.screen.IdlecraftScreen.getActiveScreen(),
                            payload.nodeId(),
                            payload.parentName(),
                            payload.removedCount()));
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(StructureBlockedPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> {
                    StructureBreakBlocker.markProtected(payload.pos());
                    if (ctx.client().gameMode != null) {
                        ctx.client().gameMode.stopDestroyBlock();
                    }
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(StructureRegionPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> StructureBreakBlocker.addRegions(payload.boxes()))
        );
    }
}