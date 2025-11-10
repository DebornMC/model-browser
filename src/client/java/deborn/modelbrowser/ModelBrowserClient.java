package deborn.modelbrowser;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import org.lwjgl.glfw.GLFW;

public class ModelBrowserClient implements ClientModInitializer {
    private static KeyBinding openModelBrowserKey;
	private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("deborn.modelbrowser", "keybindings"));

    @Override
    public void onInitializeClient() {
        openModelBrowserKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.deborn.modelbrowser.example", 
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,              
                CATEGORY    
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openModelBrowserKey.wasPressed()) { 
                if (client.currentScreen instanceof ModelBrowserScreen) {
                    client.setScreen(null); 
                } else if (client.currentScreen == null) {
                    client.setScreen(new ModelBrowserScreen()); 
                }
            }
        });
    }
}
