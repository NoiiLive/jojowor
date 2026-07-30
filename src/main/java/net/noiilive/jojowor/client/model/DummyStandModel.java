package net.noiilive.jojowor.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DummyStandModel extends StandModel {
    private static final double BREATHE_PERIOD_TICKS = 100.0D;
    private static final double SWAY_PERIOD_TICKS = 80.0D;
    private static final double LOWER_LIMB_LAG = 0.8D;

    private static final float ARM_SPREAD_REST = 0.06F;
    private static final float ARM_SPREAD_BREATHE = 0.06F;
    private static final float ARM_SWAY = 0.07F;
    private static final float LOWER_ARM_FLEX = 0.05F;
    private static final float LEG_SWAY = 0.035F;
    private static final float LOWER_LEG_FLEX = 0.03F;

    private static final float[] DEFENSIVE_LEFT_ARM_ROT = {-69.0937F, -8.4819F, 73.3996F};
    private static final float[] DEFENSIVE_LEFT_ARM_POS = {-1.0F, -0.25F, -3.0F};
    private static final float[] DEFENSIVE_LEFT_LOWER_ROT = {0.0F, 12.5F, 75.0F};
    private static final float[] DEFENSIVE_LEFT_LEG_ROT = {-7.5F, 0.0F, 0.0F};
    private static final float[] DEFENSIVE_LEFT_LEG_LOWER_ROT = {5.0047F, -2.4905F, -0.218F};
    private static final float[] DEFENSIVE_RIGHT_LEG_ROT = {5.0F, 0.0F, 0.0F};
    private static final float[] DEFENSIVE_RIGHT_LEG_LOWER_ROT = {5.0047F, 2.4905F, 0.218F};

    private static final float[] STANCE_ARM_ROT = {64.787F, 18.1014F, 17.6665F};
    private static final float[] STANCE_LOWER_ROT = {-85.0189F, -0.4352F, 4.9811F};

    private static final float COMBAT_BODY_LEAN = 0.06F;
    private static final float COMBAT_FRONT_LEG = -0.2F;
    private static final float COMBAT_FRONT_KNEE = 0.22F;
    private static final float COMBAT_BACK_LEG = 0.15F;
    private static final float COMBAT_BACK_KNEE = 0.2F;

    private static final float[][] PUNCH_ARM_ROT = {
            {0.0F, 50.4313F, -6.4086F, -7.6926F},
            {1.667F, 54.8572F, -0.2003F, -87.4279F},
            {3.333F, -92.5F, 0.0F, -90.0F},
            {5.833F, -92.5F, 0.0F, -90.0F}};
    private static final float[][] PUNCH_ARM_POS_Z = {
            {0.0F, 0.0F},
            {1.667F, 0.0F},
            {3.333F, -1.0F},
            {4.167F, 0.0F}};
    private static final float[][] PUNCH_LOWER_ROT = {
            {0.0F, -97.4718F, -0.6518F, -4.9574F},
            {1.667F, -95.546F, -0.0204F, -0.1549F},
            {3.333F, 0.0F, 0.0F, 0.0F}};
    private static final float[][] OFF_ARM_ROT = {
            {0.0F, 64.787F, 18.1014F, 17.6665F},
            {3.333F, 68.9287F, 20.7359F, 30.8918F}};
    private static final float[][] OFF_LOWER_ROT = {
            {0.0F, -85.0189F, -0.4352F, 4.9811F},
            {3.333F, -30.0189F, -0.4352F, 4.9811F}};

    private static final float PUNCH_BLEND_IN_TICKS = 1.5F;
    private static final float PUNCH_HOLD_END_TICKS = 5.833F;
    private static final float PUNCH_RETURN_TICKS = 5.0F;

    private static final float[][] THROW_HEAD_ROT = {
            {0.0F, 0.0F, -5.0F, 0.0F},
            {2.5F, 0.0F, 0.0F, 0.0F}};
    private static final float[][] THROW_BODY_UPPER_ROT = {
            {0.0F, 0.0F, 10.0F, 0.0F},
            {2.5F, 0.0F, 0.0F, 0.0F}};
    private static final float[][] THROW_ARM_ROT = {
            {0.0F, 47.3069F, 4.0151F, 13.8366F},
            {2.5F, -87.5F, 0.0F, 0.0F},
            {5.0F, -97.5F, 0.0F, 0.0F}};
    private static final float[][] THROW_ARM_POS_Z = {
            {0.0F, 0.0F},
            {2.5F, -2.0F},
            {5.0F, -2.0F}};
    private static final float[][] THROW_LOWER_ROT = {
            {0.0F, -62.5273F, -1.7675F, 1.7683F},
            {2.5F, -2.5024F, -2.4976F, 0.1091F},
            {5.0F, -7.5024F, -2.4976F, 0.1091F}};

    private static final float THROW_BLEND_IN_TICKS = 2.0F;
    private static final float THROW_ANIM_START_TICKS = 6.0F;
    private static final float THROW_ANIM_END_TICKS = 11.0F;
    private static final float THROW_RETURN_TICKS = 4.0F;

    private static final float[][] UPPERCUT_LEFT_ARM_ROT = {
            {0.0F, 30.1178F, -22.9824F, -24.562F},
            {5.0F, 52.9513F, -10.2447F, -28.786F}};
    private static final float[][] UPPERCUT_LEFT_LOWER_ROT = {
            {0.0F, -27.5896F, 4.4338F, -2.3134F},
            {3.333F, -52.5896F, 4.4338F, -2.3134F}};
    private static final float[][] UPPERCUT_RIGHT_ARM_ROT = {
            {0.0F, 20.6469F, 14.0761F, 30.2362F},
            {3.333F, -107.0229F, 1.9246F, -1.1719F},
            {5.0F, -112.0229F, 1.9246F, -1.1719F}};
    private static final float[][] UPPERCUT_RIGHT_ARM_POS = {
            {0.0F, 0.0F, 0.0F, 0.0F},
            {3.333F, -0.1233F, -2.0F, -1.4088F}};
    private static final float[][] UPPERCUT_RIGHT_LOWER_ROT = {
            {0.0F, -77.5F, -7.5F, 0.0F},
            {3.333F, -91.8661F, 37.1273F, -90.5528F}};

    private static final float UPPERCUT_BLEND_IN_TICKS = 1.5F;
    private static final float UPPERCUT_HOLD_END_TICKS = 7.0F;
    private static final float UPPERCUT_RETURN_TICKS = 5.0F;

    private static final float[][] SLAM_BODY_ROT = {
            {0.0F, -12.5F, 0.0F, 0.0F},
            {3.333F, 15.0F, 0.0F, 0.0F}};
    private static final float[][] SLAM_BODY_UPPER_ROT = {
            {0.0F, -7.5F, 0.0F, 0.0F},
            {3.333F, 30.0F, 0.0F, 0.0F}};
    private static final float[][] SLAM_LEFT_ARM_ROT = {
            {0.0F, -147.3342F, -16.6097F, 6.941F},
            {3.333F, -66.3067F, -7.0324F, -1.2404F}};
    private static final float[][] SLAM_ARM_POS = {
            {0.0F, 0.0F, 0.0F, 0.0F},
            {3.333F, 0.0F, 0.7071F, -3.5355F}};
    private static final float[][] SLAM_LEFT_LOWER_ROT = {
            {0.0F, -35.0256F, 2.0477F, -1.4345F},
            {3.333F, -70.0256F, 2.0477F, -1.4345F}};
    private static final float[][] SLAM_LEFT_LEG_ROT = {
            {0.0F, 27.5F, 0.0F, 0.0F},
            {3.333F, 2.5024F, -2.4976F, -0.1091F}};
    private static final float[][] SLAM_LEFT_LEG_LOWER_ROT = {
            {0.0F, 15.0137F, -2.4148F, -0.6474F},
            {3.333F, 25.0136F, -2.4148F, -0.6474F}};
    private static final float[][] SLAM_RIGHT_LEG_ROT = {
            {0.0F, 17.5F, 0.0F, 0.0F},
            {3.333F, -45.0273F, 1.7675F, -1.7683F}};
    private static final float[][] SLAM_RIGHT_LEG_LOWER_ROT = {
            {0.0F, 12.5115F, 2.4407F, 0.5414F},
            {3.333F, 40.0115F, 2.4407F, 0.5414F}};

    private static final float SLAM_BLEND_IN_TICKS = 1.5F;
    private static final float SLAM_HOLD_END_TICKS = 7.0F;
    private static final float SLAM_RETURN_TICKS = 5.0F;

    private static final float BARRAGE_LOOP_TICKS = 5.0F;
    private static final float[][] BARRAGE_HEAD_ROT = {
            {0.0F, -32.5F, 0.0F, 0.0F},
            {2.5F, -27.5F, 0.0F, 0.0F},
            {5.0F, -32.5F, 0.0F, 0.0F}};
    private static final float[] BARRAGE_BODY_ROT = {10.0F, 0.0F, 0.0F};
    private static final float[] BARRAGE_BODY_UPPER_ROT = {27.5F, 0.0F, 0.0F};
    private static final float[][] BARRAGE_LEFT_ARM_ROT = {
            {0.0F, 54.4224F, -38.5957F, -85.7148F},
            {0.833F, -24.5876F, -34.5327F, -90.3023F},
            {1.667F, -49.5876F, -34.5327F, -90.3023F},
            {2.5F, -87.0875F, -34.5327F, -90.3023F},
            {3.333F, -49.5876F, -34.5327F, -90.3023F},
            {4.167F, -27.0876F, -34.5327F, -90.3023F},
            {5.0F, 54.4224F, -38.5957F, -85.7148F}};
    private static final float[][] BARRAGE_LEFT_ARM_POS = {
            {0.0F, -1.2785F, 1.5831F, 1.9834F},
            {0.833F, 1.7626F, 1.033F, 1.3713F},
            {2.5F, 1.0F, -1.2175F, -1.5867F},
            {4.167F, 1.7626F, 1.033F, 1.3713F},
            {5.0F, -1.2785F, 1.5831F, 1.9834F}};
    private static final float[][] BARRAGE_LEFT_LOWER_ROT = {
            {0.0F, -102.5F, 0.0F, 0.0F},
            {2.5F, 0.0F, 0.0F, 0.0F},
            {5.0F, -90.0F, 0.0F, 0.0F}};
    private static final float[][] BARRAGE_RIGHT_ARM_ROT = {
            {0.0F, -87.0875F, 34.5327F, 90.3023F},
            {0.833F, -49.5876F, 34.5327F, 90.3023F},
            {1.667F, -27.0876F, 34.5327F, 90.3023F},
            {2.5F, 54.4224F, 38.5957F, 85.7148F},
            {3.333F, -24.5876F, 34.5327F, 90.3023F},
            {4.167F, -49.5876F, 34.5327F, 90.3023F},
            {5.0F, -87.0875F, 34.5327F, 90.3023F}};
    private static final float[][] BARRAGE_RIGHT_ARM_POS = {
            {0.0F, -1.0F, -1.2175F, -1.5867F},
            {1.667F, -1.7626F, 1.033F, 1.3713F},
            {2.5F, 1.2785F, 1.5831F, 1.9834F},
            {3.333F, -1.7626F, 1.033F, 1.3713F},
            {5.0F, -1.0F, -1.2175F, -1.5867F}};
    private static final float[][] BARRAGE_RIGHT_LOWER_ROT = {
            {0.0F, 0.0F, 0.0F, 0.0F},
            {2.5F, -102.5F, 0.0F, 0.0F},
            {5.0F, 0.0F, 0.0F, 0.0F}};
    private static final float[][] BARRAGE_LEFT_LEG_ROT = {
            {0.0F, -80.0F, 0.0F, 0.0F},
            {2.5F, -95.0F, 0.0F, 0.0F},
            {5.0F, -80.0F, 0.0F, 0.0F}};
    private static final float[][] BARRAGE_LEFT_LEG_LOWER_ROT = {
            {0.0F, 90.0F, 0.0F, 0.0F},
            {2.5F, 105.0F, 0.0F, 0.0F},
            {5.0F, 90.0F, 0.0F, 0.0F}};
    private static final float[][] BARRAGE_RIGHT_LEG_ROT = {
            {0.0F, 25.0F, 0.0F, 0.0F},
            {2.5F, 35.0F, 0.0F, 0.0F},
            {5.0F, 25.0F, 0.0F, 0.0F}};
    private static final float[][] BARRAGE_RIGHT_LEG_LOWER_ROT = {
            {0.0F, 20.0F, 0.0F, 0.0F},
            {2.5F, 35.0F, 0.0F, 0.0F},
            {5.0F, 20.0F, 0.0F, 0.0F}};

    private final ModelPart body;
    private final ModelPart body_upper;
    private final ModelPart head;
    private final ModelPart left_arm;
    private final ModelPart left_arm_lower;
    private final ModelPart right_arm;
    private final ModelPart right_arm_lower;
    private final ModelPart left_leg;
    private final ModelPart left_leg_lower;
    private final ModelPart right_leg;
    private final ModelPart right_leg_lower;

    private final Map<String, ModelPart> posableParts;

    public DummyStandModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.body = root.getChild("body");
        this.body_upper = this.body.getChild("body_upper");
        this.head = this.body_upper.getChild("head");
        this.left_arm = this.body_upper.getChild("left_arm");
        this.left_arm_lower = this.left_arm.getChild("left_arm_lower");
        this.right_arm = this.body_upper.getChild("right_arm");
        this.right_arm_lower = this.right_arm.getChild("right_arm_lower");
        this.left_leg = this.body.getChild("left_leg");
        this.left_leg_lower = this.left_leg.getChild("left_leg_lower");
        this.right_leg = this.body.getChild("right_leg");
        this.right_leg_lower = this.right_leg.getChild("right_leg_lower");

        Map<String, ModelPart> parts = new LinkedHashMap<>();
        parts.put("body", this.body);
        parts.put("body_upper", this.body_upper);
        parts.put("head", this.head);
        parts.put("left_arm", this.left_arm);
        parts.put("left_arm_lower", this.left_arm_lower);
        parts.put("right_arm", this.right_arm);
        parts.put("right_arm_lower", this.right_arm_lower);
        parts.put("left_leg", this.left_leg);
        parts.put("left_leg_lower", this.left_leg_lower);
        parts.put("right_leg", this.right_leg);
        parts.put("right_leg_lower", this.right_leg_lower);
        this.posableParts = Collections.unmodifiableMap(parts);
    }

    @Override
    public Map<String, ModelPart> posableParts() {
        return this.posableParts;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(24, 16).addBox(-4.0F, 6.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition body_upper = body.addOrReplaceChild("body_upper", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -6.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));

        PartDefinition head = body_upper.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, 0.0F));

        PartDefinition left_arm = body_upper.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 26).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -4.0F, 0.0F));

        PartDefinition left_arm_lower = left_arm.addOrReplaceChild("left_arm_lower", CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 4.0F, 0.0F));

        PartDefinition right_arm = body_upper.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 26).mirror().addBox(-3.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-5.0F, -4.0F, 0.0F));

        PartDefinition right_arm_lower = right_arm.addOrReplaceChild("right_arm_lower", CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-1.0F, 4.0F, 0.0F));

        PartDefinition left_leg = body.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 26).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 12.0F, 0.0F));

        PartDefinition left_leg_lower = left_leg.addOrReplaceChild("left_leg_lower", CubeListBuilder.create().texOffs(32, 26).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 6.0F, 0.0F));

        PartDefinition right_leg = body.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 26).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.0F, 12.0F, 0.0F));

        PartDefinition right_leg_lower = right_leg.addOrReplaceChild("right_leg_lower", CubeListBuilder.create().texOffs(32, 26).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 6.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(StandAnimation animation) {
        this.body.getAllParts().forEach(ModelPart::resetPose);

        float defensive = animation.defensive();
        float leftT = animation.leftPunch();
        float rightT = animation.rightPunch();
        float throwT = animation.throwTime();
        float barrage = animation.barrage();
        float uppercutT = animation.uppercutTime();
        float slamT = animation.slamTime();
        float leftWeight = punchWeight(leftT);
        float rightWeight = punchWeight(rightT);
        float throwWeight = throwWeight(throwT);
        float uppercutWeight = uppercutWeight(uppercutT);
        float slamWeight = slamWeight(slamT);
        float punchActivity = Math.max(Math.max(Math.max(leftWeight, rightWeight), uppercutWeight), slamWeight);
        float fullBody = Math.max(barrage, slamWeight);
        float stance = animation.combat() * (1.0F - defensive) * (1.0F - fullBody);
        float overrideAll = Math.max(punchActivity, barrage);
        float poseAmount = 1.0F - Math.max(defensive * (1.0F - fullBody) + fullBody,
                Math.max(stance, overrideAll));
        float idleAmount = 1.0F - overrideAll;
        float rightArmPoseScale = 1.0F - throwWeight;

        applyPose(animation.pose(), name ->
                "right_arm".equals(name) || "right_arm_lower".equals(name)
                        ? poseAmount * rightArmPoseScale
                        : poseAmount);

        this.body.xRot += animation.leanForward() * Mth.DEG_TO_RAD;
        this.body.zRot += -animation.leanRight() * Mth.DEG_TO_RAD;

        this.head.yRot += animation.headYaw() * Mth.DEG_TO_RAD;
        this.head.xRot += (animation.headPitch() - animation.leanForward()) * Mth.DEG_TO_RAD;
        this.head.zRot += animation.leanRight() * Mth.DEG_TO_RAD;

        applyDefensive(defensive * (1.0F - fullBody));

        float leftArmAnim = Math.max(Math.max(fullBody, uppercutWeight),
                leftT >= 0.0F ? leftWeight : (rightT >= 0.0F ? rightWeight : 0.0F));
        float rightArmAnim = Math.max(Math.max(fullBody, uppercutWeight), Math.max(throwWeight,
                rightT >= 0.0F ? rightWeight : (leftT >= 0.0F ? leftWeight : 0.0F)));
        applyStance(stance, stance * (1.0F - leftArmAnim), stance * (1.0F - rightArmAnim));

        if (barrage > 0.01F) {
            applyBarrage(animation.barrageTime(), barrage);
        }
        if (uppercutWeight > 0.01F) {
            applyUppercut(uppercutT, uppercutWeight);
        }
        if (slamWeight > 0.01F) {
            applySlam(slamT, slamWeight);
        }

        if (leftT >= 0.0F) {
            applyPunchRole(leftT, leftWeight, true);
        } else if (rightT >= 0.0F) {
            applyOffRole(rightT, rightWeight, true);
        }
        if (throwT >= 0.0F) {
            applyThrow(throwT, throwWeight);
        } else if (rightT >= 0.0F) {
            applyPunchRole(rightT, rightWeight, false);
        } else if (leftT >= 0.0F) {
            applyOffRole(leftT, leftWeight, false);
        }

        if (idleAmount > 0.01F) {
            applyIdle(animation.ageTicks(), idleAmount, 1.0F - throwWeight);
        }
    }

    private void applyUppercut(float time, float weight) {
        applySampledRotation(this.left_arm, UPPERCUT_LEFT_ARM_ROT, time, weight);
        applySampledRotation(this.left_arm_lower, UPPERCUT_LEFT_LOWER_ROT, time, weight);
        applySampledRotation(this.right_arm, UPPERCUT_RIGHT_ARM_ROT, time, weight);
        applySampledPosition(this.right_arm, UPPERCUT_RIGHT_ARM_POS, time, weight);
        applySampledRotation(this.right_arm_lower, UPPERCUT_RIGHT_LOWER_ROT, time, weight);
    }

    private static float uppercutWeight(float time) {
        if (time < 0.0F) {
            return 0.0F;
        }
        if (time < UPPERCUT_BLEND_IN_TICKS) {
            float progress = time / UPPERCUT_BLEND_IN_TICKS;
            return progress * progress * (3.0F - 2.0F * progress);
        }
        if (time < UPPERCUT_HOLD_END_TICKS) {
            return 1.0F;
        }
        float progress = (time - UPPERCUT_HOLD_END_TICKS) / UPPERCUT_RETURN_TICKS;
        if (progress >= 1.0F) {
            return 0.0F;
        }
        float eased = progress * progress * (3.0F - 2.0F * progress);
        return 1.0F - eased;
    }

    private void applySlam(float time, float weight) {
        applySampledRotation(this.body, SLAM_BODY_ROT, time, weight);
        applySampledRotation(this.body_upper, SLAM_BODY_UPPER_ROT, time, weight);

        this.left_arm.xRot += sample(SLAM_LEFT_ARM_ROT, time, 1) * Mth.DEG_TO_RAD * weight;
        this.left_arm.yRot += sample(SLAM_LEFT_ARM_ROT, time, 2) * Mth.DEG_TO_RAD * weight;
        this.left_arm.zRot += sample(SLAM_LEFT_ARM_ROT, time, 3) * Mth.DEG_TO_RAD * weight;
        this.right_arm.xRot += sample(SLAM_LEFT_ARM_ROT, time, 1) * Mth.DEG_TO_RAD * weight;
        this.right_arm.yRot += -sample(SLAM_LEFT_ARM_ROT, time, 2) * Mth.DEG_TO_RAD * weight;
        this.right_arm.zRot += -sample(SLAM_LEFT_ARM_ROT, time, 3) * Mth.DEG_TO_RAD * weight;
        applySampledPosition(this.left_arm, SLAM_ARM_POS, time, weight);
        applySampledPosition(this.right_arm, SLAM_ARM_POS, time, weight);

        this.left_arm_lower.xRot += sample(SLAM_LEFT_LOWER_ROT, time, 1) * Mth.DEG_TO_RAD * weight;
        this.left_arm_lower.yRot += sample(SLAM_LEFT_LOWER_ROT, time, 2) * Mth.DEG_TO_RAD * weight;
        this.left_arm_lower.zRot += sample(SLAM_LEFT_LOWER_ROT, time, 3) * Mth.DEG_TO_RAD * weight;
        this.right_arm_lower.xRot += sample(SLAM_LEFT_LOWER_ROT, time, 1) * Mth.DEG_TO_RAD * weight;
        this.right_arm_lower.yRot += -sample(SLAM_LEFT_LOWER_ROT, time, 2) * Mth.DEG_TO_RAD * weight;
        this.right_arm_lower.zRot += -sample(SLAM_LEFT_LOWER_ROT, time, 3) * Mth.DEG_TO_RAD * weight;

        applySampledRotation(this.left_leg, SLAM_LEFT_LEG_ROT, time, weight);
        applySampledRotation(this.left_leg_lower, SLAM_LEFT_LEG_LOWER_ROT, time, weight);
        applySampledRotation(this.right_leg, SLAM_RIGHT_LEG_ROT, time, weight);
        applySampledRotation(this.right_leg_lower, SLAM_RIGHT_LEG_LOWER_ROT, time, weight);
    }

    private static float slamWeight(float time) {
        if (time < 0.0F) {
            return 0.0F;
        }
        if (time < SLAM_BLEND_IN_TICKS) {
            float progress = time / SLAM_BLEND_IN_TICKS;
            return progress * progress * (3.0F - 2.0F * progress);
        }
        if (time < SLAM_HOLD_END_TICKS) {
            return 1.0F;
        }
        float progress = (time - SLAM_HOLD_END_TICKS) / SLAM_RETURN_TICKS;
        if (progress >= 1.0F) {
            return 0.0F;
        }
        float eased = progress * progress * (3.0F - 2.0F * progress);
        return 1.0F - eased;
    }

    private void applyBarrage(float time, float weight) {
        float loop = time % BARRAGE_LOOP_TICKS;

        applyRotation(this.body, BARRAGE_BODY_ROT, weight, 1.0F);
        applyRotation(this.body_upper, BARRAGE_BODY_UPPER_ROT, weight, 1.0F);
        this.head.xRot += sample(BARRAGE_HEAD_ROT, loop, 1) * Mth.DEG_TO_RAD * weight;

        applySampledRotation(this.left_arm, BARRAGE_LEFT_ARM_ROT, loop, weight);
        applySampledPosition(this.left_arm, BARRAGE_LEFT_ARM_POS, loop, weight);
        applySampledRotation(this.left_arm_lower, BARRAGE_LEFT_LOWER_ROT, loop, weight);

        applySampledRotation(this.right_arm, BARRAGE_RIGHT_ARM_ROT, loop, weight);
        applySampledPosition(this.right_arm, BARRAGE_RIGHT_ARM_POS, loop, weight);
        applySampledRotation(this.right_arm_lower, BARRAGE_RIGHT_LOWER_ROT, loop, weight);

        applySampledRotation(this.left_leg, BARRAGE_LEFT_LEG_ROT, loop, weight);
        applySampledRotation(this.left_leg_lower, BARRAGE_LEFT_LEG_LOWER_ROT, loop, weight);
        applySampledRotation(this.right_leg, BARRAGE_RIGHT_LEG_ROT, loop, weight);
        applySampledRotation(this.right_leg_lower, BARRAGE_RIGHT_LEG_LOWER_ROT, loop, weight);
    }

    private static void applySampledRotation(ModelPart part, float[][] keys, float time, float weight) {
        part.xRot += sample(keys, time, 1) * Mth.DEG_TO_RAD * weight;
        part.yRot += sample(keys, time, 2) * Mth.DEG_TO_RAD * weight;
        part.zRot += sample(keys, time, 3) * Mth.DEG_TO_RAD * weight;
    }

    private static void applySampledPosition(ModelPart part, float[][] keys, float time, float weight) {
        part.x += sample(keys, time, 1) * weight;
        part.y += sample(keys, time, 2) * weight;
        part.z += sample(keys, time, 3) * weight;
    }

    private void applyDefensive(float amount) {
        if (amount <= 0.0F) {
            return;
        }

        applyRotation(this.left_arm, DEFENSIVE_LEFT_ARM_ROT, amount, 1.0F);
        applyRotation(this.right_arm, DEFENSIVE_LEFT_ARM_ROT, amount, -1.0F);
        this.left_arm.x += DEFENSIVE_LEFT_ARM_POS[0] * amount;
        this.left_arm.y += DEFENSIVE_LEFT_ARM_POS[1] * amount;
        this.left_arm.z += DEFENSIVE_LEFT_ARM_POS[2] * amount;
        this.right_arm.x += -DEFENSIVE_LEFT_ARM_POS[0] * amount;
        this.right_arm.y += DEFENSIVE_LEFT_ARM_POS[1] * amount;
        this.right_arm.z += DEFENSIVE_LEFT_ARM_POS[2] * amount;

        applyRotation(this.left_arm_lower, DEFENSIVE_LEFT_LOWER_ROT, amount, 1.0F);
        applyRotation(this.right_arm_lower, DEFENSIVE_LEFT_LOWER_ROT, amount, -1.0F);

        applyRotation(this.left_leg, DEFENSIVE_LEFT_LEG_ROT, amount, 1.0F);
        applyRotation(this.left_leg_lower, DEFENSIVE_LEFT_LEG_LOWER_ROT, amount, 1.0F);
        applyRotation(this.right_leg, DEFENSIVE_RIGHT_LEG_ROT, amount, 1.0F);
        applyRotation(this.right_leg_lower, DEFENSIVE_RIGHT_LEG_LOWER_ROT, amount, 1.0F);
    }

    private void applyStance(float legAmount, float leftArmAmount, float rightArmAmount) {
        if (legAmount > 0.0F) {
            this.body.xRot += COMBAT_BODY_LEAN * legAmount;
            this.left_leg.xRot += COMBAT_FRONT_LEG * legAmount;
            this.left_leg_lower.xRot += COMBAT_FRONT_KNEE * legAmount;
            this.right_leg.xRot += COMBAT_BACK_LEG * legAmount;
            this.right_leg_lower.xRot += COMBAT_BACK_KNEE * legAmount;
        }
        if (leftArmAmount > 0.0F) {
            applyRotation(this.left_arm, STANCE_ARM_ROT, leftArmAmount, -1.0F);
            applyRotation(this.left_arm_lower, STANCE_LOWER_ROT, leftArmAmount, -1.0F);
        }
        if (rightArmAmount > 0.0F) {
            applyRotation(this.right_arm, STANCE_ARM_ROT, rightArmAmount, 1.0F);
            applyRotation(this.right_arm_lower, STANCE_LOWER_ROT, rightArmAmount, 1.0F);
        }
    }

    private void applyPunchRole(float time, float weight, boolean leftSide) {
        ModelPart arm = leftSide ? this.left_arm : this.right_arm;
        ModelPart lower = leftSide ? this.left_arm_lower : this.right_arm_lower;
        float mirror = leftSide ? 1.0F : -1.0F;

        arm.xRot += sample(PUNCH_ARM_ROT, time, 1) * Mth.DEG_TO_RAD * weight;
        arm.yRot += sample(PUNCH_ARM_ROT, time, 2) * Mth.DEG_TO_RAD * mirror * weight;
        arm.zRot += sample(PUNCH_ARM_ROT, time, 3) * Mth.DEG_TO_RAD * mirror * weight;
        arm.z += sample(PUNCH_ARM_POS_Z, time, 1) * weight;

        lower.xRot += sample(PUNCH_LOWER_ROT, time, 1) * Mth.DEG_TO_RAD * weight;
        lower.yRot += sample(PUNCH_LOWER_ROT, time, 2) * Mth.DEG_TO_RAD * mirror * weight;
        lower.zRot += sample(PUNCH_LOWER_ROT, time, 3) * Mth.DEG_TO_RAD * mirror * weight;
    }

    private void applyOffRole(float time, float weight, boolean leftSide) {
        ModelPart arm = leftSide ? this.left_arm : this.right_arm;
        ModelPart lower = leftSide ? this.left_arm_lower : this.right_arm_lower;
        float mirror = leftSide ? -1.0F : 1.0F;

        arm.xRot += sample(OFF_ARM_ROT, time, 1) * Mth.DEG_TO_RAD * weight;
        arm.yRot += sample(OFF_ARM_ROT, time, 2) * Mth.DEG_TO_RAD * mirror * weight;
        arm.zRot += sample(OFF_ARM_ROT, time, 3) * Mth.DEG_TO_RAD * mirror * weight;

        lower.xRot += sample(OFF_LOWER_ROT, time, 1) * Mth.DEG_TO_RAD * weight;
        lower.yRot += sample(OFF_LOWER_ROT, time, 2) * Mth.DEG_TO_RAD * mirror * weight;
        lower.zRot += sample(OFF_LOWER_ROT, time, 3) * Mth.DEG_TO_RAD * mirror * weight;
    }

    private void applyThrow(float time, float weight) {
        float animTime = time - THROW_ANIM_START_TICKS;

        this.head.yRot += sample(THROW_HEAD_ROT, animTime, 2) * Mth.DEG_TO_RAD * weight;
        this.body_upper.yRot += sample(THROW_BODY_UPPER_ROT, animTime, 2) * Mth.DEG_TO_RAD * weight;

        this.right_arm.xRot += sample(THROW_ARM_ROT, animTime, 1) * Mth.DEG_TO_RAD * weight;
        this.right_arm.yRot += sample(THROW_ARM_ROT, animTime, 2) * Mth.DEG_TO_RAD * weight;
        this.right_arm.zRot += sample(THROW_ARM_ROT, animTime, 3) * Mth.DEG_TO_RAD * weight;
        this.right_arm.z += sample(THROW_ARM_POS_Z, animTime, 1) * weight;

        this.right_arm_lower.xRot += sample(THROW_LOWER_ROT, animTime, 1) * Mth.DEG_TO_RAD * weight;
        this.right_arm_lower.yRot += sample(THROW_LOWER_ROT, animTime, 2) * Mth.DEG_TO_RAD * weight;
        this.right_arm_lower.zRot += sample(THROW_LOWER_ROT, animTime, 3) * Mth.DEG_TO_RAD * weight;
    }

    private static float throwWeight(float time) {
        if (time < 0.0F) {
            return 0.0F;
        }
        if (time < THROW_BLEND_IN_TICKS) {
            float progress = time / THROW_BLEND_IN_TICKS;
            return progress * progress * (3.0F - 2.0F * progress);
        }
        if (time < THROW_ANIM_END_TICKS) {
            return 1.0F;
        }
        float progress = (time - THROW_ANIM_END_TICKS) / THROW_RETURN_TICKS;
        if (progress >= 1.0F) {
            return 0.0F;
        }
        float eased = progress * progress * (3.0F - 2.0F * progress);
        return 1.0F - eased;
    }

    @Override
    public void translateToRightHand(PoseStack poseStack) {
        this.body.translateAndRotate(poseStack);
        this.body_upper.translateAndRotate(poseStack);
        this.right_arm.translateAndRotate(poseStack);
        this.right_arm_lower.translateAndRotate(poseStack);
    }

    private static void applyRotation(ModelPart part, float[] degrees, float amount, float mirror) {
        part.xRot += degrees[0] * Mth.DEG_TO_RAD * amount;
        part.yRot += degrees[1] * Mth.DEG_TO_RAD * mirror * amount;
        part.zRot += degrees[2] * Mth.DEG_TO_RAD * mirror * amount;
    }

    private static float sample(float[][] keys, float time, int component) {
        if (time <= keys[0][0]) {
            return keys[0][component];
        }
        for (int i = 1; i < keys.length; i++) {
            if (time <= keys[i][0]) {
                float progress = (time - keys[i - 1][0]) / (keys[i][0] - keys[i - 1][0]);
                return Mth.lerp(progress, keys[i - 1][component], keys[i][component]);
            }
        }
        return keys[keys.length - 1][component];
    }

    private static float punchWeight(float time) {
        if (time < 0.0F) {
            return 0.0F;
        }
        if (time < PUNCH_BLEND_IN_TICKS) {
            float progress = time / PUNCH_BLEND_IN_TICKS;
            return progress * progress * (3.0F - 2.0F * progress);
        }
        if (time < PUNCH_HOLD_END_TICKS) {
            return 1.0F;
        }
        float progress = (time - PUNCH_HOLD_END_TICKS) / PUNCH_RETURN_TICKS;
        if (progress >= 1.0F) {
            return 0.0F;
        }
        float eased = progress * progress * (3.0F - 2.0F * progress);
        return 1.0F - eased;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    private void applyIdle(double ageTicks, float amount, float rightArmScale) {
        float breathe = wave(ageTicks, BREATHE_PERIOD_TICKS, 0.0D) * amount;
        float sway = wave(ageTicks, SWAY_PERIOD_TICKS, 0.0D) * amount;
        float breatheLag = wave(ageTicks, BREATHE_PERIOD_TICKS, LOWER_LIMB_LAG) * amount;
        float swayLag = wave(ageTicks, SWAY_PERIOD_TICKS, LOWER_LIMB_LAG) * amount;

        float spread = (ARM_SPREAD_REST * amount) + breathe * ARM_SPREAD_BREATHE;
        this.left_arm.zRot -= spread;
        this.right_arm.zRot += spread * rightArmScale;
        this.left_arm.xRot += sway * ARM_SWAY;
        this.right_arm.xRot -= sway * ARM_SWAY * rightArmScale;

        this.left_arm_lower.xRot += breatheLag * LOWER_ARM_FLEX;
        this.right_arm_lower.xRot += breatheLag * LOWER_ARM_FLEX * rightArmScale;

        this.left_leg.xRot -= sway * LEG_SWAY;
        this.right_leg.xRot += sway * LEG_SWAY;

        this.left_leg_lower.xRot += swayLag * LOWER_LEG_FLEX;
        this.right_leg_lower.xRot += swayLag * LOWER_LEG_FLEX;
    }
}
