package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

import java.util.List;

public record SetStandSkinPayload(int skin, List<Integer> colors, boolean colored) implements CustomPacketPayload {
    public static final Type<SetStandSkinPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "set_stand_skin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetStandSkinPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetStandSkinPayload::skin,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SetStandSkinPayload::colors,
            ByteBufCodecs.BOOL, SetStandSkinPayload::colored,
            SetStandSkinPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
