package deborn.modelbrowser.creative;

import deborn.modelbrowser.ModelBrowser;
import deborn.modelbrowser.ModelListData;
import deborn.modelbrowser.config.ModConfig;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public class ModelCreativeTab {
    
    public static final Identifier TAB = Identifier.tryParse(ModelBrowser.MOD_ID);
    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, TAB);

    public static void register() {
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                TAB,
                FabricItemGroup.builder()
                        .title(Component.literal("Model Browser"))
                        .icon(() -> new ItemStack(Items.BRUSH))
                        .displayItems((enabled, entries) -> {
                            if (ModConfig.INSTANCE.showCreativeInventoryTab) {
                                var stacks = ModelListData.getStacks();
                                if (!stacks.isEmpty())
                                    entries.acceptAll(stacks);
                                else {
                                    ItemStack placeholder = new ItemStack(Items.BARRIER);
                                    placeholder.set(DataComponents.ITEM_NAME, Component.translatable("modelbrowser.no_models_loaded"));
                                    placeholder.set(DataComponents.CREATIVE_SLOT_LOCK, Unit.INSTANCE);
                                    placeholder.set(DataComponents.RARITY, Rarity.COMMON);
                                    entries.accept(placeholder, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                                }
                            }
                        })
                        .build()
        );
    }
}
