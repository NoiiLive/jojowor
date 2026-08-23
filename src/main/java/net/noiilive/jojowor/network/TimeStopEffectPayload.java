package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public record TimeStopEffectPayload(int ownerEntityId, double x, double y, double z,
                                    float radius, boolean global, boolean active, boolean silent) implements CustomPacketPayload {
    public static final Type<TimeStopEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "time_stop_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TimeStopEffectPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.ownerEntityId());
                buf.writeDouble(payload.x());
                buf.writeDouble(payload.y());
                buf.writeDouble(payload.z());
                buf.writeFloat(payload.radius());
                buf.writeBoolean(payload.global());
                buf.writeBoolean(payload.active());
                buf.writeBoolean(payload.silent());
            },
            buf -> new TimeStopEffectPayload(
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
