package net.noiilive.jojowor.mixin.client;

import net.minecraft.client.particle.Particle;
import net.noiilive.jojowor.client.FrozenParticle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
public abstract class ParticleMixin implements FrozenParticle {
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;
    @Shadow
    protected double xo;
    @Shadow
    protected double yo;
    @Shadow
    protected double zo;
    @Shadow
    protected float roll;
    @Shadow
    protected float oRoll;

    @Unique
    private boolean jojowor$timeStopAsh;

    @Override
    public void jojowor$snapToPosition() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
    }

    @Override
    public void jojowor$markTimeStopAsh() {
        this.jojowor$timeStopAsh = true;
    }

    @Override
    public boolean jojowor$isTimeStopAsh() {
        return this.jojowor$timeStopAsh;
    }
}
