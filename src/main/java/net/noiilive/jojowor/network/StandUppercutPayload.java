package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public record StandUppercutPayload(int targetEntityId) implements CustomPacketPayload {
    public static final Type<StandUppercutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_uppercut"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandUppercutPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StandUppercutPayload::targetEntityId,
            StandUppercutPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
