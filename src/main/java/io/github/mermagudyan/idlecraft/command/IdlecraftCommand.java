package io.github.mermagudyan.idlecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import io.github.mermagudyan.idlecraft.screen.SkillNode;
import io.github.mermagudyan.idlecraft.screen.SkillNodeRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class IdlecraftCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("idlecraft")
                .then(literal("debug")
                        .then(literal("on").executes(ctx -> setDebug(ctx.getSource(), true)))
                        .then(literal("off").executes(ctx -> setDebug(ctx.getSource(), false)))
                )
                .then(literal("open")
                        .requires(IdlecraftCommand::isDebugOn)
                        .then(argument("node", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerPlayer player = source.getPlayer();
                                    Set<String> unlocked = new HashSet<>();
                                    if (player != null && player.level().getServer() != null) {
                                        unlocked.addAll(PlayerData.getServer(player.level().getServer())
                                                .getUnlockedNodes(player.getUUID()));
                                    }
                                    for (SkillNode n : SkillNodeRegistry.getAll()) {
                                        if (!unlocked.contains(n.id)) {
                                            builder.suggest(n.id);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> openNode(ctx.getSource(), StringArgumentType.getString(ctx, "node")))))
                .then(literal("points")
                        .requires(IdlecraftCommand::isDebugOn)
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
                        .requires(IdlecraftCommand::isDebugOn)
                        .executes(ctx -> resetPlayer(ctx.getSource())))
        );
    }

    private static boolean isDebugOn(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return true;
        if (player.level().getServer() == null) return true;
        return PlayerData.getServer(player.level().getServer()).isDebug(player.getUUID());
    }

    private static int requireDebug(CommandSourceStack source) {
        if (isDebugOn(source)) return 1;
        source.sendSuccess(() -> Component.literal("[Idlecraft] Debug mode is off. Use /idlecraft debug on."), false);
        return 0;
    }

    private static int setDebug(CommandSourceStack source, boolean on) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("[Idlecraft] Run this command in-game."), false);
            return 0;
        }
        PlayerData.getServer(player.level().getServer()).setDebug(player.getUUID(), on);
        IdlecraftNetworking.syncDebugToClient(player);
        source.sendSuccess(() -> Component.literal("[Idlecraft] Debug mode " + (on ? "ON" : "OFF")), false);
        return 1;
    }

    private static int openNode(CommandSourceStack source, String nodeArg) {
        if (requireDebug(source) == 0) return 0;
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("[Idlecraft] Run this command in-game."), false);
            return 0;
        }

        SkillNode target = null;
        for (SkillNode n : SkillNodeRegistry.getAll()) {
            if (n.id.equalsIgnoreCase(nodeArg) || n.name.equalsIgnoreCase(nodeArg)) {
                target = n;
                break;
            }
        }
        if (target == null) {
            source.sendSuccess(() -> Component.literal("[Idlecraft] Unknown node: " + nodeArg), false);
            return 0;
        }

        PlayerData data = PlayerData.getServer(player.level().getServer());
        final String targetName = target.name;
        if (data.getUnlockedNodes(player.getUUID()).contains(target.id)) {
            source.sendSuccess(() -> Component.literal("[Idlecraft] Node already unlocked: " + targetName), false);
            return 0;
        }
        Set<String> toUnlock = new LinkedHashSet<>();
        String cur = target.id;
        while (cur != null) {
            toUnlock.add(cur);
            SkillNode curNode = null;
            for (SkillNode n : SkillNodeRegistry.getAll()) {
                if (n.id.equals(cur)) { curNode = n; break; }
            }
            cur = curNode != null ? curNode.parentId : null;
        }
        for (String id : toUnlock) data.unlockNode(player.getUUID(), id);

        final String unlockedName = target.name;
        final int prereqCount = toUnlock.size() - 1;
        IdlecraftNetworking.syncNodesToClient(player);
        source.sendSuccess(() -> Component.literal("[Idlecraft] Unlocked: " + unlockedName
                + (prereqCount > 0 ? " (+ " + prereqCount + " prerequisite(s))" : "")), false);
        return 1;
    }

    private static int addPoints(CommandSourceStack source, int amount) {
        if (requireDebug(source) == 0) return 0;
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = PlayerData.getServer(player.level().getServer());
        data.addPoints(player.getUUID(), amount);
        IdlecraftNetworking.syncPointsToClient(player);
        source.sendSuccess(() -> Component.literal("[Idlecraft] +" + amount + " points"), false);
        return 1;
    }

    private static int setPoints(CommandSourceStack source, int amount) {
        if (requireDebug(source) == 0) return 0;
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = PlayerData.getServer(player.level().getServer());
        data.setPoints(player.getUUID(), amount);
        IdlecraftNetworking.syncPointsToClient(player);
        source.sendSuccess(() -> Component.literal("[Idlecraft] Points set to " + amount), false);
        return 1;
    }

    private static int getPoints(CommandSourceStack source) {
        if (requireDebug(source) == 0) return 0;
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = PlayerData.getServer(player.level().getServer());
        int pts = data.getPoints(player.getUUID());
        source.sendSuccess(() -> Component.literal("[Idlecraft] Current points: " + pts), false);
        return 1;
    }

    private static int resetPlayer(CommandSourceStack source) {
        if (requireDebug(source) == 0) return 0;
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
