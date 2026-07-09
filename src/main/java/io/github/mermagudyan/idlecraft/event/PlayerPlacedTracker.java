package io.github.mermagudyan.idlecraft.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PlayerPlacedTracker {

    private PlayerPlacedTracker() {
    }

    private static final Map<ResourceKey<Level>, Set<BlockPos>> PLACED = new HashMap<>();

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel level) {
                remove(level, pos.immutable());
            }
            return true;
        });
    }

    private static Set<BlockPos> key(ServerLevel level) {
        return PLACED.computeIfAbsent(level.dimension(), k -> new HashSet<>());
    }

    public static void add(ServerLevel level, BlockPos pos) {
        key(level).add(pos.immutable());
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        Set<BlockPos> set = PLACED.get(level.dimension());
        if (set != null) {
            set.remove(pos.immutable());
        }
    }

    public static boolean isPlayerPlaced(ServerLevel level, BlockPos pos) {
        Set<BlockPos> set = PLACED.get(level.dimension());
        return set != null && set.contains(pos.immutable());
    }
}
