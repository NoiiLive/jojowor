package net.noiilive.jojowor.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.noiilive.jojowor.client.ClientTimeStop;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private float jojowor$freezeYaw(float yaw, Entity entity) {
        return ClientTimeStop.isFrozen(entity) ? entity.getYRot() : yaw;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private float jojowor$freezePartialTick(float partialTick, Entity entity) {
        return ClientTimeStop.isFrozen(entity) ? 0.0F : partialTick;
    }
}
