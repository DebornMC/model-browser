package deborn.modelbrowser.config;

import deborn.modelbrowser.creative.CreativeScreenManager;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.util.ActionResult;


@Config(name = "modelbrowser")
public class ModConfig implements ConfigData
{
	@ConfigEntry.Gui.Excluded
	public static ModConfig INSTANCE;

	public static void register() {
		AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
		var holder = AutoConfig.getConfigHolder(ModConfig.class);
		INSTANCE = holder.getConfig();
		
		holder.registerSaveListener((configHolder, config) -> {
			CreativeScreenManager.markRefreshPending();
			return ActionResult.SUCCESS;
		});
	}


	@ConfigEntry.Gui.Tooltip()
	public boolean showCreativeInventoryTab = true;

	@ConfigEntry.Gui.Tooltip()
	public boolean showAnvilScreenTab = true;
}