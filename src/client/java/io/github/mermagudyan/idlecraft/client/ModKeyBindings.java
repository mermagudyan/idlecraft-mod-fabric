package io.github.mermagudyan.idlecraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import io.github.mermagudyan.idlecraft.client.screen.IdlecraftScreen;

public class ModKeyBindings implements ClientModInitializer {

    public static KeyMapping openPrestigeKey;

    public static final KeyMapping.Category IDLECRAFT_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("idlecraft", "categories/idlecraft"));

    @Override
    public void onInitializeClient() {
        openPrestigeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.idlecraft.prestige",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                IDLECRAFT_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            while (openPrestigeKey.consumeClick()) {
                if (minecraft.player != null) {
                    minecraft.setScreenAndShow(new IdlecraftScreen());
                }
            }
        });
    }
}