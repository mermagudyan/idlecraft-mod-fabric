package io.github.mermagudyan.idlecraft.network;

import io.github.mermagudyan.idlecraft.screen.SkillNodeRegistry;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import io.github.mermagudyan.idlecraft.event.StatTracker;
import java.util.Map;
import java.util.List;
import io.github.mermagudyan.idlecraft.screen.SkillNode;
import io.github.mermagudyan.idlecraft.screen.SacrificeRequirement;
import net.minecraft.item.ItemStack;
import java.util.HashMap;

public class IdlecraftNetworking {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(PointsSyncPayload.ID, PointsSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NodesSyncPayload.ID, NodesSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConditionProgressPayload.ID, ConditionProgressPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NodePurchasePayload.ID, NodePurchasePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ResetRewardedPayload.ID, ResetRewardedPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SacrificeOfferPayload.ID, SacrificeOfferPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SacrificeStatePayload.ID, SacrificeStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SacrificeOfferPayload.ID,
                (payload, ctx) -> {
                    ServerPlayerEntity player = ctx.player();
                    MinecraftServer server = player.getEntityWorld().getServer();
                    if (server == null) return;
                    server.execute(() -> handleSacrifice(player, server, payload.nodeId()));
                }
        );
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

                        if ("stone_1".equals(nodeId)) {
                            if (!unlocked.contains("axe_node")) {
                                player.sendMessage(net.minecraft.text.Text.literal("[Idlecraft] Condition not met: Unlock axe branch."), false);
                                return;
                            }
                        }

                        if ("wooden_tools".equals(nodeId)) {
                            int progress = StatTracker.getWoodMined(player) - data.getStatBase(player.getUuid(), "wood_mined");
                            if (progress < 15) {
                                player.sendMessage(net.minecraft.text.Text.literal("[Idlecraft] Condition not met: Mine 15 wood."), false);
                                return;
                            }
                        }

                        if ("axe_node".equals(nodeId)) {
                            int progress = StatTracker.getWoodMined(player) - data.getStatBase(player.getUuid(), "wood_mined_axe");
                            if (progress < 16) {
                                player.sendMessage(net.minecraft.text.Text.literal("[Idlecraft] Condition not met: Mine 16 wood with axe."), false);
                                return;
                            }
                        }

                        if ("tech_1".equals(nodeId)) {
                            var adv = server.getAdvancementLoader().get(net.minecraft.util.Identifier.of("minecraft", "husbandry/plant_seed"));
                            if (adv == null || !player.getAdvancementTracker().getProgress(adv).isDone()) {
                                player.sendMessage(net.minecraft.text.Text.literal("[Idlecraft] Condition not met: Earn 'A Seedy Place'."), false);
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

    private static void handleSacrifice(ServerPlayerEntity player, MinecraftServer server, String nodeId) {
        PlayerData data = PlayerData.getServer(server);
        if (data.getUnlockedNodes(player.getUuid()).contains(nodeId)) return;

        SkillNode node = null;
        for (SkillNode n : SkillNodeRegistry.getAll()) {
            if (n.id.equals(nodeId)) { node = n; break; }
        }
        if (node == null || node.sacrifices.isEmpty()) return;

        if (node.parentId != null && !data.getUnlockedNodes(player.getUuid()).contains(node.parentId)) return;

        boolean allMet = true;
        for (int i = 0; i < node.sacrifices.size(); i++) {
            SacrificeRequirement req = node.sacrifices.get(i);
            List<Integer> prog = data.getSacrificeProgress(player.getUuid(), nodeId);
            int current = i < prog.size() ? prog.get(i) : 0;
            if (current < req.amount()) {
                allMet = false;
                if (player.getInventory().contains(new ItemStack(req.item()))) {
                    int slot = player.getInventory().getSlotWithStack(new ItemStack(req.item()));
                    player.getInventory().getStack(slot).decrement(1);
                    data.setSacrificeProgress(player.getUuid(), nodeId, i, current + 1);
                    syncSacrificeState(player);
                    allMet = true;
                    for (int j = 0; j < node.sacrifices.size(); j++) {
                        SacrificeRequirement r = node.sacrifices.get(j);
                        List<Integer> p = data.getSacrificeProgress(player.getUuid(), nodeId);
                        int c = j < p.size() ? p.get(j) : 0;
                        if (c < r.amount()) { allMet = false; break; }
                    }
                    break;
                }
            }
        }

        if (allMet) {
            data.unlockNode(player.getUuid(), nodeId);
            data.clearSacrificeProgress(player.getUuid(), nodeId);
            syncPointsToClient(player);
            syncNodesToClient(player);
            syncSacrificeState(player);
        }
    }

    public static void syncSacrificeState(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;
        PlayerData data = PlayerData.getServer(server);
        Map<String, int[]> progress = new HashMap<>();
        for (SkillNode n : SkillNodeRegistry.getAll()) {
            if (n.sacrifices.isEmpty()) continue;
            List<Integer> nodeProgress = data.getSacrificeProgress(player.getUuid(), n.id);
            int[] arr = new int[n.sacrifices.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = i < nodeProgress.size() ? nodeProgress.get(i) : 0;
            }
            progress.put(n.id, arr);
        }
        ServerPlayNetworking.send(player, new SacrificeStatePayload(progress));
    }

    private static int getCost(String nodeId) {
        for (SkillNode n : SkillNodeRegistry.getAll()) {
            if (n.id.equals(nodeId)) return n.cost;
        }
        return 0;
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