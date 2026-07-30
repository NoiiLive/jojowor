package net.noiilive.jojowor.stand;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.registry.ModStands;

import org.jetbrains.annotations.Nullable;

public class Stand {
    @Nullable
    private String descriptionId;
    private final java.util.List<net.noiilive.jojowor.stand.skin.StandSkin> skins = new java.util.ArrayList<>();

    public Stand addSkin(net.noiilive.jojowor.stand.skin.StandSkin skin) {
        this.skins.add(skin);
        return this;
    }

    public java.util.List<net.noiilive.jojowor.stand.skin.StandSkin> getSkins() {
        return java.util.Collections.unmodifiableList(this.skins);
    }

    public ResourceLocation getId() {
        return ModStands.REGISTRY.getKey(this);
    }

    public String getDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("stand", ModStands.REGISTRY.getKey(this));
        }
        return this.descriptionId;
    }

    public Component getDisplayName() {
        return Component.translatable(getDescriptionId());
    }

    @Override
    public String toString() {
        return "Stand[" + getId() + "]";
    }
}
