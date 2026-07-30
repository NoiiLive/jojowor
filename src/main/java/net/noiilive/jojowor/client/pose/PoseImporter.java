package net.noiilive.jojowor.client.pose;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.StandPose;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PoseImporter {
    public static final String EXPORT_KEY = "jojowor_pose";

    private static final Pattern CHANNEL = Pattern.compile(
            "\\.addAnimation\\(\"(\\w+)\",\\s*new AnimationChannel\\(AnimationChannel\\.Targets\\.(\\w+)\\s*,");
    private static final Pattern KEYFRAME = Pattern.compile(
            "new Keyframe\\(\\s*([-0-9.]+)[Ff]?\\s*,\\s*KeyframeAnimations\\.(?:degreeVec|posVec)\\(\\s*"
                    + "([-0-9.]+)[Ff]?\\s*,\\s*([-0-9.]+)[Ff]?\\s*,\\s*([-0-9.]+)[Ff]?\\s*\\)");

    private record Transform(float rx, float ry, float rz, float ox, float oy, float oz) {
        static final Transform IDENTITY = new Transform(0, 0, 0, 0, 0, 0);

        Transform withRotationDegrees(float x, float y, float z) {
            return new Transform((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z),
                    this.ox, this.oy, this.oz);
        }

        Transform withPosition(float x, float y, float z) {
            return new Transform(this.rx, this.ry, this.rz, x, -y, z);
        }

        StandPose.Part toPart() {
            return new StandPose.Part(this.rx, this.ry, this.rz, this.ox, this.oy, this.oz);
        }
    }

    private PoseImporter() {}

    public static Optional<StandPose> importFile(Path path) {
        try {
            String content = Files.readString(path).trim();
            if (content.startsWith("{")) {
                return importJson(content);
            }
            if (content.contains("AnimationDefinition")) {
                return importJavaExport(content);
            }
        } catch (Exception exception) {
            JoJoWoR.LOGGER.warn("Failed to import pose from {}", path, exception);
        }
        return Optional.empty();
    }

    private static Optional<StandPose> importJson(String content) {
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        if (root.has(EXPORT_KEY)) {
            return StandPose.CODEC.parse(JsonOps.INSTANCE, root.get(EXPORT_KEY))
                    .result().map(StandPose::sanitized);
        }
        if (root.has("animations")) {
            return importBbmodel(root);
        }
        return Optional.empty();
    }

    private static Optional<StandPose> importBbmodel(JsonObject root) {
        JsonArray animations = root.getAsJsonArray("animations");
        if (animations.isEmpty()) {
            return Optional.empty();
        }
        JsonObject animation = animations.get(0).getAsJsonObject();
        if (!animation.has("animators")) {
            return Optional.empty();
        }

        Map<String, Transform> bones = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : animation.getAsJsonObject("animators").entrySet()) {
            JsonObject animator = entry.getValue().getAsJsonObject();
            if (!animator.has("name") || !animator.has("keyframes")) {
                continue;
            }
            String bone = animator.get("name").getAsString();
            JsonObject rotation = firstKeyframe(animator.getAsJsonArray("keyframes"), "rotation");
            JsonObject position = firstKeyframe(animator.getAsJsonArray("keyframes"), "position");
            if (rotation == null && position == null) {
                continue;
            }
            Transform transform = Transform.IDENTITY;
            if (rotation != null) {
                float[] values = dataPoint(rotation);
                transform = transform.withRotationDegrees(values[0], values[1], values[2]);
            }
            if (position != null) {
                float[] values = dataPoint(position);
                transform = transform.withPosition(values[0], values[1], values[2]);
            }
            bones.put(bone, transform);
        }
        return buildPose(bones);
    }

    @org.jetbrains.annotations.Nullable
    private static JsonObject firstKeyframe(JsonArray keyframes, String channel) {
        JsonObject best = null;
        double bestTime = Double.MAX_VALUE;
        for (JsonElement element : keyframes) {
            JsonObject keyframe = element.getAsJsonObject();
            if (!channel.equals(keyframe.get("channel").getAsString())) {
                continue;
            }
            double time = parseFloat(keyframe.get("time"));
            if (time < bestTime) {
                bestTime = time;
                best = keyframe;
            }
        }
        return best;
    }

    private static float[] dataPoint(JsonObject keyframe) {
        JsonArray points = keyframe.getAsJsonArray("data_points");
        if (points == null || points.isEmpty()) {
            return new float[3];
        }
        JsonObject point = points.get(0).getAsJsonObject();
        return new float[]{
                parseFloat(point.get("x")),
                parseFloat(point.get("y")),
                parseFloat(point.get("z"))};
    }

    private static float parseFloat(@org.jetbrains.annotations.Nullable JsonElement element) {
        if (element == null) {
            return 0.0F;
        }
        try {
            return Float.parseFloat(element.getAsString().trim());
        } catch (NumberFormatException exception) {
            return 0.0F;
        }
    }

    private static Optional<StandPose> importJavaExport(String content) {
        int firstBuilder = content.indexOf("AnimationDefinition.Builder");
        if (firstBuilder < 0) {
            return Optional.empty();
        }
        int buildEnd = content.indexOf(".build()", firstBuilder);
        String definition = buildEnd < 0
                ? content.substring(firstBuilder)
                : content.substring(firstBuilder, buildEnd);

        record ChannelStart(String bone, String target, int start) {}
        java.util.List<ChannelStart> channels = new java.util.ArrayList<>();
        Matcher channelMatcher = CHANNEL.matcher(definition);
        while (channelMatcher.find()) {
            channels.add(new ChannelStart(channelMatcher.group(1), channelMatcher.group(2), channelMatcher.end()));
        }

        Map<String, Transform> bones = new LinkedHashMap<>();
        for (int i = 0; i < channels.size(); i++) {
            ChannelStart channel = channels.get(i);
            int regionEnd = i + 1 < channels.size() ? channels.get(i + 1).start() : definition.length();
            Matcher keyframeMatcher = KEYFRAME.matcher(definition.substring(channel.start(), regionEnd));
            if (!keyframeMatcher.find()) {
                continue;
            }
            float x = Float.parseFloat(keyframeMatcher.group(2));
            float y = Float.parseFloat(keyframeMatcher.group(3));
            float z = Float.parseFloat(keyframeMatcher.group(4));

            Transform transform = bones.getOrDefault(channel.bone(), Transform.IDENTITY);
            if ("ROTATION".equals(channel.target())) {
                transform = transform.withRotationDegrees(x, y, z);
            } else if ("POSITION".equals(channel.target())) {
                transform = transform.withPosition(x, y, z);
            } else {
                continue;
            }
            bones.put(channel.bone(), transform);
        }
        return buildPose(bones);
    }

    private static Optional<StandPose> buildPose(Map<String, Transform> bones) {
        Map<String, StandPose.Part> parts = new LinkedHashMap<>();
        bones.forEach((bone, transform) -> parts.put(bone, transform.toPart()));
        StandPose pose = new StandPose(parts).sanitized();
        return pose.isEmpty() ? Optional.empty() : Optional.of(pose);
    }
}
