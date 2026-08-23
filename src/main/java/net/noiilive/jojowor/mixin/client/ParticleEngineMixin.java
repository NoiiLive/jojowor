package net.noiilive.jojowor.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.noiilive.jojowor.client.FrozenParticle;
import net.noiilive.jojowor.stand.ability.TimeStopWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$freezeParticleTick(Particle particle, CallbackInfo ci) {
        if (particle instanceof FrozenParticle ash && ash.jojowor$isTimeStopAsh()) {
            return;
        }
        AABB box = particle.getBoundingBox();
        Vec3 center = box.getCenter();
        if (!TimeStopWorld.frozen(Minecraft.getInstance().level, center.x, center.y, center.z)) {
            return;
        }
        if (particle instanceof FrozenParticle frozen) {
            frozen.jojowor$snapToPosition();
        }
        ci.cancel();
    }
}
