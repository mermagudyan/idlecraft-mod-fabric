package io.github.mermagudyan.idlecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class IdlecraftCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("idlecraft")
                .then(literal("points")
                        .then(literal("add")
                                .then(argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> addPoints(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(literal("set")
                                .then(argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> setPoints(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(literal("get")
                                .executes(ctx -> getPoints(ctx.getSource())))
                )
                .then(literal("reset")
                        .executes(ctx -> resetPlayer(ctx.getSource())))
        );
    }

    private static int addPoints(ServerCommandSource source, int amount) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        var data = PlayerData.getServer(player.getEntityWorld().getServer());
        data.addPoints(player.getUuid(), amount);
        IdlecraftNetworking.syncPointsToClient(player);
        source.sendFeedback(() -> Text.literal("[Idlecraft] +" + amount + " points"), false);
        return 1;
    }

    private static int setPoints(ServerCommandSource source, int amount) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        var data = PlayerData.getServer(player.getEntityWorld().getServer());
        data.setPoints(player.getUuid(), amount);
        IdlecraftNetworking.syncPointsToClient(player);
        source.sendFeedback(() -> Text.literal("[Idlecraft] Points set to " + amount), false);
        return 1;
    }

    private static int getPoints(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        var data = PlayerData.getServer(player.getEntityWorld().getServer());
        int pts = data.getPoints(player.getUuid());
        source.sendFeedback(() -> Text.literal("[Idlecraft] Current points: " + pts), false);
        return 1;
    }

    private static int resetPlayer(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        var data = PlayerData.getServer(player.getEntityWorld().getServer());
        data.resetAll(player.getUuid());
        IdlecraftNetworking.syncPointsToClient(player);
        IdlecraftNetworking.syncNodesToClient(player);
        source.sendFeedback(() -> Text.literal("[Idlecraft] Progress reset."), false);
        return 1;
    }
}