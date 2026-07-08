package io.github.mermagudyan.idlecraft.client.screen;

import io.github.mermagudyan.idlecraft.screen.SkillNode;
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
import net.minecraft.item.Item;
import io.github.mermagudyan.idlecraft.network.NodePurchasePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import io.github.mermagudyan.idlecraft.network.SacrificeOfferPayload;
import net.minecraft.item.Items;

public class IdlecraftScreen extends Screen {

    private float expandProgress = 0.0f;
    private boolean prevCtrlHeld = false;
    private boolean prevHovering = false;
    private float expandLinear = 0.0f;

    private static final float WORLD_HALF = 5000.0f;
    private static final float MIN_ZOOM = 0.2f;
    private static final float MAX_ZOOM = 1.0f;
    private static final int HOLD_TICKS = 20;
    private static final int FLASH_TICKS = 20;

    private boolean animatingCamera = false;
    private float animStartX, animStartY;
    private float animTargetX, animTargetY;
    private float animProgress = 0.0f;

    // Camera
    private float cameraX = 0.0f, cameraY = 0.0f, zoom = 1.0f;
    private boolean dragging = false;
    private double lastDragX, lastDragY;
    private double mouseX = 0, mouseY = 0;
    private final boolean[] keys = new boolean[5];

    // Data
    private final Map<String, io.github.mermagudyan.idlecraft.screen.SkillNode> nodes = new HashMap<>();
    private io.github.mermagudyan.idlecraft.screen.SkillNode hoverNode = null;
    private io.github.mermagudyan.idlecraft.screen.SkillNode lastUnlockedNode = null;
    String text = "Points: " + ClientState.getPoints();

    // Hold-to-upgrade
    private io.github.mermagudyan.idlecraft.screen.SkillNode pressedNode = null;
    private float holdProgress = 0.0f;

    // Flash on unlock
    private io.github.mermagudyan.idlecraft.screen.SkillNode flashNode = null;
    private float flashTime = 0.0f;
    // Return button
    private float returnAlpha = 0.0f;
    private float returnAngle = 0.0f;
    private int returnX = 0, returnY = 0;

    public IdlecraftScreen() {
        super(Text.literal("Idlecraft"));
        for (io.github.mermagudyan.idlecraft.screen.SkillNode n : io.github.mermagudyan.idlecraft.screen.SkillNode.defaults()) nodes.put(n.id, n);
        for (String id : ClientState.getUnlockedNodes()) {
            io.github.mermagudyan.idlecraft.screen.SkillNode n = nodes.get(id);
            if (n != null) n.unlocked = true;

        }
        List<String> unlocked = ClientState.getUnlockedNodes();
        if (!unlocked.isEmpty()) {
            lastUnlockedNode = nodes.get(unlocked.get(unlocked.size() - 1));
        }
    }

    private void startCameraAnim(float targetX, float targetY) {
        animatingCamera = true;
        animStartX = cameraX;
        animStartY = cameraY;
        animTargetX = targetX;
        animTargetY = targetY;
        animProgress = 0.0f;
    }

    private void updateCameraAnim(float delta) {
        if (!animatingCamera) return;
        animProgress += delta / 20.0f;
        if (animProgress >= 1.0f) {
            animProgress = 1.0f;
            cameraX = animTargetX;
            cameraY = animTargetY;
            animatingCamera = false;
            return;
        }
        float t = animProgress;
        float ease = t * t * (3 - 2 * t);
        cameraX = animStartX + (animTargetX - animStartX) * ease;
        cameraY = animStartY + (animTargetY - animStartY) * ease;
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
        if (!isNodeVisible(n)) return false;
        if (!isConditionMet(n)) return false;
        if (n.cost > 0 && n.cost > ClientState.getPoints()) return false;

        if (!n.sacrifices.isEmpty()) {
            int[] progress = ClientState.getSacrificeProgress(n.id);
            for (int i = 0; i < n.sacrifices.size(); i++) {
                int current = i < progress.length ? progress[i] : 0;
                if (current < n.sacrifices.get(i).amount()) return false;
            }
        }
        return true;
    }

