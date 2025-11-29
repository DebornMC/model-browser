package deborn.modelbrowser;

import deborn.modelbrowser.creative.CreativeScreenManager;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public class ModelBrowserReloadListener implements SynchronousResourceReloader {
    public static final Identifier ID = Identifier.of("modelbrowser", "model_reload");
    private boolean firstReload = true;
    
    @Override
    public void reload(ResourceManager manager) {
        ModelListLoader.loadAsync();
        ModelBrowser.LOGGER.info("Reloaded!");

        if (firstReload) {
            firstReload = false;
        } else {
            CreativeScreenManager.markRefreshPending();
        }
    }

    public static void register() {
        ResourceLoader.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloader(ID, new ModelBrowserReloadListener());
    }
}

