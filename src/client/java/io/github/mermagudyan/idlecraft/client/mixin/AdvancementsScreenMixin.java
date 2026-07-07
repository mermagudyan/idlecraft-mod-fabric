package io.github.mermagudyan.idlecraft.client.mixin;

import io.github.mermagudyan.idlecraft.client.debug.DebugState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {

    protected AdvancementsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addResetButton(CallbackInfo ci) {
        if (this.client == null || this.client.player == null) return;
        if (!DebugState.isAvailable(this.client.player)) return;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Reset Advancements"),
                b -> {
                    if (this.client.player == null) return;
                    var handler = this.client.player.networkHandler;
                    if (handler == null) return;

                    handler.sendChatCommand("advancement revoke @s everything");
                    handler.sendChatCommand("advancement grant @s only minecraft:story/root");
                    io.github.mermagudyan.idlecraft.network.ClientState.setUnlockedNodes(
                            io.github.mermagudyan.idlecraft.network.ClientState.getUnlockedNodes()
                    );
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new io.github.mermagudyan.idlecraft.network.ResetRewardedPayload()
                    );

                    this.client.player.sendMessage(
                            Text.literal("[Idlecraft] Advancements reset. Points can be re-earned."),
                            false
                    );
                }
        ).dimensions(this.width / 2 - 75, this.height - 56, 150, 20).build());
    }
}