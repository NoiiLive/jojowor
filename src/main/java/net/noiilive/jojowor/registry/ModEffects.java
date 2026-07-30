package net.noiilive.jojowor.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.effect.StunEffect;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, JoJoWoR.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> STUN = EFFECTS.register("stun", StunEffect::new);

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
