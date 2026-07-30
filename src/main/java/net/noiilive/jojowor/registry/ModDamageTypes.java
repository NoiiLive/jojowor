package net.noiilive.jojowor.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.noiilive.jojowor.JoJoWoR;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> STAND =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand"));

    private ModDamageTypes() {}

    public static DamageSource stand(ServerPlayer owner) {
        return new DamageSource(
                owner.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(STAND),
                owner, owner);
    }
}
