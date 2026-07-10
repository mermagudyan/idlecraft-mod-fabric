package io.github.mermagudyan.idlecraft.screen;

import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import io.github.mermagudyan.idlecraft.screen.SkillNodeCategory;
import io.github.mermagudyan.idlecraft.screen.SacrificeRequirement;

public class SkillNodeRegistry {

    public static final String BRANCH_TUTORIAL = "Tutorial";
    public static final String BRANCH_MINING = "Mining";
    public static final String BRANCH_TECH = "Tech";
    public static final String BRANCH_STONE = "Stone Knowledge";
    public static final String BRANCH_COPPER = "Copper Vengeance";

    public static SkillNode[] getAll() {
        List<SkillNode> all = new ArrayList<>();
        all.addAll(simple());
        all.addAll(medium());
        all.addAll(complex());
        all.addAll(copper());
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
                "open_menu",
                BRANCH_TUTORIAL
        ));

        list.add(new SkillNode(
                "tech_1", 700, 0, 80,
                "Technology",
                "The starting point of the technology branch.",
                "Opens after the Bread Sacrifice. From here the tech nodes branch out.",
                0, Items.REDSTONE_BLOCK, "start",
                "custom", "Complete Bread Sacrifice",
                SkillNodeCategory.SIMPLE,
                null,
                BRANCH_TECH
        ));

        list.add(new SkillNode(
                "first_steps", 0, -200, 60,
                "First Steps",
                "Get 5 sticks to unlock wood breaking.",
                "Collect 5 sticks by breaking leaves or dead bushes.",
                0, Items.STICK, "start",
                "custom", "Get 5 sticks",
                SkillNodeCategory.SIMPLE,
                "effect.wood_breaking_unlock",
                BRANCH_TUTORIAL
        ));

        list.add(new SkillNode(
                "sticky", 0, -400, 60,
                "First Tree",
                "Mine 5 wood to advance the lumberjack branch.",
                "A node in the lumberjack progression. Has no effect. Requires mining 5 wood.",
                0, Items.OAK_LOG, "first_steps",
                "custom", "Get 5 wood",
                SkillNodeCategory.SIMPLE,
                null,
                BRANCH_TUTORIAL
        ));

        list.add(new SkillNode(
                "village_visit", -300, -600, 60,
                "Village Visit",
                "Visit a village.",
                "Find and enter a village to unlock stonecutter recipes.",
                0, Items.EMERALD, "sticky",
                "custom", "Visit a village",
                SkillNodeCategory.SIMPLE,
                "trigger.village",
                BRANCH_TUTORIAL
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
                List.of(new SacrificeRequirement(Items.OAK_LOG, 5, true)), false,
                BRANCH_TUTORIAL
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
                List.of(new SacrificeRequirement(Items.WHEAT_SEEDS, 20)), false,
                BRANCH_TUTORIAL
        ));

        list.add(new SkillNode(
                "axe_node", -300, -1200, 60,
                "Axe Master",
                "Sacrifice 16 wood + 2 apples.",
                "Craft a wooden axe, mine 16 wood with it, then sacrifice 16 wood and 2 apples to unlock stone.",
                0, Items.WOODEN_AXE, "wooden_tools",
                null, null,
                SkillNodeCategory.SIMPLE,
                "trigger.axe",
                List.of(
                        new SacrificeRequirement(Items.OAK_LOG, 16, true),
                        new SacrificeRequirement(Items.APPLE, 2)
                ), false,
                BRANCH_TUTORIAL
        ));

        list.add(new SkillNode(
                "stonecutter", 0, -760, 60,
                "Stonecutter",
                "Craft planks on a stonecutter.",
                "After visiting a village, craft any planks using a stonecutter to unlock this node.",
                0, Items.STONECUTTER, "village_visit",
                "custom", "Craft 8 planks from stonecutter",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(), true,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "stone_1", 300, 200, 60,
                "Miner I",
                "Mine stone to advance the mining branch.",
                "A node in the mining progression. Has no effect.",
                0, Items.COBBLESTONE, "start",
                "custom", "Unlock axe branch",
                SkillNodeCategory.SIMPLE,
                null,
                BRANCH_MINING
        ));

        list.add(new SkillNode(
                "cobblestone", 150, 420, 60,
                "Cobblestone",
                "Mine 18 stone to unlock cobblestone gathering.",
                "Allows breaking cobblestone with any tool. Unlocks further stone crafting.",
                0, Items.COBBLESTONE, "stone_1",
                 "custom", "Mine 18 stone",
                SkillNodeCategory.SIMPLE,
                null,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "furnace", 450, 420, 60,
                "Furnace",
                "Sacrifice 9 cobblestone to unlock furnace crafting.",
                "Unlocks crafting a furnace. Opens the smelting branch.",
                0, Items.FURNACE, "stone_1",
                null, null,
                SkillNodeCategory.SIMPLE,
                null,
                List.of(new SacrificeRequirement(Items.COBBLESTONE, 9)), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "stone_tools", 0, 640, 60,
                "Stone Tools",
                "Sacrifice 4 planks and 9 stone to unlock stone tool crafting.",
                "Unlocks crafting all stone tools. Starts a 5-minute lock on the durability branch.",
                0, Items.STONE_PICKAXE, "cobblestone",
                null, null,
                SkillNodeCategory.SIMPLE,
                null,
                List.of(
                        new SacrificeRequirement(Items.OAK_PLANKS, 4),
                        new SacrificeRequirement(Items.COBBLESTONE, 9)
                ), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "durability", 150, 640, 60,
                "Improved Durability",
                "Sacrifice 8 planks and 4 string to improve tool durability.",
                "Tool durability becomes half of the original (2x less than base). Starts a 5-minute lock on the stone tools branch.",
                0, Items.STRING, "cobblestone",
                null, null,
                SkillNodeCategory.SIMPLE,
                null,
                List.of(
                        new SacrificeRequirement(Items.OAK_PLANKS, 8),
                        new SacrificeRequirement(Items.STRING, 4)
                ), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "burning_knowledge", 350, 640, 60,
                "Burning Knowledge",
                "Use a furnace 5 times and take an item from its output each time.",
                "Smelting fuel depletes 4x slower; planks become valid furnace fuel alongside sticks.",
                0, Items.COAL, "furnace",
                "custom", "Take items from furnace output 5 times",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "coal_knowledge", 550, 640, 60,
                "Coal Knowledge",
                "Enter a furnace and cook any food.",
                "Allows mining coal, but it cannot be used in furnaces or crafting yet.",
                0, Items.COAL, "furnace",
                "custom", "Enter a furnace and cook any food",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "enhanced_smelting", 650, 420, 60,
                "Enhanced Smelting",
                "Sacrifice 1 sapling, 1 seed, 1 plank and 1 stick to improve smelting.",
                "After upgrade, furnace fuel accepts sticks (if First Steps is unlocked), planks, and all wooden/plant items with operation count <= 1 (tools, signs, doors, slabs, bowls, saplings, dead bush, wool, carpets, bamboo, scaffolding).",
                0, Items.BLAST_FURNACE, "furnace",
                null, null,
                SkillNodeCategory.SIMPLE,
                null,
                List.of(
                        new SacrificeRequirement(Items.OAK_SAPLING, 1),
                        new SacrificeRequirement(Items.WHEAT_SEEDS, 1),
                        new SacrificeRequirement(Items.OAK_PLANKS, 1),
                        new SacrificeRequirement(Items.STICK, 1)
                ), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "bread_sac", 700, -400, 60,
                "Bread Sacrifice",
                "Sacrifice 3 bread to advance the technology branch.",
                "Earns 'A Seedy Place' to open the tech branch, then sacrifice 3 bread. Has no effect.",
                1, Items.BREAD, "sticky",
                "custom", "Earn 'A Seedy Place'",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(new SacrificeRequirement(Items.BREAD, 3)), false,
                BRANCH_TUTORIAL
        ));

        list.add(new SkillNode(
                "filigree", 550, 840, 60,
                "Filigree Processing",
                "Sacrifice 7 coal to learn charcoal craft.",
                "After upgrade, any log can be smelted into charcoal in a furnace. Charcoal burns faster (1.5x) than coal and can be used as furnace fuel; before the upgrade charcoal cannot be smelted or used.",
                0, Items.CHARCOAL, "coal_knowledge",
                null, null,
                SkillNodeCategory.SIMPLE,
                null,
                List.of(new SacrificeRequirement(Items.COAL, 7)), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "smoking_rack", 750, 840, 60,
                "Smokehouse",
                "Unlock Stone Tools, then sacrifice 5 coal, 5 charcoal and 9 stone.",
                "After upgrade, the vanilla Smoker gets special logic: it only cooks meat if you have ever held meat in your inventory; otherwise the progress bar will not advance. Per-player - if one player has never held meat, others are unaffected.",
                0, Items.SMOKER, "coal_knowledge",
                "custom", "Unlock Stone Tools",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(
                        new SacrificeRequirement(Items.COAL, 5),
                        new SacrificeRequirement(Items.CHARCOAL, 5),
                        new SacrificeRequirement(Items.COBBLESTONE, 9)
                ), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "decorative", -150, 420, 60,
                "Decorative Diversity",
                "Sacrifice 1 stone pickaxe and 10 wood (any).",
                "After upgrade, granite, andesite and diorite can be mined with any tool.",
                0, Items.GRANITE, "cobblestone",
                null, null,
                SkillNodeCategory.SIMPLE,
                null,
                List.of(
                        new SacrificeRequirement(Items.STONE_PICKAXE, 1),
                        new SacrificeRequirement(Items.OAK_LOG, 10, true)
                ), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "cave_explorer", -150, 640, 60,
                "Cave Explorer",
                "Take damage from the unknown and sacrifice 16 cobblestone.",
                "After upgrade, below y=60 in caves you may descend to y=15 (and y=15 normally; normally the limit is y=30 unless in a cave, where it starts at y=60). If not upgraded, a darkness effect appears below the limit, infinitely extended from 0s to 1s, hitting the player 2 hearts until they leave the forbidden zone.",
                0, Items.SPYGLASS, "cobblestone",
                "custom", "Take damage from the unknown",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(new SacrificeRequirement(Items.COBBLESTONE, 16)), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "cave_master", -450, 640, 60,
                "Skilled Cave Explorer",
                "Sacrifice 10 torches and 10 copper.",
                "After upgrade you may descend to y=-1; below that the same effect applies again.",
                0, Items.LANTERN, "cave_explorer",
                "custom", "Complete Decorative Diversity",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(
                        new SacrificeRequirement(Items.TORCH, 10),
                        new SacrificeRequirement(Items.COPPER_ORE, 10)
                ), false,
                BRANCH_STONE
        ));

        list.add(new SkillNode(
                "light_up", 220, -520, 60,
                "Time to Light Up",
                "Take damage from Underground Starvation, then sacrifice 1 coal, 1 stick, 1 string.",
                "After upgrade, a torch can be crafted at the crafting tree (coal receives its first recipe).",
                0, Items.TORCH, null,
                "custom", "Take damage from Underground Starvation",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(
                        new SacrificeRequirement(Items.COAL, 1),
                        new SacrificeRequirement(Items.STICK, 1),
                        new SacrificeRequirement(Items.STRING, 1)
                ), false,
                BRANCH_TUTORIAL,
                true, 0
        ));

        list.add(new SkillNode(
                "guardian", 260, -200, 60,
                "Guardian",
                "Unlock Miner I and survive 1 day, then sacrifice 8 planks (any).",
                "After upgrade, a chest can be crafted at the crafting table, and chests can be opened (previously no chest could be opened, even a trapped chest).",
                0, Items.CHEST, null,
                "custom", "Unlock Miner I and survive 1 day",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(new SacrificeRequirement(Items.OAK_PLANKS, 8, false, true)), false,
                BRANCH_TUTORIAL,
                true, 0
        ));

        return list;
    }

    public static List<SkillNode> copper() {
        List<SkillNode> list = new ArrayList<>();

        list.add(new SkillNode(
                "copper_start", -1600, 0, 80,
                "Dawn of the Copper Age",
                "The start of the Copper Vengeance branch.",
                "Sacrifice 1 stone pickaxe, then wait 30s for the node to repair, then click (not hold) with a 90% chance to open it; if lucky, hold to unlock. Appears after the whole Stone Knowledge branch is unlocked.",
                0, Items.COPPER_ORE, null,
                null, null,
                SkillNodeCategory.COMPLEX,
                null,
                List.of(new SacrificeRequirement(Items.STONE_PICKAXE, 1)), false,
                BRANCH_COPPER,
                true, 30
        ));

        list.add(new SkillNode(
                "copper_smelting", -1700, -250, 60,
                "Copper Smelting",
                "Sacrifice 11 copper ore.",
                "After upgrade, copper ore can be smelted into copper ingots (before the upgrade the progress bar will not advance).",
                0, Items.COPPER_INGOT, "copper_start",
                null, null,
                SkillNodeCategory.COMPLEX,
                null,
                List.of(new SacrificeRequirement(Items.COPPER_ORE, 11)), false,
                BRANCH_COPPER
        ));

        list.add(new SkillNode(
                "bad_caster", -1600, -450, 60,
                "Poor Caster",
                "Sacrifice 11 copper ingots.",
                "After upgrade, all poor-quality copper tools can be crafted at the crafting table (new mechanic for copper, iron, gold, diamond, netherite tools and armour: default 'Poor' quality).",
                0, Items.COPPER_INGOT, "copper_smelting",
                null, null,
                SkillNodeCategory.COMPLEX,
                null,
                List.of(new SacrificeRequirement(Items.COPPER_INGOT, 11)), false,
                BRANCH_COPPER
        ));

        list.add(new SkillNode(
                "good_caster", -1600, -650, 60,
                "Good Caster",
                "Craft your first quality item, then sacrifice one of each copper tool.",
                "After upgrade, besides poor quality you can craft 'So-so' and 'Normal' quality.",
                0, Items.COPPER_PICKAXE, "bad_caster",
                "custom", "Craft an item of any quality",
                SkillNodeCategory.COMPLEX,
                null,
                List.of(
                        new SacrificeRequirement(Items.COPPER_PICKAXE, 1),
                        new SacrificeRequirement(Items.COPPER_AXE, 1),
                        new SacrificeRequirement(Items.COPPER_SHOVEL, 1),
                        new SacrificeRequirement(Items.COPPER_HOE, 1),
                        new SacrificeRequirement(Items.COPPER_SWORD, 1)
                ), false,
                BRANCH_COPPER
        ));

        return list;
    }

    public static List<SkillNode> medium() {
        List<SkillNode> list = new ArrayList<>();

        list.add(new SkillNode(
                "tech_2", 1000, -150, 60,
                "Efficiency II",
                "A deeper technology branch node. Has no effect.",
                "Second technology progression node. Has no effect.",
                 0, Items.REPEATER, "tech_1",
                null, null,
                SkillNodeCategory.MEDIUM,
                null,
                List.of(), true,
                BRANCH_TECH
        ));

        return list;
    }

    public static List<SkillNode> complex() {
        List<SkillNode> list = new ArrayList<>();

        list.add(new SkillNode(
                "tech_3", 1000, 150, 60,
                "Overclock",
                "A deeper technology branch node. Has no effect.",
                "Third technology progression node. Has no effect.",
                 0, Items.REDSTONE_BLOCK, "tech_1",
                null, null,
                SkillNodeCategory.COMPLEX,
                null,
                List.of(), true,
                BRANCH_TECH
        ));

        return list;
    }

    public static List<SkillNode> getLeafNodes() {
        SkillNode[] all = getAll();
        List<SkillNode> leaves = new ArrayList<>();
        for (SkillNode n : all) {
            boolean hasChild = false;
            for (SkillNode o : all) {
                if (n.id.equals(o.parentId)) { hasChild = true; break; }
            }
            if (!hasChild) leaves.add(n);
        }
        return leaves;
    }

    public static List<SkillNode> getLeavesOfBranch(String branchName) {
        SkillNode[] all = getAll();
        List<SkillNode> leaves = new ArrayList<>();
        for (SkillNode n : all) {
            if (!branchName.equals(n.branch)) continue;
            boolean hasChild = false;
            for (SkillNode o : all) {
                if (n.id.equals(o.parentId)) { hasChild = true; break; }
            }
            if (!hasChild) leaves.add(n);
        }
        return leaves;
    }

    public static boolean areLeavesUnlocked(List<String> unlocked, String rootId) {
        for (SkillNode n : getLeavesExceptBranch(rootId)) {
            if (!unlocked.contains(n.id)) return false;
        }
        return true;
    }

    public static boolean areBranchLeavesUnlocked(List<String> unlocked, String branchName) {
        for (SkillNode n : getLeavesOfBranch(branchName)) {
            if (!unlocked.contains(n.id)) return false;
        }
        return true;
    }

    public static List<SkillNode> getLeavesExceptBranch(String rootId) {
        SkillNode[] all = getAll();
        java.util.Set<String> inBranch = new java.util.HashSet<>();
        java.util.List<String> queue = new ArrayList<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            String cur = queue.remove(0);
            inBranch.add(cur);
            for (SkillNode n : all) {
                if (cur.equals(n.parentId)) queue.add(n.id);
            }
        }
        List<SkillNode> leaves = new ArrayList<>();
        for (SkillNode n : all) {
            boolean hasChild = false;
            for (SkillNode o : all) {
                if (n.id.equals(o.parentId)) { hasChild = true; break; }
            }
            if (!hasChild && !inBranch.contains(n.id)) leaves.add(n);
        }
        return leaves;
    }
}
