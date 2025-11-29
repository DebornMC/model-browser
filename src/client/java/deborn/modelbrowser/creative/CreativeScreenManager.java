package deborn.modelbrowser.creative;

import deborn.modelbrowser.ModelBrowserClient;
import deborn.modelbrowser.config.ModConfig;
import deborn.modelbrowser.mixin.CreativeInventoryScreenAccessor;
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
        refreshCreativeInventoryScreen(screen);
        if (!ModConfig.INSTANCE.showCreativeInventoryTab) {
            System.out.println(screen.getCurrentPage());
            screen.switchToPage(0);
            
        }
        
    }

    public static void refreshCreativeInventoryScreen(CreativeInventoryScreen screen) {
        System.out.println("refreshCreativeInventoryScreen() called");
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        client.setScreen(null);
        boolean operatorTabEnabled = ((CreativeInventoryScreenAccessor) screen).hasOperatorTabs();
        screen = new CreativeInventoryScreen(player, ModelBrowserClient.enabledFeatures, operatorTabEnabled);
        client.setScreen(screen);
    }
}