package net.noiilive.jojowor.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.Stand;

public class ModStands {
    public static final ResourceKey<Registry<Stand>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand"));

    public static final DeferredRegister<Stand> STANDS = DeferredRegister.create(REGISTRY_KEY, JoJoWoR.MODID);

    public static final Registry<Stand> REGISTRY = STANDS.makeRegistry(builder -> builder.sync(true));

    public static final DeferredHolder<Stand, Stand> DUMMY = STANDS.register("dummy", () -> new Stand()
            .addSkin(new net.noiilive.jojowor.stand.skin.StandSkin("Default", java.util.List.of(
                    new net.noiilive.jojowor.stand.skin.StandSkin.Layer(
                            "static", entityTexture("dummy_layer_static"), 0xFFFFFF, false),
                    new net.noiilive.jojowor.stand.skin.StandSkin.Layer(
                            "primary", entityTexture("dummy_layer_primary"), 0xFFD524, true),
                    new net.noiilive.jojowor.stand.skin.StandSkin.Layer(
                            "secondary", entityTexture("dummy_layer_secondary"), 0x2D2B28, true)))));

    private static ResourceLocation entityTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "textures/entity/" + name + ".png");
    }

    public static void register(IEventBus modEventBus) {
        STANDS.register(modEventBus);
    }
}
