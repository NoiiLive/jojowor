package net.noiilive.jojowor.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

import java.util.Optional;

public record StandAttackEffectPayload(int ownerEntityId, int targetEntityId,
                                       Optional<BlockPos> blockTarget) implements CustomPacketPayload {
    public static final Type<StandAttackEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_attack_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandAttackEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StandAttackEffectPayload::ownerEntityId,
            ByteBufCodecs.VAR_INT, StandAttackEffectPayload::targetEntityId,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), StandAttackEffectPayload::blockTarget,
            StandAttackEffectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
