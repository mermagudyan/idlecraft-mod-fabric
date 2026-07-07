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

import io.github.mermagudyan.idlecraft.network.NodePurchasePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class IdlecraftScreen extends Screen {

    private float expandProgress = 0.0f;
    private boolean prevCtrlHeld = false;
    private boolean prevHovering = false;
    private float expandLinear = 0.0f; // линейный прогресс 0..1

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
        if (debug) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Reset Idlecraft").formatted(Formatting.RED),
                    b -> {
                        if (this.client != null && this.client.player != null
                                && this.client.player.networkHandler != null) {
                            this.client.player.networkHandler.sendChatCommand("idlecraft reset");
                            this.client.player.sendMessage(
                                    Text.literal("[Idlecraft] Progress reset."), false);
                        }
                    }
            ).dimensions(this.width / 2 - 75, this.height - 25, 150, 20).build());
        }
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Prestige").formatted(Formatting.GOLD),
                b -> this.client.setScreen(new PrestigeConfirmScreen(this))
        ).dimensions(this.width / 2 - 155, yPrestige, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                b -> this.close()
        ).dimensions(this.width / 2 + 5, yPrestige, 150, 20).build());

    }

    // ---------- Helpers ----------
    private boolean canUnlock(SkillNode n) {
        if (n.unlocked) return false;
        if (n.cost > ClientState.getPoints()) return false;
        if (n.parentId != null) {
            SkillNode p = nodes.get(n.parentId);
            if (p == null || !p.unlocked) return false;
        }
        // Клиентская проверка условий
        if (!isConditionMet(n)) return false;
        return true;
    }

    private boolean isConditionMet(SkillNode n) {
        if (n.conditionText == null || n.conditionText.isEmpty()) return true;
        if (n.unlocked) return true;

        if ("wood_1".equals(n.id)) {
            return ClientState.getProgress("wood_1") >= 5;
        }
        if ("stone_1".equals(n.id)) {
            return areLastNodesOfTopBranchUnlocked();
        }
        return true;
    }

    // Проверка: все последние ноды верхней ветки (Y < 0) разблокированы
    private boolean areLastNodesOfTopBranchUnlocked() {
        for (SkillNode n : nodes.values()) {
            if (n.y >= 0) continue; // только верхняя ветка
            if (n.parentId == null) continue;
            // Проверяем, есть ли у этой ноды дочерние ноды
            boolean hasChildren = false;
            for (SkillNode other : nodes.values()) {
                if (n.id.equals(other.parentId)) {
                    hasChildren = true;
                    break;
                }
            }
            // Если дочерних нет — это "последняя нода"
            if (!hasChildren && !n.unlocked) {
                return false;
            }
        }
        return true;
    }

    private void unlockNode(SkillNode n) {
        // НЕ меняем локальное состояние — ждём подтверждения от сервера
        lastUnlockedNode = n;
        flashNode = n;
        flashTime = 1.0f;
        // Отправляем пакет на сервер
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
        // WASD движение камеры
        float baseSpeed = 6.0f;
        float speed = (keys[4] ? baseSpeed * 3.0f : baseSpeed) / zoom;
        if (keys[0]) cameraY -= speed;
        if (keys[2]) cameraY += speed;
        if (keys[1]) cameraX -= speed;
        if (keys[3]) cameraX += speed;
        clampCamera();   // ← ПЕРВЫЙ clampCamera (для камеры)

        // Синхронизируем состояние нод с сервером
        java.util.List<String> serverUnlocked = ClientState.getUnlockedNodes();
        for (SkillNode n : nodes.values()) {
            boolean wasUnlocked = n.unlocked;
            n.unlocked = serverUnlocked.contains(n.id);
            // Если только что разблокировалась — обновляем lastUnlockedNode
            if (n.unlocked && !wasUnlocked) {
                lastUnlockedNode = n;
                flashNode = n;
                flashTime = 1.0f;
            }
        }

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

        hoverNode = null;
        for (SkillNode n : nodes.values()) {
            if (!isNodeVisible(n)) continue;
            int half = (int)(n.size * zoom / 2);
            int cx = worldToScreenX(n.x);
            int cy = worldToScreenY(n.y);
            if (mx >= cx - half && mx <= cx + half && my >= cy - half && my <= cy + half) {
                hoverNode = n;
                break;
            }
        }

        // Логика Ctrl-расширения
        boolean hovering = (hoverNode != null);
        boolean ctrlHeld = isCtrlHeld();

        if (hovering && ctrlHeld) {
            if (!prevHovering && prevCtrlHeld) {
                expandLinear = 1.0f;
            } else {
                expandLinear = Math.min(1.0f, expandLinear + delta / 10.0f);
            }
        } else {
            expandLinear = Math.max(0.0f, expandLinear - delta / 10.0f);
        }
// Ease-in: прогресс растёт медленно в начале, быстро в конце
        expandProgress = expandLinear * expandLinear;
        prevCtrlHeld = ctrlHeld;
        prevHovering = hovering;

        renderFlash(ctx);

        if (hoverNode != null) {
            renderTooltip(ctx, hoverNode, mx, my);
        } else {
            tooltipFade = Math.max(0.0f, tooltipFade - delta / 20.0f);
        }

        if (tooltipFade > 0.01f) {
            int darkAlpha = (int)(0xCC * tooltipFade);  // 80% вместо 50%
            ctx.fill(0, 0, this.width, this.height, (darkAlpha << 24));
        }

        for (SkillNode n : nodes.values()) {
            if (!isNodeVisible(n)) continue;
            boolean dim = (hoverNode != null && n != hoverNode);
            renderNode(ctx, n, mx, my, dim);
        }

        if (hoverNode != null) {
            renderTooltip(ctx, hoverNode, mx, my);
        }

        renderPoints(ctx);
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

    private float tooltipFade = 0.0f; // 0..1, для анимации затемнения

    private boolean isNodeVisible(SkillNode n) {
        if (n.unlockCondition == null) return true;
        if (n.parentId == null) return true;

        if ("parent_unlocked".equals(n.unlockCondition)) {
            SkillNode parent = nodes.get(n.parentId);
            return parent != null && parent.unlocked;
        }
        if ("custom".equals(n.unlockCondition)) {
            // stone_1: "Unlock top branch" → видна если wood_2 или wood_3 разблокирована
            if ("stone_1".equals(n.id)) {
                SkillNode wood2 = nodes.get("wood_2");
                SkillNode wood3 = nodes.get("wood_3");
                return (wood2 != null && wood2.unlocked) || (wood3 != null && wood3.unlocked);
            }
            // wood_1: "Chop 5 wood" → тут нужна серверная статистика, пока всегда видна
            // (когда добавим статистику — заменим)
            return true;
        }
        return true;
    }

    private void renderConnections(DrawContext ctx) {
        for (SkillNode n : nodes.values()) {
            if (n.parentId == null) continue;
            SkillNode p = nodes.get(n.parentId);
            if (p == null) continue;
            if (!isNodeVisible(n)) continue;

            int x1 = worldToScreenX(p.x);
            int y1 = worldToScreenY(p.y);
            int x2 = worldToScreenX(n.x);
            int y2 = worldToScreenY(n.y);

            int color = (p.unlocked && n.unlocked) ? 0xFF55FF55
                    : p.unlocked ? 0xFFAAAA55 : 0xFF555555;

            drawLine(ctx, x1, y1, x2, y2, color);
        }
    }

    private void drawLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            ctx.fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            ctx.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    private void renderNode(DrawContext ctx, SkillNode n, int mx, int my, boolean dim) {
        int shakeX = 0, shakeY = 0;
        if (pressedNode == n && holdProgress > 0) {
            shakeX = (int)((Math.random() - 0.5) * 3);
            shakeY = (int)((Math.random() - 0.5) * 3);
        }

        int half = (int)(n.size * zoom / 2);
        int cx = worldToScreenX(n.x) + shakeX;
        int cy = worldToScreenY(n.y) + shakeY;
        int x1 = cx - half, y1 = cy - half, x2 = cx + half, y2 = cy + half;

        boolean hovered = (n == hoverNode);

        if (pressedNode == n && holdProgress > 0) {
            int bloom = (int)(half * (1 + 0.4f * holdProgress));
            int bloomAlpha = (int)(holdProgress * 100);
            ctx.fill(cx - bloom, cy - bloom, cx + bloom, cy + bloom, (bloomAlpha << 24) | 0x00FFFFFF);
        }

        int fill = n.unlocked
                ? (dim ? 0xFF182820 : 0xE0205030)
                : (hovered ? 0xE0404048 : (dim ? 0xFF181820 : 0xE0282830));
        ctx.fill(x1, y1, x2, y2, fill);

        int border = n.unlocked
                ? (dim ? 0xFF305530 : 0xFF55FF55)
                : (pressedNode == n
                   ? (dim ? 0xFFCCCCCC : 0xFFFFFFFF)
                   : (hovered
                      ? (dim ? 0xFFCCCCCC : 0xFFFFFFFF)
                      : (dim ? 0xFF444444 : 0xFF888888)));
        ctx.fill(x1, y1, x2, y1 + 1, border);
        ctx.fill(x1, y2 - 1, x2, y2, border);
        ctx.fill(x1, y1, x1 + 1, y2, border);
        ctx.fill(x2 - 1, y1, x2, y2, border);

        if (pressedNode == n && holdProgress > 0) {
            int fillH = (int)((y2 - y1) * holdProgress);
            int fillAlpha = (int)(holdProgress * 180);
            ctx.fill(x1, y2 - fillH, x2, y2, (fillAlpha << 24) | 0x00FFFFFF);
        }

        float iconSize = Math.max(8, n.size * zoom * 0.5f);
        float scale = iconSize / 16f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx - iconSize / 2, cy - iconSize / 2);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawItem(new ItemStack(n.icon), 0, 0);
        ctx.getMatrices().popMatrix();

        // Затемнение иконки
        if (dim) {
            ctx.fill(cx - (int)iconSize/2, cy - (int)iconSize/2,
                    cx + (int)iconSize/2, cy + (int)iconSize/2,
                    0x80000000);
        }

        if (zoom > 0.4f) {
            int labelY = y2 + 4;
            String name = n.name;
            ctx.drawTextWithShadow(this.textRenderer, n.getNameText(), cx - this.textRenderer.getWidth(name) / 2, labelY, dim ? 0x80FFFFFF : 0xFFFFFFFF);
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
    private void renderTooltip(DrawContext ctx, SkillNode n, int mx, int my) {
        if (n == null) return;
        int pad = 6;
        int lineH = 11;

        String pointsStr = "Your points: " + ClientState.getPoints();
        String costStr = "Cost:    " + n.cost;

        // Условие
        String condStr = null;
        boolean conditionMet = false;
        if (!n.unlocked && n.conditionText != null && !n.conditionText.isEmpty()) {
            if ("wood_1".equals(n.id)) {
                int progress = ClientState.getProgress("wood_1");
                condStr = "Condition: " + n.conditionText + " (" + progress + "/5)";
                conditionMet = (progress >= 5);
            } else {
                condStr = "Condition: " + n.conditionText;
                // stone_1: условие — открыть верхнюю ветку
                if ("stone_1".equals(n.id)) {
                    conditionMet = areLastNodesOfTopBranchUnlocked();
                }
            }
        }

        // Расширенная информация (при Ctrl)
        String grantsStr = null;
        String unlocksStr = null;
        int extraLines = 0;
        if (expandProgress > 0.01f) {
            grantsStr = "Grants: " + n.description;
            extraLines++;
            // Что разблокирует этот нод
            StringBuilder children = new StringBuilder();
            for (SkillNode child : nodes.values()) {
                if (n.id.equals(child.parentId)) {
                    if (children.length() > 0) children.append(", ");
                    children.append(child.name);
                }
            }
            if (children.length() > 0) {
                unlocksStr = "Unlocks: " + children;
                extraLines++;
            }
        }

        // Расчёт размеров
        int wName = this.textRenderer.getWidth(n.name);
        int wDesc = this.textRenderer.getWidth(n.description);
        int wCost = this.textRenderer.getWidth(costStr);
        int wPts  = this.textRenderer.getWidth(pointsStr);
        int wCond = condStr != null ? this.textRenderer.getWidth(condStr) : 0;
        int wGrants = grantsStr != null ? this.textRenderer.getWidth(grantsStr) : 0;
        int wUnlocks = unlocksStr != null ? this.textRenderer.getWidth(unlocksStr) : 0;

        int maxW = Math.max(Math.max(Math.max(wName, wDesc), wCost), Math.max(wPts, Math.max(wCond, Math.max(wGrants, wUnlocks))));
        int w = maxW + pad * 2;
        int baseLines = 4 + (condStr != null ? 1 : 0);
        float animatedExtraLines = extraLines * expandProgress;
        int h = (int)(lineH * (baseLines + animatedExtraLines) + pad * 2);


        // Позиция
        int nodeScreenX = worldToScreenX(n.x);
        int nodeScreenY = worldToScreenY(n.y);
        int halfNode = (int)(n.size * zoom / 2);

        int x = nodeScreenX - w / 2;
        // По умолчанию — сверху. Если не влезает — снизу.
        boolean aboveNode = true;
        int y = nodeScreenY - halfNode - h - 8;
        if (y < 4) {
            aboveNode = false;
            y = nodeScreenY + halfNode + 8;
        }
        int bottom = y + h;
        if (x < 4) x = 4;
        if (x + w > this.width - 4) x = this.width - w - 4;

        // Фон
        ctx.fill(x, y, x + w, y + h, 0xF0000000);
        ctx.fill(x, y, x + w, y + 1, 0xFFAAAAFF);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFAAAAFF);
        ctx.fill(x, y, x + 1, y + h, 0xFFAAAAFF);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFAAAAFF);

        // Текст
        int textY = y + pad;
        ctx.drawTextWithShadow(this.textRenderer, n.getNameText(), x + pad, textY, 0xFFFFFFFF);
        textY += lineH;
        ctx.drawTextWithShadow(this.textRenderer, n.getDescText(), x + pad, textY, 0xFFAAAAAA);
        textY += lineH;
        ctx.drawTextWithShadow(this.textRenderer, n.getCostText(), x + pad, textY, 0xFFFFAA00);
        textY += lineH;
        if (condStr != null) {
            int condColor = conditionMet ? 0xFF55FF55 : 0xFFFF5555;
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(condStr), x + pad, textY, condColor);
            textY += lineH;
        }
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(pointsStr), x + pad, textY, 0xFF55FFFF);

        int extraTextY = textY + lineH;
        if (grantsStr != null && expandProgress > 0.01f) {
            // Рисуем только если есть место
            if (extraTextY + lineH <= bottom) {
                ctx.drawTextWithShadow(this.textRenderer,
                        Text.literal(grantsStr).formatted(Formatting.AQUA),
                        x + pad, extraTextY, 0xFF55FFFF);
            }
            extraTextY += lineH;
        }
        if (unlocksStr != null && expandProgress > 0.5f) {
            if (extraTextY + lineH <= bottom) {
                ctx.drawTextWithShadow(this.textRenderer,
                        Text.literal(unlocksStr).formatted(Formatting.LIGHT_PURPLE),
                        x + pad, extraTextY, 0xFFFF55FF);
            }
        }
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

    private boolean isCtrlHeld() {
        if (this.client == null) return false;
        return net.minecraft.client.util.InputUtil.isKeyPressed(
                this.client.getWindow(),
                org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL)
                || net.minecraft.client.util.InputUtil.isKeyPressed(
                this.client.getWindow(),
                org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL);
    }


}