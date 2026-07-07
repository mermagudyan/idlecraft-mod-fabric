package io.github.mermagudyan.idlecraft.client.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import io.github.mermagudyan.idlecraft.client.debug.DebugState;
import io.github.mermagudyan.idlecraft.network.ClientState;
import io.github.mermagudyan.idlecraft.network.ClientState;
import io.github.mermagudyan.idlecraft.network.NodePurchasePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class IdlecraftScreen extends Screen {

    private static final float WORLD_HALF = 5000.0f;
    private static final float MIN_ZOOM = 0.2f;
    private static final float MAX_ZOOM = 1.0f;
    private static final int HOLD_TICKS = 20;       // 1 sec to unlock
    private static final int FLASH_TICKS = 20;      // 1 sec flash

    // Camera
    private float cameraX = 0.0f, cameraY = 0.0f, zoom = 1.0f;
    private boolean dragging = false;
    private double lastDragX, lastDragY;
    private double mouseX = 0, mouseY = 0;
    private final boolean[] keys = new boolean[5];

    // Data
    private final Map<String, SkillNode> nodes = new HashMap<>();
    private SkillNode hoverNode = null;
    private SkillNode lastUnlockedNode = null;
    String text = "Points: " + ClientState.getPoints();

    // Hold-to-upgrade
    private SkillNode pressedNode = null;
    private float holdProgress = 0.0f;     // 0..1

    // Flash on unlock
    private SkillNode flashNode = null;
    private float flashTime = 0.0f;        // 1..0

    // Return button
    private float returnAlpha = 0.0f;      // 0..1
    private float returnAngle = 0.0f;      // radians
    private int returnX = 0, returnY = 0;

    public IdlecraftScreen() {
        super(Text.literal("Idlecraft"));
        for (SkillNode n : SkillNode.defaults()) nodes.put(n.id, n);
        // Загружаем разблокированные ноды из ClientState
        for (String id : ClientState.getUnlockedNodes()) {
            SkillNode n = nodes.get(id);
            if (n != null) n.unlocked = true;
            // Находим последнюю разблокированную
            // (просто берём последнюю из списка)
        }
        // Устанавливаем lastUnlockedNode на последнюю разблокированную
        List<String> unlocked = ClientState.getUnlockedNodes();
        if (!unlocked.isEmpty()) {
            lastUnlockedNode = nodes.get(unlocked.get(unlocked.size() - 1));
        }
    }

    // ---------- Coordinates ----------
    private float screenToWorldX(double sx) { return cameraX + (float)((sx - this.width / 2.0) / zoom); }
    private float screenToWorldY(double sy) { return cameraY + (float)((sy - this.height / 2.0) / zoom); }
    private int worldToScreenX(float wx) { return (int)(this.width / 2.0 + (wx - cameraX) * zoom); }
    private int worldToScreenY(float wy) { return (int)(this.height / 2.0 + (wy - cameraY) * zoom); }

    // ---------- Buttons ----------
    @Override
    protected void init() {
        boolean debug = DebugState.isAvailable(this.client != null ? this.client.player : null);

        int yPrestige = debug ? this.height - 50 : this.height - 25;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Prestige").formatted(Formatting.GOLD),
                b -> this.client.setScreen(new PrestigeConfirmScreen(this))
        ).dimensions(this.width / 2 - 155, yPrestige, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                b -> this.close()
        ).dimensions(this.width / 2 + 5, yPrestige, 150, 20).build());

    }

    private void resetAll() {
        for (SkillNode n : nodes.values()) n.unlocked = false;
        lastUnlockedNode = null;
        flashNode = null;
        flashTime = 0;
        ClientState.setPoints(0);
    }

    // ---------- Helpers ----------
    private boolean canUnlock(SkillNode n) {
        if (n.unlocked) return false;
        if (n.cost > ClientState.getPoints()) return false;
        if (n.parentId != null) {
            SkillNode p = nodes.get(n.parentId);
            if (p == null || !p.unlocked) return false;
        }
        return true;
    }

    private void unlockNode(SkillNode n) {
        n.unlocked = true;
        ClientState.addUnlocked(n.id);
        lastUnlockedNode = n;
        flashNode = n;
        flashTime = 1.0f;
        // Отправляем пакет на сервер для синхронизации и сохранения
        ClientPlayNetworking.send(new NodePurchasePayload(n.id));
    }

    // ---------- Input ----------
    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        int button = click.button();
        double mx = click.x(), my = click.y();
        mouseX = mx; mouseY = my;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Return button hit-test
            if (returnAlpha > 0.5f) {
                int r = 18;
                if (mx >= returnX - r && mx <= returnX + r && my >= returnY - r && my <= returnY + r) {
                    if (lastUnlockedNode != null) {
                        cameraX = lastUnlockedNode.x;
                        cameraY = lastUnlockedNode.y;
                    }
                    return true;
                }
            }
            // Node press
            if (hoverNode != null && canUnlock(hoverNode)) {
                pressedNode = hoverNode;
                holdProgress = 0.0f;
                return true;
            }
            if (super.mouseClicked(click, doubleClick)) return true;
            dragging = true;
            lastDragX = mx; lastDragY = my;
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
            if (pressedNode != null) {
                pressedNode = null;
                holdProgress = 0.0f;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        mouseX = mx; mouseY = my;
        if (dragging) {
            cameraX -= (float)((mx - lastDragX) / zoom);
            cameraY -= (float)((my - lastDragY) / zoom);
            lastDragX = mx; lastDragY = my;
            clampCamera();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (v == 0) return false;
        float worldX = screenToWorldX(mx);
        float worldY = screenToWorldY(my);
        zoom = MathHelper.clamp(zoom * (float)Math.pow(1.15, v), MIN_ZOOM, MAX_ZOOM);
        cameraX = worldX - (float)((mx - this.width / 2.0) / zoom);
        cameraY = worldY - (float)((my - this.height / 2.0) / zoom);
        clampCamera();
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput key) {
        int code = key.key();
        switch (code) {
            case GLFW.GLFW_KEY_W -> keys[0] = true;
            case GLFW.GLFW_KEY_A -> keys[1] = true;
            case GLFW.GLFW_KEY_S -> keys[2] = true;
            case GLFW.GLFW_KEY_D -> keys[3] = true;
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> keys[4] = true;
            case GLFW.GLFW_KEY_ESCAPE -> { this.close(); return true; }
        }
        return super.keyPressed(key);
    }

    @Override
    public boolean keyReleased(KeyInput key) {
        int code = key.key();
        switch (code) {
            case GLFW.GLFW_KEY_W -> keys[0] = false;
            case GLFW.GLFW_KEY_A -> keys[1] = false;
            case GLFW.GLFW_KEY_S -> keys[2] = false;
            case GLFW.GLFW_KEY_D -> keys[3] = false;
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> keys[4] = false;
        }
        return super.keyReleased(key);
    }

    private void clampCamera() {
        cameraX = MathHelper.clamp(cameraX, -WORLD_HALF, WORLD_HALF);
        cameraY = MathHelper.clamp(cameraY, -WORLD_HALF, WORLD_HALF);
    }

    // ---------- Tick ----------
    @Override
    public void tick() {
        float baseSpeed = 6.0f;
        float speed = (keys[4] ? baseSpeed * 3.0f : baseSpeed) / zoom;
        if (keys[0]) cameraY -= speed;
        if (keys[2]) cameraY += speed;
        if (keys[1]) cameraX -= speed;
        if (keys[3]) cameraX += speed;
        clampCamera();

        // Hold progression
        if (pressedNode != null) {
            if (hoverNode == pressedNode) {
                holdProgress += 1.0f / HOLD_TICKS;
                if (holdProgress >= 1.0f) {
                    unlockNode(pressedNode);
                    pressedNode = null;
                    holdProgress = 0.0f;
                }
            } else {
                pressedNode = null;
                holdProgress = 0.0f;
            }
        }

        // Flash timer
        if (flashTime > 0) flashTime -= 1.0f / FLASH_TICKS;

        // Return button logic
        updateReturnButton();
    }

    private void updateReturnButton() {
        float target = 0.0f;
        if (lastUnlockedNode != null) {
            int tx = worldToScreenX(lastUnlockedNode.x);
            int ty = worldToScreenY(lastUnlockedNode.y);
            int margin = 60;
            boolean off = tx < margin || tx > this.width - margin
                    || ty < margin || ty > this.height - margin;
            if (off) {
                target = 1.0f;
                double dx = tx - this.width / 2.0;
                double dy = ty - this.height / 2.0;
                returnAngle = (float)Math.atan2(dy, dx);
                double radius = Math.min(this.width, this.height) / 2.0 - 50;
                double bx = this.width / 2.0 + Math.cos(returnAngle) * radius;
                double by = this.height / 2.0 + Math.sin(returnAngle) * radius;
                returnX = (int)MathHelper.clamp(bx, 40, this.width - 40);
                returnY = (int)MathHelper.clamp(by, 40, this.height - 40);
            }
        }
        returnAlpha = MathHelper.lerp(0.2f, returnAlpha, target);
    }

    // ---------- Render ----------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xC0101014);
        boolean debug = DebugState.isAvailable(this.client != null ? this.client.player : null);
        if (debug) renderGrid(ctx);
        renderConnections(ctx);
        if (hoverNode != null) {
            int nx = worldToScreenX(hoverNode.x);
            int ny = worldToScreenY(hoverNode.y);
            int half = (int)(hoverNode.size * zoom / 2);
            renderTooltip(ctx, hoverNode, nx, ny - half - 80);
        }
        hoverNode = null;
        for (SkillNode n : nodes.values()) renderNode(ctx, n, mx, my);
        renderFlash(ctx);
        if (returnAlpha < 0.5f) renderPoints(ctx);
        renderReturnButton(ctx);
        renderHud(ctx);
        super.render(ctx, mx, my, delta);
    }

    private void renderGrid(DrawContext ctx) {
        int spacing = 100;
        if ((int)(spacing * zoom) < 8) return;
        float left = cameraX - (float)(this.width / 2.0 / zoom);
        float right = cameraX + (float)(this.width / 2.0 / zoom);
        float top = cameraY - (float)(this.height / 2.0 / zoom);
        float bottom = cameraY + (float)(this.height / 2.0 / zoom);
        for (float wx = (float)Math.floor(left / spacing) * spacing; wx < right; wx += spacing) {
            int sx = worldToScreenX(wx);
            ctx.fill(sx, 0, sx + 1, this.height, 0x22FFFFFF);
        }
        for (float wy = (float)Math.floor(top / spacing) * spacing; wy < bottom; wy += spacing) {
            int sy = worldToScreenY(wy);
            ctx.fill(0, sy, this.width, sy + 1, 0x22FFFFFF);
        }
        int ox = worldToScreenX(0), oy = worldToScreenY(0);
        if (ox >= 0 && ox < this.width)  ctx.fill(ox, 0, ox + 1, this.height, 0x66FFFFFF);
        if (oy >= 0 && oy < this.height) ctx.fill(0, oy, this.width, oy + 1, 0x66FFFFFF);
    }

    private void renderConnections(DrawContext ctx) {
        for (SkillNode n : nodes.values()) {
            if (n.parentId == null) continue;
            SkillNode p = nodes.get(n.parentId);
            if (p == null) continue;
            int x1 = worldToScreenX(p.x), y1 = worldToScreenY(p.y);
            int x2 = worldToScreenX(n.x), y2 = worldToScreenY(n.y);
            int color = (p.unlocked && n.unlocked) ? 0xFF55FF55
                    : p.unlocked ? 0xFFAAAA55 : 0xFF555555;
            ctx.fill(Math.min(x1, x2), y1 - 1, Math.max(x1, x2) + 1, y1 + 1, color);
            ctx.fill(x2 - 1, Math.min(y1, y2), x2 + 1, Math.max(y1, y2) + 1, color);
        }
    }

    private void renderNode(DrawContext ctx, SkillNode n, int mx, int my) {
        // Shake while holding
        int shakeX = 0, shakeY = 0;
        if (pressedNode == n && holdProgress > 0) {
            shakeX = (int)((Math.random() - 0.5) * 3);
            shakeY = (int)((Math.random() - 0.5) * 3);
        }

        int half = (int)(n.size * zoom / 2);
        int cx = worldToScreenX(n.x) + shakeX;
        int cy = worldToScreenY(n.y) + shakeY;
        int x1 = cx - half, y1 = cy - half, x2 = cx + half, y2 = cy + half;

        boolean hovered = mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
        if (hovered) hoverNode = n;

        // Bloom behind while holding
        if (pressedNode == n && holdProgress > 0) {
            int bloom = (int)(half * (1 + 0.4f * holdProgress));
            int bloomAlpha = (int)(holdProgress * 100);
            ctx.fill(cx - bloom, cy - bloom, cx + bloom, cy + bloom,
                    (bloomAlpha << 24) | 0x00FFFFFF);
        }

        // Body
        int fill = n.unlocked ? 0xE0205030 : (hovered ? 0xE0404048 : 0xE0282830);
        ctx.fill(x1, y1, x2, y2, fill);

        // Border
        int border = n.unlocked ? 0xFF55FF55
                : (pressedNode == n ? 0xFFFFFFFF : (hovered ? 0xFFFFFFFF : 0xFF888888));
        ctx.fill(x1, y1, x2, y1 + 1, border);
        ctx.fill(x1, y2 - 1, x2, y2, border);
        ctx.fill(x1, y1, x1 + 1, y2, border);
        ctx.fill(x2 - 1, y1, x2, y2, border);

        // Fill overlay (bottom to top while holding)
        if (pressedNode == n && holdProgress > 0) {
            int fillH = (int)((y2 - y1) * holdProgress);
            int fillAlpha = (int)(holdProgress * 180);
            ctx.fill(x1, y2 - fillH, x2, y2, (fillAlpha << 24) | 0x00FFFFFF);
        }

        // Icon (scales with zoom, never disappears)
        float iconSize = Math.max(8, n.size * zoom * 0.5f);
        float scale = iconSize / 16f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx - iconSize / 2, cy - iconSize / 2);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawItem(new ItemStack(n.icon), 0, 0);
        ctx.getMatrices().popMatrix();

        // Labels (name + description + cost)
        if (zoom > 0.4f) {
            int labelY = y2 + 4;
            String name = n.name;
            ctx.drawTextWithShadow(this.textRenderer, n.getNameText(),
                    cx - this.textRenderer.getWidth(name) / 2, labelY, 0xFFFFFF);
        }
    }

    private void renderFlash(DrawContext ctx) {
        if (flashNode == null || flashTime <= 0) return;
        int cx = worldToScreenX(flashNode.x);
        int cy = worldToScreenY(flashNode.y);
        int alpha = (int)(flashTime * 255) & 0xFF;
        int half = (int)(flashNode.size * zoom * (1 + (1 - flashTime) * 2.5f) / 2);
        // Expanding ring
        ctx.fill(cx - half, cy - half, cx + half, cy - half + 2, (alpha << 24) | 0x00FFFFFF);
        ctx.fill(cx - half, cy + half - 2, cx + half, cy + half, (alpha << 24) | 0x00FFFFFF);
        ctx.fill(cx - half, cy - half, cx - half + 2, cy + half, (alpha << 24) | 0x00FFFFFF);
        ctx.fill(cx + half - 2, cy - half, cx + half, cy + half, (alpha << 24) | 0x00FFFFFF);
        // Node flash overlay
        int nh = (int)(flashNode.size * zoom / 2);
        ctx.fill(cx - nh, cy - nh, cx + nh, cy + nh, (alpha << 24) | 0x00FFFFFF);
    }

    private void renderPoints(DrawContext ctx) {
        String text = "Points: " + ClientState.getPoints();
        int w = this.textRenderer.getWidth(text) + 16;
        int x = this.width - w - 10, y = 10;
        ctx.fill(x, y, x + w, y + 20, 0x80000000);
        ctx.fill(x, y, x + 1, y + 20, 0xFFAAAAFF);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(text),
                x + 8, y + 6, 0xFF55FFFF);
    }
    private void renderTooltip(DrawContext ctx, SkillNode n, int anchorX, int anchorY) {
        if (n == null) return;
        int pad = 6;
        int lineH = 11;

        String pointsStr = "Your points: " + ClientState.getPoints();
        int wName = this.textRenderer.getWidth(n.name);
        int wDesc = this.textRenderer.getWidth(n.description);
        int wCost = this.textRenderer.getWidth("Cost:    " + n.cost);
        int wPts  = this.textRenderer.getWidth(pointsStr);
        int w = Math.max(Math.max(Math.max(wName, wDesc), wCost), wPts) + pad * 2;
        int h = lineH * 4 + pad * 2;

        // Позиция: над нодой, по центру
        int x = anchorX - w / 2;
        int y = anchorY;

        // Корректировка границ экрана
        if (x < 4) x = 4;
        if (x + w > this.width - 4) x = this.width - w - 4;
        if (y < 4) y = anchorY + 200; // если не влезает сверху — показываем снизу
        if (y + h > this.height - 4) y = this.height - h - 4;

        ctx.fill(x, y, x + w, y + h, 0xE0000000);
        ctx.fill(x, y, x + w, y + 1, 0xFFAAAAFF);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFAAAAFF);
        ctx.fill(x, y, x + 1, y + h, 0xFFAAAAFF);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFAAAAFF);

        ctx.drawTextWithShadow(this.textRenderer,
                n.getNameText(), x + pad, y + pad, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
                n.getDescText(), x + pad, y + pad + lineH, 0xFFAAAAAA);
        ctx.drawTextWithShadow(this.textRenderer,
                n.getCostText(), x + pad, y + pad + lineH * 2, 0xFFFFAA00);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(pointsStr),
                x + pad, y + pad + lineH * 3, 0xFF55FFFF);
    }
    private void renderReturnButton(DrawContext ctx) {
        if (returnAlpha < 0.02f) return;
        int a = (int)(returnAlpha * 255) & 0xFF;
        int r = 16;
        int x1 = returnX - r, y1 = returnY - r, x2 = returnX + r, y2 = returnY + r;
        ctx.fill(x1, y1, x2, y2, (a << 24) | 0x00404048);
        ctx.fill(x1, y1, x2, y1 + 1, (a << 24) | 0x00FFFFFF);
        ctx.fill(x1, y2 - 1, x2, y2, (a << 24) | 0x00FFFFFF);
        ctx.fill(x1, y1, x1 + 1, y2, (a << 24) | 0x00FFFFFF);
        ctx.fill(x2 - 1, y1, x2, y2, (a << 24) | 0x00FFFFFF);

        // Rotated arrow ">" pointing toward target
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(returnX, returnY);
        ctx.getMatrices().rotate(returnAngle);
        ctx.getMatrices().scale(1.5f, 1.5f);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(">"), -this.textRenderer.getWidth(">") / 2, -4,
                (a << 24) | 0x00FFFFFF);
        ctx.getMatrices().popMatrix();
    }

    private void renderHud(DrawContext ctx) {
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(String.format("Zoom: %.0f%%", zoom * 100)).formatted(Formatting.WHITE),
                10, 10, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(String.format("Cam:  %.0f, %.0f", cameraX, cameraY)).formatted(Formatting.WHITE),
                10, 22, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal("WASD/LMB=pan | Shift=fast | Wheel=zoom | HOLD node=upgrade | ESC=close")
                        .formatted(Formatting.GRAY),
                10, this.height - 35, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() { return false; }
}