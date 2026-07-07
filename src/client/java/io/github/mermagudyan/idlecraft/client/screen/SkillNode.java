package io.github.mermagudyan.idlecraft.client.screen;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SkillNode {
    public final String id;
    public final float x, y;
    public final int size;
    public final String name;
    public final String description;
    public final int cost;
    public final Item icon;
    public final String parentId;

    // null = видна всегда; "parent_unlocked" = видна после разблокировки parent
    public final String unlockCondition;
    public final String conditionText;

    public boolean unlocked = false;

    public SkillNode(String id, float x, float y, int size,
                     String name, String description, int cost, Item icon, String parentId) {
        this(id, x, y, size, name, description, cost, icon, parentId, "parent_unlocked", null);
    }

    public SkillNode(String id, float x, float y, int size,
                     String name, String description, int cost, Item icon, String parentId,
                     String unlockCondition, String conditionText) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.size = size;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.icon = icon;
        this.parentId = parentId;
        this.unlockCondition = unlockCondition;
        this.conditionText = conditionText;
    }

    public Text getNameText() {
        return Text.literal(name).formatted(unlocked ? Formatting.GREEN : Formatting.WHITE);
    }

    public Text getDescText() {
        return Text.literal(description).formatted(Formatting.GRAY);
    }

    public Text getCostText() {
        return Text.literal("Cost:    " + cost).formatted(Formatting.GOLD);
    }

    public static SkillNode[] defaults() {
        return new SkillNode[] {
                // ЦЕНТР
                new SkillNode("start", 0, 0, 80,
                        "Start",
                        "The beginning of your idle journey.",
                        0, Items.GRASS_BLOCK, null),

                // ВЕРХ — ветка Wood
                new SkillNode("wood_1", -100, -200, 60,
                        "Lumberjack I",
                        "Chop wood.",
                        0, Items.OAK_LOG, "start",
                        "custom", "Chop 5 wood"),
                new SkillNode("wood_2", -254, -360, 60,
                        "Lumberjack II",
                        "Auto-replant saplings.",
                        25, Items.STRIPPED_OAK_LOG, "wood_1"),
                new SkillNode("wood_3", 25, -320, 60,
                        "Arborist",
                        "Reveal all trees in 64-block radius.",
                        100, Items.GOLDEN_AXE, "wood_1"),

                // НИЗ — ветка Stone
                new SkillNode("stone_1", -180, 200, 60,
                        "Miner I",
                        "Mine stone.",
                        5, Items.COBBLESTONE, "start",
                        "custom", "Unlock top branches"),
                new SkillNode("stone_2", -250, 360, 60,
                        "Miner II",
                        "+10% ore drop. Auto-smelt 20% of ores.",
                        25, Items.IRON_ORE, "stone_1"),
                new SkillNode("stone_3", 10, 380, 60,
                        "Prospector",
                        "Reveal ores in 32-block radius. +2 points per ore mined.",
                        100, Items.DIAMOND_PICKAXE, "stone_1"),

                // ПРАВО — ветка Tech
                new SkillNode("tech_1", 200, 0, 60,
                        "Efficiency I",
                        "+10% movement speed.",
                        10, Items.REDSTONE, "start"),
                new SkillNode("tech_2", 360, 0, 60,
                        "Efficiency II",
                        "+20% attack speed. +1 max health.",
                        40, Items.REPEATER, "tech_1"),
                new SkillNode("tech_3", 520, 0, 60,
                        "Overclock",
                        "+15% global action speed (mine, chop, attack, walk).",
                        200, Items.REDSTONE_BLOCK, "tech_2"),
        };
    }
}