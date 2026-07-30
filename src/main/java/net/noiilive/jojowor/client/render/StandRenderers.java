package net.noiilive.jojowor.client.render;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.client.ModModelLayers;
import net.noiilive.jojowor.client.model.DummyStandModel;
import net.noiilive.jojowor.client.model.StandModel;
import net.noiilive.jojowor.registry.ModStands;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class StandRenderers {
    private static final Map<ResourceLocation, Definition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Baked> BAKED = new HashMap<>();

    static {
        register(ModStands.DUMMY.getId(), ModModelLayers.DUMMY_STAND, "dummy_full", DummyStandModel::new);
    }

    private StandRenderers() {}

    private static void register(ResourceLocation standId, ModelLayerLocation layer, String texture,
                                 Function<ModelPart, StandModel> factory) {
        DEFINITIONS.put(standId, new Definition(layer, entityTexture(texture), factory));
    }

    public static void bake(EntityModelSet models) {
        BAKED.clear();
        DEFINITIONS.forEach((standId, definition) -> BAKED.put(standId,
                new Baked(definition.factory().apply(models.bakeLayer(definition.layer())), definition.texture())));
    }

    @Nullable
    public static Baked get(ResourceLocation standId) {
        return BAKED.get(standId);
    }

    private static ResourceLocation entityTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "textures/entity/" + name + ".png");
    }

    public static java.util.List<SkinLayer> skinLayers(net.noiilive.jojowor.stand.Stand stand,
                                                       net.noiilive.jojowor.stand.StandData data, Baked baked) {
        java.util.List<net.noiilive.jojowor.stand.skin.StandSkin> skins = stand.getSkins();
        if (skins.isEmpty() || !data.skinColored()) {
            return java.util.List.of(new SkinLayer(baked.texture(), 0xFFFFFF));
        }
        int index = net.minecraft.util.Mth.clamp(data.skin(), 0, skins.size() - 1);
        net.noiilive.jojowor.stand.skin.StandSkin skin = skins.get(index);
        java.util.List<SkinLayer> layers = new java.util.ArrayList<>(skin.layers().size());
        int recolorIndex = 0;
        for (net.noiilive.jojowor.stand.skin.StandSkin.Layer layer : skin.layers()) {
            int color = layer.recolorable() ? skin.colorAt(recolorIndex++, data.skinColors()) : 0xFFFFFF;
            layers.add(new SkinLayer(layer.texture(), color));
        }
        return layers;
    }

    private record Definition(ModelLayerLocation layer, ResourceLocation texture, Function<ModelPart, StandModel> factory) {}

    public record Baked(StandModel model, ResourceLocation texture) {}

    public record SkinLayer(ResourceLocation texture, int color) {}
}
