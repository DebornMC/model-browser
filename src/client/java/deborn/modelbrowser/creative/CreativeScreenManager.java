package deborn.modelbrowser.creative;

import deborn.modelbrowser.ModelBrowser;
import deborn.modelbrowser.ModelBrowserClient;
import deborn.modelbrowser.config.ModConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;

public class CreativeScreenManager {
    
    private static boolean refreshPending = false;

    public static void markRefreshPending() {
        refreshPending = true;
        System.out.println("markRefreshPending() called");

    }

    public static void onCreativeScreenOpened(CreativeInventoryScreen screen) {
        if (!refreshPending) return;

        refreshPending = false;
        refreshCreativeInventoryScreen();
        if (!ModConfig.INSTANCE.showCreativeInventoryTab) {
            System.out.println(screen.getCurrentPage());
            screen.switchToPage(0);
        }
        
    }

    public static void refreshCreativeInventoryScreen() {
        System.out.println("refreshCreativeInventoryScreen() called");
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        client.setScreen(null);
        CreativeInventoryScreen screen = new CreativeInventoryScreen(player, ModelBrowserClient.enabledFeatures, true);
        client.setScreen(screen);
    }
}