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
import net.minecraft.world.item.Items;
import io.github.mermagudyan.idlecraft.event.StatTracker;
import java.util.Map;
import java.util.List;
import io.github.mermagudyan.idlecraft.screen.SkillNode;
import io.github.mermagudyan.idlecraft.screen.SacrificeRequirement;
import io.github.mermagudyan.idlecraft.command.IdlecraftCommand;
import net.minecraft.world.item.Item;
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
        PayloadTypeRegistry.clientboundPlay().register(DebugStatePayload.TYPE, DebugStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClearConfirmPayload.TYPE, ClearConfirmPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClearNodesPayload.TYPE, ClearNodesPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SacrificeOfferPayload.TYPE,
                (payload, ctx) -> {
                    ServerPlayer player = ctx.player();
                    MinecraftServer server = player.level().getServer();
                    if (server == null) return;
                    server.execute(() -> handleSacrifice(player, server, payload.nodeId()));
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(ClearNodesPayload.TYPE,
                (payload, ctx) -> {
                    ServerPlayer player = ctx.player();
                    MinecraftServer server = player.level().getServer();
                    if (server == null) return;
                    server.execute(() -> {
                        String nodeId = payload.nodeId();
                        SkillNode target = null;
                        for (SkillNode n : SkillNodeRegistry.getAll()) {
                            if (n.id.equals(nodeId)) { target = n; break; }
                        }
                        if (target == null) return;
                        java.util.List<String> toRemove = new java.util.ArrayList<>();
                        IdlecraftCommand.collectDescendants(SkillNodeRegistry.getAll(), nodeId, toRemove);
                        if (!toRemove.contains(nodeId)) toRemove.add(nodeId);
                        IdlecraftCommand.performClear(player, target, toRemove);
                    });
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
                            for (SkillNode leaf : SkillNodeRegistry.getLeavesOfBranch(SkillNodeRegistry.BRANCH_TUTORIAL)) {
                                if (!unlocked.contains(leaf.id)) {
                                    player.sendSystemMessage(Component.literal("[Idlecraft] Condition not met: Upgrade tutorial branch."));
                                    return;
                                }
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

        for (SacrificeRequirement req : node.sacrifices) {
            List<Integer> prog = data.getSacrificeProgress(player.getUUID(), nodeId);
            int idx = node.sacrifices.indexOf(req);
            int current = idx < prog.size() ? prog.get(idx) : 0;
            if (current < req.amount() && hasRequiredItem(player, req)) {
                infuseItem(player, server, node, req);
                break;
            }
        }
    }

    private static boolean hasRequiredItem(ServerPlayer player, SacrificeRequirement req) {
        if (req.anyWood()) {
            for (Item wood : WOOD_ITEMS) {
                if (player.getInventory().contains(new ItemStack(wood))) return true;
            }
            return false;
        }
        return player.getInventory().contains(new ItemStack(req.item()));
    }

    private static Item findInfuseItem(ServerPlayer player, SacrificeRequirement req) {
        if (req.anyWood()) {
            for (Item wood : WOOD_ITEMS) {
                if (player.getInventory().contains(new ItemStack(wood))) return wood;
            }
            return null;
        }
        return req.item();
    }

    private static final java.util.List<Item> WOOD_ITEMS = java.util.List.of(
            Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG,
            Items.ACACIA_LOG, Items.DARK_OAK_LOG, Items.CHERRY_LOG, Items.MANGROVE_LOG,
            Items.PALE_OAK_LOG, Items.CRIMSON_STEM, Items.WARPED_STEM,
            Items.OAK_WOOD, Items.SPRUCE_WOOD, Items.BIRCH_WOOD, Items.JUNGLE_WOOD,
            Items.ACACIA_WOOD, Items.DARK_OAK_WOOD, Items.CHERRY_WOOD, Items.MANGROVE_WOOD,
            Items.PALE_OAK_WOOD, Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE
    );

    public static void infuseItem(ServerPlayer player, MinecraftServer server, SkillNode node, SacrificeRequirement req) {
        PlayerData data = PlayerData.getServer(server);
        String nodeId = node.id;
        int idx = node.sacrifices.indexOf(req);
        if (idx < 0) return;
        List<Integer> prog = data.getSacrificeProgress(player.getUUID(), nodeId);
        int current = idx < prog.size() ? prog.get(idx) : 0;
        if (current >= req.amount()) return;
        Item item = findInfuseItem(player, req);
        if (item == null) return;
        int slot = player.getInventory().findSlotMatchingItem(new ItemStack(item));
        if (slot < 0) return;
        player.getInventory().getItem(slot).shrink(1);
        data.setSacrificeProgress(player.getUUID(), nodeId, idx, current + 1);
        syncSacrificeState(player);

        boolean allMet = true;
        List<Integer> updated = data.getSacrificeProgress(player.getUUID(), nodeId);
        for (int j = 0; j < node.sacrifices.size(); j++) {
            SacrificeRequirement r = node.sacrifices.get(j);
            int c = j < updated.size() ? updated.get(j) : 0;
            if (c < r.amount()) { allMet = false; break; }
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

    private static void collectDescendants(SkillNode[] all, String rootId, java.util.List<String> out) {
        for (SkillNode n : all) {
            if (rootId.equals(n.parentId) && !out.contains(n.id)) {
                out.add(n.id);
                collectDescendants(all, n.id, out);
            }
        }
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

    public static void syncDebugToClient(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        boolean d = PlayerData.getServer(server).isDebug(player.getUUID());
        ServerPlayNetworking.send(player, new DebugStatePayload(d));
    }

    public static void sendClearConfirm(ServerPlayer player, String nodeId, String targetName, String parentName, int removedCount) {
        ServerPlayNetworking.send(player, new ClearConfirmPayload(nodeId, targetName, parentName, removedCount));
    }
}