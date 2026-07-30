package net.noiilive.jojowor.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.noiilive.jojowor.JoJoWoR;

public record StandThrowEffectPayload(int ownerEntityId, ItemStack stack) implements CustomPacketPayload {
    public static final Type<StandThrowEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_throw_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandThrowEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StandThrowEffectPayload::ownerEntityId,
            ItemStack.STREAM_CODEC, StandThrowEffectPayload::stack,
            StandThrowEffectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
