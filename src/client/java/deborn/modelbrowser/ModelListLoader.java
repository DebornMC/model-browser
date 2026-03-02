package deborn.modelbrowser;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModelListLoader {
    
    public List<ItemStack> allModelStacks = new ArrayList<>();
    public static void loadAsync() {
        new Thread(() -> loadModels(MinecraftClient.getInstance().getResourceManager())).start();
    }

    private static void loadModels(ResourceManager manager) {
        List<ItemStack> stacks = new ArrayList<>();

        try {
            for (String namespace : manager.getAllNamespaces()) {
                Map<Identifier, Resource> resources = manager.findResources("items", path -> path.getPath().endsWith(".json"));

                for (Identifier resourceId : resources.keySet()) {
                    if (!resourceId.getNamespace().equals(namespace)) continue;
                    Resource resource = resources.get(resourceId);

                    JsonElement element;
                    try (BufferedReader reader = resource.getReader()) {
                        element = JsonParser.parseReader(reader);
                    }

                    if (!element.isJsonObject()) continue;
                    JsonObject obj = element.getAsJsonObject();

                    if (obj.has("unlisted") && obj.get("unlisted").getAsBoolean()) {
                        continue;
                    }

                    if (obj.has("model")) {
                        JsonObject modelObj = obj.getAsJsonObject("model");

                        if (modelObj.has("type")
                            && modelObj.get("type").getAsString().equals("minecraft:select")
                            && modelObj.has("property")
                            && modelObj.get("property").getAsString().equals("minecraft:component")
                            && modelObj.has("component")
                            && modelObj.get("component").getAsString().equals("minecraft:custom_name")
                            && modelObj.has("cases")) {

                            String path = resourceId.getPath();
                            String itemId = path.substring("items/".length(), path.length() - ".json".length());
                            Identifier itemIdentifier = Identifier.tryParse(namespace + ":" + itemId);
                            if (itemIdentifier == null) continue;

                            for (JsonElement caseEl : modelObj.getAsJsonArray("cases")) {
                                JsonObject caseObj = caseEl.getAsJsonObject();
                                if (!caseObj.has("when")) continue;

                                Item item = Registries.ITEM.get(itemIdentifier);
                                ItemStack stack = new ItemStack(item);

                                String when = caseObj.get("when").getAsString();
                                stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(when));

                                NbtCompound compound = new NbtCompound();
                                compound.putBoolean("model_browser_data", true);
                                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(compound));

                                stacks.add(stack);
                            }
                        }
                    }
                    if (namespace.equals("minecraft")) continue;
                    
                    String path = resourceId.getPath();
                    if (!path.startsWith("items/") || !path.endsWith(".json")) continue;

                    String itemId = path.substring("items/".length(), path.length() - ".json".length());
                    Identifier itemIdentifier = Identifier.tryParse(namespace + ":" + itemId);
                    if (itemIdentifier == null) continue;

                    ItemStack stack = new ItemStack(Items.IRON_NUGGET);
                    stack.set(DataComponentTypes.ITEM_MODEL, itemIdentifier);
                    stack.set(DataComponentTypes.ITEM_NAME, Text.literal(itemIdentifier.toString()));
                    
                    EquippableComponent equippable = EquippableComponent.builder(EquipmentSlot.HEAD).build();
                    stack.set(DataComponentTypes.EQUIPPABLE, equippable);

                    NbtCompound compound = new NbtCompound();
                    compound.putBoolean("model_browser_data", true);
                    NbtComponent customData = NbtComponent.of(compound);
                    stack.set(DataComponentTypes.CUSTOM_DATA, customData);

                    stacks.add(stack);
                }
            }
                
            
            ModelBrowser.LOGGER.info("Loaded " + stacks.size() + " models");
                MinecraftClient.getInstance().execute(() -> {
                    ModelListData.setStacks(stacks);
                });
        } catch (Exception e) {
            ModelBrowser.LOGGER.error("Failed to load models!");
            e.printStackTrace();
        }
    }
}
