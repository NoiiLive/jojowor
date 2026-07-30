package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public record StandBarragePayload(boolean active) implements CustomPacketPayload {
    public static final Type<StandBarragePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_barrage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandBarragePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StandBarragePayload::active,
            StandBarragePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
