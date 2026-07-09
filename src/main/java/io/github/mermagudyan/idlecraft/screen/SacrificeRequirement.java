package io.github.mermagudyan.idlecraft.screen;

import net.minecraft.world.item.Item;

public record SacrificeRequirement(Item item, int amount, boolean anyWood) {
    public SacrificeRequirement(Item item, int amount) {
        this(item, amount, false);
    }
}
