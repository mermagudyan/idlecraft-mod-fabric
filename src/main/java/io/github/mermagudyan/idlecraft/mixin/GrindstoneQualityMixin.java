package io.github.mermagudyan.idlecraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.mermagudyan.idlecraft.IdleMod;
import io.github.mermagudyan.idlecraft.common.GrindstoneCleanser;
import io.github.mermagudyan.idlecraft.common.QualityComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneQualityMixin implements GrindstoneCleanser {

    @Shadow
    @Final
    private Container resultSlots;

    @Shadow
    @Final
    private Container repairSlots;

    @Unique
    private Player idlecraft$player;

    @Unique
    private boolean idlecraft$cleanse;

    @Unique
    private int idlecraft$cleanseCost;

    @Unique
    private Item idlecraft$material;

    @Unique
    private int idlecraft$consume;

    @Unique
    private int idlecraft$bottomCount;

    @Unique
    private int idlecraft$materialSlot;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void idlecraft$capturePlayer(CallbackInfo ci, @Local(argsOnly = true) Inventory inventory) {
        idlecraft$player = inventory.player;
        NonNullList<Slot> slots = ((AbstractContainerMenuAccessor) (Object) this).idlecraft$slots();
        
        
        
        if (slots.size() >= 3) {
            slots.set(0, new io.github.mermagudyan.idlecraft.common.GrindstoneInputSlot(repairSlots, 0, 49, 19, 0));
            slots.set(1, new io.github.mermagudyan.idlecraft.common.GrindstoneInputSlot(repairSlots, 1, 49, 40, 1));
            IdleMod.LOGGER.info("[IDLECRAFT][Grindstone] replaced repair slots 0 and 1 (size={})", slots.size());
        } else {
            IdleMod.LOGGER.warn("[IDLECRAFT][Grindstone] slot list too small to replace: {}", slots.size());
        }
    }

    @Unique
    private Player idlecraft$resolvePlayer() {
        if (idlecraft$player != null) return idlecraft$player;
        for (Slot slot : ((AbstractContainerMenuAccessor) (Object) this).idlecraft$slots()) {
            Container c = ((SlotAccessor) (Object) slot).idlecraft$container();
            if (c instanceof Inventory inv) {
                idlecraft$player = inv.player;
                break;
            }
        }
        return idlecraft$player;
    }

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void idlecraft$logSlots(Container container, CallbackInfo ci) {
        if (container != repairSlots) return;
        ItemStack top = repairSlots.getItem(0);
        ItemStack bottom = repairSlots.getItem(1);
        IdleMod.LOGGER.info("[IDLECRAFT][Grindstone] slotsChanged top={} q={} bottom={} q={}",
                top.getItem(), QualityComponent.getQuality(top), bottom.getItem(), QualityComponent.getQuality(bottom));
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void idlecraft$resolve(CallbackInfo ci) {
        idlecraft$cleanse = false;
        idlecraft$cleanseCost = 0;
        Player player = idlecraft$resolvePlayer();
        if (player == null) {
            IdleMod.LOGGER.warn("[IDLECRAFT][Grindstone] createResult: player not captured");
            return;
        }

        ItemStack top = repairSlots.getItem(0);
        ItemStack bottom = repairSlots.getItem(1);
        if (top.isEmpty() || bottom.isEmpty()) {
            return;
        }

        
        
        
        ItemStack tool = top;
        ItemStack matStack = bottom;
        if (QualityComponent.isEligible(tool) && QualityComponent.getQuality(tool) == QualityComponent.CORRUPTED) {
            Item mat = QualityComponent.forgeMaterial(tool);
            int consume = QualityComponent.craftMaterialCount(tool.getItem());
            int xp = player.experienceLevel;
            boolean matMatches = mat != null && matStack.getItem() == mat;
            boolean enoughXp = xp >= 5;
            boolean enoughCount = matStack.getCount() >= consume;
            Integer prev = tool.get(QualityComponent.CLEANSE_COUNT);
            int cost = 5 + (prev == null ? 0 : prev);

            IdleMod.LOGGER.info("[IDLECRAFT][Grindstone] cleanse topQ=CORRUPTED mat={} count={} matMatches={} consume={} xp={} enoughXp={} enoughCount={} cost={} prev={}",
                    mat, matStack.getCount(), matMatches, consume, xp, enoughXp, enoughCount, cost, prev);

            if (matMatches && enoughCount && enoughXp) {
                ItemStack out = tool.copy();
                QualityComponent.applyQuality(out, QualityComponent.POOR);
                out.set(QualityComponent.CLEANSE_COUNT, (prev == null ? 0 : prev) + 1);
                resultSlots.setItem(0, out);
                ((AbstractContainerMenu) (Object) this).broadcastChanges();

                idlecraft$cleanse = true;
                idlecraft$cleanseCost = cost;
                idlecraft$material = mat;
                idlecraft$consume = consume;
                idlecraft$materialSlot = 1;
                idlecraft$bottomCount = matStack.getCount();
                IdleMod.LOGGER.info("[IDLECRAFT][Grindstone] cleanse result set, outQ={} cost={}", QualityComponent.getQuality(out), cost);
                return;
            }
        }

        
        
        
        Integer topQ = top.get(QualityComponent.QUALITY);
        Integer botQ = bottom.get(QualityComponent.QUALITY);
        int qt = QualityComponent.getQuality(top);
        int qb = QualityComponent.getQuality(bottom);
        boolean corrupted = qt == QualityComponent.CORRUPTED || qb == QualityComponent.CORRUPTED;
        boolean bothAssigned = topQ != null && botQ != null;
        boolean mismatch = bothAssigned && qt != qb;

        IdleMod.LOGGER.info("[IDLECRAFT][Grindstone] combine topQ={} botQ={} corrupted={} bothAssigned={} mismatch={}",
                qt, qb, corrupted, bothAssigned, mismatch);

        if (corrupted || mismatch) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            ((AbstractContainerMenu) (Object) this).broadcastChanges();
            IdleMod.LOGGER.info("[IDLECRAFT][Grindstone] blocked combine: topQ={} bottomQ={}", qt, qb);
        }
    }

    @Override
    public boolean idlecraft$isCleansing() {
        return idlecraft$cleanse;
    }

    @Override
    public int idlecraft$cleanseCost() {
        return idlecraft$cleanseCost;
    }

    @Override
    public void idlecraft$grindstoneCleanseTaken(Player player) {
        if (!idlecraft$cleanse) {
            return;
        }
        idlecraft$cleanse = false;
        if (!player.level().isClientSide()) {
            player.giveExperienceLevels(-idlecraft$cleanseCost);
        }

        int leftover = idlecraft$bottomCount - idlecraft$consume;
        if (leftover > 0 && idlecraft$material != null) {
            repairSlots.setItem(idlecraft$materialSlot, new ItemStack(idlecraft$material, leftover));
        } else {
            repairSlots.setItem(idlecraft$materialSlot, ItemStack.EMPTY);
        }
        resultSlots.setItem(0, ItemStack.EMPTY);
        IdleMod.LOGGER.info("[IDLECRAFT][Grindstone] cleanse taken, cost={} bottomCount={} consume={} leftover={}",
                idlecraft$cleanseCost, idlecraft$bottomCount, idlecraft$consume, leftover);
    }
}
