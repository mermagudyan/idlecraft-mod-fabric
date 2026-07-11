package io.github.mermagudyan.idlecraft.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingSlotMixin {

    
    
    
    @Inject(method = "<init>", at = @At("TAIL"))
    private void idlecraft$openSlots(CallbackInfo ci) {
        NonNullList<Slot> slots = ((AbstractContainerMenuAccessor) (Object) this).idlecraft$slots();
        Container input = ((ItemCombinerMenuAccessor) (Object) this).idlecraft$inputSlots();
        for (int i : new int[]{SmithingMenu.BASE_SLOT, SmithingMenu.ADDITIONAL_SLOT}) {
            if (i >= slots.size()) continue;
            Slot old = slots.get(i);
            int containerSlot = old.getContainerSlot();
            int x = old.x;
            int y = old.y;
            slots.set(i, new Slot(input, containerSlot, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true;
                }
            });
        }
    }
}
