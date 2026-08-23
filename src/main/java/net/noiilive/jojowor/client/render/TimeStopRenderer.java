package net.noiilive.jojowor.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.ability.TimeStops;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;


@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public final class TimeStopRenderer {
    public static final float EXPAND_TICKS = 8.0F;
    public static final float HOLD_TICKS = 5.0F;
    public static final float CONTRACT_TICKS = 22.0F;
    public static final float FADE_OUT_TICKS = 20.0F;
    public static final float GLOBAL_RADIUS = 320.0F;
    public static final float SKY_UNAFFECTED_DISTANCE = 1.0E9F;
    public static final float UNBOUNDED_GRAY_RADIUS = 1.0E5F;
    public static final float SKY_WAVE_FACTOR = 0.85F;
    public static final float GRAY_FADE_IN_TICKS = 3.0F;
    public static final float BLOWUP_START = 0.75F;
    public static final float RING_WIDTH = 1.1F;
    public static final float RING_STRENGTH = 0.9F;
    public static final float SECONDARY_RING_OFFSET = 4.5F;
    public static final float SECONDARY_RING_STRENGTH = 0.55F;
    public static final float GRAY_RING_WIDTH = 1.4F;
    public static final float GRAY_RING_STRENGTH = 0.35F;
    public static final float HUE_AMOUNT = 0.07F;
    public static final float HUE_SPEED = 0.45F;
    public static final float DISTORT_PULL = 0.34F;
    public static final float DISTORT_SQUIGGLE = 0.006F;
    public static final float DISTORT_WAVE_SCALE = 0.55F;
    public static final float DISTORT_TIME_SCALE = 0.3F;
    public static final float MASK_DEPTH_BIAS = 2.5F;

    @Nullable
    private static RenderTarget scratchTarget;
    @Nullable
    private static RenderTarget maskTarget;

    private static boolean active;
    private static int ownerEntityId;
    private static Vec3 center = Vec3.ZERO;
    private static float radius;
    private static boolean global;
    private static long startGameTime;
    private static float fadeStart = -1.0F;

    private TimeStopRenderer() {}

    public static void start(int ownerEntityId, Vec3 center, float radius, boolean global) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            active = false;
            return;
        }
        TimeStopRenderer.ownerEntityId = ownerEntityId;
        TimeStopRenderer.center = center;
        TimeStopRenderer.radius = radius;
        TimeStopRenderer.global = global;
        TimeStopRenderer.startGameTime = minecraft.level.getGameTime();
        TimeStopRenderer.fadeStart = -1.0F;
        active = true;
    }

    public static void stop() {
        if (!active || fadeStart >= 0.0F) {
            return;
        }
        fadeStart = elapsed(0.0F);
    }

    public static void clear() {
        active = false;
        fadeStart = -1.0F;
    }

    public static boolean isActive() {
        return active;
    }

    private static float elapsed(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        return minecraft.level.getGameTime() - startGameTime + partialTick;
    }

    private static Vec3 currentCenter(ClientLevel level, float partialTick) {
        Entity owner = level.getEntity(ownerEntityId);
        if (owner == null) {
            return center;
        }
        return new Vec3(
                Mth.lerp(partialTick, owner.xOld, owner.getX()),
                Mth.lerp(partialTick, owner.yOld, owner.getY())
                        + owner.getBbHeight() * TimeStops.CHEST_HEIGHT_FACTOR,
                Mth.lerp(partialTick, owner.zOld, owner.getZ()));
    }

    private static float smooth(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static boolean unbounded() {
        return global;
    }

    private static float maxRadius() {
        if (!unbounded()) {
            return radius;
        }
        return Math.min(GLOBAL_RADIUS,
                Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0F + 32.0F);
    }

    private static float waveRadius(float progress) {
        float eased = smooth(progress);
        float max = maxRadius();
        if (!unbounded() || eased <= BLOWUP_START) {
            return max * eased;
        }
        float anchor = max * BLOWUP_START;
        float extra = (eased - BLOWUP_START) / (1.0F - BLOWUP_START);
        return anchor * (float) Math.pow(UNBOUNDED_GRAY_RADIUS / anchor, extra);
    }

    private static float invertRadius(float time) {
        if (time < EXPAND_TICKS) {
            return waveRadius(time / EXPAND_TICKS);
        }
        if (time < EXPAND_TICKS + HOLD_TICKS) {
            return waveRadius(1.0F);
        }
        float contract = (time - EXPAND_TICKS - HOLD_TICKS) / CONTRACT_TICKS;
        return contract >= 1.0F ? 0.0F : waveRadius(1.0F - contract);
    }

    private static float ringStrength(float time) {
        return time <= EXPAND_TICKS + HOLD_TICKS + CONTRACT_TICKS ? RING_STRENGTH : 0.0F;
    }

    private static float grayRadius(float time) {
        if (unbounded()) {
            return UNBOUNDED_GRAY_RADIUS;
        }
        float max = maxRadius();
        return time < EXPAND_TICKS ? max * smooth(time / EXPAND_TICKS) : max;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!active || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ShaderInstance shader = ModShaders.timeStop();
        ClientLevel level = minecraft.level;
        if (shader == null || level == null) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = elapsed(partialTick);

        float fade = 1.0F;
        if (fadeStart >= 0.0F) {
            fade = Mth.clamp(1.0F - (time - fadeStart) / FADE_OUT_TICKS, 0.0F, 1.0F);
            if (fade <= 0.0F) {
                clear();
                return;
            }
        }

        float invertRadius = invertRadius(time);
        float grayRadius = grayRadius(time);
        float invertStrength = fade;
        float grayStrength = fade * Mth.clamp(time / GRAY_FADE_IN_TICKS, 0.0F, 1.0F);

        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget scratch = ensureScratch(main.width, main.height);
        RenderTarget mask = ensureMask(main.width, main.height);

        Vec3 cameraPos = event.getCamera().getPosition();
        renderMask(minecraft, level, mask, main, event.getModelViewMatrix(), cameraPos, partialTick,
                event.getPartialTick().getGameTimeDeltaPartialTick(true));

        Matrix4f forward = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        Matrix4f inverse = new Matrix4f(forward).invert();
        Vec3 relativeCenter = currentCenter(level, partialTick).subtract(cameraPos);

        shader.setSampler("DiffuseSampler", main.getColorTextureId());
        shader.setSampler("DepthSampler", main.getDepthTextureId());
        shader.setSampler("MaskSampler", mask.getColorTextureId());
        shader.safeGetUniform("InverseTransform").set(inverse);
        shader.safeGetUniform("ForwardTransform").set(forward);
        shader.safeGetUniform("SphereCenter").set(
                (float) relativeCenter.x, (float) relativeCenter.y, (float) relativeCenter.z);
        shader.safeGetUniform("EffectParams").set(
                invertRadius, grayRadius, grayStrength, invertStrength);
        float ringStrength = ringStrength(time) * fade;
        shader.safeGetUniform("RingParams").set(
                RING_WIDTH, ringStrength, SECONDARY_RING_OFFSET, ringStrength * SECONDARY_RING_STRENGTH);
        shader.safeGetUniform("GrayRingParams").set(GRAY_RING_WIDTH, GRAY_RING_STRENGTH * fade);
        shader.safeGetUniform("HueParams").set(HUE_AMOUNT, time * HUE_SPEED);
        shader.safeGetUniform("DistortParams").set(
                DISTORT_PULL * fade, DISTORT_SQUIGGLE * fade, DISTORT_WAVE_SCALE, time * DISTORT_TIME_SCALE);
        shader.safeGetUniform("SkyDistance").set(
                unbounded() ? maxRadius() * SKY_WAVE_FACTOR : SKY_UNAFFECTED_DISTANCE);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.viewport(0, 0, main.width, main.height);

        scratch.bindWrite(false);
        RenderSystem.setShader(() -> shader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(-1.0F, -1.0F, 0.0F);
        builder.addVertex(1.0F, -1.0F, 0.0F);
        builder.addVertex(1.0F, 1.0F, 0.0F);
        builder.addVertex(-1.0F, 1.0F, 0.0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        shader.clear();
        scratch.unbindWrite();

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, scratch.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);

        main.bindWrite(true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderMask(Minecraft minecraft, ClientLevel level, RenderTarget mask, RenderTarget main,
                                   Matrix4f modelView, Vec3 cameraPos, float partialTick, float standPartialTick) {
        mask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mask.clear(Minecraft.ON_OSX);
        mask.copyDepthFrom(main);
        mask.bindWrite(false);
        RenderSystem.viewport(0, 0, main.width, main.height);
        RenderSystem.enableDepthTest();
        RenderSystem.polygonOffset(-MASK_DEPTH_BIAS, -MASK_DEPTH_BIAS);
        RenderSystem.enablePolygonOffset();

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(modelView);

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        dispatcher.setRenderShadow(false);

        for (int moverId : net.noiilive.jojowor.client.ClientTimeStop.owners()) {
            Entity mover = level.getEntity(moverId);
            if (mover == null || mover.isSpectator()) {
                continue;
            }
            boolean hidden = firstPerson && mover == minecraft.getCameraEntity();
            if (!hidden) {
                double x = Mth.lerp(partialTick, mover.xOld, mover.getX()) - cameraPos.x;
                double y = Mth.lerp(partialTick, mover.yOld, mover.getY()) - cameraPos.y;
                double z = Mth.lerp(partialTick, mover.zOld, mover.getZ()) - cameraPos.z;
                float yaw = Mth.lerp(partialTick, mover.yRotO, mover.getYRot());
                dispatcher.render(mover, x, y, z, yaw, partialTick, poseStack, bufferSource,
                        LightTexture.FULL_BRIGHT);
            }
            if (mover instanceof Player player) {
                StandRenderHandler.render(player, level, poseStack, bufferSource, cameraPos, standPartialTick,
                        1.0F, true);
            }
        }

        bufferSource.endBatch();
        dispatcher.setRenderShadow(true);
        RenderSystem.disablePolygonOffset();
        RenderSystem.polygonOffset(0.0F, 0.0F);
        mask.unbindWrite();
        RenderSystem.depthMask(true);
    }

    private static RenderTarget ensureScratch(int width, int height) {
        if (scratchTarget == null) {
            scratchTarget = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            scratchTarget.setFilterMode(GL30.GL_NEAREST);
        } else if (scratchTarget.width != width || scratchTarget.height != height) {
            scratchTarget.resize(width, height, Minecraft.ON_OSX);
        }
        return scratchTarget;
    }

    private static RenderTarget ensureMask(int width, int height) {
        if (maskTarget == null) {
            maskTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            maskTarget.setFilterMode(GL30.GL_NEAREST);
        } else if (maskTarget.width != width || maskTarget.height != height) {
            maskTarget.resize(width, height, Minecraft.ON_OSX);
        }
        return maskTarget;
    }
}
