package deborn.modelbrowser;

import com.mojang.brigadier.arguments.BoolArgumentType;
import deborn.modelbrowser.config.ServerConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelBrowser implements ModInitializer {
	public static final String MOD_ID = "modelbrowser";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerConfig.load();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal(MOD_ID)
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
				.then(literal("items_always_equippable")
					.then(argument("value", BoolArgumentType.bool())
						.executes(context -> {
							ServerConfig.itemsAlwaysEquippable = BoolArgumentType.getBool(context, "value");
							ServerConfig.save();
							context.getSource().sendSuccess(() -> Component.literal("Model Browser: items are "
								+ (ServerConfig.itemsAlwaysEquippable ? "now" : "no longer") + " always equippable."), true);
							return 1;
						})))
				.then(literal("items_always_remove_glint")
					.then(argument("value", BoolArgumentType.bool())
						.executes(context -> {
							ServerConfig.itemsAlwaysRemoveGlint = BoolArgumentType.getBool(context, "value");
							ServerConfig.save();
							context.getSource().sendSuccess(() -> Component.literal("Model Browser: item glint is "
								+ (ServerConfig.itemsAlwaysRemoveGlint ? "now" : "no longer") + " removed by default."), true);
							return 1;
						})))
				.then(argument("value", BoolArgumentType.bool())
					.executes(context -> {
						ServerConfig.itemsAlwaysEquippable = BoolArgumentType.getBool(context, "value");
						ServerConfig.save();
						context.getSource().sendSuccess(() -> Component.literal("Model Browser: items are "
							+ (ServerConfig.itemsAlwaysEquippable ? "now" : "no longer") + " always equippable."), true);
						return 1;
					})));
		});
	}
}