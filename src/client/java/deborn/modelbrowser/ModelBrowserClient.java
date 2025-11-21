package deborn.modelbrowser;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;

public class ModelBrowserClient implements ClientModInitializer {
    public static FeatureSet enabledFeatures = FeatureFlags.FEATURE_MANAGER.getFeatureSet();
    @Override
    public void onInitializeClient() {
        ModelBrowserScreen.INSTANCE = new ModelBrowserScreen();
        ModelBrowserReloadListener.register();
        ModelCreativeTab.registerTab();
        ModelListLoader.loadAsync();

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof CreativeInventoryScreen creativeScreen) {
                CreativeScreenManager.onCreativeScreenOpened(creativeScreen);
            }
        });
    }
}
