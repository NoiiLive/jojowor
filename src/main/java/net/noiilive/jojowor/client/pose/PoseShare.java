package net.noiilive.jojowor.client.pose;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.StandPose;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public final class PoseShare {
    private PoseShare() {}

    public static void openUpload(Consumer<StandPose> onLoaded) {
        Thread thread = new Thread(() -> {
            String path;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var patterns = stack.mallocPointer(3);
                patterns.put(stack.UTF8("*.json"));
                patterns.put(stack.UTF8("*.bbmodel"));
                patterns.put(stack.UTF8("*.java"));
                patterns.flip();
                path = TinyFileDialogs.tinyfd_openFileDialog(
                        "Import Stand Pose", (CharSequence) null, patterns,
                        "Pose / Blockbench animation (*.json, *.bbmodel, *.java)", false);
            }
            if (path == null) {
                return;
            }
            Optional<StandPose> pose = PoseImporter.importFile(Path.of(path));
            Minecraft.getInstance().execute(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player == null) {
                    return;
                }
                if (pose.isPresent()) {
                    onLoaded.accept(pose.get());
                    minecraft.player.displayClientMessage(
                            Component.translatable("jojowor.pose.import.success"), false);
                } else {
                    minecraft.player.displayClientMessage(
                            Component.translatable("jojowor.pose.import.failed"), false);
                }
            });
        }, "jojowor-pose-upload");
        thread.setDaemon(true);
        thread.start();
    }

    public static void openDownload(StandPose pose) {
        Thread thread = new Thread(() -> {
            String path;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var patterns = stack.mallocPointer(1);
                patterns.put(stack.UTF8("*.json"));
                patterns.flip();
                path = TinyFileDialogs.tinyfd_saveFileDialog(
                        "Export Stand Pose", "stand_pose.json", patterns, "Stand pose (*.json)");
            }
            if (path == null) {
                return;
            }
            boolean success = write(Path.of(path), pose);
            Minecraft.getInstance().execute(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.translatable(
                            success ? "jojowor.pose.export.success" : "jojowor.pose.export.failed"), false);
                }
            });
        }, "jojowor-pose-download");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean write(Path path, StandPose pose) {
        try {
            JsonObject root = new JsonObject();
            root.add(PoseImporter.EXPORT_KEY, StandPose.CODEC.encodeStart(JsonOps.INSTANCE, pose)
                    .result().orElseThrow());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            return true;
        } catch (Exception exception) {
            JoJoWoR.LOGGER.warn("Failed to export pose to {}", path, exception);
            return false;
        }
    }
}
