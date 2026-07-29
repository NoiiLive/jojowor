package net.noiilive.jojowor.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.noiilive.jojowor.JoJoWoR;

/** All blocks registered under the {@code jojowor} namespace. */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JoJoWoR.MODID);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
