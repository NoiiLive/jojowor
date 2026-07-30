package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public record StandSlamEffectPayload(int ownerEntityId, int targetEntityId) implements CustomPacketPayload {
    public static final Type<StandSlamEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_slam_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandSlamEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StandSlamEffectPayload::ownerEntityId,
            ByteBufCodecs.VAR_INT, StandSlamEffectPayload::targetEntityId,
            StandSlamEffectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
