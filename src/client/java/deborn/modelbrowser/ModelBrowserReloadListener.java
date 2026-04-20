package deborn.modelbrowser;

import deborn.modelbrowser.creative.CreativeScreenManager;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import java.util.Objects;

public class ModelBrowserReloadListener implements ResourceManagerReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("modelbrowser", "model_reload");
    private boolean firstReload = true;

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        if (Minecraft.getInstance().level == null)
            return;
        ModelListLoader.loadAsync();

        if (firstReload) {
            firstReload = false;
        } else {
            CreativeScreenManager.markRefreshPending();
        }
    }

    public static void register() {
        ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
                .registerReloadListener(Objects.requireNonNull(ID), new ModelBrowserReloadListener());
    }
}
