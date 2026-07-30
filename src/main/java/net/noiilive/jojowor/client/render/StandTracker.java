package net.noiilive.jojowor.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.network.StandAttackPayload;
import net.noiilive.jojowor.stand.GuardMode;
import net.noiilive.jojowor.stand.StandAttacks;
import net.noiilive.jojowor.stand.StandOffset;
import net.noiilive.jojowor.stand.Stands;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public final class StandTracker {
    public static final int FADE_TICKS = 6;
    public static final int GUARD_BLEND_TICKS = 6;
    public static final int COMBAT_IN_TICKS = 6;
    public static final int COMBAT_OUT_TICKS = 8;
    public static final int COMBAT_LINGER_TICKS = 40;

    public static final double COMBAT_FORWARD = 0.55D;
    public static final double COMBAT_RIGHT = 0.8D;
    public static final double GUARD_FORWARD = 0.9D;

    public static final double IDLE_FOLLOW = 0.25D;
    public static final double ACTION_FOLLOW = 0.5D;
    public static final double SNAP_DISTANCE = 8.0D;
    public static final double MIN_PLAYER_DISTANCE = 0.6D;
    public static final double VELOCITY_SMOOTHING = 0.35D;

    public static final float BODY_YAW_SMOOTHING = 0.35F;
    public static final float HEAD_YAW_SMOOTHING = 0.5F;

    public static final float PUNCH_WINDUP_END = 2.0F;
    public static final float PUNCH_STRIKE_END = 4.0F;
    public static final float PUNCH_HOLD_END = 6.0F;
    public static final float PUNCH_TOTAL = 14.0F;

    public static final float LUNGE_OUT_END = 5.5F;
    public static final float LUNGE_HOLD_END = 7.5F;

    public static final double MISS_LUNGE = 1.1D;
    public static final double MAX_LUNGE = 3.0D;
    public static final double LUNGE_ARM_REACH = 1.6D;
    public static final float LUNGE_PULLBACK = 0.12F;
    public static final float LUNGE_SUSTAIN_THRESHOLD = 0.5F;
    public static final double LUNGE_TRACK_RATE = 0.35D;
    public static final float LUNGE_SNAP_BELOW = 0.1F;
    public static final double LUNGE_Y_REFERENCE = 1.6D;
    public static final double LUNGE_Y_MAX = 1.5D;

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static final int THROW_TOTAL_TICKS = 15;
    public static final int BARRAGE_BLEND_TICKS = 3;
    public static final int GHOST_FISTS_PER_TICK = 2;
    public static final int GHOST_FIST_MAX = 24;

    public static final int UPPERCUT_TOTAL_TICKS = 12;
    public static final int SLAM_TOTAL_TICKS = 12;

    public record Pose(Vec3 position, float yaw, float alpha, float defensive, float combat,
                       float leftPunch, float rightPunch, float throwTime,
                       float barrage, float barrageTime, float uppercutTime, float slamTime) {}

    public static final class GhostFist {
        public final boolean rightSide;
        public final Vec3 start;
        public final Vec3 control;
        public final Vec3 end;
        public final float rotX;
        public final float rotY;
        public final float rotZ;
        public final int lifetime;
        public int age;

        GhostFist(boolean rightSide, Vec3 start, Vec3 control, Vec3 end,
                  float rotX, float rotY, float rotZ, int lifetime) {
            this.rightSide = rightSide;
            this.start = start;
            this.control = control;
            this.end = end;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.lifetime = lifetime;
        }

        public Vec3 position(float progress) {
            float inverse = 1.0F - progress;
            return this.start.scale(inverse * inverse)
                    .add(this.control.scale(2.0F * inverse * progress))
                    .add(this.end.scale(progress * progress));
        }
    }

    private StandTracker() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        STATES.keySet().removeIf(uuid -> level.getPlayerByUUID(uuid) == null);
        for (Player player : level.players()) {
            STATES.computeIfAbsent(player.getUUID(), uuid -> new State(player)).tick(player);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
    }

    public static void startPunch(Player owner, int targetEntityId, Optional<BlockPos> blockTarget) {
        STATES.computeIfAbsent(owner.getUUID(), uuid -> new State(owner))
                .startPunch(owner, targetEntityId, blockTarget);
    }

    public static void clearQueuedPunch(Player player) {
        State state = STATES.get(player.getUUID());
        if (state != null) {
            state.queued = false;
        }
    }

    public static void startThrow(Player owner, ItemStack stack) {
        State state = STATES.computeIfAbsent(owner.getUUID(), uuid -> new State(owner));
        state.throwTime = 0.0F;
        state.thrownItem = stack.copyWithCount(1);
    }

    public static void startUppercut(Player owner, int targetEntityId) {
        State state = STATES.computeIfAbsent(owner.getUUID(), uuid -> new State(owner));
        state.uppercutTime = 0.0F;
        state.uppercutCooldown = StandAttacks.UPPERCUT_COOLDOWN_TICKS;
        Entity target = owner.level().getEntity(targetEntityId);
        if (target != null && target.isAlive()) {
            state.aimLunge(owner, target.position(), target.getY() + target.getBbHeight() * 0.75D);
        }
    }

    public static boolean uppercutReady(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null || (state.uppercutCooldown <= 0 && state.uppercutTime < 0.0F);
    }

    public static void startSlam(Player owner, int targetEntityId) {
        State state = STATES.computeIfAbsent(owner.getUUID(), uuid -> new State(owner));
        state.slamTime = 0.0F;
        state.slamCooldown = StandAttacks.SLAM_COOLDOWN_TICKS;
        Entity target = owner.level().getEntity(targetEntityId);
        if (target != null && target.isAlive()) {
            state.aimLunge(owner, target.position(), target.getY() + target.getBbHeight() * 0.75D);
        }
    }

    public static boolean slamReady(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null || (state.slamCooldown <= 0 && state.slamTime < 0.0F);
    }

    public static boolean specialActive(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && (state.uppercutTime >= 0.0F || state.slamTime >= 0.0F);
    }

    public static void setBarraging(Player player, boolean barraging) {
        State state = STATES.computeIfAbsent(player.getUUID(), uuid -> new State(player));
        if (state.barraging && !barraging) {
            int elapsed = state.barrageClock < 0.0F ? 0 : (int) state.barrageClock;
            state.barrageCooldown = StandAttacks.barrageCooldownFor(elapsed);
            state.barrageCooldownTotal = Math.max(1, state.barrageCooldown);
        }
        state.barraging = barraging;
    }

    public static int barrageCooldownRemaining(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.barrageCooldown;
    }

    public static float barrageDuration(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null || state.barrageClock < 0.0F || !state.barraging ? 0.0F : state.barrageClock;
    }

    public static java.util.List<GhostFist> ghostFists(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null ? java.util.List.of() : state.ghostFists;
    }

    public static ItemStack thrownItem(Player player, float partialTick) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.throwTime < 0.0F
                || state.throwTime + partialTick >= StandAttacks.THROW_RELEASE_TICKS) {
            return ItemStack.EMPTY;
        }
        return state.thrownItem;
    }

    public static int resolveTarget(Player player, int directTargetId) {
        State state = STATES.computeIfAbsent(player.getUUID(), uuid -> new State(player));
        if (directTargetId != StandAttackPayload.NO_TARGET) {
            state.lockOnTarget = directTargetId;
            return directTargetId;
        }
        if (state.lockOnTarget == StandAttackPayload.NO_TARGET) {
            return StandAttackPayload.NO_TARGET;
        }
        Entity locked = player.level().getEntity(state.lockOnTarget);
        if (locked == null || !locked.isAlive() || locked.isSpectator()) {
            state.lockOnTarget = StandAttackPayload.NO_TARGET;
            return StandAttackPayload.NO_TARGET;
        }
        if (player.distanceTo(locked) > lockRange(player) || !player.hasLineOfSight(locked)) {
            return StandAttackPayload.NO_TARGET;
        }
        return state.lockOnTarget;
    }

    private static double lockRange(Player player) {
        return player.onGround()
                ? StandAttacks.LOCK_RANGE
                : StandAttacks.LOCK_RANGE * StandAttacks.LOCK_AIR_RANGE_SCALE;
    }

    public static void queuePunch(Player player) {
        STATES.computeIfAbsent(player.getUUID(), uuid -> new State(player)).tryQueue();
    }

    public static boolean tryConsumeQueuedPunch(Player player, boolean entityTarget) {
        State state = STATES.get(player.getUUID());
        return state != null && state.tryConsumeQueued(entityTarget);
    }

    public static boolean hasValidLockTarget(Player player) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.lockOnTarget == StandAttackPayload.NO_TARGET) {
            return false;
        }
        Entity locked = player.level().getEntity(state.lockOnTarget);
        return locked != null && locked.isAlive() && !locked.isSpectator()
                && player.distanceTo(locked) <= lockRange(player)
                && player.hasLineOfSight(locked);
    }

    public static float punchCooldownProgress(Player player, float partialTick) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return 1.0F;
        }
        if (state.barraging && state.barrageClock >= 0.0F) {
            float cycle = (state.barrageClock + partialTick) % StandAttacks.BARRAGE_HIT_INTERVAL_TICKS;
            return Mth.clamp(cycle / StandAttacks.BARRAGE_HIT_INTERVAL_TICKS, 0.0F, 1.0F);
        }
        if (state.barrageCooldown > 0) {
            return Mth.clamp(1.0F - (state.barrageCooldown - partialTick)
                    / state.barrageCooldownTotal, 0.0F, 1.0F);
        }
        int cooldown = state.comboCount >= StandAttacks.COMBO_SIZE
                ? StandAttacks.COMBO_END_COOLDOWN_TICKS
                : StandAttacks.MIN_PUNCH_INTERVAL_TICKS;
        return Mth.clamp((state.ticksSincePunch + partialTick) / cooldown, 0.0F, 1.0F);
    }

    public static Pose pose(Player player, float partialTick) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return new Pose(anchorIdle(player, player.yBodyRot), player.yHeadRot,
                    0.0F, 0.0F, 0.0F, -1.0F, -1.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F);
        }
        return state.pose(partialTick);
    }

    public static Vec3 velocity(Player player, float partialTick) {
        State state = STATES.get(player.getUUID());
        return state == null ? Vec3.ZERO : state.previousVelocity.lerp(state.velocity, partialTick);
    }

    private static Vec3 forwardOf(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    private static Vec3 rightOf(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
    }

    private static Vec3 anchorIdle(Player player, float bodyYawDegrees) {
        StandOffset offset = Stands.getData(player).offset();
        return player.position()
                .add(forwardOf(bodyYawDegrees).scale(offset.forward()))
                .add(rightOf(bodyYawDegrees).scale(offset.right()))
                .add(0.0D, offset.up(), 0.0D);
    }

    private static Vec3 anchorCombat(Player player, float headYawDegrees) {
        return player.position()
                .add(forwardOf(headYawDegrees).scale(COMBAT_FORWARD))
                .add(rightOf(headYawDegrees).scale(COMBAT_RIGHT));
    }

    private static Vec3 anchorGuard(Player player, float headYawDegrees) {
        return player.position().add(forwardOf(headYawDegrees).scale(GUARD_FORWARD));
    }

    private static Vec3 clampFromPlayer(Player player, Vec3 pos) {
        double dx = pos.x - player.getX();
        double dz = pos.z - player.getZ();
        double distance = Math.hypot(dx, dz);
        if (distance >= MIN_PLAYER_DISTANCE || distance < 1.0E-4D) {
            return pos;
        }
        double scale = MIN_PLAYER_DISTANCE / distance;
        return new Vec3(player.getX() + dx * scale, pos.y, player.getZ() + dz * scale);
    }

    private static float easeOutQuart(float t) {
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv * inv;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3.0D) / 2.0F;
    }

    private static float smoothstep(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static float sustainedLungeEnvelope(float t) {
        if (t < 0.0F) {
            return 0.0F;
        }
        if (t < LUNGE_HOLD_END) {
            return 1.0F;
        }
        if (t < PUNCH_TOTAL) {
            return 1.0F - easeInOutCubic((t - LUNGE_HOLD_END) / (PUNCH_TOTAL - LUNGE_HOLD_END));
        }
        return 0.0F;
    }

    private static float lungeEnvelope(float t) {
        if (t < 0.0F) {
            return 0.0F;
        }
        if (t < PUNCH_WINDUP_END) {
            return -LUNGE_PULLBACK * smoothstep(t / PUNCH_WINDUP_END);
        }
        if (t < LUNGE_OUT_END) {
            return Mth.lerp(easeOutQuart((t - PUNCH_WINDUP_END) / (LUNGE_OUT_END - PUNCH_WINDUP_END)),
                    -LUNGE_PULLBACK, 1.0F);
        }
        if (t < LUNGE_HOLD_END) {
            return 1.0F;
        }
        if (t < PUNCH_TOTAL) {
            return 1.0F - easeInOutCubic((t - LUNGE_HOLD_END) / (PUNCH_TOTAL - LUNGE_HOLD_END));
        }
        return 0.0F;
    }

    private static final class State {
        private float previousAlpha;
        private float alpha;
        private float previousDefensive;
        private float defensive;
        private float previousCombat;
        private float combat;
        private float previousBodyYaw;
        private float bodyYaw;
        private float previousHeadYaw;
        private float headYaw;
        private Vec3 previousPosition;
        private Vec3 position;
        private Vec3 previousVelocity = Vec3.ZERO;
        private Vec3 velocity = Vec3.ZERO;
        private Vec3 previousFinal;
        private Vec3 finalPos;
        private float leftTime = -1.0F;
        private float rightTime = -1.0F;
        private float throwTime = -1.0F;
        private ItemStack thrownItem = ItemStack.EMPTY;
        private boolean barraging;
        private int barrageCooldown;
        private int barrageCooldownTotal = 1;
        private float barrageClock = -1.0F;
        private float uppercutTime = -1.0F;
        private int uppercutCooldown;
        private float slamTime = -1.0F;
        private int slamCooldown;
        private int barrageStallTicks;
        private float previousBarrageEnv;
        private float barrageEnv;
        private final java.util.List<GhostFist> ghostFists = new java.util.ArrayList<>();
        private boolean leftSustain;
        private boolean rightSustain;
        private boolean nextRight = true;
        private Vec3 lungeTargetH = Vec3.ZERO;
        private double lungeTargetY;
        private Vec3 previousLungeH = Vec3.ZERO;
        private Vec3 lungeH = Vec3.ZERO;
        private double previousLungeY;
        private double lungeYCur;
        private float previousLunge;
        private float lunge;
        private int ticksSincePunch = 1000;
        private int comboCount;
        private boolean queued;
        private int lockOnTarget = StandAttackPayload.NO_TARGET;

        State(Player owner) {
            this.bodyYaw = owner.yBodyRot;
            this.previousBodyYaw = this.bodyYaw;
            this.headYaw = owner.yHeadRot;
            this.previousHeadYaw = this.headYaw;
            this.position = anchorIdle(owner, this.bodyYaw);
            this.previousPosition = this.position;
            this.finalPos = this.position;
            this.previousFinal = this.position;
        }

        void startPunch(Player owner, int targetEntityId, Optional<BlockPos> blockTarget) {
            this.ticksSincePunch = 0;
            boolean sustain = this.lunge > LUNGE_SUSTAIN_THRESHOLD;
            boolean right = this.nextRight;
            this.nextRight = !right;
            if (right) {
                this.rightTime = 0.0F;
                this.rightSustain = sustain;
            } else {
                this.leftTime = 0.0F;
                this.leftSustain = sustain;
            }

            Entity target = targetEntityId == StandAttackPayload.NO_TARGET
                    ? null
                    : owner.level().getEntity(targetEntityId);
            if (target != null && target.isAlive()) {
                aimLunge(owner, target.position(), target.getY() + target.getBbHeight() * 0.75D);
            } else if (blockTarget.isPresent()) {
                Vec3 center = Vec3.atCenterOf(blockTarget.get());
                aimLunge(owner, center, center.y);
            } else {
                this.lungeTargetH = forwardOf(this.headYaw).scale(MISS_LUNGE);
                this.lungeTargetY = Mth.clamp(owner.getViewVector(1.0F).y * MISS_LUNGE, -1.0D, 1.0D);
            }

            if (this.lunge < LUNGE_SNAP_BELOW) {
                this.lungeH = this.lungeTargetH;
                this.previousLungeH = this.lungeTargetH;
                this.lungeYCur = this.lungeTargetY;
                this.previousLungeY = this.lungeTargetY;
            }

            if (this.comboCount >= StandAttacks.COMBO_SIZE) {
                this.lockOnTarget = StandAttackPayload.NO_TARGET;
            }
        }

        void tryQueue() {
            if (this.comboCount >= StandAttacks.COMBO_SIZE
                    && this.ticksSincePunch < StandAttacks.COMBO_END_COOLDOWN_TICKS) {
                return;
            }
            this.queued = true;
        }

        private static GhostFist randomGhostFist(Player owner) {
            var random = owner.getRandom();
            double side = random.nextBoolean() ? 1.0D : -1.0D;
            Vec3 start = new Vec3(
                    side * (0.3D + random.nextDouble() * 0.4D),
                    -0.1D + random.nextDouble() * 0.9D,
                    0.7D + random.nextDouble() * 0.5D);
            Vec3 control = new Vec3(
                    side * (1.0D + random.nextDouble() * 0.5D),
                    -0.3D + random.nextDouble() * 1.0D,
                    0.1D + random.nextDouble() * 0.4D);
            Vec3 end = new Vec3(
                    (random.nextDouble() - 0.5D) * 1.0D,
                    -0.6D + random.nextDouble() * 1.3D,
                    -(1.2D + random.nextDouble() * 0.8D));
            float rotX = (float) Math.toRadians(-90.0D + (random.nextDouble() - 0.5D) * 50.0D);
            float rotY = (float) Math.toRadians((random.nextDouble() - 0.5D) * 40.0D);
            float rotZ = (float) Math.toRadians((random.nextDouble() - 0.5D) * 40.0D);
            return new GhostFist(side > 0.0D, start, control, end, rotX, rotY, rotZ,
                    4 + random.nextInt(3));
        }

        private void updateBarrageLunge(Player owner) {
            double range = StandAttacks.BARRAGE_RANGE;
            Vec3 eye = owner.getEyePosition();
            Vec3 view = owner.getViewVector(1.0F);
            Vec3 end = eye.add(view.scale(range));

            net.minecraft.world.phys.BlockHitResult blockHit = owner.level().clip(
                    new net.minecraft.world.level.ClipContext(eye, end,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, owner));
            if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                end = blockHit.getLocation();
            }

            var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                    owner.level(), owner, eye, end,
                    owner.getBoundingBox().expandTowards(view.scale(range)).inflate(1.0D),
                    entity -> entity != owner && entity.isPickable() && entity.isAlive() && !entity.isSpectator());

            if (entityHit != null) {
                Entity target = entityHit.getEntity();
                this.barrageStallTicks = StandAttacks.BARRAGE_STALL_GRACE_TICKS;
                aimLunge(owner, target.position(), target.getY() + target.getBbHeight() * 0.75D);
            } else if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                Vec3 center = Vec3.atCenterOf(blockHit.getBlockPos());
                aimLunge(owner, center, center.y);
            } else {
                this.lungeTargetH = forwardOf(this.headYaw).scale(MISS_LUNGE);
                this.lungeTargetY = Mth.clamp(view.y * MISS_LUNGE, -1.0D, 1.0D);
            }
        }

        private void aimLunge(Player owner, Vec3 targetPos, double targetHeight) {
            Vec3 toTarget = targetPos.subtract(owner.position());
            double horizontal = toTarget.horizontalDistance();
            Vec3 direction = horizontal < 1.0E-4D
                    ? forwardOf(this.headYaw)
                    : new Vec3(toTarget.x / horizontal, 0.0D, toTarget.z / horizontal);
            this.lungeTargetH = direction.scale(Mth.clamp(horizontal - LUNGE_ARM_REACH, 0.0D, MAX_LUNGE));
            this.lungeTargetY = Mth.clamp(targetHeight - (owner.getY() + LUNGE_Y_REFERENCE),
                    -LUNGE_Y_MAX, LUNGE_Y_MAX);
        }

        boolean tryConsumeQueued(boolean entityTarget) {
            if (!this.queued || this.barrageCooldown > 0
                    || this.uppercutTime >= 0.0F || this.slamTime >= 0.0F) {
                return false;
            }
            int cooldown = this.comboCount >= StandAttacks.COMBO_SIZE
                    ? StandAttacks.COMBO_END_COOLDOWN_TICKS
                    : StandAttacks.MIN_PUNCH_INTERVAL_TICKS;
            if (this.ticksSincePunch < cooldown) {
                return false;
            }
            if (entityTarget) {
                if (this.comboCount >= StandAttacks.COMBO_SIZE) {
                    this.comboCount = 0;
                }
                this.comboCount++;
            } else {
                this.comboCount = 0;
            }
            this.queued = false;
            return true;
        }

        void tick(Player owner) {
            this.ticksSincePunch++;

            this.previousAlpha = this.alpha;
            float fadeStep = 1.0F / FADE_TICKS;
            this.alpha = Mth.clamp(this.alpha + (Stands.isSummoned(owner) ? fadeStep : -fadeStep), 0.0F, 1.0F);

            GuardMode guardMode = Stands.guardMode(owner);
            float guardStep = 1.0F / GUARD_BLEND_TICKS;
            this.previousDefensive = this.defensive;
            this.defensive = Mth.clamp(this.defensive
                    + (guardMode != GuardMode.NONE ? guardStep : -guardStep), 0.0F, 1.0F);

            this.previousCombat = this.combat;
            boolean combatActive = Stands.isSummoned(owner) && this.ticksSincePunch < COMBAT_LINGER_TICKS;
            float combatStep = combatActive ? 1.0F / COMBAT_IN_TICKS : -1.0F / COMBAT_OUT_TICKS;
            this.combat = Mth.clamp(this.combat + combatStep, 0.0F, 1.0F);
            if (!combatActive) {
                this.lockOnTarget = StandAttackPayload.NO_TARGET;
            }
            if (this.ticksSincePunch > StandAttacks.COMBO_RESET_TICKS) {
                this.comboCount = 0;
                this.queued = false;
            }

            this.previousBodyYaw = this.bodyYaw;
            this.bodyYaw = Mth.rotLerp(BODY_YAW_SMOOTHING, this.bodyYaw, owner.yBodyRot);
            this.previousHeadYaw = this.headYaw;
            this.headYaw = Mth.rotLerp(HEAD_YAW_SMOOTHING, this.headYaw, owner.yHeadRot);

            if (this.leftTime >= 0.0F && (this.leftTime += 1.0F) >= PUNCH_TOTAL) {
                this.leftTime = -1.0F;
            }
            if (this.rightTime >= 0.0F && (this.rightTime += 1.0F) >= PUNCH_TOTAL) {
                this.rightTime = -1.0F;
            }
            if (this.throwTime >= 0.0F && (this.throwTime += 1.0F) >= THROW_TOTAL_TICKS) {
                this.throwTime = -1.0F;
                this.thrownItem = ItemStack.EMPTY;
            }

            if (this.barrageCooldown > 0) {
                this.barrageCooldown--;
            }
            if (this.uppercutCooldown > 0) {
                this.uppercutCooldown--;
            }
            if (this.uppercutTime >= 0.0F && (this.uppercutTime += 1.0F) >= UPPERCUT_TOTAL_TICKS) {
                this.uppercutTime = -1.0F;
            }
            if (this.slamCooldown > 0) {
                this.slamCooldown--;
            }
            if (this.slamTime >= 0.0F && (this.slamTime += 1.0F) >= SLAM_TOTAL_TICKS) {
                this.slamTime = -1.0F;
            }
            boolean barrageActive = this.barraging && Stands.isSummoned(owner);
            this.previousBarrageEnv = this.barrageEnv;
            float barrageStep = 1.0F / BARRAGE_BLEND_TICKS;
            this.barrageEnv = Mth.clamp(this.barrageEnv + (barrageActive ? barrageStep : -barrageStep), 0.0F, 1.0F);
            if (this.barrageEnv > 0.0F) {
                this.barrageClock = this.barrageClock < 0.0F ? 0.0F : this.barrageClock + 1.0F;
            } else {
                this.barrageClock = -1.0F;
            }

            if (barrageActive && this.barrageEnv > 0.3F && this.ghostFists.size() < GHOST_FIST_MAX) {
                for (int i = 0; i < GHOST_FISTS_PER_TICK; i++) {
                    this.ghostFists.add(randomGhostFist(owner));
                }
            }
            this.ghostFists.removeIf(fist -> ++fist.age >= fist.lifetime);

            this.previousLunge = this.lunge;
            this.lunge = lungeValue();
            if (this.barrageEnv > 0.0F) {
                updateBarrageLunge(owner);
                this.lunge = Math.max(this.lunge, smoothstep(this.barrageEnv));
            }
            if (this.barrageStallTicks > 0) {
                this.barrageStallTicks--;
                if (this.barraging && owner == Minecraft.getInstance().player && !owner.onGround()) {
                    Vec3 velocity = owner.getDeltaMovement();
                    if (velocity.y < -StandAttacks.BARRAGE_STALL_DRIFT) {
                        owner.setDeltaMovement(velocity.x, -StandAttacks.BARRAGE_STALL_DRIFT, velocity.z);
                    }
                    owner.fallDistance = 0.0F;
                }
            }
            if (this.uppercutTime >= 0.0F) {
                float uppercutProgress = this.uppercutTime / UPPERCUT_TOTAL_TICKS;
                this.lunge = Math.max(this.lunge, Mth.sin(uppercutProgress * Mth.PI));
            }
            if (this.slamTime >= 0.0F) {
                float slamProgress = this.slamTime / SLAM_TOTAL_TICKS;
                this.lunge = Math.max(this.lunge, Mth.sin(slamProgress * Mth.PI));
            }

            this.previousLungeH = this.lungeH;
            this.lungeH = this.lungeH.lerp(this.lungeTargetH, LUNGE_TRACK_RATE);
            this.previousLungeY = this.lungeYCur;
            this.lungeYCur = Mth.lerp(LUNGE_TRACK_RATE, this.lungeYCur, this.lungeTargetY);

            this.previousPosition = this.position;
            float combatAmount = smoothstep(this.combat);
            float guardAmount = smoothstep(this.defensive);
            Vec3 anchor = anchorIdle(owner, this.bodyYaw)
                    .lerp(anchorCombat(owner, this.headYaw), combatAmount)
                    .lerp(anchorGuard(owner, this.headYaw), guardAmount);

            boolean teleported = this.position.distanceToSqr(anchor) > SNAP_DISTANCE * SNAP_DISTANCE;
            if (teleported) {
                this.position = anchor;
            } else {
                double rate = Mth.lerp(Math.max(combatAmount, guardAmount), IDLE_FOLLOW, ACTION_FOLLOW);
                this.position = clampFromPlayer(owner, this.position.lerp(anchor, rate));
            }

            this.previousFinal = this.finalPos;
            this.finalPos = this.position
                    .add(this.lungeH.scale(this.lunge))
                    .add(0.0D, this.lungeYCur * Math.max(0.0F, this.lunge), 0.0D);

            Vec3 moved = teleported
                    ? Vec3.ZERO
                    : new Vec3(this.finalPos.x - this.previousFinal.x, 0.0D, this.finalPos.z - this.previousFinal.z);
            this.previousVelocity = this.velocity;
            this.velocity = this.velocity.lerp(moved, VELOCITY_SMOOTHING);
        }

        private float lungeValue() {
            if (this.leftTime < 0.0F && this.rightTime < 0.0F) {
                return 0.0F;
            }
            float value = -Float.MAX_VALUE;
            if (this.leftTime >= 0.0F) {
                value = Math.max(value, this.leftSustain
                        ? sustainedLungeEnvelope(this.leftTime)
                        : lungeEnvelope(this.leftTime));
            }
            if (this.rightTime >= 0.0F) {
                value = Math.max(value, this.rightSustain
                        ? sustainedLungeEnvelope(this.rightTime)
                        : lungeEnvelope(this.rightTime));
            }
            return value;
        }

        Pose pose(float partialTick) {
            float lungeAmount = Mth.lerp(partialTick, this.previousLunge, this.lunge);
            Vec3 lungeOffset = this.previousLungeH.lerp(this.lungeH, partialTick);
            double lungeHeight = Mth.lerp(partialTick, this.previousLungeY, this.lungeYCur);
            Vec3 position = this.previousPosition.lerp(this.position, partialTick)
                    .add(lungeOffset.scale(lungeAmount))
                    .add(0.0D, lungeHeight * Math.max(0.0F, lungeAmount), 0.0D);

            float left = this.leftTime < 0.0F ? -1.0F : this.leftTime + partialTick;
            float right = this.rightTime < 0.0F ? -1.0F : this.rightTime + partialTick;
            float throwing = this.throwTime < 0.0F ? -1.0F : this.throwTime + partialTick;

            return new Pose(
                    position,
                    Mth.rotLerp(partialTick, this.previousHeadYaw, this.headYaw),
                    Mth.lerp(partialTick, this.previousAlpha, this.alpha),
                    smoothstep(Mth.lerp(partialTick, this.previousDefensive, this.defensive)),
                    smoothstep(Mth.lerp(partialTick, this.previousCombat, this.combat)),
                    left,
                    right,
                    throwing,
                    smoothstep(Mth.lerp(partialTick, this.previousBarrageEnv, this.barrageEnv)),
                    this.barrageClock < 0.0F ? 0.0F : this.barrageClock + partialTick,
                    this.uppercutTime < 0.0F ? -1.0F : this.uppercutTime + partialTick,
                    this.slamTime < 0.0F ? -1.0F : this.slamTime + partialTick);
        }
    }
}
