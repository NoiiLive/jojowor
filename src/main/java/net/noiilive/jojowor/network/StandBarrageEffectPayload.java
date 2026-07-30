package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public record StandBarrageEffectPayload(int ownerEntityId, boolean active) implements CustomPacketPayload {
    public static final Type<StandBarrageEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_barrage_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandBarrageEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StandBarrageEffectPayload::ownerEntityId,
            ByteBufCodecs.BOOL, StandBarrageEffectPayload::active,
            StandBarrageEffectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
