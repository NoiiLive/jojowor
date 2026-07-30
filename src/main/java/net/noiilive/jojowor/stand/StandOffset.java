package net.noiilive.jojowor.stand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record StandOffset(float forward, float right, float up) {
    public static final StandOffset DEFAULT = new StandOffset(-0.75F, 0.45F, 0.0F);

    public static final float MAX_HORIZONTAL = 2.0F;
    public static final float MIN_UP = -1.0F;
    public static final float MAX_UP = 1.5F;

    public static final Codec<StandOffset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("forward", DEFAULT.forward()).forGetter(StandOffset::forward),
            Codec.FLOAT.optionalFieldOf("right", DEFAULT.right()).forGetter(StandOffset::right),
            Codec.FLOAT.optionalFieldOf("up", DEFAULT.up()).forGetter(StandOffset::up)
    ).apply(instance, StandOffset::new));

    public static final StreamCodec<ByteBuf, StandOffset> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, StandOffset::forward,
            ByteBufCodecs.FLOAT, StandOffset::right,
            ByteBufCodecs.FLOAT, StandOffset::up,
            StandOffset::new);

    public StandOffset clamped() {
        return new StandOffset(
                Mth.clamp(this.forward, -MAX_HORIZONTAL, MAX_HORIZONTAL),
                Mth.clamp(this.right, -MAX_HORIZONTAL, MAX_HORIZONTAL),
                Mth.clamp(this.up, MIN_UP, MAX_UP));
    }
}
