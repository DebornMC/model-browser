package deborn.modelbrowser.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "modelbrowser")
public class AutoConfigIntegration implements ConfigData {
	public static void init() {
		AutoConfig.register(AutoConfigIntegration.class, GsonConfigSerializer::new);
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