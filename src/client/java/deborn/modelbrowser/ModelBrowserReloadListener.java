package deborn.modelbrowser;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public class ModelBrowserReloadListener implements SynchronousResourceReloader {
    public static final Identifier ID = Identifier.of("modelbrowser", "model_reload");

    @Override
    public void reload(ResourceManager manager) {
        if (ModelBrowserScreen.INSTANCE == null) {
            ModelBrowserScreen.INSTANCE = new ModelBrowserScreen();
        }

        // Synchronous reload
        ModelBrowserScreen.INSTANCE.loadResourcePackItemModels(manager);
    }

    public static void register() {
        ResourceLoader.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES)
                .registerReloader(ID, new ModelBrowserReloadListener());
    }
}

