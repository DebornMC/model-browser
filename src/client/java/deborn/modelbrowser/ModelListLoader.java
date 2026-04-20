package deborn.modelbrowser;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.Equippable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import deborn.modelbrowser.config.ModConfig;

public class ModelListLoader {
    
    public List<ItemStack> allModelStacks = new ArrayList<>();
    public static void loadAsync() {
        new Thread(() -> loadModels(Minecraft.getInstance().getResourceManager())).start();
    }
    
    private static void loadModels(ResourceManager manager) {
        List<ItemStack> stacks = new ArrayList<>();

        try {
            for (String namespace : manager.getNamespaces()) {
                Map<Identifier, Resource> resources = manager.listResources("items", path -> path.getPath().endsWith(".json"));

                for (Identifier resourceId : resources.keySet()) {
                    if (!resourceId.getNamespace().equals(namespace)) continue;
                    Resource resource = resources.get(resourceId);

                    JsonElement element;
                    try (BufferedReader reader = resource.openAsReader()) {
                        element = JsonParser.parseReader(reader);
                    }

                    if (!element.isJsonObject()) continue;
                    JsonObject obj = element.getAsJsonObject();

                    if (obj.has("unlisted") && obj.get("unlisted").getAsBoolean()) {
                        continue;
                    }
                    
                    // Vanilla CIT model definition
                    if (ModConfig.INSTANCE.showRenameableItems) {
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

                                    Item item = BuiltInRegistries.ITEM.getValue(itemIdentifier);
                                    ItemStack stack = new ItemStack(item);

                                    String when = caseObj.get("when").getAsString();
                                    stack.set(DataComponents.CUSTOM_NAME, Component.literal(when));

                                    CompoundTag compound = new CompoundTag();
                                    compound.putBoolean("model_browser_data", true);
                                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(compound));

                                    stacks.add(stack);
                                }
                            }
                        }
                    }
                    if (namespace.equals("minecraft")) continue;
                    
                    // Legacy item model definition
                    if (ModConfig.INSTANCE.showItemModelDefinitionItems) {
                        String path = resourceId.getPath();
                        if (!path.startsWith("items/") || !path.endsWith(".json")) continue;

                        String itemId = path.substring("items/".length(), path.length() - ".json".length());
                        Identifier itemIdentifier = Identifier.tryParse(namespace + ":" + itemId);
                        if (itemIdentifier == null) continue;

                        ItemStack stack = new ItemStack(Items.IRON_NUGGET);
                        stack.set(DataComponents.ITEM_MODEL, itemIdentifier);
                        stack.set(DataComponents.ITEM_NAME, Component.literal(itemIdentifier.toString()));
                        
                        Equippable equippable = Equippable.builder(EquipmentSlot.HEAD).build();
                        stack.set(DataComponents.EQUIPPABLE, equippable);

                        CompoundTag compound = new CompoundTag();
                        compound.putBoolean("model_browser_data", true);
                        CustomData customData = CustomData.of(compound);
                        stack.set(DataComponents.CUSTOM_DATA, customData);

                        stacks.add(stack);
                    }
                }
            }
                
            
            ModelBrowser.LOGGER.info("Loaded " + stacks.size() + " models");
                Minecraft.getInstance().execute(() -> {
                    ModelListData.setStacks(stacks);
                });
        } catch (Exception e) {
            ModelBrowser.LOGGER.error("Failed to load models!");
            e.printStackTrace();
        }
    }
}
