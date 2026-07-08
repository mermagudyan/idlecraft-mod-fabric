package io.github.mermagudyan.idlecraft.client.mixin;

import io.github.mermagudyan.idlecraft.client.debug.DebugState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {

    protected AdvancementsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addResetButton(CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        if (!DebugState.isAvailable(this.minecraft.player)) return;

        this.addRenderableWidget(Button.builder(
                Component.literal("Reset Advancements"),
                b -> {
                    if (this.minecraft.player == null) return;
                    var handler = this.minecraft.player.connection;
                    if (handler == null) return;

                    handler.sendCommand("advancement revoke @s everything");
                    handler.sendCommand("advancement grant @s only minecraft:story/root");
                    io.github.mermagudyan.idlecraft.network.ClientState.setUnlockedNodes(
                            io.github.mermagudyan.idlecraft.network.ClientState.getUnlockedNodes()
                    );
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new io.github.mermagudyan.idlecraft.network.ResetRewardedPayload()
                    );

                    this.minecraft.player.sendSystemMessage(
                            Component.literal("[Idlecraft] Advancements reset. Points can be re-earned.")
                    );
                }
        ).bounds(this.width / 2 - 75, this.height - 56, 150, 20).build());
    }
}