package deborn.modelbrowser;

import deborn.modelbrowser.config.ModConfig;
import deborn.modelbrowser.creative.CreativeScreenManager;
import deborn.modelbrowser.creative.ModelCreativeTab;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.fabricmc.loader.api.FabricLoader;

public class ModelBrowserClient implements ClientModInitializer {
    public static FeatureSet enabledFeatures = FeatureFlags.FEATURE_MANAGER.getFeatureSet();
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("modmenu") && FabricLoader.getInstance().isModLoaded("cloth-config")) {
            ModConfig.register();
        }
        ModelBrowserReloadListener.register();
        ModelCreativeTab.register();

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof CreativeInventoryScreen creativeScreen) {
                CreativeScreenManager.onCreativeScreenOpened(creativeScreen);
            }
        });
    }
}
