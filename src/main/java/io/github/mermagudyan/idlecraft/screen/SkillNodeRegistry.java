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
                null, null,
                SkillNodeCategory.SIMPLE,
                null,
                List.of(), true,
                BRANCH_TUTORIAL
        ));

        list.add(new SkillNode(
                "stone_1", 300, 200, 60,
                "Miner I",
                "Mine stone to advance the mining branch.",
                "A node in the mining progression. Has no effect.",
                5, Items.COBBLESTONE, "start",
                "custom", "Unlock axe branch",
                SkillNodeCategory.SIMPLE,
                null,
                BRANCH_MINING
        ));

        list.add(new SkillNode(
                "bread_sac", 700, -400, 60,
                "Bread Sacrifice",
                "Sacrifice 3 bread to advance the technology branch.",
                "Earns 'A Seedy Place' to open the tech branch, then sacrifice 3 bread. Has no effect.",
                0, Items.BREAD, "sticky",
                "custom", "Earn 'A Seedy Place'",
                SkillNodeCategory.SIMPLE,
                null,
                List.of(new SacrificeRequirement(Items.BREAD, 3)), false,
                BRANCH_TUTORIAL
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
                 40, Items.REPEATER, "tech_1",
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
                 200, Items.REDSTONE_BLOCK, "tech_1",
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
