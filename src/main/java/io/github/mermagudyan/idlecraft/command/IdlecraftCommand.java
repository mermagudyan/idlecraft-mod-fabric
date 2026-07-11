package io.github.mermagudyan.idlecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.mermagudyan.idlecraft.common.ClaimStore;
import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.world.BlockConverter;
import io.github.mermagudyan.idlecraft.data.PlayerData;
import io.github.mermagudyan.idlecraft.network.IdlecraftNetworking;
import io.github.mermagudyan.idlecraft.screen.SkillNode;
import io.github.mermagudyan.idlecraft.screen.SkillNodeRegistry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class IdlecraftCommand {

    private static final String[] QUALITY_NAMES = {
            "poor", "so-so", "normal", "excellent", "superior", "corrupted"
    };

    private static final DynamicCommandExceptionType ERROR_QUALITY =
            new DynamicCommandExceptionType(o -> Component.literal("[Idlecraft] Unknown quality: " + o
                    + " (poor, so-so, normal, excellent, superior, corrupted)"));
    private static final Dynamic2CommandExceptionType ERROR_ENCHANT_CAP =
            new Dynamic2CommandExceptionType((lvl, cap) -> Component.literal(
                    "[Idlecraft] Enchant level " + lvl + " exceeds the quality cap (" + cap + ")"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("idlecraft")
                .then(literal("create")
                        .requires(IdlecraftCommand::isDebugOn)
                        .then(argument("item", ItemArgument.item(buildContext))
                                .then(argument("durability", IntegerArgumentType.integer(1, 100))
                                        .then(argument("quality", StringArgumentType.word())
                                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(QUALITY_NAMES, b))
                                                .executes(ctx -> createItem(ctx, false))
                                                .then(argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                                                        .then(argument("level", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> createItem(ctx, true))))))))
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
                .then(literal("clear")
                        .requires(IdlecraftCommand::isDebugOn)
                        .then(argument("node", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (SkillNode n : SkillNodeRegistry.getAll()) {
                                        builder.suggest(n.id);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> clearNode(ctx.getSource(), StringArgumentType.getString(ctx, "node")))))
                .then(literal("claim")
                        .executes(ctx -> claimChunk(ctx.getSource())))
                .then(literal("unclaim")
                        .executes(ctx -> unclaimChunk(ctx.getSource())))
                .then(literal("structureprotect")
                        .executes(ctx -> toggleStructureProtect(ctx.getSource(), true))
                        .then(literal("on").executes(ctx -> toggleStructureProtect(ctx.getSource(), true)))
                        .then(literal("off").executes(ctx -> toggleStructureProtect(ctx.getSource(), false))))
                .then(literal("convert")
                        .requires(IdlecraftCommand::isDebugOn)
                        .then(argument("radius", IntegerArgumentType.integer(0, 64))
                                .executes(ctx -> convertBlocks(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))))
                        .executes(ctx -> convertBlocks(ctx.getSource(), 8)))
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

        String lower = nodeArg.toLowerCase();
        if (lower.equals("all") || lower.equals("full")) {
            PlayerData data = PlayerData.getServer(player.level().getServer());
            List<String> alreadyUnlocked = data.getUnlockedNodes(player.getUUID());
            List<String> newlyUnlocked = new ArrayList<>();
            for (SkillNode n : SkillNodeRegistry.getAll()) {
                if (!alreadyUnlocked.contains(n.id)) {
                    data.unlockNode(player.getUUID(), n.id);
                    newlyUnlocked.add(n.id);
                }
                for (int i = 0; i < n.sacrifices.size(); i++) {
                    data.setSacrificeProgress(player.getUUID(), n.id, i, n.sacrifices.get(i).amount());
                }
            }
            IdlecraftNetworking.syncNodesToClient(player);
            IdlecraftNetworking.syncSacrificeState(player);
            source.sendSuccess(() -> Component.literal("[Idlecraft] Unlocked all nodes"
                    + (newlyUnlocked.isEmpty() ? "" : " (" + newlyUnlocked.size() + " new)")), false);
            return 1;
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

        List<String> alreadyUnlocked = data.getUnlockedNodes(player.getUUID());
        List<String> newlyUnlocked = new ArrayList<>();
        for (String id : toUnlock) {
            if (!alreadyUnlocked.contains(id)) {
                data.unlockNode(player.getUUID(), id);
                newlyUnlocked.add(id);
            }
        }

        final String unlockedName = target.name;
        final int prereqCount = newlyUnlocked.size() - 1;
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

    private static int clearNode(CommandSourceStack source, String nodeArg) {
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

        List<String> toRemove = new ArrayList<>();
        collectDescendants(SkillNodeRegistry.getAll(), target.id, toRemove);
        if (!toRemove.contains(target.id)) toRemove.add(target.id);

        if (toRemove.size() <= 1) {
            performClear(player, target, toRemove);
            return 1;
        }

        String parentName = "Start";
        if (target.parentId != null) {
            for (SkillNode n : SkillNodeRegistry.getAll()) {
                if (n.id.equals(target.parentId)) {
                    parentName = n.name;
                    break;
                }
            }
        }
        IdlecraftNetworking.sendClearConfirm(player, target.id, target.name, parentName, toRemove.size());
        return 1;
    }

    private static int createItem(CommandContext<CommandSourceStack> ctx,
                                  boolean withEnchant) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("[Idlecraft] Run this command in-game."), false);
            return 0;
        }

        ItemInput input = ItemArgument.getItem(ctx, "item");
        int durability = IntegerArgumentType.getInteger(ctx, "durability");
        int quality = parseQuality(StringArgumentType.getString(ctx, "quality").toLowerCase());

        ItemStack stack = input.createItemStack(1);

        QualityComponent.applyQuality(stack, quality);
        int max = stack.getMaxDamage();
        if (max > 0) {
            int remaining = Math.max(1, (int) Math.round(max * (durability / 100.0)));
            remaining = Math.min(remaining, max);
            stack.setDamageValue(max - remaining);
        }

        if (withEnchant) {
            int level = IntegerArgumentType.getInteger(ctx, "level");
            Holder.Reference<Enchantment> ench = ResourceArgument.getEnchantment(ctx, "enchantment");
            int cap = QualityComponent.enchantCap(quality);
            if (level > cap) {
                throw ERROR_ENCHANT_CAP.create(level, cap == Integer.MAX_VALUE ? "unlimited" : cap);
            }
            stack.enchant(ench, level);
        }

        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        final int q = quality;
        final String itemName = stack.getHoverName().getString();
        source.sendSuccess(() -> Component.literal("[Idlecraft] Created " + itemName
                + " [" + QualityComponent.tierName(q) + ", " + durability + "% durability]"), false);
        return 1;
    }

    private static int parseQuality(String s) throws CommandSyntaxException {
        return switch (s) {
            case "poor" -> QualityComponent.POOR;
            case "so-so", "soso" -> QualityComponent.SO_SO;
            case "normal" -> QualityComponent.NORMAL;
            case "excellent" -> QualityComponent.EXCELLENT;
            case "superior" -> QualityComponent.SUPERIOR;
            case "corrupted" -> QualityComponent.CORRUPTED;
            default -> throw ERROR_QUALITY.create(s);
        };
    }

    public static void collectDescendants(SkillNode[] all, String rootId, List<String> out) {
        for (SkillNode n : all) {
            if (rootId.equals(n.parentId) && !out.contains(n.id)) {
                out.add(n.id);
                collectDescendants(all, n.id, out);
            }
        }
    }

    public static void performClear(ServerPlayer player, SkillNode target, List<String> toRemove) {
        PlayerData data = PlayerData.getServer(player.level().getServer());
        List<String> unlocked = data.getUnlockedNodes(player.getUUID());
        for (String id : toRemove) {
            unlocked.remove(id);
            data.clearSacrificeProgress(player.getUUID(), id);
        }
        
        
        data.clearStatBases(player.getUUID());
        for (String key : new String[] {
                "cave_dark_damage", "cave_hunger_damage", "furnace_opened",
                "food_cooked", "furnace_takes", "crafted_quality", "days_survived"
        }) {
            data.setFurnaceCounter(player.getUUID(), key, 0);
        }
        IdlecraftNetworking.syncNodesToClient(player);
        IdlecraftNetworking.syncSacrificeState(player);
        player.sendSystemMessage(Component.literal("[Idlecraft] Cleared " + toRemove.size()
                + " node(s) up to " + target.name));
    }

    private static int resetPlayer(CommandSourceStack source) {
        if (requireDebug(source) == 0) return 0;
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        PlayerData data = PlayerData.getServer(player.level().getServer());
        data.resetAll(player.getUUID());
        IdlecraftNetworking.syncPointsToClient(player);
        IdlecraftNetworking.syncNodesToClient(player);
        IdlecraftNetworking.syncSacrificeState(player);
        source.sendSuccess(() -> Component.literal("[Idlecraft] Progress reset."), false);
        return 1;
    }

    private static int claimChunk(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("[Idlecraft] Run this command in-game."), false);
            return 0;
        }
        ServerLevel level = player.level();
        ChunkPos chunk = player.chunkPosition();
        ClaimStore.claim(level, chunk, player.getUUID());
        source.sendSuccess(() -> Component.literal("[Idlecraft] Claimed chunk " + chunk
                + ". You may now break/rebuild inside protected structures here."), false);
        return 1;
    }

    private static int unclaimChunk(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("[Idlecraft] Run this command in-game."), false);
            return 0;
        }
        ServerLevel level = player.level();
        ChunkPos chunk = player.chunkPosition();
        ClaimStore.unclaim(level, chunk);
        source.sendSuccess(() -> Component.literal("[Idlecraft] Unclaimed chunk " + chunk + "."), false);
        return 1;
    }

    private static int toggleStructureProtect(CommandSourceStack source, boolean enabled) {
        io.github.mermagudyan.idlecraft.common.StructureProtection.setProtectionEnabled(enabled);
        source.sendSuccess(() -> Component.literal("[Idlecraft] Structure protection "
                + (enabled ? "ENABLED" : "DISABLED") + "."), true);
        return 1;
    }

    private static int convertBlocks(CommandSourceStack source, int radius) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("[Idlecraft] Run this command in-game."), false);
            return 0;
        }
        int converted = BlockConverter.convertAround(player, radius);
        source.sendSuccess(() -> Component.literal("[Idlecraft] Converted " + converted
                + " vanilla workstation block(s) to Idlecraft variants."), true);
        return 1;
    }
}
