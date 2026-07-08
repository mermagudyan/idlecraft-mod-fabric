package io.github.mermagudyan.idlecraft.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientState {
    private static int points = 0;
    private static List<String> unlockedNodes = new ArrayList<>();
    private static Map<String, Integer> conditionProgress = new HashMap<>();

    public static int getPoints() { return points; }
    public static void setPoints(int p) { points = p; }

    public static List<String> getUnlockedNodes() { return new ArrayList<>(unlockedNodes); }
    public static void setUnlockedNodes(List<String> nodes) { unlockedNodes = new ArrayList<>(nodes); }
    public static boolean isUnlocked(String nodeId) { return unlockedNodes.contains(nodeId); }
    public static void addUnlocked(String nodeId) {
        if (!unlockedNodes.contains(nodeId)) unlockedNodes.add(nodeId);
    }
    private static Map<String, int[]> sacrificeProgress = new HashMap<>();

    public static int[] getSacrificeProgress(String nodeId) {
        return sacrificeProgress.getOrDefault(nodeId, new int[0]);
    }

    public static void setSacrificeProgress(Map<String, int[]> progress) {
        sacrificeProgress = new HashMap<>(progress);
    }
    public static int getProgress(String nodeId) { return conditionProgress.getOrDefault(nodeId, 0); }
    public static void setConditionProgress(Map<String, Integer> p) { conditionProgress = new HashMap<>(p); }
}