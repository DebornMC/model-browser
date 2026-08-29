package deborn.modelbrowser.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

public final class ServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("modelbrowser-server.json");

    public static boolean itemsAlwaysEquippable = true;
    public static boolean itemsAlwaysRemoveGlint = true;

    private ServerConfig() {
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            itemsAlwaysEquippable = getBoolean(json, "items_always_equippable", true);
            itemsAlwaysRemoveGlint = getBoolean(json, "items_always_remove_glint", true);
        } catch (Exception exception) {
            System.err.println("Failed to load Model Browser server config: " + exception.getMessage());
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("items_always_equippable", itemsAlwaysEquippable);
        json.addProperty("items_always_remove_glint", itemsAlwaysRemoveGlint);

        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(json));
        } catch (IOException exception) {
            System.err.println("Failed to save Model Browser server config: " + exception.getMessage());
        }
    }

    private static boolean getBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isBoolean()
            ? json.get(key).getAsBoolean()
            : defaultValue;
    }
}