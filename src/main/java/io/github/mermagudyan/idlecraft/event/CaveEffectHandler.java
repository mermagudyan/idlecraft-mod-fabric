package io.github.mermagudyan.idlecraft.event;

import io.github.mermagudyan.idlecraft.data.PlayerData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CaveEffectHandler {

    private static int tickCounter = 0;
    private static final Map<UUID, Integer> zoneSeconds = new HashMap<>();
    private static final int GRACE_SECONDS = 7;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < 20) return;
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                handlePlayer(player, server);
            }
        });
    }

    private static void handlePlayer(ServerPlayer player, MinecraftServer server) {
        if (player.isCreative() || player.isSpectator()) {
            zoneSeconds.remove(player.getUUID());
            return;
        }

        PlayerData data = PlayerData.getServer(server);
        var unlocked = data.getUnlockedNodes(player.getUUID());
        boolean caveExplorer = unlocked.contains("cave_explorer");
        boolean caveMaster = unlocked.contains("cave_master");

        boolean inCave = isInCaveAir(player);

        double limit;
        if (caveMaster) {
            limit = -1.0;
        } else if (caveExplorer) {
            limit = 15.0;
        } else {
            limit = inCave ? 60.0 : 30.0;
        }

        double y = player.getY();
        if (y < limit) {
            int seconds = zoneSeconds.getOrDefault(player.getUUID(), 0) + 1;
            zoneSeconds.put(player.getUUID(), seconds);

            // "The unknown": a darkness effect that is continuously refreshed while inside the
            // forbidden zone, so it stays infinitely extended (cycling from 0s up to 2s).
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));

            // Grace period: give the player GRACE_SECONDS to climb back out before it starts
            // dealing damage. Only actual damage from this effect counts towards the conditions.
            if (seconds > GRACE_SECONDS) {
                player.hurt(player.level().damageSources().generic(), 4.0f);
                data.setFurnaceCounter(player.getUUID(), "cave_hunger_damage", 1);
                if (!caveExplorer) {
                    data.setFurnaceCounter(player.getUUID(), "cave_dark_damage", 1);
                }
            }
        } else {
            zoneSeconds.remove(player.getUUID());
        }
    }

    private static boolean isInCaveAir(Player player) {
        BlockPos feet = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        return player.level().getBlockState(feet).is(Blocks.CAVE_AIR)
                || player.level().getBlockState(eyes).is(Blocks.CAVE_AIR);
    }
}
