package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class StatTracker {

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 20) return;
            tickCounter = 0;

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                trackPlayer(player, server);
            }
        });
    }

    private static void trackPlayer(ServerPlayerEntity player, MinecraftServer server) {
        PlayerData data = PlayerData.getServer(server);
        Map<String, Integer> progress = new HashMap<>();

        if (!data.hasStatBase(player.getUuid(), "sticks_picked")) {
            data.setStatBase(player.getUuid(), "sticks_picked", getSticksPicked(player));
        }
        int baseSticks = data.getStatBase(player.getUuid(), "sticks_picked");
        int currentSticks = getSticksPicked(player);
        int deltaSticks = Math.max(0, currentSticks - baseSticks);
        progress.put("first_steps", Math.min(deltaSticks, 5));

        if (!data.hasStatBase(player.getUuid(), "wood_mined")) {
            data.setStatBase(player.getUuid(), "wood_mined", getWoodMined(player));
        }
        int baseWood = data.getStatBase(player.getUuid(), "wood_mined");
        int currentWood = getWoodMined(player);
        int deltaWood = Math.max(0, currentWood - baseWood);
        progress.put("wooden_tools", Math.min(deltaWood, 15));

        if (!data.hasStatBase(player.getUuid(), "wood_mined_axe")) {
            data.setStatBase(player.getUuid(), "wood_mined_axe", getWoodMinedWithAxe(player));
        }
        int baseAxe = data.getStatBase(player.getUuid(), "wood_mined_axe");
        int currentAxe = getWoodMinedWithAxe(player);
        int deltaAxe = Math.max(0, currentAxe - baseAxe);
        progress.put("axe_node", Math.min(deltaAxe, 16));

        if (data.hasVisitedVillage(player.getUuid())) {
            progress.put("crafting_table_unlock", 1);
        }

        var advManager = server.getAdvancementLoader();
        var adv = advManager.get(Identifier.of("minecraft", "husbandry/plant_seed"));
        if (adv != null && player.getAdvancementTracker().getProgress(adv).isDone()) {
            progress.put("seedy_place", 1);
        }

        IdlecraftNetworking.syncConditionProgress(player, progress);
    }

    public static int getSticksPicked(ServerPlayerEntity player) {
        return player.getStatHandler().getStat(
                Stats.PICKED_UP.getOrCreateStat(net.minecraft.item.Items.STICK)
        );
    }

    public static int getWoodMined(ServerPlayerEntity player) {
        int total = 0;
        Block[] logs = {
                Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CHERRY_LOG, Blocks.MANGROVE_LOG,
                Blocks.PALE_OAK_LOG, Blocks.CRIMSON_STEM, Blocks.WARPED_STEM
        };
        for (Block log : logs) {
            total += player.getStatHandler().getStat(Stats.MINED.getOrCreateStat(log));
        }
        return total;
    }

    public static int getWoodMinedWithAxe(ServerPlayerEntity player) {
        int total = 0;
        Block[] logs = {
                Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CHERRY_LOG, Blocks.MANGROVE_LOG,
                Blocks.PALE_OAK_LOG, Blocks.CRIMSON_STEM, Blocks.WARPED_STEM
        };
        ItemStack mainHand = player.getMainHandStack();
        boolean isAxe = mainHand.isIn(net.minecraft.registry.tag.ItemTags.AXES);
        if (!isAxe) return 0;
        for (Block log : logs) {
            total += player.getStatHandler().getStat(Stats.MINED.getOrCreateStat(log));
        }
        return total;
    }
}