package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public record StandUppercutEffectPayload(int ownerEntityId, int targetEntityId) implements CustomPacketPayload {
    public static final Type<StandUppercutEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_uppercut_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandUppercutEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StandUppercutEffectPayload::ownerEntityId,
            ByteBufCodecs.VAR_INT, StandUppercutEffectPayload::targetEntityId,
            StandUppercutEffectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
