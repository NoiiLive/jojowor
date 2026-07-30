package net.noiilive.jojowor.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.StandData;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, JoJoWoR.MODID);

    public static final Supplier<AttachmentType<StandData>> STAND = ATTACHMENT_TYPES.register(
            "stand", () -> AttachmentType.builder(() -> StandData.EMPTY)
                    .serialize(StandData.CODEC)
                    .sync(StandData.STREAM_CODEC)
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
