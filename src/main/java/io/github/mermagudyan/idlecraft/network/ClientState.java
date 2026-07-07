package io.github.mermagudyan.idlecraft.network;

import java.util.ArrayList;
import java.util.List;

public class ClientState {
    private static int points = 0;
    private static List<String> unlockedNodes = new ArrayList<>();

    public static int getPoints() { return points; }
    public static void setPoints(int p) { points = p; }

    public static List<String> getUnlockedNodes() { return new ArrayList<>(unlockedNodes); }
    public static void setUnlockedNodes(List<String> nodes) { unlockedNodes = new ArrayList<>(nodes); }
    public static boolean isUnlocked(String nodeId) { return unlockedNodes.contains(nodeId); }
    public static void addUnlocked(String nodeId) {
        if (!unlockedNodes.contains(nodeId)) unlockedNodes.add(nodeId);
    }
}