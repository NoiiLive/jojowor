package net.noiilive.jojowor.client.skin;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.noiilive.jojowor.JoJoWoR;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SkinColorSlots {
    public static final int SLOT_COUNT = 5;

    public record SavedColors(int skin, List<Integer> colors) {}

    private static List<SavedColors> slots;

    private SkinColorSlots() {}

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("jojowor_skin_slots.json");
    }

    private static List<SavedColors> slots() {
        if (slots == null) {
            slots = load();
        }
        return slots;
    }

    private static List<SavedColors> load() {
        List<SavedColors> loaded = new ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            loaded.add(null);
        }
        try {
            Path path = file();
            if (!Files.exists(path)) {
                return loaded;
            }
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("slots");
            for (int i = 0; i < Math.min(SLOT_COUNT, array.size()); i++) {
                JsonElement element = array.get(i);
                if (element != null && element.isJsonObject()) {
                    JsonObject object = element.getAsJsonObject();
                    List<Integer> colors = new ArrayList<>();
                    for (JsonElement color : object.getAsJsonArray("colors")) {
                        colors.add(color.getAsInt() & 0xFFFFFF);
                    }
                    loaded.set(i, new SavedColors(object.get("skin").getAsInt(), List.copyOf(colors)));
                }
            }
        } catch (Exception exception) {
            JoJoWoR.LOGGER.warn("Failed to load skin color slots", exception);
        }
        return loaded;
    }

    private static void persist() {
        try {
            JsonArray array = new JsonArray();
            for (SavedColors saved : slots()) {
                if (saved == null) {
                    array.add(JsonNull.INSTANCE);
                } else {
                    JsonObject object = new JsonObject();
                    object.addProperty("skin", saved.skin());
                    JsonArray colors = new JsonArray();
                    saved.colors().forEach(colors::add);
                    object.add("colors", colors);
                    array.add(object);
                }
            }
            JsonObject root = new JsonObject();
            root.add("slots", array);
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception exception) {
            JoJoWoR.LOGGER.warn("Failed to save skin color slots", exception);
        }
    }

    @Nullable
    public static SavedColors get(int index) {
        return slots().get(index);
    }

    public static boolean isEmpty(int index) {
        return get(index) == null;
    }

    public static void save(int index, SavedColors saved) {
        slots().set(index, saved);
        persist();
    }

    public static void clear(int index) {
        slots().set(index, null);
        persist();
    }
}
