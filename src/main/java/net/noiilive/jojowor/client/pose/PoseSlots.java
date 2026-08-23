package net.noiilive.jojowor.client.pose;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.Minecraft;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.StandPose;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PoseSlots {
    public static final int SLOT_COUNT = 3;

    private static List<StandPose> slots;

    private PoseSlots() {}

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("jojowor_pose_slots.json");
    }

    private static List<StandPose> slots() {
        if (slots == null) {
            slots = load();
        }
        return slots;
    }

    private static List<StandPose> load() {
        List<StandPose> loaded = new ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            loaded.add(StandPose.EMPTY);
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
                    int index = i;
                    StandPose.CODEC.parse(JsonOps.INSTANCE, element).result()
                            .ifPresent(pose -> loaded.set(index, pose.sanitized()));
                }
            }
        } catch (Exception exception) {
            JoJoWoR.LOGGER.warn("Failed to load pose slots", exception);
        }
        return loaded;
    }

    private static void persist() {
        try {
            JsonArray array = new JsonArray();
            for (StandPose pose : slots()) {
                if (pose.isEmpty()) {
                    array.add(com.google.gson.JsonNull.INSTANCE);
                } else {
                    array.add(StandPose.CODEC.encodeStart(JsonOps.INSTANCE, pose).result()
                            .orElse(com.google.gson.JsonNull.INSTANCE));
                }
            }
            JsonObject root = new JsonObject();
            root.add("slots", array);
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception exception) {
            JoJoWoR.LOGGER.warn("Failed to save pose slots", exception);
        }
    }

    public static StandPose get(int index) {
        return slots().get(index);
    }

    public static boolean isEmpty(int index) {
        return get(index).isEmpty();
    }

    public static void save(int index, StandPose pose) {
        slots().set(index, pose);
        persist();
    }

    public static void clear(int index) {
        slots().set(index, StandPose.EMPTY);
        persist();
    }
}
