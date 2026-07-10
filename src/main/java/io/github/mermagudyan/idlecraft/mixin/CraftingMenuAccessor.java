package io.github.mermagudyan.idlecraft.mixin;

import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractCraftingMenu.class)
public interface CraftingMenuAccessor {
    @Accessor("craftSlots") CraftingContainer idlecraft$craftSlots();
}
