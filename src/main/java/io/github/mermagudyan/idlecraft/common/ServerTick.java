package io.github.mermagudyan.idlecraft.common;

import java.util.HashMap;
import java.util.Map;

public final class ServerTick {

    private ServerTick() {
    }

    private static final Map<String, Integer> COUNTERS = new HashMap<>();

    

    public static boolean every(String key, int interval) {
        int current = COUNTERS.getOrDefault(key, 0) + 1;
        if (current >= interval) {
            COUNTERS.put(key, 0);
            return true;
        }
        COUNTERS.put(key, current);
        return false;
    }

    public static void reset(String key) {
        COUNTERS.put(key, 0);
    }
}
