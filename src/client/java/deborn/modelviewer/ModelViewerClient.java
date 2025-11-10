package deborn.modelviewer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import org.lwjgl.glfw.GLFW;

public class ModelViewerClient implements ClientModInitializer {
    private static KeyBinding openModelViewerKey;
	private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("examplemod", "test"));

    @Override
    public void onInitializeClient() {
        openModelViewerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.examplemod.example", 
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,              
                CATEGORY    
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openModelViewerKey.wasPressed()) { 
                if (client.currentScreen instanceof ModelViewerScreen) {
                    client.setScreen(null); 
                } else if (client.currentScreen == null) {
                    client.setScreen(new ModelViewerScreen()); 
                }
            }
        });
    }
}
