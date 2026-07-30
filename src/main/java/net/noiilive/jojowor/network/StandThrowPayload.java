package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.noiilive.jojowor.JoJoWoR;

public record StandThrowPayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<StandThrowPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_throw"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandThrowPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.map(
                    offhand -> offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                    hand -> hand == InteractionHand.OFF_HAND),
            StandThrowPayload::hand,
            StandThrowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
