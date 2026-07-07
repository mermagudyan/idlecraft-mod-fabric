package io.github.mermagudyan.idlecraft.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerData extends PersistentState {
    public static final String KEY = "idlecraft_player_data";

    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, List<String>> unlockedNodes = new HashMap<>();

    // Пустой конструктор для дефолта
    public PlayerData() {}

    // Конструктор для десериализации
    public PlayerData(Map<String, Integer> pts, Map<String, List<String>> nodes, Map<String, List<String>> rewarded) {
        pts.forEach((k, v) -> this.points.put(UUID.fromString(k), v));
        nodes.forEach((k, v) -> this.unlockedNodes.put(UUID.fromString(k), v));
        rewarded.forEach((k, v) -> {
            Set<String> set = new HashSet<>(v);
            this.rewardedAdvancements.put(UUID.fromString(k), set);
        });
    }

    public void clearRewardedAdvancements(UUID id) {
        rewardedAdvancements.remove(id);
        markDirty();
    }

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .optionalFieldOf("points", Map.of())
                            .forGetter(d -> toStringMap(d.points)),
                    Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING))
                            .optionalFieldOf("nodes", Map.of())
                            .forGetter(d -> toStringMapNodes(d.unlockedNodes)),
                    Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING))
                            .optionalFieldOf("rewarded", Map.of())
                            .forGetter(d -> toStringMapSet(d.rewardedAdvancements))
            ).apply(instance, PlayerData::new)
    );

    private static Map<String, List<String>> toStringMapSet(Map<UUID, Set<String>> in) {
        Map<String, List<String>> out = new HashMap<>();
        in.forEach((k, v) -> out.put(k.toString(), new ArrayList<>(v)));
        return out;
    }

    private static Map<String, Integer> toStringMap(Map<UUID, Integer> in) {
        Map<String, Integer> out = new HashMap<>();
        in.forEach((k, v) -> out.put(k.toString(), v));
        return out;
    }

    private static Map<String, List<String>> toStringMapNodes(Map<UUID, List<String>> in) {
        Map<String, List<String>> out = new HashMap<>();
        in.forEach((k, v) -> out.put(k.toString(), v));
        return out;
    }

    private static PersistentStateType<PlayerData> type() {
        return new PersistentStateType<>(KEY, () -> new PlayerData(), CODEC, null);
    }

    public static PlayerData getServer(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(type());
    }

    public int getPoints(UUID id) { return points.getOrDefault(id, 0); }

    public void addPoints(UUID id, int amount) {
        points.merge(id, amount, Integer::sum);
        markDirty();
    }

    public void setPoints(UUID id, int amount) {
        points.put(id, amount);
        markDirty();
    }

    public List<String> getUnlockedNodes(UUID id) {
        return unlockedNodes.computeIfAbsent(id, k -> new ArrayList<>());
    }

    public void unlockNode(UUID id, String nodeId) {
        getUnlockedNodes(id).add(nodeId);

        markDirty();
    }
    public boolean isAdvancementRewarded(UUID id, String advId) {
        return rewardedAdvancements.getOrDefault(id, new HashSet<>()).contains(advId);
    }

    public void markAdvancementRewarded(UUID id, String advId) {
        rewardedAdvancements.computeIfAbsent(id, k -> new HashSet<>()).add(advId);
        markDirty();
    }

    private final Map<UUID, Set<String>> rewardedAdvancements = new HashMap<>();

    public void resetAll(UUID id) {
        points.remove(id);
        unlockedNodes.remove(id);
        rewardedAdvancements.remove(id);
        markDirty();
    }
}