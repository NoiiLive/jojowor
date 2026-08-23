package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public record StandAbilityPayload(int slot) implements CustomPacketPayload {
    public static final Type<StandAbilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StandAbilityPayload::slot,
            StandAbilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
