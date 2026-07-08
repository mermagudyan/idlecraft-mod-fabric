package io.github.mermagudyan.idlecraft.network;

import io.github.mermagudyan.idlecraft.screen.SkillNodeRegistry;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import io.github.mermagudyan.idlecraft.event.StatTracker;
import java.util.Map;
import java.util.List;
import io.github.mermagudyan.idlecraft.screen.SkillNode;
import io.github.mermagudyan.idlecraft.screen.SacrificeRequirement;
import java.util.HashMap;

public class IdlecraftNetworking {

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(PointsSyncPayload.TYPE, PointsSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NodesSyncPayload.TYPE, NodesSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConditionProgressPayload.TYPE, ConditionProgressPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NodePurchasePayload.TYPE, NodePurchasePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ResetRewardedPayload.TYPE, ResetRewardedPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SacrificeOfferPayload.TYPE, SacrificeOfferPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SacrificeStatePayload.TYPE, SacrificeStatePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SacrificeOfferPayload.TYPE,
                (payload, ctx) -> {
                    ServerPlayer player = ctx.player();
                    MinecraftServer server = player.level().getServer();
                    if (server == null) return;
                    server.execute(() -> handleSacrifice(player, server, payload.nodeId()));
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(ResetRewardedPayload.TYPE,
                (payload, ctx) -> {
                    ServerPlayer player = ctx.player();
                    MinecraftServer server = player.level().getServer();
                    if (server == null) return;
                    server.execute(() -> {
                        PlayerData data = PlayerData.getServer(server);
                        data.clearRewardedAdvancements(player.getUUID());
                        System.out.println("[IDLECRAFT] ResetRewarded: cleared for "
                                + player.getName().getString());
                        player.sendSystemMessage(
                                Component.literal("[Idlecraft] Rewarded list cleared.")
                        );
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(NodePurchasePayload.TYPE,
                (payload, ctx) -> {
                    ServerPlayer player = ctx.player();
                    MinecraftServer server = player.level().getServer();
                    if (server == null) return;

                    server.execute(() -> {
                        PlayerData data = PlayerData.getServer(server);
                        String nodeId = payload.nodeId();
                        List<String> unlocked = data.getUnlockedNodes(player.getUUID());

                        System.out.println("[IDLECRAFT] Purchase request: " + nodeId
                                + " by " + player.getName().getString()
                                + " | already unlocked: " + unlocked.contains(nodeId));

                        if (unlocked.contains(nodeId)) {
                            System.out.println("[IDLECRAFT] Already unlocked, skip: " + nodeId);
                            return;
                        }

                        int cost = getCost(nodeId);
                        int currentPoints = data.getPoints(player.getUUID());
                        if (currentPoints < cost) {
                            System.out.println("[IDLECRAFT] Purchase REJECTED (points): " + nodeId
                                    + " | need=" + cost + " | have=" + currentPoints);
                            player.sendSystemMessage(Component.literal("[Idlecraft] Not enough points."));
                            return;
                        }

                        if ("stone_1".equals(nodeId)) {
                            if (!unlocked.contains("axe_node")) {
                                player.sendSystemMessage(Component.literal("[Idlecraft] Condition not met: Unlock axe branch."));
                                return;
                            }
                        }

                        if ("first_steps".equals(nodeId)) {
                            int progress = StatTracker.getSticksPicked(player) - data.getStatBase(player.getUUID(), "sticks_picked");
                            if (progress < 5) {
                                player.sendSystemMessage(Component.literal("[Idlecraft] Condition not met: Get 5 sticks."));
                                return;
                            }
                        }

                        if ("village_visit".equals(nodeId)) {
                            if (!data.hasVisitedVillage(player.getUUID())) {
                                player.sendSystemMessage(Component.literal("[Idlecraft] Condition not met: Visit a village."));
                                return;
                            }
                        }

                        if ("wooden_tools".equals(nodeId)) {
                            int progress = StatTracker.getWoodMined(player) - data.getStatBase(player.getUUID(), "wood_mined");
                            if (progress < 15) {
                                player.sendSystemMessage(Component.literal("[Idlecraft] Condition not met: Mine 15 wood."));
                                return;
                            }
                        }

                        if ("axe_node".equals(nodeId)) {
                            int progress = StatTracker.getWoodMined(player) - data.getStatBase(player.getUUID(), "wood_mined_axe");
                            if (progress < 16) {
                                player.sendSystemMessage(Component.literal("[Idlecraft] Condition not met: Mine 16 wood with axe."));
                                return;
                            }
                        }

                        if ("tech_1".equals(nodeId)) {
                            var adv = server.getAdvancements().get(Identifier.fromNamespaceAndPath("minecraft", "husbandry/plant_seed"));
                            if (adv == null || !player.getAdvancements().getOrStartProgress(adv).isDone()) {
                                player.sendSystemMessage(Component.literal("[Idlecraft] Condition not met: Earn 'A Seedy Place'."));
                                return;
                            }
                        }

                        System.out.println("[IDLECRAFT] Purchase OK: " + nodeId
                                + " | cost=" + cost
                                + " | points before=" + currentPoints
                                + " | points after=" + (currentPoints - cost));

                        data.setPoints(player.getUUID(), currentPoints - cost);
                        data.unlockNode(player.getUUID(), nodeId);

                        syncPointsToClient(player);
                        syncNodesToClient(player);

                        System.out.println("[IDLECRAFT] Synced to client. Total unlocked: "
                                + data.getUnlockedNodes(player.getUUID()).size());
                    });
                });
    }

    private static void handleSacrifice(ServerPlayer player, MinecraftServer server, String nodeId) {
        PlayerData data = PlayerData.getServer(server);
        if (data.getUnlockedNodes(player.getUUID()).contains(nodeId)) return;

        SkillNode node = null;
        for (SkillNode n : SkillNodeRegistry.getAll()) {
            if (n.id.equals(nodeId)) { node = n; break; }
        }
        if (node == null || node.sacrifices.isEmpty()) return;

        if (node.parentId != null && !data.getUnlockedNodes(player.getUUID()).contains(node.parentId)) return;

        boolean allMet = true;
        for (int i = 0; i < node.sacrifices.size(); i++) {
            SacrificeRequirement req = node.sacrifices.get(i);
            List<Integer> prog = data.getSacrificeProgress(player.getUUID(), nodeId);
            int current = i < prog.size() ? prog.get(i) : 0;
            if (current < req.amount()) {
                allMet = false;
                if (player.getInventory().contains(new ItemStack(req.item()))) {
                    int slot = player.getInventory().findSlotMatchingItem(new ItemStack(req.item()));
                    if (slot < 0) continue;
                    player.getInventory().getItem(slot).shrink(1);
                    data.setSacrificeProgress(player.getUUID(), nodeId, i, current + 1);
                    syncSacrificeState(player);
                    allMet = true;
                    for (int j = 0; j < node.sacrifices.size(); j++) {
                        SacrificeRequirement r = node.sacrifices.get(j);
                        List<Integer> p = data.getSacrificeProgress(player.getUUID(), nodeId);
                        int c = j < p.size() ? p.get(j) : 0;
                        if (c < r.amount()) { allMet = false; break; }
                    }
                    break;
                }
            }
        }

        if (allMet) {
            data.unlockNode(player.getUUID(), nodeId);
            data.clearSacrificeProgress(player.getUUID(), nodeId);
            syncPointsToClient(player);
            syncNodesToClient(player);
            syncSacrificeState(player);
        }
    }

    public static void syncSacrificeState(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerData data = PlayerData.getServer(server);
        Map<String, int[]> progress = new HashMap<>();
        for (SkillNode n : SkillNodeRegistry.getAll()) {
            if (n.sacrifices.isEmpty()) continue;
            List<Integer> nodeProgress = data.getSacrificeProgress(player.getUUID(), n.id);
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

    public static void syncPointsToClient(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        int pts = PlayerData.getServer(server).getPoints(player.getUUID());
        ServerPlayNetworking.send(player, new PointsSyncPayload(pts));
    }

    public static void syncNodesToClient(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        List<String> nodes = PlayerData.getServer(server).getUnlockedNodes(player.getUUID());
        ServerPlayNetworking.send(player, new NodesSyncPayload(nodes));
    }

    public static void syncConditionProgress(ServerPlayer player, Map<String, Integer> progress) {
        ServerPlayNetworking.send(player, new ConditionProgressPayload(progress));
    }
}