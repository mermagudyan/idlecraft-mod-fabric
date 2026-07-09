package io.github.mermagudyan.idlecraft.client.screen;

import io.github.mermagudyan.idlecraft.screen.SkillNode;
import io.github.mermagudyan.idlecraft.screen.SkillNodeRegistry;
import io.github.mermagudyan.idlecraft.screen.SacrificeRequirement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import io.github.mermagudyan.idlecraft.client.debug.DebugState;
import io.github.mermagudyan.idlecraft.network.ClientState;
import net.minecraft.world.item.Item;
import io.github.mermagudyan.idlecraft.network.NodePurchasePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import io.github.mermagudyan.idlecraft.network.SacrificeOfferPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

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

    private float cameraX = 0.0f, cameraY = 0.0f, zoom = 1.0f;
    private static float savedCameraX = 0.0f, savedCameraY = 0.0f, savedZoom = 1.0f;
    private static boolean cameraSaved = false;
    private static IdlecraftScreen activeScreen = null;
    private boolean dragging = false;
    private double lastDragX, lastDragY;
    private double mouseX = 0, mouseY = 0;
    private final boolean[] keys = new boolean[5];

    private final Map<String, SkillNode> nodes = new HashMap<>();
    private SkillNode hoverNode = null;
    private SkillNode lastUnlockedNode = null;

    private SkillNode pressedNode = null;
    private float holdProgress = 0.0f;

    private SkillNode flashNode = null;
    private float flashTime = 0.0f;
    private float returnAlpha = 0.0f;
    private float returnAngle = 0.0f;
    private int returnX = 0, returnY = 0;
    private float tooltipFade = 0.0f;
    private int reportBtnX = 0, reportBtnY = 0, reportBtnW = 0;

    public IdlecraftScreen() {
        super(Component.literal("Idlecraft"));
        activeScreen = this;
        for (SkillNode n : SkillNode.defaults()) nodes.put(n.id, n);
        for (String id : ClientState.getUnlockedNodes()) {
            SkillNode n = nodes.get(id);
            if (n != null) n.unlocked = true;
        }
        lastUnlockedNode = nodes.get("start");
        List<String> unlocked = ClientState.getUnlockedNodes();
        if (!unlocked.isEmpty()) {
            lastUnlockedNode = nodes.get(unlocked.get(unlocked.size() - 1));
        }

        SkillNode startNode = nodes.get("start");
        boolean startUnlocked = unlocked.contains("start");
        if (cameraSaved && startUnlocked) {
            cameraX = savedCameraX;
            cameraY = savedCameraY;
            zoom = savedZoom;
        } else if (startNode != null) {
            cameraX = startNode.x;
            cameraY = startNode.y;
            zoom = 1.0f;
        }
    }

    @Override
    public void onClose() {
        savedCameraX = cameraX;
        savedCameraY = cameraY;
        savedZoom = zoom;
        cameraSaved = true;
        if (activeScreen == this) activeScreen = null;
        super.onClose();
    }

    public static IdlecraftScreen getActiveScreen() {
        return activeScreen;
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

    private float screenToWorldX(double sx) { return cameraX + (float)((sx - this.width / 2.0) / zoom); }
    private float screenToWorldY(double sy) { return cameraY + (float)((sy - this.height / 2.0) / zoom); }
    private int worldToScreenX(float wx) { return (int)(this.width / 2.0 + (wx - cameraX) * zoom); }
    private int worldToScreenY(float wy) { return (int)(this.height / 2.0 + (wy - cameraY) * zoom); }

    @Override
    protected void init() {
        boolean debug = DebugState.isAvailable(this.minecraft != null ? this.minecraft.player : null);

        int yPrestige = debug ? this.height - 50 : this.height - 25;
        if (debug) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Reset Idlecraft").withStyle(ChatFormatting.RED),
                    b -> {
                        if (this.minecraft != null && this.minecraft.player != null
                                && this.minecraft.player.connection != null) {
                            this.minecraft.player.connection.sendCommand("idlecraft reset");
                            this.minecraft.player.sendSystemMessage(
                                    Component.literal("[Idlecraft] Progress reset."));
                        }
                    }
            ).bounds(this.width / 2 - 75, this.height - 25, 150, 20).build());
        }
        this.addRenderableWidget(Button.builder(
                Component.literal("Prestige").withStyle(ChatFormatting.GOLD),
                b -> this.minecraft.setScreenAndShow(new PrestigeConfirmScreen(this))
        ).bounds(this.width / 2 - 155, yPrestige, 150, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                b -> this.onClose()
        ).bounds(this.width / 2 + 5, yPrestige, 150, 20).build());

        int reportW = this.font.width("Report Bugs") + 24;
        this.reportBtnX = this.width - reportW - 6;
        this.reportBtnY = this.height - 25;
        this.reportBtnW = reportW;
        this.addRenderableWidget(Button.builder(
                Component.literal("  Report Bugs"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreenAndShow(new ConfirmLinkScreen(
                                accepted -> this.minecraft.setScreenAndShow(this),
                                "https://github.com/mermagudyan/idlecraft-mod-fabric/discussions",
                                true));
                    }
                }
        ).bounds(this.reportBtnX, this.reportBtnY, reportW, 20).build());
    }

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
        if ("sticky".equals(n.id)) {
            return ClientState.getProgress("sticky") >= 5;
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
            if (axeNode == null || !axeNode.unlocked) return false;
            List<SkillNode> branchLeaves = SkillNodeRegistry.getLeavesOfBranch(SkillNodeRegistry.BRANCH_TUTORIAL);
            for (SkillNode ln : branchLeaves) {
                if (!ClientState.getUnlockedNodes().contains(ln.id)) return false;
            }
            return true;
        }
        if ("tech_1".equals(n.id)) {
            return ClientState.getUnlockedNodes().contains("bread_sac");
        }
        return true;
    }

    private boolean areLastNodesOfTopBranchUnlocked() {
        for (SkillNode n : nodes.values()) {
            if (n.y >= 0) continue;
            if (n.parentId == null) continue;
            boolean hasChildren = false;
            for (SkillNode other : nodes.values()) {
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

    private void unlockNode(SkillNode n) {
        lastUnlockedNode = n;
        flashNode = n;
        flashTime = 1.0f;
        ClientPlayNetworking.send(new NodePurchasePayload(n.id));
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        double mx = event.x(), my = event.y();
        this.mouseX = mx; this.mouseY = my;
        int button = event.button();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (returnAlpha > 0.5f) {
                int r = 18;
                if (mx >= returnX - r && mx <= returnX + r && my >= returnY - r && my <= returnY + r) {
                    SkillNode target = getReturnTarget();
                    if (target != null) {
                        startCameraAnim(target.x, target.y);
                    }
                    return true;
                }
            }
            if (hoverNode != null) {
                if (!isNodeVisible(hoverNode)) {
                    return true;
                }
                startCameraAnim(hoverNode.x, hoverNode.y);
                if (canUnlock(hoverNode)) {
                    pressedNode = hoverNode;
                    holdProgress = 0.0f;
                    return true;
                } else if (!hoverNode.sacrifices.isEmpty()) {
                    if (!animatingCamera) {
                        ClientPlayNetworking.send(new SacrificeOfferPayload(hoverNode.id));
                    }
                    return true;
                }
                return true;
            }
            if (super.mouseClicked(event, doubleClick)) return true;
            dragging = true;
            lastDragX = mx; lastDragY = my;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
            if (pressedNode != null) {
                pressedNode = null;
                holdProgress = 0.0f;
            }
        }
        return super.mouseReleased(event);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double v = scrollY;
        if (v == 0) return false;
        float worldX = screenToWorldX(mouseX);
        float worldY = screenToWorldY(mouseY);
        zoom = Mth.clamp(zoom * (float)Math.pow(1.15, v), MIN_ZOOM, MAX_ZOOM);
        cameraX = worldX - (float)((mouseX - this.width / 2.0) / zoom);
        cameraY = worldY - (float)((mouseY - this.height / 2.0) / zoom);
        clampCamera();
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        int keyCode = event.key();
        switch (keyCode) {
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
                this.onClose();
                return true;
            default:
                return super.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(final KeyEvent event) {
        int keyCode = event.key();
        switch (keyCode) {
            case GLFW.GLFW_KEY_W -> keys[0] = false;
            case GLFW.GLFW_KEY_A -> keys[1] = false;
            case GLFW.GLFW_KEY_S -> keys[2] = false;
            case GLFW.GLFW_KEY_D -> keys[3] = false;
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> keys[4] = false;
        }
        return super.keyReleased(event);
    }

    private void clampCamera() {
        cameraX = Mth.clamp(cameraX, -WORLD_HALF, WORLD_HALF);
        cameraY = Mth.clamp(cameraY, -WORLD_HALF, WORLD_HALF);
    }

    @Override
    public void tick() {
        if (animatingCamera && pressedNode != null) {
            hoverNode = pressedNode;
        }

        float baseSpeed = 6.0f;
        float speed = (keys[4] ? baseSpeed * 3.0f : baseSpeed) / zoom;
        if (keys[0]) cameraY -= speed;
        if (keys[2]) cameraY += speed;
        if (keys[1]) cameraX -= speed;
        if (keys[3]) cameraX += speed;
        clampCamera();

        List<String> serverUnlocked = ClientState.getUnlockedNodes();
        boolean anyUnlocked = false;
        for (SkillNode n : nodes.values()) {
            boolean wasUnlocked = n.unlocked;
            n.unlocked = serverUnlocked.contains(n.id);
            if (n.unlocked) anyUnlocked = true;
            if (n.unlocked && !wasUnlocked) {
                lastUnlockedNode = n;
                flashNode = n;
                flashTime = 1.0f;
            }
        }
        if (!anyUnlocked) {
            lastUnlockedNode = nodes.get("start");
        }

        if (pressedNode != null) {
            if (hoverNode == pressedNode) {
                holdProgress += 1.0f / HOLD_TICKS;
                if (holdProgress >= 1.0f) {
                    if (canUnlock(pressedNode)) {
                        unlockNode(pressedNode);
                    }
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
        SkillNode target = getReturnTarget();
        float targetAlpha = 0.0f;
        if (target != null) {
            int tx = worldToScreenX(target.x);
            int ty = worldToScreenY(target.y);
            int margin = 60;
            boolean off = tx < margin || tx > this.width - margin
                    || ty < margin || ty > this.height - margin;
            if (off) {
                targetAlpha = 1.0f;
                double dx = tx - this.width / 2.0;
                double dy = ty - this.height / 2.0;
                returnAngle = (float)Math.atan2(dy, dx);
                double radius = Math.min(this.width, this.height) / 2.0 - 50;
                double bx = this.width / 2.0 + Math.cos(returnAngle) * radius;
                double by = this.height / 2.0 + Math.sin(returnAngle) * radius;
                returnX = (int)Mth.clamp(bx, 40, this.width - 40);
                returnY = (int)Mth.clamp(by, 40, this.height - 40);
            }
        }
        returnAlpha = Mth.lerp(0.2f, returnAlpha, targetAlpha);
    }

    private SkillNode getReturnTarget() {
        SkillNode bestUnlockable = null;
        float bestUnlockableDist = Float.MAX_VALUE;
        SkillNode bestLocked = null;
        float bestLockedDist = Float.MAX_VALUE;
        for (SkillNode n : nodes.values()) {
            if (n.unlocked || !isNodeVisible(n)) continue;
            float dist = (n.x - cameraX) * (n.x - cameraX) + (n.y - cameraY) * (n.y - cameraY);
            if (canUnlock(n)) {
                if (dist < bestUnlockableDist) {
                    bestUnlockableDist = dist;
                    bestUnlockable = n;
                }
            } else {
                if (dist < bestLockedDist) {
                    bestLockedDist = dist;
                    bestLocked = n;
                }
            }
        }
        if (bestUnlockable != null) return bestUnlockable;
        if (bestLocked != null) return bestLocked;
        return nodes.get("start");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xC0101014);
        boolean debug = DebugState.isAvailable(this.minecraft != null ? this.minecraft.player : null);
        if (debug) renderGrid(guiGraphics);
        renderConnections(guiGraphics);

        hoverNode = null;
        for (SkillNode n : nodes.values()) {
            if (!isNodeVisible(n)) continue;
            int half = (int)(n.size * zoom / 2);
            int cx = worldToScreenX(n.x);
            int cy = worldToScreenY(n.y);
            if (mouseX >= cx - half && mouseX <= cx + half && mouseY >= cy - half && mouseY <= cy + half) {
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
                expandLinear = Math.min(1.0f, expandLinear + partialTick / 10.0f);
            }
        } else {
            expandLinear = Math.max(0.0f, expandLinear - partialTick / 10.0f);
        }
        expandProgress = expandLinear * expandLinear;
        prevCtrlHeld = ctrlHeld;
        prevHovering = hovering;

        renderFlash(guiGraphics);

        if (hoverNode != null) {
            renderTooltip(guiGraphics, hoverNode, mouseX, mouseY);
        } else {
            tooltipFade = Math.max(0.0f, tooltipFade - partialTick / 20.0f);
        }

        if (tooltipFade > 0.01f) {
            int darkAlpha = (int)(0xCC * tooltipFade);
            guiGraphics.fill(0, 0, this.width, this.height, (darkAlpha << 24));
        }

        for (SkillNode n : nodes.values()) {
            if (!isNodeVisible(n)) continue;
            boolean dim = (hoverNode != null && n != hoverNode);
            renderNode(guiGraphics, n, mouseX, mouseY, dim);
        }

        if (hoverNode != null) {
            renderTooltip(guiGraphics, hoverNode, mouseX, mouseY);
        }

        renderPoints(guiGraphics);
        renderReturnButton(guiGraphics);
        updateCameraAnim(partialTick);
        renderHud(guiGraphics);

        int iconSize = 12;
        int iconX = this.reportBtnX + 4;
        int iconY = this.reportBtnY + (20 - iconSize) / 2;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("idlecraft", "bug"), iconX, iconY, iconSize, iconSize);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderGrid(GuiGraphicsExtractor guiGraphics) {
        int spacing = 100;
        if ((int)(spacing * zoom) < 8) return;
        float left = cameraX - (float)(this.width / 2.0 / zoom);
        float right = cameraX + (float)(this.width / 2.0 / zoom);
        float top = cameraY - (float)(this.height / 2.0 / zoom);
        float bottom = cameraY + (float)(this.height / 2.0 / zoom);
        for (float wx = (float)Math.floor(left / spacing) * spacing; wx < right; wx += spacing) {
            int sx = worldToScreenX(wx);
            guiGraphics.fill(sx, 0, sx + 1, this.height, 0x22FFFFFF);
        }
        for (float wy = (float)Math.floor(top / spacing) * spacing; wy < bottom; wy += spacing) {
            int sy = worldToScreenY(wy);
            guiGraphics.fill(0, sy, this.width, sy + 1, 0x22FFFFFF);
        }
        int ox = worldToScreenX(0), oy = worldToScreenY(0);
        if (ox >= 0 && ox < this.width)  guiGraphics.fill(ox, 0, ox + 1, this.height, 0x66FFFFFF);
        if (oy >= 0 && oy < this.height) guiGraphics.fill(0, oy, this.width, oy + 1, 0x66FFFFFF);
    }

    private boolean shouldDrawLine(SkillNode child) {
        return isNodeVisible(child);
    }

    private boolean isNodeVisible(SkillNode n) {
        if (n.unlocked) return true;
        if (n.id.equals("start")) return true;

        if (n.id.equals("axe_node")) {
            SkillNode a = nodes.get("crafting_table_unlock");
            SkillNode b = nodes.get("wooden_tools");
            return a != null && a.unlocked && b != null && b.unlocked;
        }

        if (n.id.equals("stone_1")) {
            SkillNode axe = nodes.get("axe_node");
            return axe != null && isNodeVisible(axe);
        }

        if (n.id.equals("bread_sac")) {
            SkillNode parent = nodes.get(n.parentId);
            return parent != null && parent.unlocked && ClientState.getProgress("seedy_place") >= 1;
        }

        if (n.id.equals("tech_1")) {
            return ClientState.getUnlockedNodes().contains("bread_sac");
        }

        if (n.id.equals("tech_2") || n.id.equals("tech_3")) {
            SkillNode parent = nodes.get("tech_1");
            return parent != null && parent.unlocked;
        }

        if (n.parentId == null) return false;

        SkillNode parent = nodes.get(n.parentId);
        if (parent == null || !parent.unlocked) {
            return false;
        }
        return true;
    }

    private void renderConnections(GuiGraphicsExtractor guiGraphics) {
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

            drawLine(guiGraphics, x1, y1, x2, y2, color);
        }
    }

    private void drawLine(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            guiGraphics.fill(x1 - 1, y1 - 1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            guiGraphics.fill(x - 1, y - 1, x + 1, y + 1, color);
        }
    }

    private void renderNode(GuiGraphicsExtractor guiGraphics, SkillNode n, int mx, int my, boolean dim) {
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
            guiGraphics.fill(cx - bloom, cy - bloom, cx + bloom, cy + bloom, (bloomAlpha << 24) | 0x00FFFFFF);
        }

        int fill = n.unlocked ? (dim ? 0xFF182820 : 0xE0205030)
                : (masked ? (dim ? 0xFF141414 : 0xE01A1A1A)
                   : (hovered ? 0xE0404048 : (dim ? 0xFF181820 : 0xE0282830)));
        guiGraphics.fill(x1, y1, x2, y2, fill);

        int border = n.unlocked ? (dim ? 0xFF305530 : 0xFF55FF55)
                : (masked ? (dim ? 0xFF333333 : 0xFF555555)
                   : (pressedNode == n ? (dim ? 0xFFCCCCCC : 0xFFFFFFFF)
                      : (hovered ? (dim ? 0xFFCCCCCC : 0xFFFFFFFF) : (dim ? 0xFF444444 : 0xFF888888))));
        guiGraphics.fill(x1, y1, x2, y1 + 1, border);
        guiGraphics.fill(x1, y2 - 1, x2, y2, border);
        guiGraphics.fill(x1, y1, x1 + 1, y2, border);
        guiGraphics.fill(x2 - 1, y1, x2, y2, border);

        if (pressedNode == n && holdProgress > 0 && !masked) {
            int fillH = (int)((y2 - y1) * holdProgress);
            int fillAlpha = (int)(holdProgress * 180);
            guiGraphics.fill(x1, y2 - fillH, x2, y2, (fillAlpha << 24) | 0x00FFFFFF);
        }

        float iconSize = Math.max(8, n.size * zoom * 0.5f);
        float scale = iconSize / 16f;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(cx - iconSize / 2, cy - iconSize / 2);
        guiGraphics.pose().scale(scale, scale);

        Item iconToDraw = n.icon;
        guiGraphics.item(new ItemStack(iconToDraw), 0, 0);
        guiGraphics.pose().popMatrix();

        if (zoom > 0.4f) {
            int labelY = y2 + 4;
            String name = n.name;
            guiGraphics.text(this.font,
                    Component.literal(name).withStyle(n.unlocked ? ChatFormatting.GREEN : ChatFormatting.WHITE),
                    cx - this.font.width(name) / 2, labelY,
                    dim ? 0x80FFFFFF : 0xFFFFFFFF, true);
        }
    }

    private void renderFlash(GuiGraphicsExtractor guiGraphics) {
        if (flashNode == null || flashTime <= 0) return;
        int cx = worldToScreenX(flashNode.x);
        int cy = worldToScreenY(flashNode.y);
        int alpha = (int)(flashTime * 255) & 0xFF;
        int half = (int)(flashNode.size * zoom * (1 + (1 - flashTime) * 2.5f) / 2);
        guiGraphics.fill(cx - half, cy - half, cx + half, cy - half + 2, (alpha << 24) | 0x00FFFFFF);
        guiGraphics.fill(cx - half, cy + half - 2, cx + half, cy + half, (alpha << 24) | 0x00FFFFFF);
        guiGraphics.fill(cx - half, cy - half, cx - half + 2, cy + half, (alpha << 24) | 0x00FFFFFF);
        guiGraphics.fill(cx + half - 2, cy - half, cx + half, cy + half, (alpha << 24) | 0x00FFFFFF);
        int nh = (int)(flashNode.size * zoom / 2);
        guiGraphics.fill(cx - nh, cy - nh, cx + nh, cy + nh, (alpha << 24) | 0x00FFFFFF);
    }

    private void renderPoints(GuiGraphicsExtractor guiGraphics) {
        String text = "Points: " + ClientState.getPoints();
        int w = this.font.width(text) + 16;
        int x = this.width - w - 10, y = 10;
        guiGraphics.fill(x, y, x + w, y + 20, 0x80000000);
        guiGraphics.fill(x, y, x + 1, y + 20, 0xFFAAAAFF);
        guiGraphics.text(this.font,
                Component.literal(text),
                x + 8, y + 6, 0xFF55FFFF, true);
    }

    private void renderTooltip(GuiGraphicsExtractor guiGraphics, SkillNode n, int mx, int my) {
        if (n == null) return;
        int pad = 6;
        int lineH = 11;

        String nameStr = n.name;
        String descStr = n.description;
        String costStr = "Cost: " + n.cost;
        String pointsStr = "Your points: " + ClientState.getPoints();

        String condStr = null;
        String condStr2 = null;
        boolean conditionMet = false;
        boolean conditionMet2 = false;
        if (!n.unlocked && n.conditionText != null && !n.conditionText.isEmpty()) {
            if ("first_steps".equals(n.id)) {
                int progress = ClientState.getProgress("first_steps");
                condStr = "Condition: " + n.conditionText + " (" + progress + "/5)";
                conditionMet = (progress >= 5);
            } else if ("sticky".equals(n.id)) {
                int progress = ClientState.getProgress("sticky");
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
            } else             if ("stone_1".equals(n.id)) {
                SkillNode axeNode = nodes.get("axe_node");
                boolean axeDone = axeNode != null && axeNode.unlocked;
                condStr = "Condition: " + n.conditionText + (axeDone ? " (Done)" : "");
                conditionMet = axeDone;
                List<SkillNode> branchLeaves = SkillNodeRegistry.getLeavesOfBranch(SkillNodeRegistry.BRANCH_TUTORIAL);
                int total = branchLeaves.size();
                int done = 0;
                for (SkillNode ln : branchLeaves) {
                    if (ClientState.getUnlockedNodes().contains(ln.id)) done++;
                }
                condStr2 = "Condition: Upgrade tutorial branch (" + done + "/" + total + ")";
                conditionMet2 = (done >= total);
            } else if ("tech_1".equals(n.id)) {
                conditionMet = ClientState.getProgress("seedy_place") >= 1;
                condStr = "Condition: " + n.conditionText + (conditionMet ? " (Done)" : "");
            } else {
                condStr = "Condition: " + n.conditionText;
            }
        }

        String sacStr = null;
        boolean sacrificeDone = false;
        if (!n.sacrifices.isEmpty()) {
            int[] prog = ClientState.getSacrificeProgress(n.id);
            StringBuilder req = new StringBuilder();
            boolean allMet = true;
            for (int i = 0; i < n.sacrifices.size(); i++) {
                SacrificeRequirement r = n.sacrifices.get(i);
                int cur = i < prog.length ? prog[i] : 0;
                if (cur < r.amount()) allMet = false;
                if (req.length() > 0) req.append(", ");
                req.append(cur).append("/").append(r.amount());
                if (!r.anyWood()) req.append("x ").append(r.item().getName(new ItemStack(r.item())).getString());
            }
            sacStr = "Sacrifice: " + req;
            sacrificeDone = allMet;
        }

        String grantsStr = null;
        String unlocksStr = null;
        int extraLines = 0;
        if (expandProgress > 0.01f) {
            grantsStr = n.detailedDescription;
            extraLines++;
            StringBuilder children = new StringBuilder();
            for (SkillNode child : nodes.values()) {
                if (n.id.equals(child.parentId)) {
                    if (children.length() > 0) children.append(", ");
                    children.append(isNodeVisible(child) ? child.name : "???");
                }
            }
            if (children.length() > 0) {
                unlocksStr = "Unlocks: " + children;
                extraLines++;
            }
        }

        int wName = this.font.width(nameStr);
        int wDesc = this.font.width(descStr);
        int wCost = this.font.width(costStr);
        int wPts  = this.font.width(pointsStr);
        int wCond = condStr != null ? this.font.width(condStr) : 0;
        int prefixW = this.font.width("Condition: ");
        int wCond2 = condStr2 != null ? this.font.width(condStr2) : 0;
        int wSac = sacStr != null ? this.font.width(sacStr) : 0;
        int wGrants = grantsStr != null ? this.font.width(grantsStr) : 0;
        int wUnlocks = unlocksStr != null ? this.font.width(unlocksStr) : 0;

        int maxW = Math.max(Math.max(Math.max(wName, wDesc), wCost), Math.max(wPts, Math.max(Math.max(wCond, wCond2), Math.max(Math.max(wGrants, wUnlocks), wSac))));
        int w = maxW + pad * 2;
        int baseLines = 4 + (condStr != null ? 1 : 0) + (condStr2 != null ? 1 : 0) + (sacStr != null ? 1 : 0);
        float animatedExtraLines = extraLines * expandProgress;
        int h = (int)(lineH * (baseLines + animatedExtraLines) + pad * 2);

        int nodeScreenX = worldToScreenX(n.x);
        int nodeScreenY = worldToScreenY(n.y);
        int halfNode = (int)(n.size * zoom / 2);

        int x = nodeScreenX - w / 2;
        int y = nodeScreenY - halfNode - h - 8;
        if (y < 4) {
            y = nodeScreenY + halfNode + 8;
        }
        if (x < 4) x = 4;
        if (x + w > this.width - 4) x = this.width - w - 4;
        int bottom = y + h;

        guiGraphics.fill(x, y, x + w, y + h, 0xF0000000);
        guiGraphics.fill(x, y, x + w, y + 1, 0xFFAAAAFF);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, 0xFFAAAAFF);
        guiGraphics.fill(x, y, x + 1, y + h, 0xFFAAAAFF);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, 0xFFAAAAFF);

        int textY = y + pad;
        guiGraphics.text(this.font,
                Component.literal(nameStr).withStyle(n.unlocked ? ChatFormatting.GREEN : ChatFormatting.WHITE),
                x + pad, textY, 0xFFFFFFFF, true);
        textY += lineH;
        guiGraphics.text(this.font,
                Component.literal(descStr).withStyle(ChatFormatting.GRAY),
                x + pad, textY, 0xFFAAAAAA, true);
        textY += lineH;
        guiGraphics.text(this.font,
                Component.literal(costStr).withStyle(ChatFormatting.GOLD),
                x + pad, textY, 0xFFFFAA00, true);
        textY += lineH;
        if (condStr != null) {
            int condColor = conditionMet ? 0xFF55FF55 : 0xFFFF5555;
            guiGraphics.text(this.font, Component.literal(condStr), x + pad, textY, condColor, true);
            textY += lineH;
        }
        if (condStr2 != null) {
            int condColor2 = conditionMet2 ? 0xFF55FF55 : 0xFFFF5555;
            guiGraphics.text(this.font, Component.literal(condStr2), x + pad, textY, condColor2, true);
            textY += lineH;
        }
        if (sacStr != null) {
            int sacColor = sacrificeDone ? 0xFF55FF55 : 0xFFFF5555;
            guiGraphics.text(this.font, Component.literal(sacStr), x + pad, textY, sacColor, true);
            textY += lineH;
        }
        guiGraphics.text(this.font, Component.literal(pointsStr), x + pad, textY, 0xFF55FFFF, true);

        int extraTextY = textY + lineH;
        if (grantsStr != null && expandProgress > 0.01f) {
            if (extraTextY + lineH <= bottom) {
                guiGraphics.text(this.font,
                        Component.literal(grantsStr).withStyle(ChatFormatting.AQUA),
                        x + pad, extraTextY, 0xFF55FFFF, true);
            }
            extraTextY += lineH;
        }
        if (unlocksStr != null && expandProgress > 0.5f) {
            if (extraTextY + lineH <= bottom) {
                guiGraphics.text(this.font,
                        Component.literal(unlocksStr).withStyle(ChatFormatting.LIGHT_PURPLE),
                        x + pad, extraTextY, 0xFFFF55FF, true);
            }
        }
    }

    private void renderReturnButton(GuiGraphicsExtractor guiGraphics) {
        if (returnAlpha < 0.02f) return;
        int a = (int)(returnAlpha * 255) & 0xFF;
        int r = 16;
        int x1 = returnX - r, y1 = returnY - r, x2 = returnX + r, y2 = returnY + r;
        guiGraphics.fill(x1, y1, x2, y2, (a << 24) | 0x00404048);
        guiGraphics.fill(x1, y1, x2, y1 + 1, (a << 24) | 0x00FFFFFF);
        guiGraphics.fill(x1, y2 - 1, x2, y2, (a << 24) | 0x00FFFFFF);
        guiGraphics.fill(x1, y1, x1 + 1, y2, (a << 24) | 0x00FFFFFF);
        guiGraphics.fill(x2 - 1, y1, x2, y2, (a << 24) | 0x00FFFFFF);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(returnX, returnY);
        guiGraphics.pose().rotate(returnAngle);
        guiGraphics.pose().scale(1.5F, 1.5F);
        guiGraphics.text(this.font,
                Component.literal(">"), -this.font.width(">") / 2, -4,
                (a << 24) | 0x00FFFFFF);
        guiGraphics.pose().popMatrix();
    }

    private void renderHud(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.text(this.font,
                Component.literal(String.format("Zoom: %.0f%%", zoom * 100)).withStyle(ChatFormatting.WHITE),
                10, 10, 0xFFFFFF, true);
        guiGraphics.text(this.font,
                Component.literal(String.format("Cam:  %.0f, %.0f", cameraX, cameraY)).withStyle(ChatFormatting.WHITE),
                10, 22, 0xFFFFFF, true);
        guiGraphics.text(this.font,
                Component.literal("WASD/LMB=pan | Shift=fast | Wheel=zoom | HOLD node=upgrade | ESC=close")
                        .withStyle(ChatFormatting.GRAY),
                10, this.height - 35, 0xFFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private boolean isCtrlHeld() {
        if (this.minecraft == null) return false;
        return InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(this.minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
}