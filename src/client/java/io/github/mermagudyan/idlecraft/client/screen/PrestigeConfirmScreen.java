package io.github.mermagudyan.idlecraft.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PrestigeConfirmScreen extends Screen {

    private final Screen parent;

    public PrestigeConfirmScreen(Screen parent) {
        super(Text.literal("Prestige"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Да
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Yes, prestige!").formatted(Formatting.RED),
                button -> {
                    // TODO: отправить network packet на сервер для сброса
                    assert this.client != null;
                    this.client.setScreen(null);
                }
        ).dimensions(this.width / 2 - 155, this.height / 2 + 10, 150, 20).build());

        // Нет
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cansel"),
                button -> this.client.setScreen(parent)
        ).dimensions(this.width / 2 + 5, this.height / 2 + 10, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Do you want to prestige?").formatted(Formatting.GOLD),
                this.width / 2, this.height / 2 - 30,
                0xFFFFFF
        );
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("All progress will be reset.").formatted(Formatting.GRAY),
                this.width / 2, this.height / 2 - 10,
                0xFFFFFF
        );
    }
}