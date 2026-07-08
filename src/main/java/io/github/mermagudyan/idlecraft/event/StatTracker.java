package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

public class StatTracker {

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 20) return;
            tickCounter = 0;

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

        var advManager = server.getAdvancements();
        var adv = advManager.get(Identifier.fromNamespaceAndPath("minecraft", "husbandry/plant_seed"));
        if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone()) {
            progress.put("seedy_place", 1);
        }

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
}