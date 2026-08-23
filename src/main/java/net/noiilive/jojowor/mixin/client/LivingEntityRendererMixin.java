package net.noiilive.jojowor.mixin.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.noiilive.jojowor.client.ClientTimeStop;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @SuppressWarnings("unchecked")
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"),
            require = 0)
    private void jojowor$freezeSetupAnim(EntityModel<?> model, Entity entity, float limbSwing, float limbSwingAmount,
                                         float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity instanceof LivingEntity living && ClientTimeStop.isFrozen(living)) {
            ClientTimeStop.FrozenPose pose = ClientTimeStop.pose(living);
            ((EntityModel<Entity>) model).setupAnim(entity, pose.walkPosition(), pose.walkSpeed(), ageInTicks,
                    pose.yHeadRot() - pose.yBodyRot(), pose.xRot());
            return;
        }
        ((EntityModel<Entity>) model).setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @SuppressWarnings("unchecked")
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;prepareMobModel(Lnet/minecraft/world/entity/Entity;FFF)V"),
            require = 0)
    private void jojowor$freezePrepareMobModel(EntityModel<?> model, Entity entity, float limbSwing,
                                               float limbSwingAmount, float partialTick) {
        if (entity instanceof LivingEntity living && ClientTimeStop.isFrozen(living)) {
            ClientTimeStop.FrozenPose pose = ClientTimeStop.pose(living);
            ((EntityModel<Entity>) model).prepareMobModel(entity, pose.walkPosition(), pose.walkSpeed(), 0.0F);
            return;
        }
        ((EntityModel<Entity>) model).prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }
}