    private boolean isConditionMet(SkillNode n) {
        if (n.unlocked) return true;
        if (n.conditionText == null || n.conditionText.isEmpty()) return true;

        if ("first_steps".equals(n.id)) {
            return ClientState.getProgress("first_steps") >= 5;
        }
        if ("village_visit".equals(n.id)) {
            return ClientState.getProgress("crafting_table_unlock") >= 1;
        }
        if ("wooden_tools".equals(n.id)) {
            return ClientState.getProgress("wood_mined") >= 15;
        }
        if ("axe_node".equals(n.id)) {
            return ClientState.getProgress("wood_mined_axe") >= 16;
        }
        if ("stone_1".equals(n.id)) {
            SkillNode axeNode = nodes.get("axe_node");
            return axeNode != null && axeNode.unlocked;
        }
        if ("tech_1".equals(n.id)) {
            return ClientState.getProgress("seedy_place") >= 1;
        }
        return true;
    }

    private boolean areLastNodesOfTopBranchUnlocked() {
        for (io.github.mermagudyan.idlecraft.screen.SkillNode n : nodes.values()) {
            if (n.y >= 0) continue;
            if (n.parentId == null) continue;
            boolean hasChildren = false;
            for (io.github.mermagudyan.idlecraft.screen.SkillNode other : nodes.values()) {
                if (n.id.equals(other.parentId)) {
                    hasChildren = true;
                    break;
                }
            }
            if (!hasChildren && !n.unlocked) {
                return false;
            }
        }
        return true;
    }

    private void unlockNode(io.github.mermagudyan.idlecraft.screen.SkillNode n) {
        lastUnlockedNode = n;
        flashNode = n;
        flashTime = 1.0f;
        ClientPlayNetworking.send(new NodePurchasePayload(n.id));
    }

