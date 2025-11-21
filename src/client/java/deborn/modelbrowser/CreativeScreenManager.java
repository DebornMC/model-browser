package deborn.modelbrowser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;

public class CreativeScreenManager {
    
    private static boolean refreshPending = false;

    public static void markRefreshPending() {
        refreshPending = true;
    }

    public static void onCreativeScreenOpened(CreativeInventoryScreen screen) {
        if (refreshPending) {
            refreshPending = false;
            refreshCreativeInventoryScreen();
        }
    }

    public static void refreshCreativeInventoryScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null)
            return;
        client.setScreen(null);
        client.setScreen(new CreativeInventoryScreen(player, ModelBrowserClient.enabledFeatures, true));
    }
}