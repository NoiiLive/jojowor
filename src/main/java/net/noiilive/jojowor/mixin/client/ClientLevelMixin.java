package net.noiilive.jojowor.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.noiilive.jojowor.stand.ability.TimeStopWorld;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    private boolean jojowor$frozenAt(double x, double y, double z) {
        return TimeStopWorld.frozen((Level) (Object) this, x, y, z);
    }

    @Inject(method = "doAnimateTick", at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$freezeAnimateTick(int posX, int posY, int posZ, int range, RandomSource random,
                                           @Nullable Block block, BlockPos.MutableBlockPos blockPos,
                                           CallbackInfo ci) {
        if (jojowor$frozenAt(posX, posY, posZ)) {
            ci.cancel();
        }
    }

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$freezeParticle(ParticleOptions options, double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (jojowor$frozenAt(x, y, z)) {
            ci.cancel();
        }
    }

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$freezeForcedParticle(ParticleOptions options, boolean force, double x, double y, double z,
                                              double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (jojowor$frozenAt(x, y, z)) {
            ci.cancel();
        }
    }

    @Inject(method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$freezeVisibleParticle(ParticleOptions options, double x, double y, double z,
                                               double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (jojowor$frozenAt(x, y, z)) {
            ci.cancel();
        }
    }

    @Inject(method = "addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$freezeVisibleRangedParticle(ParticleOptions options, boolean ignoreRange,
                                                     double x, double y, double z,
                                                     double xSpeed, double ySpeed, double zSpeed, CallbackInfo ci) {
        if (jojowor$frozenAt(x, y, z)) {
            ci.cancel();
        }
    }
}
