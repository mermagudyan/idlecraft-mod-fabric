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
import java.util.Set;
import java.util.HashSet;

public class PlayerData extends PersistentState {
    public static final String KEY = "idlecraft_player_data";

    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, List<String>> unlockedNodes = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> statBases = new HashMap<>();
    private final Set<UUID> visitedVillage = new HashSet<>();
    private final Map<UUID, Set<String>> rewardedAdvancements = new HashMap<>();
    public PlayerData() {}

    public PlayerData(Map<String, Integer> pts, Map<String, List<String>> nodes,
                      Map<String, List<String>> rewarded, Map<String, Map<String, Integer>> statBases,
                      Set<String> visited, Map<String, Map<String, List<Integer>>> sacrificeProgress) {
        pts.forEach((k, v) -> this.points.put(UUID.fromString(k), v));
        nodes.forEach((k, v) -> this.unlockedNodes.put(UUID.fromString(k), v));
        rewarded.forEach((k, v) -> {
            Set<String> set = new HashSet<>(v);
            this.rewardedAdvancements.put(UUID.fromString(k), set);
        });
        statBases.forEach((k, v) -> this.statBases.put(UUID.fromString(k), new HashMap<>(v)));
        visited.forEach(s -> this.visitedVillage.add(UUID.fromString(s)));
        sacrificeProgress.forEach((k, v) -> {
            Map<String, List<Integer>> inner = new HashMap<>();
            v.forEach((nk, nv) -> inner.put(nk, new ArrayList<>(nv)));
            this.sacrificeProgress.put(UUID.fromString(k), inner);
        });
    }

    public boolean hasStatBase(UUID id, String key) {
        return statBases.getOrDefault(id, new HashMap<>()).containsKey(key);
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
                    Codec.unboundedMap(Codec.STRING, listStringCodec())
                            .optionalFieldOf("nodes", Map.of())
                            .forGetter(d -> toStringMapNodes(d.unlockedNodes)),
                    Codec.unboundedMap(Codec.STRING, listStringCodec())
                            .optionalFieldOf("rewarded", Map.of())
                            .forGetter(d -> toStringMapSet(d.rewardedAdvancements)),
                    Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.INT))
                            .optionalFieldOf("statBases", Map.of())
                            .forGetter(d -> toStringMap2(d.statBases)),
                    Codec.list(Codec.STRING).xmap(
                                    list -> new HashSet<>(list),
                                    set -> new ArrayList<>(set)
                            )
                            .optionalFieldOf("visitedVillage", new HashSet<>())
                            .forGetter(d -> toStringSet(d.visitedVillage)),
                    Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.list(Codec.INT)))
                            .optionalFieldOf("sacrificeProgress", Map.of())
                            .forGetter(d -> toStringMapSacrifice(d.sacrificeProgress))
            ).apply(instance, PlayerData::new)
    );

    private static HashSet<String> toStringSet(Set<UUID> in) {
        HashSet<String> out = new HashSet<>();
        in.forEach(uuid -> out.add(uuid.toString()));
        return out;
    }

    private static Codec<List<String>> listStringCodec() {
        return Codec.list(Codec.STRING).xmap(ArrayList::new, ArrayList::new);
    }

    public boolean hasVisitedVillage(UUID id) {
        return visitedVillage.contains(id);
    }

    public void markVillageVisited(UUID id) {
        if (visitedVillage.add(id)) {
            markDirty();
        }
    }

    public void clearVillageVisit(UUID id) {
        visitedVillage.remove(id);
        markDirty();
    }

    private static Map<String, Map<String, List<Integer>>> toStringMapSacrifice(Map<UUID, Map<String, List<Integer>>> in) {
        Map<String, Map<String, List<Integer>>> out = new HashMap<>();
        in.forEach((k, v) -> out.put(k.toString(), v));
        return out;
    }

    private static Map<String, Map<String, Integer>> toStringMap2(Map<UUID, Map<String, Integer>> in) {
        Map<String, Map<String, Integer>> out = new HashMap<>();
        in.forEach((k, v) -> out.put(k.toString(), new HashMap<>(v)));
        return out;
    }

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
        List<String> list = getUnlockedNodes(id);
        if (!list.contains(nodeId)) {
            list.add(nodeId);
        }
        markDirty();
    }
    public boolean isAdvancementRewarded(UUID id, String advId) {
        return rewardedAdvancements.getOrDefault(id, new HashSet<>()).contains(advId);
    }

    public void markAdvancementRewarded(UUID id, String advId) {
        rewardedAdvancements.computeIfAbsent(id, k -> new HashSet<>()).add(advId);
        markDirty();
    }

    private final Map<UUID, Map<String, List<Integer>>> sacrificeProgress = new HashMap<>();

    public List<Integer> getSacrificeProgress(UUID id, String nodeId) {
        return sacrificeProgress.computeIfAbsent(id, k -> new HashMap<>())
                .computeIfAbsent(nodeId, k -> new ArrayList<>());
    }
    public void setSacrificeProgress(UUID id, String nodeId, int index, int value) {
        Map<String, List<Integer>> playerProgress = sacrificeProgress.computeIfAbsent(id, k -> new HashMap<>());
        List<Integer> nodeProgress = playerProgress.computeIfAbsent(nodeId, k -> new ArrayList<>());
        while (nodeProgress.size() <= index) nodeProgress.add(0);
        nodeProgress.set(index, value);
        markDirty();
    }

    public void clearSacrificeProgress(UUID id, String nodeId) {
        Map<String, List<Integer>> playerProgress = sacrificeProgress.get(id);
        if (playerProgress != null) {
            playerProgress.remove(nodeId);
            markDirty();
        }
    }
    public void resetAll(UUID id) {
        points.remove(id);
        unlockedNodes.remove(id);
        rewardedAdvancements.remove(id);
        statBases.remove(id);
        visitedVillage.remove(id);
        sacrificeProgress.remove(id);
        markDirty();
    }

    public int getStatBase(UUID id, String statKey) {
        return statBases.getOrDefault(id, new HashMap<>()).getOrDefault(statKey, 0);
    }

    public void setStatBase(UUID id, String statKey, int value) {
        statBases.computeIfAbsent(id, k -> new HashMap<>()).put(statKey, value);
        markDirty();
    }

    public void clearStatBases(UUID id) {
        statBases.remove(id);
        markDirty();
    }
}