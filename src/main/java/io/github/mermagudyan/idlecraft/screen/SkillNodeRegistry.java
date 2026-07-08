package io.github.mermagudyan.idlecraft.screen;

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

    public static List<SkillNode> simple() {
        List<SkillNode> list = new ArrayList<>();

        list.add(new SkillNode(
                "start", 0, 0, 80,
                "Start",
                "The beginning of your idle journey.",
                "Unlocks the Idlecraft menu and basic progression.",
                0, Items.GRASS_BLOCK, null,
                null, null,
                SkillNodeCategory.SIMPLE,
                "open_menu"
        ));

        list.add(new SkillNode(
                "first_steps", 0, -200, 60,
                "First Steps",
                "Get 5 sticks to unlock wood breaking.",
                "Collect 5 sticks by breaking leaves or dead bushes.",
                0, Items.STICK, "start",
                "custom", "Get 5 sticks",
                SkillNodeCategory.SIMPLE,
                "effect.wood_breaking_unlock"
        ));

        list.add(new SkillNode(
                "sticky", 0, -400, 60,
                "Lumberjack I",
                "Chop wood. +5% axe speed.",
                "Grants +5% block break speed when using an axe.",
                0, Items.OAK_LOG, "first_steps",
                null, null,
                SkillNodeCategory.SIMPLE,
                "effect.axe_speed_5"
        ));

        list.add(new SkillNode(
                "village_visit", -300, -600, 60,
                "Village Visit",
                "Visit a village.",
                "Find and enter a village to unlock stonecutter recipes.",
                0, Items.EMERALD, "sticky",
                "custom", "Visit a village",
                SkillNodeCategory.SIMPLE,
                "trigger.village"
        ));

        list.add(new SkillNode(
                "crafting_table_unlock", -300, -800, 60,
                "Crafting Table",
                "Sacrifice 5 wood to unlock crafting table.",
                "Use a stonecutter to duplicate wood, then sacrifice 5 logs to unlock the crafting table.",
                0, Items.CRAFTING_TABLE, "village_visit",
                null, null,
                SkillNodeCategory.SIMPLE,
                "unlock.crafting_table",
                List.of(new SacrificeRequirement(Items.OAK_LOG, 5)), false
        ));

        list.add(new SkillNode(
                "wooden_tools", -300, -1000, 60,
                "Wooden Tools",
                "Sacrifice 20 seeds. Mine 15 wood.",
                "Craft a hoe, plant seeds, earn 'A Seedy Place'. Sacrifice 20 seeds and mine 15 wood to unlock wooden tools.",
                0, Items.WOODEN_HOE, "crafting_table_unlock",
                "custom", "Mine 15 wood",
                SkillNodeCategory.SIMPLE,
                "unlock.wooden_tools",
                List.of(new SacrificeRequirement(Items.WHEAT_SEEDS, 20)), false
        ));

        list.add(new SkillNode(
                "axe_node", 300, -600, 60,
                "Axe Master",
                "Sacrifice 16 wood + 2 apples.",
                "Craft a wooden axe, mine 16 wood with it, then sacrifice 16 wood and 2 apples to unlock stone.",
                0, Items.WOODEN_AXE, "sticky",
                null, null,
                SkillNodeCategory.SIMPLE,
                "trigger.axe",
                List.of(
                        new SacrificeRequirement(Items.OAK_LOG, 16),
                        new SacrificeRequirement(Items.APPLE, 2)
                ), false
        ));

        list.add(new SkillNode(
                "stone_1", 300, -800, 60,
                "Miner I",
                "Mine stone. +5% pickaxe speed.",
                "Grants +5% block break speed when using a pickaxe.",
                5, Items.COBBLESTONE, "axe_node",
                "custom", "Unlock axe branch",
                SkillNodeCategory.SIMPLE,
                "effect.pickaxe_speed_5"
        ));

        list.add(new SkillNode(
                "tech_1", 0, -700, 60,
                "Efficiency I",
                "+10% movement speed.",
                "Sacrifice 3 bread to unlock movement speed boost.",
                0, Items.BREAD, "sticky",
                "custom", "Earn 'A Seedy Place'",
                SkillNodeCategory.SIMPLE,
                "effect.move_speed_10",
                List.of(new SacrificeRequirement(Items.BREAD, 3)), false
        ));

        return list;
    }

    public static List<SkillNode> medium() {
        List<SkillNode> list = new ArrayList<>();

        list.add(new SkillNode(
                "stone_2", 150, -1000, 60,
                "Miner II",
                "+10% ore drop. Auto-smelt 20% of ores.",
                "10% chance for extra ore drop. 20% of mined ores are auto-smelted.",
                25, Items.IRON_ORE, "stone_1",
                null, null,
                SkillNodeCategory.MEDIUM,
                "effect.ore_drop_10",
                List.of(), true
        ));

        list.add(new SkillNode(
                "stone_3", 450, -1000, 60,
                "Prospector",
                "Reveal ores in 32-block radius.",
                "Highlights ores within 32 blocks.",
                100, Items.DIAMOND_PICKAXE, "stone_1",
                null, null,
                SkillNodeCategory.MEDIUM,
                "effect.ore_reveal",
                List.of(), true
        ));

        list.add(new SkillNode(
                "tech_2", -150, -850, 60,
                "Efficiency II",
                "+20% attack speed. +1 max health.",
                "Applies attack speed boost and increases max health by 1.",
                40, Items.REPEATER, "tech_1",
                null, null,
                SkillNodeCategory.MEDIUM,
                "effect.attack_speed_20",
                List.of(), true
        ));

        return list;
    }

    public static List<SkillNode> complex() {
        List<SkillNode> list = new ArrayList<>();

        list.add(new SkillNode(
                "tech_3", -150, -1050, 60,
                "Overclock",
                "+15% global action speed.",
                "Increases ALL action speeds by 15%.",
                200, Items.REDSTONE_BLOCK, "tech_2",
                null, null,
                SkillNodeCategory.COMPLEX,
                "effect.global_speed_15",
                List.of(), true
        ));

        return list;
    }
}