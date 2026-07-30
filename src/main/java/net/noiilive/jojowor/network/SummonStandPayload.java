package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public record SummonStandPayload() implements CustomPacketPayload {
    public static final SummonStandPayload INSTANCE = new SummonStandPayload();

    public static final Type<SummonStandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "summon_stand"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonStandPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
