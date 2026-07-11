package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.common.ServerTick;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

public class StatTracker {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!ServerTick.every("stat_tracker", 20)) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                trackPlayer(player, server);
            }
        });
    }

    private static void trackPlayer(ServerPlayer player, MinecraftServer server) {
        PlayerData data = PlayerData.getServer(server);
        Map<String, Integer> progress = new HashMap<>();

        if (!data.hasStatBase(player.getUUID(), "sticks_picked")) {
            data.setStatBase(player.getUUID(), "sticks_picked", getSticksPicked(player));
        }
        int baseSticks = data.getStatBase(player.getUUID(), "sticks_picked");
        int currentSticks = getSticksPicked(player);
        int deltaSticks = Math.max(0, currentSticks - baseSticks);
        progress.put("first_steps", Math.min(deltaSticks, 5));

        if (!data.hasStatBase(player.getUUID(), "wood_mined")) {
            data.setStatBase(player.getUUID(), "wood_mined", getWoodMined(player));
        }
        int baseWood = data.getStatBase(player.getUUID(), "wood_mined");
        int currentWood = getWoodMined(player);
        int deltaWood = Math.max(0, currentWood - baseWood);
        progress.put("wood_mined", Math.min(deltaWood, 15));
        progress.put("sticky", Math.min(deltaWood, 5));

        if (!data.hasStatBase(player.getUUID(), "wood_mined_axe")) {
            data.setStatBase(player.getUUID(), "wood_mined_axe", getWoodMinedWithAxe(player));
        }
        int baseAxe = data.getStatBase(player.getUUID(), "wood_mined_axe");
        int currentAxe = getWoodMinedWithAxe(player);
        int deltaAxe = Math.max(0, currentAxe - baseAxe);
        progress.put("wood_mined_axe", Math.min(deltaAxe, 16));

        if (data.hasVisitedVillage(player.getUUID())) {
            progress.put("crafting_table_unlock", 1);
        }

        if (!data.hasStatBase(player.getUUID(), "cobblestone_mined")) {
            data.setStatBase(player.getUUID(), "cobblestone_mined", getCobblestoneMined(player));
        }
        int baseCobble = data.getStatBase(player.getUUID(), "cobblestone_mined");
        int currentCobble = getCobblestoneMined(player);
        int deltaCobble = Math.max(0, currentCobble - baseCobble);
        progress.put("cobblestone", Math.min(deltaCobble, 18));

        if (!data.hasStatBase(player.getUUID(), "planks_crafted")) {
            data.setStatBase(player.getUUID(), "planks_crafted", getPlanksCrafted(player));
        }
        int basePlanks = data.getStatBase(player.getUUID(), "planks_crafted");
        int currentPlanks = getPlanksCrafted(player);
        int deltaPlanks = Math.max(0, currentPlanks - basePlanks);
        progress.put("stonecutter", Math.min(deltaPlanks, 8));

        var advManager = server.getAdvancements();
        var adv = advManager.get(Identifier.fromNamespaceAndPath("minecraft", "husbandry/plant_seed"));
        if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone()) {
            progress.put("seedy_place", 1);
        }

        if (data.getUnlockedNodes(player.getUUID()).contains("stone_1")) {
            long day = player.level().getGameTime() / 24000L;
            if (day >= 1) progress.put("days_survived", 1);
        }

        progress.put("cave_dark_damage", data.getFurnaceCounter(player.getUUID(), "cave_dark_damage"));
        progress.put("cave_hunger_damage", data.getFurnaceCounter(player.getUUID(), "cave_hunger_damage"));
        progress.put("crafted_quality", data.getFurnaceCounter(player.getUUID(), "crafted_quality"));

        IdlecraftNetworking.syncConditionProgress(player, progress);
    }

    public static int getSticksPicked(ServerPlayer player) {
        return player.getStats().getValue(Stats.ITEM_PICKED_UP.get(Items.STICK));
    }

    public static int getWoodMined(ServerPlayer player) {
        int total = 0;
        Block[] logs = {
                Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CHERRY_LOG, Blocks.MANGROVE_LOG,
                Blocks.PALE_OAK_LOG, Blocks.CRIMSON_STEM, Blocks.WARPED_STEM
        };
        for (Block log : logs) {
            total += player.getStats().getValue(Stats.BLOCK_MINED.get(log));
        }
        return total;
    }

    public static int getWoodMinedWithAxe(ServerPlayer player) {
        return getWoodMined(player);
    }

    public static int getCobblestoneMined(ServerPlayer player) {
        return player.getStats().getValue(Stats.BLOCK_MINED.get(Blocks.COBBLESTONE));
    }

    public static int getPlanksCrafted(ServerPlayer player) {
        Item[] planks = {
                Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS,
                Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.CHERRY_PLANKS, Items.MANGROVE_PLANKS,
                Items.PALE_OAK_PLANKS, Items.BAMBOO_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS
        };
        int total = 0;
        for (Item plank : planks) {
            total += player.getStats().getValue(Stats.ITEM_CRAFTED.get(plank));
        }
        return total;
    }
}