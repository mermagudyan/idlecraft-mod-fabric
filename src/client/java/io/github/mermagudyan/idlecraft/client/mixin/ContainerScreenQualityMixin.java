package io.github.mermagudyan.idlecraft.client.mixin;

import io.github.mermagudyan.idlecraft.common.QualityComponent;
import io.github.mermagudyan.idlecraft.network.ClientState;
import io.github.mermagudyan.idlecraft.network.QualitySelectPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
            return true;
        }
        return false;
    }

    @Unique
    private int idlecraft$qualityCap() {
        boolean goodCaster = ClientState.getUnlockedNodes().contains("good_caster");
        if (getMenu() instanceof AnvilMenu) {
            return goodCaster ? QualityComponent.SUPERIOR : QualityComponent.NORMAL;
        }
        return goodCaster ? QualityComponent.NORMAL : QualityComponent.POOR;
    }

    @Unique
    private boolean idlecraft$shiftHeld() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void idlecraft$scrollQuality(double mx, double my, double sx, double sy,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!idlecraft$isQualitySlot()) return;
        if (!idlecraft$shiftHeld()) return;
        int cur = (getMenu() instanceof CraftingMenu)
                ? ClientState.getSelectedCraftQuality()
                : ClientState.getSelectedQuality();
        int next = Math.max(QualityComponent.POOR,
                Math.min(idlecraft$qualityCap(), cur + (sy > 0 ? 5 : -5)));
        if (next != cur) {
            if (getMenu() instanceof CraftingMenu) {
                ClientState.setSelectedCraftQuality(next);
            } else {
                ClientState.setSelectedQuality(next);
            }
            ClientPlayNetworking.send(new QualitySelectPayload(next));
        }
        cir.setReturnValue(true);
    }
}
