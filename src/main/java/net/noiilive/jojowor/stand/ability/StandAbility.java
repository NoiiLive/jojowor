package net.noiilive.jojowor.stand.ability;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.noiilive.jojowor.registry.ModAbilities;

import org.jetbrains.annotations.Nullable;

public class StandAbility {
    @Nullable
    private String descriptionId;

    public ResourceLocation getId() {
        return ModAbilities.REGISTRY.getKey(this);
    }

    public String getDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("ability", ModAbilities.REGISTRY.getKey(this));
        }
        return this.descriptionId;
    }

    public Component getDisplayName() {
        return Component.translatable(getDescriptionId());
    }

    public boolean canActivate(ServerPlayer player) {
        return true;
    }

    public boolean usableWhileFrozen() {
        return false;
    }

    public void activate(ServerPlayer player) {
    }

    @Override
    public String toString() {
        return "StandAbility[" + getId() + "]";
    }
}
