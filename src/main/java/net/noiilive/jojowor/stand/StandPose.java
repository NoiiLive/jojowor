package net.noiilive.jojowor.stand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public record StandPose(Map<String, Part> parts) {
    public static final StandPose EMPTY = new StandPose(Map.of());

    public static final int MAX_PARTS = 16;
    public static final int MAX_NAME_LENGTH = 64;
    public static final float MAX_ROTATION = (float) Math.PI;
    public static final float MAX_OFFSET = 8.0F;

    public record Part(float rx, float ry, float rz, float ox, float oy, float oz) {
        public static final Part ZERO = new Part(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        public static final Codec<Part> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("rx", 0.0F).forGetter(Part::rx),
                Codec.FLOAT.optionalFieldOf("ry", 0.0F).forGetter(Part::ry),
                Codec.FLOAT.optionalFieldOf("rz", 0.0F).forGetter(Part::rz),
                Codec.FLOAT.optionalFieldOf("ox", 0.0F).forGetter(Part::ox),
                Codec.FLOAT.optionalFieldOf("oy", 0.0F).forGetter(Part::oy),
                Codec.FLOAT.optionalFieldOf("oz", 0.0F).forGetter(Part::oz)
        ).apply(instance, Part::new));

        public static final StreamCodec<ByteBuf, Part> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Part::rx,
                ByteBufCodecs.FLOAT, Part::ry,
                ByteBufCodecs.FLOAT, Part::rz,
                ByteBufCodecs.FLOAT, Part::ox,
                ByteBufCodecs.FLOAT, Part::oy,
                ByteBufCodecs.FLOAT, Part::oz,
                Part::new);

        public boolean isZero() {
            return this.rx == 0.0F && this.ry == 0.0F && this.rz == 0.0F
                    && this.ox == 0.0F && this.oy == 0.0F && this.oz == 0.0F;
        }

        public Part clamped() {
            return new Part(
                    Mth.clamp(this.rx, -MAX_ROTATION, MAX_ROTATION),
                    Mth.clamp(this.ry, -MAX_ROTATION, MAX_ROTATION),
                    Mth.clamp(this.rz, -MAX_ROTATION, MAX_ROTATION),
                    Mth.clamp(this.ox, -MAX_OFFSET, MAX_OFFSET),
                    Mth.clamp(this.oy, -MAX_OFFSET, MAX_OFFSET),
                    Mth.clamp(this.oz, -MAX_OFFSET, MAX_OFFSET));
        }
    }

    public static final Codec<StandPose> CODEC = Codec.unboundedMap(Codec.STRING, Part.CODEC)
            .xmap(StandPose::new, StandPose::parts);

    public static final StreamCodec<ByteBuf, StandPose> STREAM_CODEC = ByteBufCodecs
            .map(HashMap::new, ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), Part.STREAM_CODEC, MAX_PARTS)
            .map(StandPose::new, pose -> new HashMap<>(pose.parts()));

    public boolean isEmpty() {
        return this.parts.isEmpty();
    }

    public StandPose sanitized() {
        Map<String, Part> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Part> entry : this.parts.entrySet()) {
            if (cleaned.size() >= MAX_PARTS) {
                break;
            }
            if (entry.getKey().length() > MAX_NAME_LENGTH) {
                continue;
            }
            Part clamped = entry.getValue().clamped();
            if (!clamped.isZero()) {
                cleaned.put(entry.getKey(), clamped);
            }
        }
        return cleaned.isEmpty() ? EMPTY : new StandPose(Map.copyOf(cleaned));
    }
}
