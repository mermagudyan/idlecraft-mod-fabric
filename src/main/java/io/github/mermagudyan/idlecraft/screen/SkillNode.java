package io.github.mermagudyan.idlecraft.screen;

import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
public class SkillNode {
    public final String id;
    public final float x, y;
    public final int size;
    public final String name;
    public final String description;
    public final String detailedDescription; // показывается при Ctrl
    public final int cost;
    public final Item icon;
    public final String parentId;
    public final String unlockCondition;
    public final String conditionText;
    public final SkillNodeCategory category;
    public final String effectId; // идентификатор эффекта для сервера

    public boolean unlocked = false;
    public final List<SacrificeRequirement> sacrifices;
    public final boolean hiddenUntilParent;

    public SkillNode(String id, float x, float y, int size,
                     String name, String description, String detailedDescription,
                     int cost, Item icon, String parentId,
                     String unlockCondition, String conditionText,
                     SkillNodeCategory category, String effectId) {
        this(id, x, y, size, name, description, detailedDescription, cost, icon, parentId,
                unlockCondition, conditionText, category, effectId, List.of(), false);
    }

    public SkillNode(String id, float x, float y, int size,
                     String name, String description, String detailedDescription,
                     int cost, Item icon, String parentId,
                     String unlockCondition, String conditionText,
                     SkillNodeCategory category, String effectId,
                     List<SacrificeRequirement> sacrifices, boolean hiddenUntilParent) {
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
        this.sacrifices = sacrifices;
        this.hiddenUntilParent = hiddenUntilParent;
    }

    public Text getNameText() {
        return Text.literal(name).formatted(unlocked ? Formatting.GREEN : Formatting.WHITE);
    }

    public Text getDescText() {
        return Text.literal(description).formatted(Formatting.GRAY);
    }

    public Text getCostText() {
        return Text.literal("Cost: " + cost).formatted(Formatting.GOLD);
    }

    public Text getCategoryText() {
        return Text.literal("[" + category.displayName + "]");
    }

    public static SkillNode[] defaults() {
        return SkillNodeRegistry.getAll();
    }
}