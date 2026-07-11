package io.github.mermagudyan.idlecraft;

import io.github.mermagudyan.idlecraft.command.IdlecraftCommand;
import io.github.mermagudyan.idlecraft.event.PlayerJoinHandler;
import io.github.mermagudyan.idlecraft.event.StatTracker;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import io.github.mermagudyan.idlecraft.event.StickToolHandler;
import io.github.mermagudyan.idlecraft.event.VillageVisitHandler;
import io.github.mermagudyan.idlecraft.event.FurnaceUsageTracker;
import io.github.mermagudyan.idlecraft.event.PlayerPlacedTracker;
import io.github.mermagudyan.idlecraft.event.CaveEffectHandler;

public class IdleMod implements ModInitializer {
    public static final String MOD_ID = "idlecraft";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Idlecraft");

    @Override
    public void onInitialize() {
        IdlecraftNetworking.register();
        PlayerJoinHandler.register();
        StatTracker.register();
        StickToolHandler.register();
        PlayerPlacedTracker.register();
        VillageVisitHandler.register();
        FurnaceUsageTracker.register();
        CaveEffectHandler.register();
        io.github.mermagudyan.idlecraft.common.QualityComponent.init();
        io.github.mermagudyan.idlecraft.common.StructureProtection.init();
        io.github.mermagudyan.idlecraft.block.ModBlocks.init();
        io.github.mermagudyan.idlecraft.block.ModCreativeTabs.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                IdlecraftCommand.register(dispatcher, registryAccess));
        LOGGER.info("Initialized.");
    }
}