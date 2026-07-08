package io.github.mermagudyan.idlecraft.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class PrestigeConfirmScreen extends Screen {

    private final Screen parent;

    public PrestigeConfirmScreen(Screen parent) {
        super(Component.literal("Prestige"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(
                Component.literal("Yes, prestige!").withStyle(ChatFormatting.RED),
                button -> {
                    assert this.minecraft != null;
                    this.minecraft.setScreenAndShow(null);
                }
        ).bounds(this.width / 2 - 155, this.height / 2 + 10, 150, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                button -> this.minecraft.setScreenAndShow(parent)
        ).bounds(this.width / 2 + 5, this.height / 2 + 10, 150, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        Component line1 = Component.literal("Do you want to prestige?").withStyle(ChatFormatting.GOLD);
        guiGraphics.text(this.font, line1, this.width / 2 - this.font.width(line1) / 2, this.height / 2 - 30, 0xFFFFFF);

        Component line2 = Component.literal("All progress will be reset.").withStyle(ChatFormatting.GRAY);
        guiGraphics.text(this.font, line2, this.width / 2 - this.font.width(line2) / 2, this.height / 2 - 10, 0xFFFFFF);
    }
}