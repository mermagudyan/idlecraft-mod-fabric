package io.github.mermagudyan.idlecraft.client.debug;

import net.minecraft.client.network.ClientPlayerEntity;

public class DebugState {
    public static boolean isAvailable(ClientPlayerEntity player) {
        if (player == null) return false;
        return player.isCreative();
    }
}