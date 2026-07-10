package io.github.mermagudyan.idlecraft.common;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * A grindstone input slot that behaves like vanilla (accepts damageable or enchanted items)
 * but additionally allows the repair material of a corrupted tool placed in the other slot,
 * so the tool can be cleansed back to Poor quality.
 */
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
        int other = selfIndex == 0 ? 1 : 0;
        ItemStack tool = repairSlots.getItem(other);
        if (QualityComponent.isEligible(tool) && QualityComponent.getQuality(tool) == QualityComponent.CORRUPTED) {
            var mat = QualityComponent.repairMaterial(tool);
            if (mat != null && stack.getItem() == mat) {
                return true;
            }
        }
        return stack.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(stack);
    }
}
