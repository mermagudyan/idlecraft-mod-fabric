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

    public boolean unlocked = false;

    public SkillNode(String id, float x, float y, int size,
                     String name, String description, int cost, Item icon, String parentId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.size = size;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.icon = icon;
        this.parentId = parentId;
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
                new SkillNode("start", 0, 0, 80,
                        "Start",
                        "The beginning of your idle journey.",
                        0, Items.GRASS_BLOCK, null),
        };
    }
}