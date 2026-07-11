package io.github.mermagudyan.idlecraft.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ClaimStore {

    private ClaimStore() {
    }

    private static final Map<Level, Map<Long, UUID>> CLAIMS = new HashMap<>();

    public static Optional<UUID> owner(ServerLevel level, BlockPos pos) {
        Map<Long, UUID> map = CLAIMS.get(level);
        if (map == null) return Optional.empty();
        return Optional.ofNullable(map.get(ChunkPos.containing(pos).pack()));
    }

    public static boolean isClaimed(ServerLevel level, ChunkPos chunk) {
        Map<Long, UUID> map = CLAIMS.get(level);
        return map != null && map.containsKey(chunk.pack());
    }

    public static void claim(ServerLevel level, ChunkPos chunk, UUID owner) {
        CLAIMS.computeIfAbsent(level, k -> new HashMap<>()).put(chunk.pack(), owner);
    }

    public static void unclaim(ServerLevel level, ChunkPos chunk) {
        Map<Long, UUID> map = CLAIMS.get(level);
        if (map != null) map.remove(chunk.pack());
    }
}
