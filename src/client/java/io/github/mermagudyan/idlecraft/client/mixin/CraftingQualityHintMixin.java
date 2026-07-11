package io.github.mermagudyan.idlecraft.client.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class CraftingQualityHintMixin {

    @Shadow protected Slot hoveredSlot;
    @Shadow public abstract AbstractContainerMenu getMenu();

    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void idlecraft$drawXpCost(GuiGraphicsExtractor guiGraphics, int xm, int ym, CallbackInfo ci) {
        AbstractContainerMenu menu = getMenu();
        int cost = idlecraft$xpCost(menu);
        if (cost <= 0) return;

        Slot resultSlot = idlecraft$resultSlot(menu);
        if (resultSlot == null) return;

        int x = resultSlot.x + 1;
        int y = resultSlot.y + 22;
        Component text = Component.literal("Experience: " + cost).withStyle(ChatFormatting.YELLOW);
        guiGraphics.text(Minecraft.getInstance().font, text, x, y, 0xFFFFFF);
    }

    @Unique
    private Slot idlecraft$resultSlot(AbstractContainerMenu menu) {
        if (menu instanceof CraftingMenu cm) {
            return cm.getResultSlot();
        }
        if (menu instanceof GrindstoneMenu gm) {
            return gm.getSlot(2);
        }
        return null;
    }

    @Unique
    private int idlecraft$xpCost(AbstractContainerMenu menu) {
        boolean goodCaster = ClientState.getUnlockedNodes().contains("good_caster");
        if (menu instanceof CraftingMenu cm) {
            int sel = ClientState.getSelectedCraftQuality();
            int cap = goodCaster ? QualityComponent.NORMAL : QualityComponent.POOR;
            int level = Math.max(QualityComponent.POOR, Math.min(sel, cap));
            return level > QualityComponent.POOR ? level : 0;
        }
        if (menu instanceof GrindstoneMenu gm) {
            ItemStack top = gm.getSlot(0).getItem();
            ItemStack bottom = gm.getSlot(1).getItem();
            if (top.isEmpty() || bottom.isEmpty() || !QualityComponent.isEligible(top)) return 0;
            if (QualityComponent.getQuality(top) != QualityComponent.CORRUPTED) return 0;
            Item mat = QualityComponent.forgeMaterial(top);
            if (mat == null || bottom.getItem() != mat) return 0;
            if (bottom.getCount() < QualityComponent.craftMaterialCount(top.getItem())) return 0;
            Integer prev = top.get(QualityComponent.CLEANSE_COUNT);
            return 5 + (prev == null ? 0 : prev);
        }
        return 0;
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("TAIL"), cancellable = true)
    private void idlecraft$appendXpToTooltip(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        AbstractContainerMenu menu = getMenu();
        if (!(menu instanceof CraftingMenu) && !(menu instanceof AnvilMenu) && !(menu instanceof GrindstoneMenu)) return;
        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;

        if (menu instanceof CraftingMenu cm) {
            if (hoveredSlot != cm.getResultSlot()) return;
        } else if (menu instanceof AnvilMenu am) {
            if (hoveredSlot != am.getSlot(0) && hoveredSlot != am.getSlot(2)) return;
        } else if (menu instanceof GrindstoneMenu gm) {
            if (hoveredSlot != gm.getSlot(0) && hoveredSlot != gm.getSlot(1) && hoveredSlot != gm.getSlot(2)) return;
        }

        ItemStack item = hoveredSlot.getItem();
        if (!QualityComponent.isEligible(item)) return;

        boolean goodCaster = ClientState.getUnlockedNodes().contains("good_caster");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative()) return;

        List<Component> original = cir.getReturnValue();
        if (original == null) return;
        List<Component> lines = new ArrayList<>(original);
        cir.setReturnValue(lines);

        if (menu instanceof AnvilMenu am && hoveredSlot == am.getSlot(0)) {
            Item expectedMat = QualityComponent.forgeMaterial(item);
            if (expectedMat != null) {
                ItemStack mat = am.getSlot(1).getItem();
                boolean present = !mat.isEmpty() && mat.getItem() == expectedMat;
                lines.add(Component.literal((present ? "Material: " : "Material (optional): "))
                        .append(Component.translatable(expectedMat.getDescriptionId()))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
