package io.github.mermagudyan.idlecraft;

import io.github.mermagudyan.idlecraft.command.IdlecraftCommand;
import io.github.mermagudyan.idlecraft.event.PlayerJoinHandler;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class IdleMod implements ModInitializer {
    public static final String MOD_ID = "idlecraft";

    @Override
    public void onInitialize() {
        IdlecraftNetworking.register();
        PlayerJoinHandler.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                IdlecraftCommand.register(dispatcher));
        System.out.println("[Idlecraft] Initialized.");
    }
}