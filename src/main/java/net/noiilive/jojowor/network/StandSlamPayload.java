package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public record StandSlamPayload(int targetEntityId) implements CustomPacketPayload {
    public static final Type<StandSlamPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_slam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandSlamPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StandSlamPayload::targetEntityId,
            StandSlamPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
