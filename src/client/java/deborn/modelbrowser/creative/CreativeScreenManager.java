package deborn.modelbrowser.creative;

import deborn.modelbrowser.ModelBrowserClient;
import deborn.modelbrowser.config.ModConfig;
import deborn.modelbrowser.mixin.CreativeInventoryScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;

public class CreativeScreenManager {
    
    private static boolean refreshPending = false;

    public static void markRefreshPending() {
        refreshPending = true;

    }

    public static void onCreativeScreenOpened(CreativeModeInventoryScreen screen) {
        if (!refreshPending) return;

        refreshPending = false;
        refreshCreativeInventoryScreen(screen);
        if (!ModConfig.INSTANCE.showCreativeInventoryTab) {
            screen.switchToPage(0);
            
        }
    }

    public static void refreshCreativeInventoryScreen(CreativeModeInventoryScreen screen) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;
        
        client.setScreen(null);
        boolean operatorTabEnabled = ((CreativeInventoryScreenAccessor) screen).hasOperatorTabs();
        screen = new CreativeModeInventoryScreen(player, ModelBrowserClient.enabledFeatures, operatorTabEnabled);
        client.setScreen(screen);
    }
}