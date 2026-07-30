package net.noiilive.jojowor.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.stand.StandPose;

import java.util.Map;
import java.util.function.Function;

public abstract class StandModel extends Model {
    protected StandModel(Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
    }

    public abstract void setupAnim(StandAnimation animation);

    public abstract Map<String, ModelPart> posableParts();

    public void translateToRightHand(com.mojang.blaze3d.vertex.PoseStack poseStack) {
    }

    protected void applyPose(StandPose pose, float amount) {
        applyPose(pose, name -> amount);
    }

    protected void applyPose(StandPose pose, java.util.function.ToDoubleFunction<String> amountForPart) {
        if (pose == null || pose.isEmpty()) {
            return;
        }
        posableParts().forEach((name, part) -> {
            StandPose.Part offsets = pose.parts().get(name);
            if (offsets == null) {
                return;
            }
            float amount = (float) amountForPart.applyAsDouble(name);
            if (amount <= 0.01F) {
                return;
            }
            part.xRot += offsets.rx() * amount;
            part.yRot += offsets.ry() * amount;
            part.zRot += offsets.rz() * amount;
            part.x += offsets.ox() * amount;
            part.y += offsets.oy() * amount;
            part.z += offsets.oz() * amount;
        });
    }

    protected static float wave(double ageTicks, double periodTicks, double phaseOffset) {
        return (float) Math.sin((ageTicks / periodTicks) * 2.0D * Math.PI - phaseOffset);
    }
}
