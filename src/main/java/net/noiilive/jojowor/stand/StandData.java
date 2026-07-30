package net.noiilive.jojowor.stand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.registry.ModStands;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record StandData(Optional<ResourceLocation> standId, boolean summoned, GuardMode guardMode,
                        StandPose pose, StandOffset offset, int skin, List<Integer> skinColors,
                        boolean skinColored) {
    public static final StandData EMPTY = new StandData(
            Optional.empty(), false, GuardMode.NONE, StandPose.EMPTY, StandOffset.DEFAULT, 0, List.of(), false);

    public static final Codec<StandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("stand").forGetter(StandData::standId),
            Codec.BOOL.optionalFieldOf("summoned", false).forGetter(StandData::summoned),
            GuardMode.CODEC.optionalFieldOf("guard_mode", GuardMode.NONE).forGetter(StandData::guardMode),
            StandPose.CODEC.optionalFieldOf("pose", StandPose.EMPTY).forGetter(StandData::pose),
            StandOffset.CODEC.optionalFieldOf("offset", StandOffset.DEFAULT).forGetter(StandData::offset),
            Codec.INT.optionalFieldOf("skin", 0).forGetter(StandData::skin),
            Codec.INT.listOf().optionalFieldOf("skin_colors", List.of()).forGetter(StandData::skinColors),
            Codec.BOOL.optionalFieldOf("skin_colored", false).forGetter(StandData::skinColored)
    ).apply(instance, StandData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeOptional(data.standId(), FriendlyByteBuf::writeResourceLocation);
                buf.writeBoolean(data.summoned());
                GuardMode.STREAM_CODEC.encode(buf, data.guardMode());
                StandPose.STREAM_CODEC.encode(buf, data.pose());
                StandOffset.STREAM_CODEC.encode(buf, data.offset());
                buf.writeVarInt(data.skin());
                buf.writeVarInt(data.skinColors().size());
                for (int color : data.skinColors()) {
                    buf.writeVarInt(color);
                }
                buf.writeBoolean(data.skinColored());
            },
            buf -> {
                Optional<ResourceLocation> standId = buf.readOptional(FriendlyByteBuf::readResourceLocation);
                boolean summoned = buf.readBoolean();
                GuardMode guardMode = GuardMode.STREAM_CODEC.decode(buf);
                StandPose pose = StandPose.STREAM_CODEC.decode(buf);
                StandOffset offset = StandOffset.STREAM_CODEC.decode(buf);
                int skin = buf.readVarInt();
                int colorCount = buf.readVarInt();
                List<Integer> colors = new ArrayList<>(colorCount);
                for (int i = 0; i < colorCount; i++) {
                    colors.add(buf.readVarInt());
                }
                boolean skinColored = buf.readBoolean();
                return new StandData(standId, summoned, guardMode, pose, offset, skin,
                        List.copyOf(colors), skinColored);
            });

    public static StandData of(@Nullable Stand stand) {
        return stand == null ? EMPTY : new StandData(Optional.of(stand.getId()), false, GuardMode.NONE,
                StandPose.EMPTY, StandOffset.DEFAULT, 0, List.of(), false);
    }

    public StandData withSummoned(boolean summoned) {
        return new StandData(this.standId, summoned,
                summoned ? this.guardMode : GuardMode.NONE, this.pose, this.offset,
                this.skin, this.skinColors, this.skinColored);
    }

    public StandData withGuardMode(GuardMode guardMode) {
        return new StandData(this.standId, this.summoned,
                this.summoned ? guardMode : GuardMode.NONE, this.pose, this.offset,
                this.skin, this.skinColors, this.skinColored);
    }

    public StandData withPose(StandPose pose) {
        return new StandData(this.standId, this.summoned, this.guardMode, pose, this.offset,
                this.skin, this.skinColors, this.skinColored);
    }

    public StandData withOffset(StandOffset offset) {
        return new StandData(this.standId, this.summoned, this.guardMode, this.pose, offset,
                this.skin, this.skinColors, this.skinColored);
    }

    public StandData withSkin(int skin, List<Integer> skinColors, boolean skinColored) {
        return new StandData(this.standId, this.summoned, this.guardMode, this.pose, this.offset,
                skin, skinColors, skinColored);
    }

    @Nullable
    public Stand stand() {
        return this.standId.map(ModStands.REGISTRY::get).orElse(null);
    }
}
