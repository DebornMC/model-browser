package deborn.modelbrowser;

import deborn.modelbrowser.config.AutoConfigIntegration;
import deborn.modelbrowser.creative.CreativeScreenManager;
import deborn.modelbrowser.creative.ModelCreativeTab;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public class ModelBrowserClient implements ClientModInitializer {
    public static FeatureFlagSet enabledFeatures = FeatureFlags.REGISTRY.allFlags();
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            AutoConfigIntegration.init();
        }
        ModelBrowserReloadListener.register();
        ModelCreativeTab.register();

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof CreativeModeInventoryScreen creativeScreen) {
                CreativeScreenManager.onCreativeScreenOpened(creativeScreen);
            }
        });
    }
}
