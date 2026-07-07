package io.github.mermagudyan.idlecraft.screen;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class SkillNodeRegistry {

    public static SkillNode[] getAll() {
        List<SkillNode> all = new ArrayList<>();
        all.addAll(simple());
        all.addAll(medium());
        all.addAll(complex());
        return all.toArray(new SkillNode[0]);
    }

    // ============================================================
    // SIMPLE
    // ============================================================
    public static List<SkillNode> simple() {
        List<SkillNode> list = new ArrayList<>();

        // ЦЕНТР
        list.add(new SkillNode(
                "start", 0, 0, 80,
                "Start",
                "The beginning of your idle journey.",
                "Unlocks the Idlecraft menu and basic progression. Your adventure starts here.",
                0, Items.GRASS_BLOCK, null,
                null, null,
                SkillNodeCategory.SIMPLE,
                "open_menu"
        ));

        // ВЕРХ — ветка Wood
        list.add(new SkillNode(
                "sticky", -100, -200, 60,
                "First Steps",
                "Get 5 sticks to unlock wood breaking.",
                "Collect 5 sticks by breaking leaves or dead bushes. Sticks can be used as a slow tool to break wood (1 stick = 1 wood block).",
                0, Items.STICK, "start",
                "custom", "Get 5 sticks",
                SkillNodeCategory.SIMPLE,
                "effect.wood_breaking_unlock"
        ));

        // НИЗ — ветка Stone
        list.add(new SkillNode(
                "stone_1", -180, 200, 60,
                "Miner I",
                "Mine stone. +5% pickaxe speed.",
                "Grants +5% block break speed when using a pickaxe. Affects stone, ores, and deepslate.",
                5, Items.COBBLESTONE, "start",
                "custom", "Unlock top branch",
                SkillNodeCategory.SIMPLE,
                "effect.pickaxe_speed_5"
        ));

        // ПРАВО — ветка Tech
        list.add(new SkillNode(
                "tech_1", 200, 0, 60,
                "Efficiency I",
                "+10% movement speed.",
                "Applies a permanent movement speed boost. Stacks with potions and beacons.",
                10, Items.REDSTONE, "start",
                "custom", null,                    // ← было null, null; стало "custom", null
                SkillNodeCategory.SIMPLE,
                "effect.move_speed_10"
        ));

        return list;
    }

    // ============================================================
    // MEDIUM
    // ============================================================
    public static List<SkillNode> medium() {
        List<SkillNode> list = new ArrayList<>();

        // Wood branch
        list.add(new SkillNode(
                "wood_2", -254, -360, 60,
                "Lumberjack II",
                "+10% wood drop. Auto-replant saplings.",
                "10% chance to drop an extra log when chopping trees. Saplings are auto-planted on the spot.",
                25, Items.STRIPPED_OAK_LOG, "wood_1",
                null, null,
                SkillNodeCategory.MEDIUM,
                "effect.wood_drop_10"
        ));
        list.add(new SkillNode(
                "wood_3", 25, -320, 60,
                "Arborist",
                "Reveal all trees in 64-block radius.",
                "Highlights all trees within 64 blocks with particles. +1 point per tree chopped.",
                100, Items.GOLDEN_AXE, "wood_1",
                null, null,
                SkillNodeCategory.MEDIUM,
                "effect.tree_reveal"
        ));

        // Stone branch
        list.add(new SkillNode(
                "stone_2", -250, 360, 60,
                "Miner II",
                "+10% ore drop. Auto-smelt 20% of ores.",
                "10% chance for extra ore drop. 20% of mined ores are auto-smelted into ingots.",
                25, Items.IRON_ORE, "stone_1",
                null, null,
                SkillNodeCategory.MEDIUM,
                "effect.ore_drop_10"
        ));
        list.add(new SkillNode(
                "stone_3", 10, 380, 60,
                "Prospector",
                "Reveal ores in 32-block radius.",
                "Highlights ores within 32 blocks. +2 points per ore mined.",
                100, Items.DIAMOND_PICKAXE, "stone_1",
                null, null,
                SkillNodeCategory.MEDIUM,
                "effect.ore_reveal"
        ));

        // Tech branch
        list.add(new SkillNode(
                "tech_2", 360, 0, 60,
                "Efficiency II",
                "+20% attack speed. +1 max health.",
                "Applies attack speed boost and increases max health by 1 (half heart).",
                40, Items.REPEATER, "tech_1",
                null, null,
                SkillNodeCategory.MEDIUM,
                "effect.attack_speed_20"
        ));

        return list;
    }

    // ============================================================
    // COMPLEX
    // ============================================================
    public static List<SkillNode> complex() {
        List<SkillNode> list = new ArrayList<>();

        list.add(new SkillNode(
                "tech_3", 520, 0, 60,
                "Overclock",
                "+15% global action speed.",
                "Increases ALL action speeds: mining, chopping, attacking, walking, and eating by 15%.",
                200, Items.REDSTONE_BLOCK, "tech_2",
                null, null,
                SkillNodeCategory.COMPLEX,
                "effect.global_speed_15"
        ));



        return list;
    }
}