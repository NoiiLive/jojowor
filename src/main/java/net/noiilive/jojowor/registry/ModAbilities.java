package net.noiilive.jojowor.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.ability.StandAbility;
import net.noiilive.jojowor.stand.ability.TimeStopAbility;

public class ModAbilities {
    public static final ResourceKey<Registry<StandAbility>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_ability"));

    public static final DeferredRegister<StandAbility> ABILITIES =
            DeferredRegister.create(REGISTRY_KEY, JoJoWoR.MODID);

    public static final Registry<StandAbility> REGISTRY = ABILITIES.makeRegistry(builder -> builder.sync(true));

    public static final DeferredHolder<StandAbility, StandAbility> TIME_STOP =
            ABILITIES.register("time_stop", TimeStopAbility::new);

    public static void register(IEventBus modEventBus) {
        ABILITIES.register(modEventBus);
    }
}
