package deborn.modelbrowser;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

public class CreativeTabRefreshHandler {

    private static ItemGroup lastTab = null;
    private static long lastRefreshTick = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!(client.currentScreen instanceof CreativeInventoryScreen cis)) {
                lastTab = null;
                return;
            }

            try {
                Field selectedTabField = CreativeInventoryScreen.class.getDeclaredField("selectedTab");
                selectedTabField.setAccessible(true);
                ItemGroup selectedTab = (ItemGroup) selectedTabField.get(cis);

                ItemGroup modelTab = Registries.ITEM_GROUP.get(ModelCreativeTab.TAB_KEY);
                if (selectedTab != modelTab) return;

                long currentTick = client.world.getTime();
                if (selectedTab == lastTab && currentTick == lastRefreshTick) return;

                lastTab = selectedTab;
                lastRefreshTick = currentTick;

                Collection<ItemStack> stacks = ModelListData.getStacks();
                if (stacks.isEmpty()) return;

                Method refresh = CreativeInventoryScreen.class.getDeclaredMethod("refreshSelectedTab", Collection.class);
                refresh.setAccessible(true);
                refresh.invoke(cis, stacks);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
