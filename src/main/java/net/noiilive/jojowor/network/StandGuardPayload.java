package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.GuardMode;

public record StandGuardPayload(GuardMode mode) implements CustomPacketPayload {
    public static final Type<StandGuardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_guard"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandGuardPayload> STREAM_CODEC = StreamCodec.composite(
            GuardMode.STREAM_CODEC, StandGuardPayload::mode,
            StandGuardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
