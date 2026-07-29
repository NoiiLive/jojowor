package net.noiilive.jojowor.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.noiilive.jojowor.JoJoWoR;

/** All items registered under the {@code jojowor} namespace. */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JoJoWoR.MODID);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
