package net.noiilive.jojowor.stand;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum GuardMode implements StringRepresentable {
    NONE("none"),
    BLOCK("block"),
    GUARD("guard");

    public static final Codec<GuardMode> CODEC = StringRepresentable.fromEnum(GuardMode::values);

    public static final StreamCodec<ByteBuf, GuardMode> STREAM_CODEC = ByteBufCodecs.BYTE.map(
            id -> values()[Math.floorMod(id, values().length)],
            mode -> (byte) mode.ordinal());

    private final String name;

    GuardMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
