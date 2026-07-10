package io.github.mermagudyan.idlecraft.mixin;

import io.github.mermagudyan.idlecraft.common.GrindstoneCleanser;
import io.github.mermagudyan.idlecraft.common.GrindstoneInputSlot;
import io.github.mermagudyan.idlecraft.common.QualityComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneQualityMixin implements GrindstoneCleanser {

    @Shadow @Final private Container resultSlots;
    @Shadow @Final private Container repairSlots;

    @Unique private Player idlecraft$player;
    @Unique private boolean idlecraft$cleanse;
    @Unique private Item idlecraft$material;
    @Unique private int idlecraft$consume;
    @Unique private int idlecraft$bottomCount;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void idlecraft$allowMaterial(int syncId, Inventory inv, ContainerLevelAccess access, CallbackInfo ci) {
        idlecraft$player = inv.player;
        List<Slot> slots = ((AbstractContainerMenuAccessor) (Object) this).idlecraft$slots();
        if (slots.size() >= 2) {
            Slot top = slots.get(0);
            Slot bottom = slots.get(1);
            SlotAccessor a0 = (SlotAccessor) (Object) top;
            SlotAccessor a1 = (SlotAccessor) (Object) bottom;
            slots.set(0, new GrindstoneInputSlot(repairSlots, 0, a0.idlecraft$x(), a0.idlecraft$y(), 0));
            slots.set(1, new GrindstoneInputSlot(repairSlots, 1, a1.idlecraft$x(), a1.idlecraft$y(), 1));
        }
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void idlecraft$cleanseResult(CallbackInfo ci) {
        idlecraft$cleanse = false;
        if (idlecraft$player == null) return;

        ItemStack top = repairSlots.getItem(0);
        ItemStack bottom = repairSlots.getItem(1);
        if (top.isEmpty() || bottom.isEmpty()) return;
        if (!QualityComponent.isEligible(top)) return;
        if (QualityComponent.getQuality(top) != QualityComponent.CORRUPTED) return;

        Item mat = QualityComponent.repairMaterial(top);
        if (mat == null || bottom.getItem() != mat) return;

        int consume = QualityComponent.craftMaterialCount(top.getItem());
        if (bottom.getCount() < consume) return;
        if (idlecraft$player.experienceLevel < 5) return;

        ItemStack out = top.copy();
        QualityComponent.applyQuality(out, QualityComponent.POOR);
        resultSlots.setItem(0, out);
        ((AbstractContainerMenu) (Object) this).broadcastChanges();

        idlecraft$cleanse = true;
        idlecraft$material = mat;
        idlecraft$consume = consume;
        idlecraft$bottomCount = bottom.getCount();
    }

    @Override
    public void idlecraft$grindstoneCleanseTaken(Player player) {
        if (!idlecraft$cleanse) return;
        idlecraft$cleanse = false;
        if (!player.level().isClientSide()) {
            player.giveExperienceLevels(-5);
        }
        int leftover = idlecraft$bottomCount - idlecraft$consume;
        if (leftover > 0 && idlecraft$material != null) {
            repairSlots.setItem(1, new ItemStack(idlecraft$material, leftover));
        } else {
            repairSlots.setItem(1, ItemStack.EMPTY);
        }
    }
}
