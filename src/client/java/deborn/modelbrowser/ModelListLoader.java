package deborn.modelbrowser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModelListLoader {
    
    public List<ItemStack> allModelStacks = new ArrayList<>();
    public static void loadAsync() {
        new Thread(() -> loadModels(MinecraftClient.getInstance().getResourceManager())).start();
    }

    private static void loadModels(ResourceManager manager) {
        List<ItemStack> stacks = new ArrayList<>();

        try {
            for (String namespace : manager.getAllNamespaces()) {
                if (namespace.equals("minecraft")) continue;

                Map<Identifier, ?> resources = manager.findResources("items", path -> path.getPath().endsWith(".json"));

                for (Identifier resourceId : resources.keySet()) {
                    if (!resourceId.getNamespace().equals(namespace)) continue;

                    String path = resourceId.getPath();
                    if (!path.startsWith("items/") || !path.endsWith(".json")) continue;

                    String itemId = path.substring("items/".length(), path.length() - ".json".length());
                    Identifier itemIdentifier = Identifier.tryParse(namespace + ":" + itemId);
                    if (itemIdentifier == null) continue;

                    ItemStack stack = new ItemStack(Items.WHITE_STAINED_GLASS);
                    stack.set(DataComponentTypes.ITEM_MODEL, itemIdentifier);
                    stacks.add(stack);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("[ModelBrowser] Loaded " + stacks.size() + " Models");
        MinecraftClient.getInstance().execute(() -> {
            ModelListData.setStacks(stacks);
        });
    }
}
