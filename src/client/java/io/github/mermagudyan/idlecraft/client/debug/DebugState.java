package io.github.mermagudyan.idlecraft.client.debug;

import net.minecraft.client.player.LocalPlayer;
import io.github.mermagudyan.idlecraft.network.ClientState;

public class DebugState {
    public static boolean isAvailable(LocalPlayer player) {
        if (player == null) return false;
        return ClientState.isDebug();
    }
}