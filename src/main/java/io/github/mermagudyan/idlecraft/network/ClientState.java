package io.github.mermagudyan.idlecraft.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientState {
    private static int points = 0;
    private static List<String> unlockedNodes = new ArrayList<>();
    private static Map<String, Integer> conditionProgress = new HashMap<>();
    private static Map<String, int[]> sacrificeProgress = new HashMap<>();
    private static boolean debug = false;
    private static Map<String, Long> repairStart = new HashMap<>();
    private static Map<String, Boolean> repairSucceeded = new HashMap<>();

    public static int getPoints() { return points; }
    public static void setPoints(int p) { points = p; }

    public static boolean isDebug() { return debug; }
    public static void setDebug(boolean d) { debug = d; }

    public static long getRepairStart(String nodeId) { return repairStart.getOrDefault(nodeId, 0L); }
    public static boolean isRepairSucceeded(String nodeId) { return repairSucceeded.getOrDefault(nodeId, false); }
    public static void setRepairState(String nodeId, long start, boolean succeeded) {
        repairStart.put(nodeId, start);
        repairSucceeded.put(nodeId, succeeded);
    }

    private static int selectedQuality = 0;

    public static int getSelectedQuality() { return selectedQuality; }
    public static void setSelectedQuality(int q) { selectedQuality = q; }

    public static List<String> getUnlockedNodes() { return new ArrayList<>(unlockedNodes); }
    public static void setUnlockedNodes(List<String> nodes) { unlockedNodes = new ArrayList<>(nodes); }
    public static boolean isUnlocked(String nodeId) { return unlockedNodes.contains(nodeId); }
    public static void addUnlocked(String nodeId) {
        if (!unlockedNodes.contains(nodeId)) unlockedNodes.add(nodeId);
    }

    public static int[] getSacrificeProgress(String nodeId) {
        return sacrificeProgress.getOrDefault(nodeId, new int[0]);
    }

    public static void setSacrificeProgress(Map<String, int[]> progress) {
        sacrificeProgress = new HashMap<>(progress);
    }

    public static int getProgress(String nodeId) { return conditionProgress.getOrDefault(nodeId, 0); }
    public static void setConditionProgress(Map<String, Integer> p) { conditionProgress.putAll(p); }
}