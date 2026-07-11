package io.github.mermagudyan.idlecraft.common;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GrindstoneInputSlot extends Slot {
    private final Container repairSlots;
    private final int selfIndex;

    public GrindstoneInputSlot(Container repairSlots, int containerIndex, int x, int y, int selfIndex) {
        super(repairSlots, containerIndex, x, y);
        this.repairSlots = repairSlots;
        this.selfIndex = selfIndex;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        
        
        if (selfIndex == 0) {
            return QualityComponent.isEligible(stack);
        }
        return true;
    }
}
