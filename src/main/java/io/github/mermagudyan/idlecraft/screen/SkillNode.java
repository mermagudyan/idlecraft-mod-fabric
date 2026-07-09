package io.github.mermagudyan.idlecraft.screen;

import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;

public class SkillNode {
    public final String id;
    public final float x, y;
    public final int size;
    public final String name;
    public final String description;
    public final String detailedDescription;
    public final int cost;
    public final Item icon;
    public final String parentId;
    public final String unlockCondition;
    public final String conditionText;
    public final SkillNodeCategory category;
    public final String effectId;
    public final String branch;

    public boolean unlocked = false;
    public final List<SacrificeRequirement> sacrifices;
    public final boolean hiddenUntilParent;

    public SkillNode(String id, float x, float y, int size,
                     String name, String description, String detailedDescription,
                     int cost, Item icon, String parentId,
                     String unlockCondition, String conditionText,
                     SkillNodeCategory category, String effectId) {
        this(id, x, y, size, name, description, detailedDescription, cost, icon, parentId,
                unlockCondition, conditionText, category, effectId, List.of(), false, "");
    }

    public SkillNode(String id, float x, float y, int size,
                     String name, String description, String detailedDescription,
                     int cost, Item icon, String parentId,
                     String unlockCondition, String conditionText,
                     SkillNodeCategory category, String effectId, String branch) {
        this(id, x, y, size, name, description, detailedDescription, cost, icon, parentId,
                unlockCondition, conditionText, category, effectId, List.of(), false, branch);
    }

    public SkillNode(String id, float x, float y, int size,
                     String name, String description, String detailedDescription,
                     int cost, Item icon, String parentId,
                     String unlockCondition, String conditionText,
                     SkillNodeCategory category, String effectId,
                     List<SacrificeRequirement> sacrifices, boolean hiddenUntilParent, String branch) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.size = size;
        this.name = name;
        this.description = description;
        this.detailedDescription = detailedDescription;
        this.cost = cost;
        this.icon = icon;
        this.parentId = parentId;
        this.unlockCondition = unlockCondition;
        this.conditionText = conditionText;
        this.category = category;
        this.effectId = effectId;
        this.branch = branch;
        this.sacrifices = sacrifices;
        this.hiddenUntilParent = hiddenUntilParent;
    }

    public Component getNameText() {
        return Component.literal(name).withStyle(unlocked ? ChatFormatting.GREEN : ChatFormatting.WHITE);
    }

    public Component getDescText() {
        return Component.literal(description).withStyle(ChatFormatting.GRAY);
    }

    public Component getCostText() {
        return Component.literal("Cost: " + cost).withStyle(ChatFormatting.GOLD);
    }

    public Component getCategoryText() {
        return Component.literal("[" + category.displayName + "]");
    }

    public static SkillNode[] defaults() {
        return SkillNodeRegistry.getAll();
    }
}