package net.noiilive.jojowor.stand.skin;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record StandSkin(String name, List<Layer> layers) {
    public record Layer(String name, ResourceLocation texture, int defaultColor, boolean recolorable) {}

    public List<Layer> recolorableLayers() {
        return this.layers.stream().filter(Layer::recolorable).toList();
    }

    public List<Integer> defaultColors() {
        return recolorableLayers().stream().map(Layer::defaultColor).toList();
    }

    public int colorAt(int recolorIndex, List<Integer> saved) {
        if (recolorIndex < saved.size()) {
            return saved.get(recolorIndex) & 0xFFFFFF;
        }
        return recolorableLayers().get(recolorIndex).defaultColor();
    }
}