    // ---------- Input ----------
    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        int button = click.button();
        double mx = click.x(), my = click.y();
        mouseX = mx; mouseY = my;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (returnAlpha > 0.5f) {
                int r = 18;
                if (mx >= returnX - r && mx <= returnX + r && my >= returnY - r && my <= returnY + r) {
                    if (lastUnlockedNode != null) {
                        startCameraAnim(lastUnlockedNode.x, lastUnlockedNode.y);
                    }
                    return true;
                }
            }
            if (hoverNode != null) {
                if (!isConditionMet(hoverNode) || !isNodeVisible(hoverNode)) {
                    return true;
                }
                if (canUnlock(hoverNode)) {
                    pressedNode = hoverNode;
                    holdProgress = 0.0f;
                    startCameraAnim(hoverNode.x, hoverNode.y);
                    return true;
                } else if (!hoverNode.sacrifices.isEmpty()) {
                    ClientPlayNetworking.send(new SacrificeOfferPayload(hoverNode.id));
                    return true;
                }
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
            case GLFW.GLFW_KEY_W:
                if (pressedNode == null) keys[0] = true;
                break;
            case GLFW.GLFW_KEY_A:
                if (pressedNode == null) keys[1] = true;
                break;
            case GLFW.GLFW_KEY_S:
                if (pressedNode == null) keys[2] = true;
                break;
            case GLFW.GLFW_KEY_D:
                if (pressedNode == null) keys[3] = true;
                break;
            case GLFW.GLFW_KEY_LEFT_SHIFT:
            case GLFW.GLFW_KEY_RIGHT_SHIFT:
                if (pressedNode == null) keys[4] = true;
                break;
            case GLFW.GLFW_KEY_ESCAPE:
                this.close();
                return true;
            default:
                return super.keyPressed(key);
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
        if (animatingCamera) {
            if (pressedNode != null) {
                hoverNode = pressedNode;
            }
            return;
        }

        float baseSpeed = 6.0f;
        float speed = (keys[4] ? baseSpeed * 3.0f : baseSpeed) / zoom;
        if (keys[0]) cameraY -= speed;
        if (keys[2]) cameraY += speed;
        if (keys[1]) cameraX -= speed;
        if (keys[3]) cameraX += speed;
        clampCamera();

        java.util.List<String> serverUnlocked = ClientState.getUnlockedNodes();
        for (SkillNode n : nodes.values()) {
            boolean wasUnlocked = n.unlocked;
            n.unlocked = serverUnlocked.contains(n.id);
            if (n.unlocked && !wasUnlocked) {
                lastUnlockedNode = n;
                flashNode = n;
                flashTime = 1.0f;
            }
        }

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

        if (flashTime > 0) flashTime -= 1.0f / FLASH_TICKS;

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
        for (io.github.mermagudyan.idlecraft.screen.SkillNode n : nodes.values()) {
            if (!isNodeVisible(n)) continue;
            int half = (int)(n.size * zoom / 2);
            int cx = worldToScreenX(n.x);
            int cy = worldToScreenY(n.y);
            if (mx >= cx - half && mx <= cx + half && my >= cy - half && my <= cy + half) {
                hoverNode = n;
                break;
            }
        }

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

        for (io.github.mermagudyan.idlecraft.screen.SkillNode n : nodes.values()) {
            if (!isNodeVisible(n)) continue;
            boolean dim = (hoverNode != null && n != hoverNode);
            renderNode(ctx, n, mx, my, dim);
        }

        if (hoverNode != null) {
            renderTooltip(ctx, hoverNode, mx, my);
        }

        renderPoints(ctx);
        renderReturnButton(ctx);
        updateCameraAnim(delta);
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

    private float tooltipFade = 0.0f;

    private boolean shouldDrawLine(SkillNode child) {
        return isNodeVisible(child);
    }

    private boolean isNodeVisible(SkillNode n) {
        if (n.id.equals("start")) return true;
        if (n.parentId == null) return false;

        SkillNode parent = nodes.get(n.parentId);
        if (parent == null || !parent.unlocked) {
            if (n.hiddenUntilParent) return false;
            return false;
        }
        return true;
    }

    private void renderConnections(DrawContext ctx) {
        for (SkillNode n : nodes.values()) {
            if (n.parentId == null) continue;
            SkillNode p = nodes.get(n.parentId);
            if (p == null) continue;
            if (!shouldDrawLine(n)) continue;

            int x1 = worldToScreenX(p.x);
            int y1 = worldToScreenY(p.y);
            int x2 = worldToScreenX(n.x);
            int y2 = worldToScreenY(n.y);

            int color = (p.unlocked && n.unlocked) ? 0xFF55FF55
                    : p.unlocked ? 0xFFAAAA55 : 0xFF555555;

            drawLine(ctx, x1, y1, x2, y2, color);
        }
    }

    private String maskIfLocked(SkillNode n, String text) {
        if (!n.unlocked && !isConditionMet(n)) {
            return "???";
        }
        return text;
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
        boolean masked = !n.unlocked && !isConditionMet(n);

        if (pressedNode == n && holdProgress > 0 && !masked) {
            int bloom = (int)(half * (1 + 0.4f * holdProgress));
            int bloomAlpha = (int)(holdProgress * 100);
            ctx.fill(cx - bloom, cy - bloom, cx + bloom, cy + bloom, (bloomAlpha << 24) | 0x00FFFFFF);
        }

        int fill = n.unlocked ? (dim ? 0xFF182820 : 0xE0205030)
                : (masked ? (dim ? 0xFF141414 : 0xE01A1A1A)
                   : (hovered ? 0xE0404048 : (dim ? 0xFF181820 : 0xE0282830)));
        ctx.fill(x1, y1, x2, y2, fill);

        int border = n.unlocked ? (dim ? 0xFF305530 : 0xFF55FF55)
                : (masked ? (dim ? 0xFF333333 : 0xFF555555)
                   : (pressedNode == n ? (dim ? 0xFFCCCCCC : 0xFFFFFFFF)
                      : (hovered ? (dim ? 0xFFCCCCCC : 0xFFFFFFFF) : (dim ? 0xFF444444 : 0xFF888888))));
        ctx.fill(x1, y1, x2, y1 + 1, border);
        ctx.fill(x1, y2 - 1, x2, y2, border);
        ctx.fill(x1, y1, x1 + 1, y2, border);
        ctx.fill(x2 - 1, y1, x2, y2, border);

        if (pressedNode == n && holdProgress > 0 && !masked) {
            int fillH = (int)((y2 - y1) * holdProgress);
            int fillAlpha = (int)(holdProgress * 180);
            ctx.fill(x1, y2 - fillH, x2, y2, (fillAlpha << 24) | 0x00FFFFFF);
        }

        float iconSize = Math.max(8, n.size * zoom * 0.5f);
        float scale = iconSize / 16f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx - iconSize / 2, cy - iconSize / 2);
        ctx.getMatrices().scale(scale, scale);

        Item iconToDraw = masked ? Items.BARRIER : n.icon;
        ctx.drawItem(new ItemStack(iconToDraw), 0, 0);
        ctx.getMatrices().popMatrix();

        if (zoom > 0.4f) {
            int labelY = y2 + 4;
            String name = masked ? "???" : n.name;
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal(name).formatted(n.unlocked ? Formatting.GREEN : Formatting.WHITE),
                    cx - this.textRenderer.getWidth(name) / 2, labelY,
                    dim ? 0x80FFFFFF : 0xFFFFFFFF);
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

        boolean masked = !n.unlocked && !isConditionMet(n);

        String nameStr = masked ? "???" : n.name;
        String descStr = masked ? "???" : n.description;
        String costStr = masked ? "Cost: ???" : "Cost: " + n.cost;
        String pointsStr = "Your points: " + ClientState.getPoints();

        String condStr = null;
        boolean conditionMet = false;
        if (!n.unlocked && n.conditionText != null && !n.conditionText.isEmpty() && !masked) {
            if ("first_steps".equals(n.id)) {
                int progress = ClientState.getProgress("first_steps");
                condStr = "Condition: " + n.conditionText + " (" + progress + "/5)";
                conditionMet = (progress >= 5);
            } else if ("village_visit".equals(n.id)) {
                conditionMet = ClientState.getProgress("crafting_table_unlock") >= 1;
                condStr = "Condition: " + n.conditionText + (conditionMet ? " (Done)" : "");
            } else if ("wooden_tools".equals(n.id)) {
                int progress = ClientState.getProgress("wood_mined");
                condStr = "Condition: " + n.conditionText + " (" + progress + "/15)";
                conditionMet = (progress >= 15);
            } else if ("axe_node".equals(n.id)) {
                int progress = ClientState.getProgress("wood_mined_axe");
                condStr = "Condition: " + n.conditionText + " (" + progress + "/16)";
                conditionMet = (progress >= 16);
            } else if ("stone_1".equals(n.id)) {
                SkillNode axeNode = nodes.get("axe_node");
                conditionMet = axeNode != null && axeNode.unlocked;
                condStr = "Condition: " + n.conditionText + (conditionMet ? " (Done)" : "");
            } else if ("tech_1".equals(n.id)) {
                conditionMet = ClientState.getProgress("seedy_place") >= 1;
                condStr = "Condition: " + n.conditionText + (conditionMet ? " (Done)" : "");
            } else {
                condStr = "Condition: " + n.conditionText;
            }
        }

        String grantsStr = null;
        String unlocksStr = null;
        int extraLines = 0;
        if (expandProgress > 0.01f) {
            grantsStr = masked ? "???" : n.detailedDescription;
            extraLines++;
            StringBuilder children = new StringBuilder();
            for (SkillNode child : nodes.values()) {
                if (n.id.equals(child.parentId)) {
                    if (children.length() > 0) children.append(", ");
                    children.append(masked ? "???" : child.name);
                }
            }
            if (children.length() > 0) {
                unlocksStr = "Unlocks: " + children;
                extraLines++;
            }
        }

        int wName = this.textRenderer.getWidth(nameStr);
        int wDesc = this.textRenderer.getWidth(descStr);
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

        int nodeScreenX = worldToScreenX(n.x);
        int nodeScreenY = worldToScreenY(n.y);
        int halfNode = (int)(n.size * zoom / 2);

        int x = nodeScreenX - w / 2;
        boolean aboveNode = true;
        int y = nodeScreenY - halfNode - h - 8;
        if (y < 4) {
            aboveNode = false;
            y = nodeScreenY + halfNode + 8;
        }
        if (x < 4) x = 4;
        if (x + w > this.width - 4) x = this.width - w - 4;
        int bottom = y + h;

        ctx.fill(x, y, x + w, y + h, 0xF0000000);
        ctx.fill(x, y, x + w, y + 1, 0xFFAAAAFF);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFAAAAFF);
        ctx.fill(x, y, x + 1, y + h, 0xFFAAAAFF);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFAAAAFF);

        int textY = y + pad;
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(nameStr).formatted(n.unlocked ? Formatting.GREEN : Formatting.WHITE),
                x + pad, textY, 0xFFFFFFFF);
        textY += lineH;
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(descStr).formatted(Formatting.GRAY),
                x + pad, textY, 0xFFAAAAAA);
        textY += lineH;
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(costStr).formatted(Formatting.GOLD),
                x + pad, textY, 0xFFFFAA00);
        textY += lineH;
        if (condStr != null) {
            int condColor = conditionMet ? 0xFF55FF55 : 0xFFFF5555;
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(condStr), x + pad, textY, condColor);
            textY += lineH;
        }
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(pointsStr), x + pad, textY, 0xFF55FFFF);

        int extraTextY = textY + lineH;
        if (grantsStr != null && expandProgress > 0.01f) {
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