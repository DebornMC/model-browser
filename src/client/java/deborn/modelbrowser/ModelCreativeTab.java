package deborn.modelbrowser;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

public class ModelCreativeTab {
    
    public static final Identifier TAB = Identifier.tryParse(ModelBrowser.MOD_ID);
    public static final RegistryKey<ItemGroup> TAB_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, TAB);

    public static void registerTab() {
        Registry.register(
                Registries.ITEM_GROUP,
                TAB,
                FabricItemGroup.builder()
                        .displayName(Text.literal("Model Browser"))
                        .icon(() -> new ItemStack(Items.BRUSH))
                        .entries((enabled, entries) -> {
                            var stacks = ModelListData.getStacks();
                            if (!stacks.isEmpty())
                                entries.addAll(stacks);
                            else {
                                ItemStack placeholder = new ItemStack(Items.BARRIER);
                                placeholder.set(DataComponentTypes.ITEM_NAME, Text.literal("No models loaded!"));
                                placeholder.set(DataComponentTypes.CREATIVE_SLOT_LOCK, Unit.INSTANCE);
                                entries.add(placeholder);
                            }
                        })
                        .build()                
        );
    }
}
