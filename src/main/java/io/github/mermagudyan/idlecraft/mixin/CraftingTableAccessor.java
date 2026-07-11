package io.github.mermagudyan.idlecraft.mixin;

import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CraftingMenu.class)
public interface CraftingTableAccessor {
    @Accessor("access") ContainerLevelAccess idlecraft$access();
}
