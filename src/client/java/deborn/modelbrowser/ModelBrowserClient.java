package deborn.modelbrowser;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;

public class ModelBrowserClient implements ClientModInitializer {
    private static KeyBinding openModelBrowserKey;
	private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("deborn.modelbrowser", "keybindings"));

    @Override
    public void onInitializeClient() {
        ModelBrowserScreen.INSTANCE = new ModelBrowserScreen();
        ModelBrowserReloadListener.register();
        CreativeTabRefreshHandler.register();
        ModelCreativeTab.registerTab();
        ModelListLoader.loadAsync();
    }
}
