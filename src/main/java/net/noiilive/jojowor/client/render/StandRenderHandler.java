package net.noiilive.jojowor.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.client.model.StandAnimation;
import net.noiilive.jojowor.stand.Stand;
import net.noiilive.jojowor.stand.Stands;

@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public final class StandRenderHandler {
    public static final double HOVER_MIN = 0.4D;
    public static final double HOVER_MAX = 0.6D;
    public static final double BOB_PERIOD_TICKS = 100.0D;
    public static final double ACTION_HOVER = 0.45D;

    public static final double LEAN_DEGREES_PER_BLOCK_PER_TICK = 45.0D;
    public static final double MAX_LEAN_DEGREES = 12.0D;

    public static final float FIRST_PERSON_ALPHA = 0.35F;

    private static final int IDLE_PHASE_SPREAD_TICKS = 400;

    private static final double HOVER_MID = (HOVER_MIN + HOVER_MAX) / 2.0D;
    private static final double HOVER_AMPLITUDE = (HOVER_MAX - HOVER_MIN) / 2.0D;

    private static final float MIN_VISIBLE_ALPHA = 0.01F;
    private static final float MODEL_Y_OFFSET = -1.501F;
    private static final double LIGHT_SAMPLE_HEIGHT = 1.0D;

    private StandRenderHandler() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        if (poseStack == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer viewer = minecraft.player;
        ClientLevel level = minecraft.level;
        if (viewer == null || level == null) {
            return;
        }

        boolean viewerHasStand = Stands.has(viewer);

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();

        for (Player owner : level.players()) {
            float alphaScale = (owner == viewer && firstPerson) ? FIRST_PERSON_ALPHA : 1.0F;
            render(owner, level, poseStack, bufferSource, cameraPos, partialTick, alphaScale, viewerHasStand);
        }
    }

    private static void render(Player owner, ClientLevel level, PoseStack poseStack,
                               MultiBufferSource bufferSource, Vec3 cameraPos, float partialTick,
                               float alphaScale, boolean viewerHasStand) {
        if (owner.isSpectator()) {
            return;
        }

        ItemStack heldItem = StandTracker.thrownItem(owner, partialTick);
        if (!viewerHasStand && heldItem.isEmpty()) {
            return;
        }

        StandTracker.Pose pose = StandTracker.pose(owner, partialTick);
        float alpha = pose.alpha() * alphaScale;
        if (alpha < MIN_VISIBLE_ALPHA) {
            return;
        }

        Stand stand = Stands.get(owner);
        if (stand == null) {
            return;
        }

        StandRenderers.Baked baked = StandRenderers.get(stand.getId());
        if (baked == null) {
            return;
        }

        float standYaw = pose.yaw();
        float headYaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot);
        float headPitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());

        Vec3 velocity = StandTracker.velocity(owner, partialTick);
        double standYawRad = Math.toRadians(standYaw);
        double sinStandYaw = Math.sin(standYawRad);
        double cosStandYaw = Math.cos(standYawRad);
        double forwardSpeed = -sinStandYaw * velocity.x + cosStandYaw * velocity.z;
        double rightSpeed = -cosStandYaw * velocity.x - sinStandYaw * velocity.z;

        float leanForward = (float) Mth.clamp(
                forwardSpeed * LEAN_DEGREES_PER_BLOCK_PER_TICK, -MAX_LEAN_DEGREES, MAX_LEAN_DEGREES);
        float leanRight = (float) Mth.clamp(
                rightSpeed * LEAN_DEGREES_PER_BLOCK_PER_TICK, -MAX_LEAN_DEGREES, MAX_LEAN_DEGREES);

        float action = Math.max(Math.max(pose.defensive(), pose.combat()), pose.barrage());
        double hover = Mth.lerp(action, hoverOffset(level, partialTick), ACTION_HOVER);

        double standX = pose.position().x;
        double standY = pose.position().y + hover;
        double standZ = pose.position().z;

        int light = LevelRenderer.getLightColor(level, BlockPos.containing(standX, standY + LIGHT_SAMPLE_HEIGHT, standZ));

        poseStack.pushPose();
        poseStack.translate(standX - cameraPos.x, standY - cameraPos.y, standZ - cameraPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - standYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);

        double ageTicks = level.getGameTime() + partialTick + idlePhaseOffset(owner);
        baked.model().setupAnim(new StandAnimation(
                ageTicks,
                Mth.wrapDegrees(headYaw - standYaw),
                headPitch,
                leanForward,
                leanRight,
                pose.defensive(),
                pose.combat(),
                pose.leftPunch(),
                pose.rightPunch(),
                pose.throwTime(),
                pose.barrage(),
                pose.barrageTime(),
                pose.uppercutTime(),
                pose.slamTime(),
                Stands.getData(owner).pose()));

        if (viewerHasStand) {
            var layers = StandRenderers.skinLayers(stand, Stands.getData(owner), baked);
            int alphaByte = Mth.floor(alpha * 255.0F);
            for (StandRenderers.SkinLayer layer : layers) {
                int color = (alphaByte << 24) | (layer.color() & 0xFFFFFF);
                VertexConsumer buffer = bufferSource.getBuffer(baked.model().renderType(layer.texture()));
                baked.model().renderToBuffer(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, color);
            }
            renderGhostFists(owner, baked, poseStack, bufferSource, layers, light, alpha, partialTick);
        }

        if (!heldItem.isEmpty()) {
            poseStack.pushPose();
            baked.model().translateToRightHand(poseStack);
            poseStack.translate(-0.03125F, 0.25F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            Minecraft.getInstance().getItemRenderer().renderStatic(heldItem,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource, level, 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void renderGhostFists(Player owner, StandRenderers.Baked baked, PoseStack poseStack,
                                         MultiBufferSource bufferSource,
                                         java.util.List<StandRenderers.SkinLayer> layers,
                                         int light, float standAlpha, float partialTick) {
        var fists = StandTracker.ghostFists(owner);
        if (fists.isEmpty()) {
            return;
        }
        for (StandTracker.GhostFist fist : fists) {
            float progress = (fist.age + partialTick) / fist.lifetime;
            if (progress >= 1.0F) {
                continue;
            }
            Vec3 position = fist.position(progress);
            float alpha = standAlpha * Mth.sin(progress * Mth.PI);
            if (alpha < MIN_VISIBLE_ALPHA) {
                continue;
            }

            var part = baked.model().posableParts().get(fist.rightSide ? "right_arm_lower" : "left_arm_lower");
            if (part == null) {
                continue;
            }
            part.x = (float) position.x * 16.0F;
            part.y = (float) position.y * 16.0F;
            part.z = (float) position.z * 16.0F;
            part.xRot = fist.rotX;
            part.yRot = fist.rotY;
            part.zRot = fist.rotZ;

            int alphaByte = Mth.floor(alpha * 255.0F);
            for (StandRenderers.SkinLayer layer : layers) {
                int color = (alphaByte << 24) | (layer.color() & 0xFFFFFF);
                VertexConsumer buffer = bufferSource.getBuffer(baked.model().renderType(layer.texture()));
                part.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, color);
            }
        }
    }

    private static int idlePhaseOffset(Player owner) {
        return Math.floorMod(owner.getUUID().hashCode(), IDLE_PHASE_SPREAD_TICKS);
    }

    private static double hoverOffset(ClientLevel level, float partialTick) {
        double time = (level.getGameTime() % (long) BOB_PERIOD_TICKS) + partialTick;
        return HOVER_MID + HOVER_AMPLITUDE * Math.sin(time / BOB_PERIOD_TICKS * 2.0D * Math.PI);
    }
}
