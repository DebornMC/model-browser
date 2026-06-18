package deborn.modelbrowser.config;

import deborn.modelbrowser.ModelListLoader;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

@Config(name = "modelbrowser")
public class AutoConfigIntegration implements ConfigData {
	public static void init() {
		AutoConfig.register(AutoConfigIntegration.class, GsonConfigSerializer::new);

		var holder = AutoConfig.getConfigHolder(AutoConfigIntegration.class);

		holder.registerLoadListener((h, config) -> {
			syncToModConfig(config);
			return InteractionResult.SUCCESS;
		});

		holder.registerSaveListener((h, config) -> {
			syncToModConfig(config);
			return InteractionResult.SUCCESS;
		});

		syncToModConfig(holder.getConfig());
	}

	private static void syncToModConfig(AutoConfigIntegration config) {
		ModConfig.INSTANCE.showCreativeInventoryTab = config.showCreativeInventoryTab;
		ModConfig.INSTANCE.showAnvilScreenTab = config.showAnvilScreenTab;
		ModConfig.INSTANCE.showItemModelDefinitionItems = config.showItemModelDefinitionItems;
		ModConfig.INSTANCE.showRenameableItems = config.showRenameableItems;
		
		Minecraft client = Minecraft.getInstance();
		if (client.level != null) {
			ModelListLoader.loadAsync();
		}
	}

	@ConfigEntry.Gui.Excluded
	private static final ModConfig DEFAULTS = new ModConfig();

	@ConfigEntry.Gui.Tooltip()
	public boolean showCreativeInventoryTab = DEFAULTS.showCreativeInventoryTab;

	@ConfigEntry.Gui.Tooltip()
	public boolean showAnvilScreenTab = DEFAULTS.showAnvilScreenTab;

	@ConfigEntry.Gui.Tooltip()
	public boolean showItemModelDefinitionItems = DEFAULTS.showItemModelDefinitionItems;

	@ConfigEntry.Gui.Tooltip()
	public boolean showRenameableItems = DEFAULTS.showRenameableItems;
}