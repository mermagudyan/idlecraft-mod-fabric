package io.github.mermagudyan.idlecraft.client.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class CraftingQualityHintMixin {

    @Shadow protected Slot hoveredSlot;
    @Shadow public abstract AbstractContainerMenu getMenu();

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"))
    private void idlecraft$renderXpHint(GuiGraphicsExtractor gui, int mx, int my, float partialTick, CallbackInfo ci) {
        AbstractContainerMenu menu = getMenu();
        if (!(menu instanceof CraftingMenu)) return;
        if (!(hoveredSlot instanceof ResultSlot)) return;

        int selected = ClientState.getSelectedQuality();
        int cap = ClientState.getUnlockedNodes().contains("good_caster")
                ? QualityComponent.NORMAL : QualityComponent.POOR;
        int level = Math.max(QualityComponent.POOR, Math.min(selected, cap));
        if (level <= QualityComponent.POOR) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative()) return;
        if (!mc.options.keyShift.isDown()) return;

        Font font = mc.font;
        String text = "Experience: " + level;
        int width = font.width(text) + 10;
        int bx = mx + 12;
        int by = my + 12;

        gui.fill(bx, by, bx + width, by + 16, 0xE0101010);
        gui.fill(bx, by, bx + width, by + 1, 0xFF55FF55);
        gui.text(font, text, bx + 5, by + 4, 0xFFFFFF55);
    }
}
