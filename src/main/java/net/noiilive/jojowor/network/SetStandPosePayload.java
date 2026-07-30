package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.StandOffset;
import net.noiilive.jojowor.stand.StandPose;

public record SetStandPosePayload(StandPose pose, StandOffset offset) implements CustomPacketPayload {
    public static final Type<SetStandPosePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "set_stand_pose"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetStandPosePayload> STREAM_CODEC = StreamCodec.composite(
            StandPose.STREAM_CODEC, SetStandPosePayload::pose,
            StandOffset.STREAM_CODEC, SetStandPosePayload::offset,
            SetStandPosePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
