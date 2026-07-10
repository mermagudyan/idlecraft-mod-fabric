package io.github.mermagudyan.idlecraft.client.screen;

import io.github.mermagudyan.idlecraft.screen.SkillNode;
import io.github.mermagudyan.idlecraft.screen.SkillNodeRegistry;
import io.github.mermagudyan.idlecraft.screen.SacrificeRequirement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Util;
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
import io.github.mermagudyan.idlecraft.network.RepairTryPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import java.util.ArrayList;
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
    private SkillNode pendingSacrificeNode = null;

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

    private boolean needsCameraAnim(SkillNode n) {
        float dx = n.x - cameraX;
        float dy = n.y - cameraY;
        return (dx * dx + dy * dy) > 400.0f;
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

        String reportLabel = "Report Bugs";
        int reportW = this.font.width(reportLabel) + 16;
        this.reportBtnX = this.width - reportW - 6;
        this.reportBtnY = this.height - 25;
        this.reportBtnW = reportW;
        this.addRenderableWidget(Button.builder(
                Component.literal(reportLabel),
                b -> {
                    if (this.minecraft != null) {
                        String url = "https://github.com/mermagudyan/idlecraft-mod-fabric/discussions/categories/bug-finders";
                        try {
                            Util.getPlatform().openUri(java.net.URI.create(url));
                        } catch (Exception e) {
                            if (this.minecraft.keyboardHandler != null) {
                                this.minecraft.keyboardHandler.setClipboard(url);
                            }
                            if (this.minecraft.player != null) {
                                this.minecraft.player.sendSystemMessage(
                                        Component.literal("[Idlecraft] Could not open browser. Link copied to clipboard."));
                            }
                        }
                    }
                }
        ).bounds(this.reportBtnX, this.reportBtnY, reportW, 20).build());
    }

    private boolean canUnlock(SkillNode n) {
        if (n.unlocked) return false;
        if (!isNodeVisible(n)) return false;
        if (isExcluded(n)) return false;
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
        if ("cobblestone".equals(n.id)) {
            return ClientState.getProgress("cobblestone") >= 18;
        }
        if ("stonecutter".equals(n.id)) {
            return ClientState.getProgress("stonecutter") >= 8;
        }
        if ("coal_knowledge".equals(n.id)) {
            return ClientState.getProgress("furnace_opened") >= 1
                    && ClientState.getProgress("food_cooked") >= 1;
        }
        if ("burning_knowledge".equals(n.id)) {
            return ClientState.getProgress("furnace_takes") >= 5;
        }
        if ("smoking_rack".equals(n.id)) {
            return ClientState.getUnlockedNodes().contains("stone_tools");
        }
        if ("cave_explorer".equals(n.id)) {
            return ClientState.getProgress("cave_dark_damage") >= 1;
        }
        if ("light_up".equals(n.id)) {
            return ClientState.getProgress("cave_hunger_damage") >= 1;
        }
        if ("guardian".equals(n.id)) {
            boolean stone = ClientState.getUnlockedNodes().contains("stone_1");
            boolean day = ClientState.getProgress("days_survived") >= 1;
            return stone && day;
        }
        if ("cave_master".equals(n.id)) {
            return ClientState.getUnlockedNodes().contains("decorative");
        }
        if ("good_caster".equals(n.id)) {
            return ClientState.getProgress("crafted_quality") >= 1;
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
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
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
                if (isUpgradePressed(event)) {
                    tryUpgrade(hoverNode);
                    return true;
                }
                if (isSacrificePressed(event)) {
                    trySacrifice(hoverNode);
                    return true;
                }
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (super.mouseClicked(event, doubleClick)) return true;
                dragging = true;
                lastDragX = mx; lastDragY = my;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isUpgradePressed(net.minecraft.client.input.MouseButtonEvent event) { return io.github.mermagudyan.idlecraft.client.ModKeyBindings.upgradeKey.matchesMouse(event); }
    private boolean isSacrificePressed(net.minecraft.client.input.MouseButtonEvent event) { return io.github.mermagudyan.idlecraft.client.ModKeyBindings.sacrificeKey.matchesMouse(event); }

    private void tryUpgrade(SkillNode n) {
        if (!isNodeVisible(n) || isExcluded(n)) return;
        if (canUnlock(n)) {
            if (needsCameraAnim(n)) startCameraAnim(n.x, n.y);
            pressedNode = n;
            holdProgress = 0.0f;
        } else {
            // Condition not met or sacrifice incomplete: always play the centring animation.
            startCameraAnim(n.x, n.y);
            if (!n.sacrifices.isEmpty()) {
                if (n.repairSeconds > 0 && isSacrificeComplete(n) && !ClientState.isRepairSucceeded(n.id)) {
                    long start = ClientState.getRepairStart(n.id);
                    long elapsed = (System.currentTimeMillis() - start) / 1000L;
                    if (start > 0 && elapsed >= n.repairSeconds) {
                        ClientPlayNetworking.send(new RepairTryPayload(n.id));
                    }
                    return;
                }
                pendingSacrificeNode = n;
            }
        }
    }

    private boolean isSacrificeComplete(SkillNode n) {
        int[] prog = ClientState.getSacrificeProgress(n.id);
        for (int i = 0; i < n.sacrifices.size(); i++) {
            int c = i < prog.length ? prog[i] : 0;
            if (c < n.sacrifices.get(i).amount()) return false;
        }
        return true;
    }

    private void trySacrifice(SkillNode n) {
        if (!isNodeVisible(n) || isExcluded(n)) return;
        if (n.sacrifices.isEmpty()) return;
        pendingSacrificeNode = n;
        if (needsCameraAnim(n)) startCameraAnim(n.x, n.y);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        dragging = false;
        if (pressedNode != null) {
            pressedNode = null;
            holdProgress = 0.0f;
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

        if (!animatingCamera && pendingSacrificeNode != null) {
            ClientPlayNetworking.send(new SacrificeOfferPayload(pendingSacrificeNode.id));
            pendingSacrificeNode = null;
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
            if (hoverNode == pressedNode && !animatingCamera) {
                holdProgress += 1.0f / HOLD_TICKS;
                if (holdProgress >= 1.0f) {
                    if (canUnlock(pressedNode)) {
                        unlockNode(pressedNode);
                    }
                    pressedNode = null;
                    holdProgress = 0.0f;
                }
            } else if (animatingCamera) {
                // wait until camera finishes centering before counting the hold
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

        if (n.single) {
            if ("light_up".equals(n.id)) {
                return ClientState.getProgress("cave_hunger_damage") >= 1;
            }
            if ("guardian".equals(n.id)) {
                return ClientState.getUnlockedNodes().contains("stone_1")
                        && ClientState.getProgress("days_survived") >= 1;
            }
            if ("copper_start".equals(n.id)) {
                return isStoneBranchComplete();
            }
            return false;
        }

        if (SkillNodeRegistry.BRANCH_COPPER.equals(n.branch)) {
            if (!isStoneBranchComplete()) return false;
        }

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

        if (n.id.equals("stonecutter")) {
            SkillNode parent = nodes.get(n.parentId);
            return parent != null && parent.unlocked;
        }

        if (n.id.equals("cave_master")) {
            SkillNode parent = nodes.get(n.parentId);
            SkillNode deco = nodes.get("decorative");
            return parent != null && parent.unlocked && deco != null && deco.unlocked;
        }

        if (n.parentId == null) return false;

        SkillNode parent = nodes.get(n.parentId);
        if (parent == null || !parent.unlocked) {
            return false;
        }
        return true;
    }

    private boolean isStoneBranchComplete() {
        for (SkillNode n : SkillNodeRegistry.getAll()) {
            if (SkillNodeRegistry.BRANCH_STONE.equals(n.branch)) {
                if (!n.unlocked && !isExcluded(n)) return false;
            }
        }
        return true;
    }

    private boolean isExcluded(SkillNode n) {
        if ("stone_tools".equals(n.id) && ClientState.getUnlockedNodes().contains("durability")) return true;
        if ("durability".equals(n.id) && ClientState.getUnlockedNodes().contains("stone_tools")) return true;
        return false;
    }

    private void renderConnections(GuiGraphicsExtractor guiGraphics) {
        for (SkillNode n : nodes.values()) {
            if (n.single) continue;
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
        boolean excluded = isExcluded(n);

        if (pressedNode == n && holdProgress > 0 && !masked) {
            int bloom = (int)(half * (1 + 0.4f * holdProgress));
            int bloomAlpha = (int)(holdProgress * 100);
            guiGraphics.fill(cx - bloom, cy - bloom, cx + bloom, cy + bloom, (bloomAlpha << 24) | 0x00FFFFFF);
        }

        int fill = n.unlocked ? (dim ? 0xFF182820 : 0xE0205030)
                : (masked ? (dim ? 0xFF141414 : 0xE01A1A1A)
                    : (excluded ? (dim ? 0xFF0E0E0E : 0xE0141414)
                        : (hovered ? 0xE0404048 : (dim ? 0xFF181820 : 0xE0282830))));
        guiGraphics.fill(x1, y1, x2, y2, fill);

        int border = n.unlocked ? (dim ? 0xFF305530 : 0xFF55FF55)
                : (masked ? (dim ? 0xFF333333 : 0xFF555555)
                    : (excluded ? (dim ? 0xFF222222 : 0xFF444444)
                        : (pressedNode == n ? (dim ? 0xFFCCCCCC : 0xFFFFFFFF)
                            : (hovered ? (dim ? 0xFFCCCCCC : 0xFFFFFFFF) : (dim ? 0xFF444444 : 0xFF888888)))));
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

        List<String> condLines = new ArrayList<>();
        boolean allCondMet = true;
        if (!n.unlocked && n.conditionText != null && !n.conditionText.isEmpty()) {
            if ("first_steps".equals(n.id)) {
                int progress = ClientState.getProgress("first_steps");
                condLines.add("Get 5 sticks (" + progress + "/5)");
                allCondMet = (progress >= 5);
            } else if ("sticky".equals(n.id)) {
                int progress = ClientState.getProgress("sticky");
                condLines.add("Get 5 wood (" + progress + "/5)");
                allCondMet = (progress >= 5);
            } else if ("village_visit".equals(n.id)) {
                boolean m = ClientState.getProgress("crafting_table_unlock") >= 1;
                condLines.add("Visit a village" + (m ? " (Done)" : ""));
                allCondMet = m;
            } else if ("wooden_tools".equals(n.id)) {
                int progress = ClientState.getProgress("wood_mined");
                condLines.add("Mine 15 wood (" + progress + "/15)");
                allCondMet = (progress >= 15);
            } else if ("axe_node".equals(n.id)) {
                int progress = ClientState.getProgress("wood_mined_axe");
                condLines.add("Mine 16 wood with an axe (" + progress + "/16)");
                allCondMet = (progress >= 16);
            } else if ("stone_1".equals(n.id)) {
                SkillNode axeNode = nodes.get("axe_node");
                boolean axeDone = axeNode != null && axeNode.unlocked;
                condLines.add("Unlock the axe branch" + (axeDone ? " (Done)" : ""));
                List<SkillNode> branchLeaves = SkillNodeRegistry.getLeavesOfBranch(SkillNodeRegistry.BRANCH_TUTORIAL);
                int total = branchLeaves.size();
                int done = 0;
                for (SkillNode ln : branchLeaves) {
                    if (ClientState.getUnlockedNodes().contains(ln.id)) done++;
                }
                condLines.add("Upgrade the tutorial branch (" + done + "/" + total + ")");
                allCondMet = axeDone && (done >= total);
            } else if ("tech_1".equals(n.id)) {
                boolean m = ClientState.getProgress("seedy_place") >= 1;
                condLines.add("Earn 'A Seedy Place'" + (m ? " (Done)" : ""));
                allCondMet = m;
            } else if ("cobblestone".equals(n.id)) {
                int progress = ClientState.getProgress("cobblestone");
                condLines.add("Mine 18 stone (" + progress + "/18)");
                allCondMet = (progress >= 18);
            } else if ("stonecutter".equals(n.id)) {
                int progress = ClientState.getProgress("stonecutter");
                condLines.add("Craft 8 planks from a stonecutter (" + progress + "/8)");
                allCondMet = (progress >= 8);
            } else if ("coal_knowledge".equals(n.id)) {
                int opened = ClientState.getProgress("furnace_opened");
                int cooked = ClientState.getProgress("food_cooked");
                condLines.add("Enter a furnace (" + opened + "/1)");
                condLines.add("Cook any food (" + cooked + "/1)");
                allCondMet = (opened >= 1 && cooked >= 1);
            } else if ("burning_knowledge".equals(n.id)) {
                int takes = ClientState.getProgress("furnace_takes");
                condLines.add("Take items from the furnace output (" + takes + "/5)");
                allCondMet = (takes >= 5);
            } else if ("smoking_rack".equals(n.id)) {
                boolean m = ClientState.getUnlockedNodes().contains("stone_tools");
                condLines.add("Unlock Stone Tools" + (m ? " (Done)" : ""));
                allCondMet = m;
            } else if ("cave_explorer".equals(n.id)) {
                int p = ClientState.getProgress("cave_dark_damage");
                condLines.add("Take damage from the unknown (" + p + "/1)");
                allCondMet = (p >= 1);
            } else if ("light_up".equals(n.id)) {
                int p = ClientState.getProgress("cave_hunger_damage");
                condLines.add("Take damage from Underground Starvation (" + p + "/1)");
                allCondMet = (p >= 1);
            } else if ("guardian".equals(n.id)) {
                boolean stone = ClientState.getUnlockedNodes().contains("stone_1");
                int day = ClientState.getProgress("days_survived");
                condLines.add("Unlock Miner I" + (stone ? " (Done)" : ""));
                condLines.add("Survive 1 day (" + day + "/1)");
                allCondMet = stone && (day >= 1);
            } else if ("cave_master".equals(n.id)) {
                boolean m = ClientState.getUnlockedNodes().contains("decorative");
                condLines.add("Complete Decorative Diversity" + (m ? " (Done)" : ""));
                allCondMet = m;
            } else if ("good_caster".equals(n.id)) {
                int p = ClientState.getProgress("crafted_quality");
                condLines.add("Craft an item of any quality (" + p + "/1)");
                allCondMet = (p >= 1);
            } else {
                condLines.add(n.conditionText);
                allCondMet = true;
            }
        }

        List<String> sacLines = new ArrayList<>();
        boolean allSacMet = true;
        if (!n.sacrifices.isEmpty()) {
            int[] prog = ClientState.getSacrificeProgress(n.id);
            for (int i = 0; i < n.sacrifices.size(); i++) {
                SacrificeRequirement r = n.sacrifices.get(i);
                int cur = i < prog.length ? prog[i] : 0;
                if (cur < r.amount()) allSacMet = false;
                String label = r.anyWood() ? "any wood"
                        : r.anyPlanks() ? "any planks"
                        : r.item().getName(new ItemStack(r.item())).getString();
                sacLines.add(cur + "/" + r.amount() + "x " + label);
            }
        }

        String lockStr = null;
        if (("stone_tools".equals(n.id) || "durability".equals(n.id)) && !n.unlocked) {
            int secs = ClientState.getProgress("stone_lock");
            if (secs > 0) {
                int m = secs / 60;
                int s = secs % 60;
                lockStr = "Branch locked: " + m + "m " + String.format("%02d", s) + "s";
            }
        }

        String repairStr = null;
        if (n.repairSeconds > 0 && !n.unlocked) {
            if (isSacrificeComplete(n)) {
                if (ClientState.isRepairSucceeded(n.id)) {
                    repairStr = "Repair complete - HOLD to unlock";
                } else {
                    long start = ClientState.getRepairStart(n.id);
                    if (start <= 0) {
                        repairStr = "Repair not started";
                    } else {
                        long elapsed = (System.currentTimeMillis() - start) / 1000L;
                        long remain = Math.max(0L, (long) n.repairSeconds - elapsed);
                        repairStr = remain > 0
                                ? "Repairing... " + remain + "s"
                                : "Click to attempt repair (90% chance)";
                    }
                }
            }
        }

        int MAX_W = 260;
        int condPrefixW = this.font.width("Condition: ");
        int sacPrefixW = this.font.width("Sacrifice: ");

        List<String> descWrap = wrapLine(descStr, MAX_W);
        List<List<String>> condGroups = new ArrayList<>();
        int condVisual = 0;
        for (String raw : condLines) {
            List<String> g = wrapLine(raw, MAX_W - condPrefixW);
            condGroups.add(g);
            condVisual += g.size();
        }
        List<List<String>> sacGroups = new ArrayList<>();
        int sacVisual = 0;
        for (String raw : sacLines) {
            List<String> g = wrapLine(raw, MAX_W - sacPrefixW);
            sacGroups.add(g);
            sacVisual += g.size();
        }
        List<String> grantsWrap = new ArrayList<>();
        List<String> unlocksWrap = new ArrayList<>();
        if (expandProgress > 0.01f) {
            if (n.detailedDescription != null && !n.detailedDescription.isEmpty())
                grantsWrap = wrapLine(n.detailedDescription, MAX_W);
            StringBuilder children = new StringBuilder();
            for (SkillNode child : nodes.values()) {
                if (n.id.equals(child.parentId)) {
                    if (children.length() > 0) children.append(", ");
                    children.append(isNodeVisible(child) ? child.name : "???");
                }
            }
            if (children.length() > 0) unlocksWrap = wrapLine("Unlocks: " + children, MAX_W);
        }

        int contentW = Math.max(this.font.width(nameStr), this.font.width(costStr));
        contentW = Math.max(contentW, this.font.width(pointsStr));
        for (String l : descWrap) contentW = Math.max(contentW, this.font.width(l));
        for (List<String> g : condGroups) for (String l : g) contentW = Math.max(contentW, condPrefixW + this.font.width(l));
        for (List<String> g : sacGroups) for (String l : g) contentW = Math.max(contentW, sacPrefixW + this.font.width(l));
        for (String l : grantsWrap) contentW = Math.max(contentW, this.font.width(l));
        for (String l : unlocksWrap) contentW = Math.max(contentW, this.font.width(l));
        if (lockStr != null) contentW = Math.max(contentW, this.font.width(lockStr));
        if (repairStr != null) contentW = Math.max(contentW, this.font.width(repairStr));
        contentW = Math.min(contentW, MAX_W);

        int w = contentW + pad * 2;
        int baseLines = 1 + descWrap.size() + 1 + condVisual + sacVisual + (lockStr != null ? 1 : 0) + (repairStr != null ? 1 : 0) + 1;
        int extraLines = grantsWrap.size() + unlocksWrap.size();
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
        for (String l : descWrap) {
            guiGraphics.text(this.font,
                    Component.literal(l).withStyle(ChatFormatting.GRAY),
                    x + pad, textY, 0xFFAAAAAA, true);
            textY += lineH;
        }
        guiGraphics.text(this.font,
                Component.literal(costStr).withStyle(ChatFormatting.GOLD),
                x + pad, textY, 0xFFFFAA00, true);
        textY += lineH;

        boolean firstCond = true;
        for (List<String> g : condGroups) {
            for (String sub : g) {
                if (firstCond) {
                    Component line = Component.literal("Condition: ")
                            .withStyle(allCondMet ? ChatFormatting.GREEN : ChatFormatting.RED)
                            .append(Component.literal(sub));
                    guiGraphics.text(this.font, line, x + pad, textY, 0xFFFFFFFF, true);
                    firstCond = false;
                } else {
                    guiGraphics.text(this.font,
                            Component.literal(sub).withStyle(allCondMet ? ChatFormatting.GREEN : ChatFormatting.RED),
                            x + pad, textY, 0xFFFFFFFF, true);
                }
                textY += lineH;
            }
        }

        boolean firstSac = true;
        for (List<String> g : sacGroups) {
            for (String sub : g) {
                if (firstSac) {
                    Component line = Component.literal("Sacrifice: ")
                            .withStyle(allSacMet ? ChatFormatting.GREEN : ChatFormatting.RED)
                            .append(Component.literal(sub));
                    guiGraphics.text(this.font, line, x + pad, textY, 0xFFFFFFFF, true);
                    firstSac = false;
                } else {
                    guiGraphics.text(this.font,
                            Component.literal(sub).withStyle(allSacMet ? ChatFormatting.GREEN : ChatFormatting.RED),
                            x + pad, textY, 0xFFFFFFFF, true);
                }
                textY += lineH;
            }
        }

        if (lockStr != null) {
            guiGraphics.text(this.font, Component.literal(lockStr), x + pad, textY, 0xFFFFAA00, true);
            textY += lineH;
        }
        if (repairStr != null) {
            guiGraphics.text(this.font, Component.literal(repairStr), x + pad, textY, 0xFF55FFFF, true);
            textY += lineH;
        }
        guiGraphics.text(this.font, Component.literal(pointsStr), x + pad, textY, 0xFF55FFFF, true);

        int extraTextY = textY + lineH;
        if (grantsWrap.size() > 0 && expandProgress > 0.01f) {
            if (extraTextY + lineH <= bottom) {
                for (String l : grantsWrap) {
                    guiGraphics.text(this.font,
                            Component.literal(l).withStyle(ChatFormatting.AQUA),
                            x + pad, extraTextY, 0xFF55FFFF, true);
                    extraTextY += lineH;
                }
            }
        }
        if (unlocksWrap.size() > 0 && expandProgress > 0.5f) {
            if (extraTextY + lineH <= bottom) {
                for (String l : unlocksWrap) {
                    guiGraphics.text(this.font,
                            Component.literal(l).withStyle(ChatFormatting.LIGHT_PURPLE),
                            x + pad, extraTextY, 0xFFFF55FF, true);
                    extraTextY += lineH;
                }
            }
        }
    }

    private List<String> wrapLine(String text, int maxW) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            out.add("");
            return out;
        }
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && this.font.width(test) > maxW) {
                out.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
        }
        out.add(cur.toString());
        return out;
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