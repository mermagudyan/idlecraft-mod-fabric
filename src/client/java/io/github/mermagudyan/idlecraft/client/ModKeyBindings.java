package io.github.mermagudyan.idlecraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import io.github.mermagudyan.idlecraft.client.screen.IdlecraftScreen;

public class ModKeyBindings implements ClientModInitializer {

    public static KeyBinding openPrestigeKey;

    public static final KeyBinding.Category IDLECRAFT_CATEGORY =
            KeyBinding.Category.create(Identifier.of("idlecraft", "idlecraft"));

    @Override
    public void onInitializeClient() {
        openPrestigeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.idlecraft.prestige",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                IDLECRAFT_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPrestigeKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new IdlecraftScreen());
                }
            }
        });
    }
}