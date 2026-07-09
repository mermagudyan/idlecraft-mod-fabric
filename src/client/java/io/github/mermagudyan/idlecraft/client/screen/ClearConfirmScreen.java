package io.github.mermagudyan.idlecraft.client.screen;

import io.github.mermagudyan.idlecraft.network.ClearNodesPayload;
import io.github.mermagudyan.idlecraft.screen.SkillNode;
import io.github.mermagudyan.idlecraft.screen.SkillNodeRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class ClearConfirmScreen extends Screen {

    private final Screen parent;
    private final String targetNodeId;
    private final String parentNodeName;
    private final int removedCount;
    private final boolean bigReset;

    public ClearConfirmScreen(Screen parent, String targetNodeId, String parentNodeName, int removedCount) {
        super(Component.literal("Idlecraft Clear"));
        this.parent = parent;
        this.targetNodeId = targetNodeId;
        this.parentNodeName = parentNodeName;
        this.removedCount = removedCount;
        this.bigReset = removedCount > 1;
    }

    @Override
    protected void init() {
        int y = this.height / 2 + 10;
        this.addRenderableWidget(Button.builder(
                Component.literal("Yes, sure.").withStyle(bigReset ? ChatFormatting.RED : ChatFormatting.WHITE),
                button -> {
                    ClientPlayNetworking.send(new ClearNodesPayload(targetNodeId));
                    if (this.minecraft != null) this.minecraft.setScreenAndShow(parent);
                }
        ).bounds(this.width / 2 - 155, y, 150, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                button -> {
                    if (this.minecraft != null) this.minecraft.setScreenAndShow(parent);
                }
        ).bounds(this.width / 2 + 5, y, 150, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        Component line1 = Component.literal("Are you sure you want to roll back to " + parentNodeName + "?")
                .withStyle(bigReset ? ChatFormatting.RED : ChatFormatting.GOLD);
        guiGraphics.text(this.font, line1, this.width / 2 - this.font.width(line1) / 2, this.height / 2 - 30, 0xFFFFFF);

        if (bigReset) {
            Component line2 = Component.literal("This will clear " + removedCount + " nodes.").withStyle(ChatFormatting.GRAY);
            guiGraphics.text(this.font, line2, this.width / 2 - this.font.width(line2) / 2, this.height / 2 - 10, 0xFFFFFF);
        }
    }
}
