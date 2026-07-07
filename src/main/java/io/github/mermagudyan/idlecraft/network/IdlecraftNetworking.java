package io.github.mermagudyan.idlecraft.network;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public class IdlecraftNetworking {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(PointsSyncPayload.ID, PointsSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NodesSyncPayload.ID, NodesSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NodePurchasePayload.ID, NodePurchasePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ResetRewardedPayload.ID, ResetRewardedPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ResetRewardedPayload.ID,
                (payload, ctx) -> {
                    ServerPlayerEntity player = ctx.player();
                    MinecraftServer server = player.getEntityWorld().getServer();
                    if (server == null) return;
                    server.execute(() -> {
                        PlayerData data = PlayerData.getServer(server);
                        data.clearRewardedAdvancements(player.getUuid());
                        player.sendMessage(
                                net.minecraft.text.Text.literal("[Idlecraft] Rewarded list cleared. You can earn points again."),
                                false
                        );
                    });
                });
        // Серверный handler: запрос на покупку ноды
        ServerPlayNetworking.registerGlobalReceiver(NodePurchasePayload.ID,
                (payload, ctx) -> {
                    ServerPlayerEntity player = ctx.player();
                    MinecraftServer server = player.getEntityWorld().getServer();
                    if (server == null) return;

                    server.execute(() -> {
                        PlayerData data = PlayerData.getServer(server);
                        String nodeId = payload.nodeId();
                        List<String> unlocked = data.getUnlockedNodes(player.getUuid());

                        // Если уже разблокирована — игнорируем
                        if (unlocked.contains(nodeId)) return;

                        // Проверяем очки (стоимость определяем на сервере)
                        int cost = getCost(nodeId);
                        int currentPoints = data.getPoints(player.getUuid());
                        if (currentPoints < cost) {
                            player.sendMessage(Text.literal("[Idlecraft] Not enough points."), false);
                            return;
                        }

                        // Списываем и разблокируем
                        data.setPoints(player.getUuid(), currentPoints - cost);
                        data.unlockNode(player.getUuid(), nodeId);

                        // Синхронизируем с клиентом
                        syncPointsToClient(player);
                        syncNodesToClient(player);
                    });
                });
    }

    private static int getCost(String nodeId) {
        return switch (nodeId) {
            case "start" -> 0;
            case "mining", "combat", "speed" -> 50;
            default -> 10;
        };
    }

    public static void syncPointsToClient(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;
        int pts = PlayerData.getServer(server).getPoints(player.getUuid());
        ServerPlayNetworking.send(player, new PointsSyncPayload(pts));
    }

    public static void syncNodesToClient(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;
        List<String> nodes = PlayerData.getServer(server).getUnlockedNodes(player.getUuid());
        ServerPlayNetworking.send(player, new NodesSyncPayload(nodes));
    }
}