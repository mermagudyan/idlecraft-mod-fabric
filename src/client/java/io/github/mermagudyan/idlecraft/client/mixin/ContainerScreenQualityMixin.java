package io.github.mermagudyan.idlecraft.client.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.network.ClientState;
import io.github.mermagudyan.idlecraft.network.QualitySelectPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenQualityMixin {

    @Shadow protected Slot hoveredSlot;

    @Shadow public abstract AbstractContainerMenu getMenu();

    @Unique
    private boolean idlecraft$isQualitySlot() {
        AbstractContainerMenu m = getMenu();
        if (m instanceof CraftingMenu) {
            return hoveredSlot != null && hoveredSlot == m.getSlot(0);
        }
        if (m instanceof AnvilMenu) {
            // Allow scrolling both over the input (slot 0) and the result (slot 2).
            return hoveredSlot != null
                    && (hoveredSlot == m.getSlot(0) || hoveredSlot == m.getSlot(2));
        }
        return false;
    }

    @Unique
    private int idlecraft$qualityCap() {
        return ClientState.getUnlockedNodes().contains("good_caster")
                ? QualityComponent.NORMAL : QualityComponent.POOR;
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void idlecraft$scrollQuality(double mx, double my, double sx, double sy,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!idlecraft$isQualitySlot()) return;
        int cur = ClientState.getSelectedQuality();
        int next = Math.max(QualityComponent.POOR,
                Math.min(idlecraft$qualityCap(), cur + (sy > 0 ? 5 : -5)));
        if (next != cur) {
            ClientState.setSelectedQuality(next);
            ClientPlayNetworking.send(new QualitySelectPayload(next));
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
    private void idlecraft$shiftQuality(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() != GLFW.GLFW_KEY_LEFT_SHIFT && event.key() != GLFW.GLFW_KEY_RIGHT_SHIFT) return;
        if (!idlecraft$isQualitySlot()) return;
        int cur = ClientState.getSelectedQuality();
        int next = cur < QualityComponent.SO_SO ? Math.min(QualityComponent.SO_SO, idlecraft$qualityCap()) : 0;
        ClientState.setSelectedQuality(next);
        ClientPlayNetworking.send(new QualitySelectPayload(next));
        cir.setReturnValue(true);
    }
}
