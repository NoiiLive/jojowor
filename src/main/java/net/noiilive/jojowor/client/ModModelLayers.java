package net.noiilive.jojowor.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public final class ModModelLayers {
    public static final ModelLayerLocation DUMMY_STAND = layer("dummy_stand");

    private ModModelLayers() {}

    private static ModelLayerLocation layer(String path) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, path), "main");
    }
}
