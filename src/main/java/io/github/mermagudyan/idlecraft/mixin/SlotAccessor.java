package io.github.mermagudyan.idlecraft.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotAccessor {
    @Accessor("x") int idlecraft$x();
    @Accessor("y") int idlecraft$y();
    @Accessor("container") Container idlecraft$container();
    @Accessor("slot") int idlecraft$index();
}
