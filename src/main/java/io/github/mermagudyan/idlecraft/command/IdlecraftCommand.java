package io.github.mermagudyan.idlecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class IdlecraftCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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

    private static int addPoints(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = PlayerData.getServer(player.level().getServer());
        data.addPoints(player.getUUID(), amount);
        IdlecraftNetworking.syncPointsToClient(player);
        source.sendSuccess(() -> Component.literal("[Idlecraft] +" + amount + " points"), false);
        return 1;
    }

    private static int setPoints(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = PlayerData.getServer(player.level().getServer());
        data.setPoints(player.getUUID(), amount);
        IdlecraftNetworking.syncPointsToClient(player);
        source.sendSuccess(() -> Component.literal("[Idlecraft] Points set to " + amount), false);
        return 1;
    }

    private static int getPoints(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = PlayerData.getServer(player.level().getServer());
        int pts = data.getPoints(player.getUUID());
        source.sendSuccess(() -> Component.literal("[Idlecraft] Current points: " + pts), false);
        return 1;
    }

    private static int resetPlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = PlayerData.getServer(player.level().getServer());
        data.resetAll(player.getUUID());
        IdlecraftNetworking.syncPointsToClient(player);
        IdlecraftNetworking.syncNodesToClient(player);
        source.sendSuccess(() -> Component.literal("[Idlecraft] Progress reset."), false);
        return 1;
    }
}