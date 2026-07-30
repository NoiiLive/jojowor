package net.noiilive.jojowor.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

import java.util.Optional;

public record StandAttackPayload(int targetEntityId, Optional<BlockPos> blockTarget) implements CustomPacketPayload {
    public static final int NO_TARGET = -1;

    public static final Type<StandAttackPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_attack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandAttackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StandAttackPayload::targetEntityId,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), StandAttackPayload::blockTarget,
            StandAttackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
