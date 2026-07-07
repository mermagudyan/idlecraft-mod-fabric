package io.github.mermagudyan.idlecraft.network;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import io.github.mermagudyan.idlecraft.event.StatTracker;
import java.util.Map;
import java.util.List;

public class IdlecraftNetworking {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(PointsSyncPayload.ID, PointsSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NodesSyncPayload.ID, NodesSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConditionProgressPayload.ID, ConditionProgressPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NodePurchasePayload.ID, NodePurchasePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ResetRewardedPayload.ID, ResetRewardedPayload.CODEC);

        // === HANDLER 1: Запрос на сброс наград ===
        ServerPlayNetworking.registerGlobalReceiver(ResetRewardedPayload.ID,
                (payload, ctx) -> {
                    ServerPlayerEntity player = ctx.player();
                    MinecraftServer server = player.getEntityWorld().getServer();
                    if (server == null) return;
                    server.execute(() -> {
                        PlayerData data = PlayerData.getServer(server);
                        data.clearRewardedAdvancements(player.getUuid());
                        System.out.println("[IDLECRAFT] ResetRewarded: cleared for "
                                + player.getName().getString());
                        player.sendMessage(
                                net.minecraft.text.Text.literal("[Idlecraft] Rewarded list cleared."),
                                false
                        );
                    });
                });

        // === HANDLER 2: Запрос на покупку ноды ===
        ServerPlayNetworking.registerGlobalReceiver(NodePurchasePayload.ID,
                (payload, ctx) -> {
                    ServerPlayerEntity player = ctx.player();
                    MinecraftServer server = player.getEntityWorld().getServer();
                    if (server == null) return;

                    server.execute(() -> {
                        PlayerData data = PlayerData.getServer(server);
                        String nodeId = payload.nodeId();
                        java.util.List<String> unlocked = data.getUnlockedNodes(player.getUuid());

                        System.out.println("[IDLECRAFT] Purchase request: " + nodeId
                                + " by " + player.getName().getString()
                                + " | already unlocked: " + unlocked.contains(nodeId));

                        if (unlocked.contains(nodeId)) {
                            System.out.println("[IDLECRAFT] Already unlocked, skip: " + nodeId);
                            return;
                        }

                        int cost = getCost(nodeId);
                        int currentPoints = data.getPoints(player.getUuid());
                        if (currentPoints < cost) {
                            System.out.println("[IDLECRAFT] Purchase REJECTED (points): " + nodeId
                                    + " | need=" + cost + " | have=" + currentPoints);
                            player.sendMessage(net.minecraft.text.Text.literal("[Idlecraft] Not enough points."), false);
                            return;
                        }

                        if ("wood_1".equals(nodeId)) {
                            int progress = StatTracker.getWoodMined(player) - data.getStatBase(player.getUuid(), "wood_mined");
                            if (progress < 5) {
                                System.out.println("[IDLECRAFT] Purchase REJECTED (condition wood_1): progress=" + progress);
                                player.sendMessage(net.minecraft.text.Text.literal("[Idlecraft] Condition not met: Chop 5 wood."), false);
                                return;
                            }
                        }

                        if ("stone_1".equals(nodeId)) {
                            if (!unlocked.contains("wood_2") && !unlocked.contains("wood_3")) {
                                System.out.println("[IDLECRAFT] Purchase REJECTED (condition stone_1)");
                                player.sendMessage(net.minecraft.text.Text.literal("[Idlecraft] Condition not met: Unlock top branch."), false);
                                return;
                            }
                        }

                        System.out.println("[IDLECRAFT] Purchase OK: " + nodeId
                                + " | cost=" + cost
                                + " | points before=" + currentPoints
                                + " | points after=" + (currentPoints - cost));

                        data.setPoints(player.getUuid(), currentPoints - cost);
                        data.unlockNode(player.getUuid(), nodeId);

                        syncPointsToClient(player);
                        syncNodesToClient(player);

                        System.out.println("[IDLECRAFT] Synced to client. Total unlocked: "
                                + data.getUnlockedNodes(player.getUuid()).size());
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

    public static void syncConditionProgress(ServerPlayerEntity player, Map<String, Integer> progress) {
        ServerPlayNetworking.send(player, new ConditionProgressPayload(progress));
    }
}