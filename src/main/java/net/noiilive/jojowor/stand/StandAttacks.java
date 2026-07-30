package net.noiilive.jojowor.stand;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.noiilive.jojowor.registry.ModDamageTypes;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class StandAttacks {
    public static final double REACH = 7.5D;
    public static final double TARGET_RANGE = 4.5D;
    public static final double LOCK_RANGE = 7.0D;
    public static final float STAND_DAMAGE = 4.0F;
    public static final int STRIKE_INVULN_TICKS = 6;
    public static final int IMPACT_DELAY_TICKS = 3;
    public static final int MIN_PUNCH_INTERVAL_TICKS = 8;
    public static final int COMBO_SIZE = 4;
    public static final int COMBO_END_COOLDOWN_TICKS = 15;
    public static final int COMBO_RESET_TICKS = 30;
    public static final float BLOCK_HARDNESS_PER_HIT = 0.5F;
    public static final int BLOCK_BREAK_TIMEOUT_TICKS = 40;
    public static final float PEARL_THROW_SPEED = 2.5F;
    public static final int PEARL_THROW_COOLDOWN_TICKS = 20;
    public static final float ARROW_THROW_SPEED = 3.5F;
    public static final int ARROW_THROW_COOLDOWN_TICKS = 10;
    public static final int THROW_RELEASE_TICKS = 8;
    public static final int BARRAGE_MAX_TICKS = 100;
    public static final int BARRAGE_HIT_INTERVAL_TICKS = 5;
    public static final float BARRAGE_DAMAGE = 1.5F;
    public static final float BARRAGE_HOLD_FACTOR = 0.25F;
    public static final double BARRAGE_STALL_DRIFT = 0.02D;
    public static final int BARRAGE_STALL_GRACE_TICKS = 6;
    public static final double LOCK_AIR_RANGE_SCALE = 0.5D;
    public static final int BARRAGED_GRACE_TICKS = BARRAGE_HIT_INTERVAL_TICKS + 2;
    public static final double CLASH_RISE = 0.06D;
    private static final net.minecraft.resources.ResourceLocation BARRAGE_SLOW_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    net.noiilive.jojowor.JoJoWoR.MODID, "barrage_slow");
    private static final net.minecraft.resources.ResourceLocation BARRAGED_SLOW_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    net.noiilive.jojowor.JoJoWoR.MODID, "barraged_slow");
    public static final int UPPERCUT_COOLDOWN_TICKS = 30;
    public static final float UPPERCUT_DAMAGE = STAND_DAMAGE;
    public static final double UPPERCUT_LAUNCH = 0.9D;
    public static final int UPPERCUT_IMPACT_DELAY_TICKS = 3;
    public static final int SLAM_COOLDOWN_TICKS = 30;
    public static final float SLAM_DAMAGE = STAND_DAMAGE;
    public static final double SLAM_FORCE = 1.6D;
    public static final int SLAM_IMPACT_DELAY_TICKS = 3;
    public static final double BARRAGE_RANGE = 3.5D;
    public static final int BARRAGE_INVULN_TICKS = 2;
    public static final float BARRAGE_BLOCK_HARDNESS_PER_HIT = 0.75F;
    public static final int BARRAGE_END_COOLDOWN_TICKS = 50;
    public static final float DEFLECT_SPEED = 1.5F;
    public static final float PUNCH_KNOCKBACK_SCALE = 0.6F;
    public static final float BARRAGE_KNOCKBACK_SCALE = 0.1F;
    public static final double THROW_OFFSET_FORWARD = 0.55D;
    public static final double THROW_OFFSET_RIGHT = 0.8D;
    public static final double THROW_OFFSET_UP = 1.9D;
    public static final double THROW_AIM_RANGE = 48.0D;

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingThrow> PENDING_THROWS = new ConcurrentHashMap<>();
    private static final Map<UUID, BarrageState> BARRAGING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> BARRAGE_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> BARRAGED_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, net.minecraft.world.entity.LivingEntity> BARRAGE_SLOWED = new ConcurrentHashMap<>();
    private static int lastServerTick;
    private static final Map<UUID, Pending> PENDING_UPPERCUTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> UPPERCUT_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Pending> PENDING_SLAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SLAM_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, ComboState> COMBO = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<BlockPos, BreakProgress>> BREAKING = new ConcurrentHashMap<>();
    private static final AtomicInteger BREAKER_IDS = new AtomicInteger(-1000);

    private record Pending(int targetId, Optional<BlockPos> blockTarget, int delay) {}

    private record PendingThrow(net.minecraft.world.item.ItemStack stack, int delay) {}

    private record ComboState(int count, int lastTick) {}

    private static final class BarrageState {
        final int startTick;
        int lastEntityHitTick = Integer.MIN_VALUE / 2;
        int lastTargetId = -1;

        BarrageState(int startTick) {
            this.startTick = startTick;
        }
    }

    private static final class BreakProgress {
        final ServerLevel level;
        final BlockPos pos;
        final int breakerId;
        float progress;
        int lastHitTick;

        BreakProgress(ServerLevel level, BlockPos pos, int breakerId, int now) {
            this.level = level;
            this.pos = pos;
            this.breakerId = breakerId;
            this.lastHitTick = now;
        }

        void clearCrack() {
            this.level.destroyBlockProgress(this.breakerId, this.pos, -1);
        }
    }

    private StandAttacks() {}

    public static boolean tryBeginPunch(ServerPlayer player, boolean entityTarget) {
        int now = player.getServer() == null ? 0 : player.getServer().getTickCount();
        if (barrageOnCooldown(player.getUUID(), now)) {
            return false;
        }
        ComboState state = COMBO.get(player.getUUID());
        int count = state == null ? 0 : state.count();
        int last = state == null ? Integer.MIN_VALUE / 2 : state.lastTick();

        if (now - last > COMBO_RESET_TICKS) {
            count = 0;
        }
        int cooldown = count >= COMBO_SIZE ? COMBO_END_COOLDOWN_TICKS : MIN_PUNCH_INTERVAL_TICKS;
        if (now - last < cooldown - 1) {
            return false;
        }
        if (entityTarget) {
            if (count >= COMBO_SIZE) {
                count = 0;
            }
            count++;
        } else {
            count = 0;
        }
        COMBO.put(player.getUUID(), new ComboState(count, now));
        return true;
    }

    public static void schedule(ServerPlayer player, int targetId, Optional<BlockPos> blockTarget) {
        PENDING.put(player.getUUID(), new Pending(targetId, blockTarget, IMPACT_DELAY_TICKS));
    }

    public static boolean tryBeginUppercut(ServerPlayer player) {
        int now = player.getServer() == null ? 0 : player.getServer().getTickCount();
        Integer last = UPPERCUT_COOLDOWNS.get(player.getUUID());
        if (last != null && now - last < UPPERCUT_COOLDOWN_TICKS) {
            return false;
        }
        UPPERCUT_COOLDOWNS.put(player.getUUID(), now);
        return true;
    }

    public static void scheduleUppercut(ServerPlayer player, int targetId) {
        PENDING_UPPERCUTS.put(player.getUUID(),
                new Pending(targetId, Optional.empty(), UPPERCUT_IMPACT_DELAY_TICKS));
    }

    public static boolean tryBeginSlam(ServerPlayer player) {
        int now = player.getServer() == null ? 0 : player.getServer().getTickCount();
        Integer last = SLAM_COOLDOWNS.get(player.getUUID());
        if (last != null && now - last < SLAM_COOLDOWN_TICKS) {
            return false;
        }
        SLAM_COOLDOWNS.put(player.getUUID(), now);
        return true;
    }

    public static void scheduleSlam(ServerPlayer player, int targetId) {
        PENDING_SLAMS.put(player.getUUID(),
                new Pending(targetId, Optional.empty(), SLAM_IMPACT_DELAY_TICKS));
    }

    private static void slamStrike(ServerPlayer player, Entity target) {
        net.minecraft.world.phys.Vec3 before = target.getDeltaMovement();
        if (!target.hurt(ModDamageTypes.stand(player), SLAM_DAMAGE)) {
            return;
        }
        target.setDeltaMovement(before.x * 0.3D, -SLAM_FORCE, before.z * 0.3D);
        target.hurtMarked = true;
        target.invulnerableTime = STRIKE_INVULN_TICKS;

        player.currentImpulseImpactPos = player.position();
        player.setIgnoreFallDamageFromCurrentImpulse(true);
        player.setDeltaMovement(player.getDeltaMovement().x, -SLAM_FORCE, player.getDeltaMovement().z);
        player.hurtMarked = true;

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    private static void uppercutStrike(ServerPlayer player, Entity target) {
        net.minecraft.world.phys.Vec3 before = target.getDeltaMovement();
        if (!target.hurt(ModDamageTypes.stand(player), UPPERCUT_DAMAGE)) {
            return;
        }
        target.setDeltaMovement(before.x * 0.3D, UPPERCUT_LAUNCH, before.z * 0.3D);
        target.hurtMarked = true;
        target.invulnerableTime = STRIKE_INVULN_TICKS;

        player.currentImpulseImpactPos = player.position();
        player.setIgnoreFallDamageFromCurrentImpulse(true);
        player.setDeltaMovement(player.getDeltaMovement().x, UPPERCUT_LAUNCH, player.getDeltaMovement().z);
        player.hurtMarked = true;

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();

        tickBarrages(server, now);

        Iterator<Map.Entry<UUID, Pending>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Pending> entry = iterator.next();
            Pending pending = entry.getValue();
            int delay = pending.delay() - 1;
            if (delay > 0) {
                entry.setValue(new Pending(pending.targetId(), pending.blockTarget(), delay));
                continue;
            }
            iterator.remove();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !Stands.isSummoned(player)) {
                continue;
            }
            if (pending.targetId() >= 0) {
                Entity target = player.level().getEntity(pending.targetId());
                if (target != null && canStrike(player, target)) {
                    strike(player, target);
                }
            } else if (pending.blockTarget().isPresent()) {
                strikeBlock(player, pending.blockTarget().get(), now, BLOCK_HARDNESS_PER_HIT);
            }
        }

        Iterator<Map.Entry<UUID, Pending>> uppercuts = PENDING_UPPERCUTS.entrySet().iterator();
        while (uppercuts.hasNext()) {
            Map.Entry<UUID, Pending> entry = uppercuts.next();
            Pending pending = entry.getValue();
            int delay = pending.delay() - 1;
            if (delay > 0) {
                entry.setValue(new Pending(pending.targetId(), pending.blockTarget(), delay));
                continue;
            }
            uppercuts.remove();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !Stands.isSummoned(player)) {
                continue;
            }
            Entity target = player.level().getEntity(pending.targetId());
            if (target != null && canStrike(player, target)) {
                uppercutStrike(player, target);
            }
        }

        Iterator<Map.Entry<UUID, Pending>> slams = PENDING_SLAMS.entrySet().iterator();
        while (slams.hasNext()) {
            Map.Entry<UUID, Pending> entry = slams.next();
            Pending pending = entry.getValue();
            int delay = pending.delay() - 1;
            if (delay > 0) {
                entry.setValue(new Pending(pending.targetId(), pending.blockTarget(), delay));
                continue;
            }
            slams.remove();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !Stands.isSummoned(player)) {
                continue;
            }
            Entity target = player.level().getEntity(pending.targetId());
            if (target != null && canStrike(player, target)) {
                slamStrike(player, target);
            }
        }

        Iterator<Map.Entry<UUID, PendingThrow>> throwsIterator = PENDING_THROWS.entrySet().iterator();
        while (throwsIterator.hasNext()) {
            Map.Entry<UUID, PendingThrow> entry = throwsIterator.next();
            PendingThrow pending = entry.getValue();
            int delay = pending.delay() - 1;
            if (delay > 0) {
                entry.setValue(new PendingThrow(pending.stack(), delay));
                continue;
            }
            throwsIterator.remove();

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.isAlive() && Stands.isSummoned(player)) {
                releaseThrow(player, pending.stack());
            }
        }

        Iterator<Map.Entry<UUID, Map<BlockPos, BreakProgress>>> breaking = BREAKING.entrySet().iterator();
        while (breaking.hasNext()) {
            Map<BlockPos, BreakProgress> playerBreaks = breaking.next().getValue();
            Iterator<Map.Entry<BlockPos, BreakProgress>> blocks = playerBreaks.entrySet().iterator();
            while (blocks.hasNext()) {
                BreakProgress progress = blocks.next().getValue();
                if (now - progress.lastHitTick > BLOCK_BREAK_TIMEOUT_TICKS) {
                    progress.clearCrack();
                    blocks.remove();
                }
            }
            if (playerBreaks.isEmpty()) {
                breaking.remove();
            }
        }
    }

    public static void strike(ServerPlayer player, Entity target) {
        net.minecraft.world.phys.Vec3 before = target.getDeltaMovement();
        if (!target.hurt(ModDamageTypes.stand(player), STAND_DAMAGE)) {
            return;
        }
        scaleKnockback(target, before, PUNCH_KNOCKBACK_SCALE);
        target.invulnerableTime = STRIKE_INVULN_TICKS;
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F,
                0.9F + player.getRandom().nextFloat() * 0.2F);
    }

    private static void scaleKnockback(Entity target, net.minecraft.world.phys.Vec3 before, float scale) {
        net.minecraft.world.phys.Vec3 after = target.getDeltaMovement();
        target.setDeltaMovement(before.add(after.subtract(before).scale(scale)));
        target.hurtMarked = true;
    }

    private static void strikeBlock(ServerPlayer player, BlockPos pos, int now, float hardnessPerHit) {
        if (player.getAbilities().instabuild) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (state.isAir() || hardness < 0.0F
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > REACH * REACH
                || !level.mayInteract(player, pos)) {
            return;
        }

        Map<BlockPos, BreakProgress> playerBreaks =
                BREAKING.computeIfAbsent(player.getUUID(), uuid -> new ConcurrentHashMap<>());
        BreakProgress progress = playerBreaks.get(pos);
        if (progress == null || progress.level != level) {
            if (progress != null) {
                progress.clearCrack();
            }
            progress = new BreakProgress(level, pos, BREAKER_IDS.getAndDecrement(), now);
            playerBreaks.put(pos, progress);
        }
        progress.lastHitTick = now;

        progress.progress += hardness == 0.0F ? 1.0F : hardnessPerHit / hardness;

        if (progress.progress >= 1.0F) {
            playerBreaks.remove(pos);
            progress.clearCrack();
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
            if (!NeoForge.EVENT_BUS.post(event).isCanceled()) {
                level.destroyBlock(pos, true, player);
            }
        } else {
            level.destroyBlockProgress(progress.breakerId, pos,
                    (int) (progress.progress * 10.0F) - 1);
            level.playSound(null, pos, state.getSoundType(level, pos, player).getHitSound(),
                    SoundSource.BLOCKS, 0.5F, 0.8F);
        }
    }

    public static void scheduleThrow(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        PENDING_THROWS.put(player.getUUID(), new PendingThrow(stack, THROW_RELEASE_TICKS));
    }

    public static boolean isBarraging(Player player) {
        return BARRAGING.containsKey(player.getUUID());
    }

    public static boolean isBeingBarraged(Entity entity) {
        Integer until = BARRAGED_UNTIL.get(entity.getUUID());
        return until != null && lastServerTick <= until;
    }

    private static void applyBarrageSlow(ServerPlayer player) {
        var attribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(BARRAGE_SLOW_ID);
            attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    BARRAGE_SLOW_ID, -0.5D,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeBarrageSlow(ServerPlayer player) {
        var attribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(BARRAGE_SLOW_ID);
        }
    }

    public static int barrageCooldownFor(int elapsedTicks) {
        return Math.min(BARRAGE_END_COOLDOWN_TICKS, elapsedTicks / 2);
    }

    private static boolean barrageOnCooldown(UUID uuid, int now) {
        Integer expiry = BARRAGE_COOLDOWNS.get(uuid);
        return expiry != null && now < expiry;
    }

    public static void startBarrage(ServerPlayer player) {
        int now = player.getServer() == null ? 0 : player.getServer().getTickCount();
        if (barrageOnCooldown(player.getUUID(), now)) {
            return;
        }
        BARRAGING.put(player.getUUID(), new BarrageState(now));
        applyBarrageSlow(player);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(player,
                new net.noiilive.jojowor.network.StandBarrageEffectPayload(player.getId(), true));
    }

    public static void stopBarrage(ServerPlayer player) {
        BarrageState state = BARRAGING.remove(player.getUUID());
        if (state != null) {
            int now = player.getServer() == null ? 0 : player.getServer().getTickCount();
            BARRAGE_COOLDOWNS.put(player.getUUID(), now + barrageCooldownFor(now - state.startTick));
            removeBarrageSlow(player);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(player,
                    new net.noiilive.jojowor.network.StandBarrageEffectPayload(player.getId(), false));
        }
    }

    private static void suppressBarragedTarget(Entity target) {
        if (!(target instanceof net.minecraft.world.entity.LivingEntity living)) {
            return;
        }
        var attribute = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute != null && attribute.getModifier(BARRAGED_SLOW_ID) == null) {
            attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    BARRAGED_SLOW_ID, -0.7D,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        BARRAGE_SLOWED.put(living.getUUID(), living);

        if (living instanceof net.minecraft.world.entity.Mob mob) {
            mob.getNavigation().stop();
            net.minecraft.world.phys.Vec3 velocity = mob.getDeltaMovement();
            mob.setDeltaMovement(velocity.x * 0.5D, velocity.y, velocity.z * 0.5D);
        }
    }

    private static void releaseBarragedTargets() {
        Iterator<Map.Entry<UUID, net.minecraft.world.entity.LivingEntity>> iterator =
                BARRAGE_SLOWED.entrySet().iterator();
        while (iterator.hasNext()) {
            net.minecraft.world.entity.LivingEntity living = iterator.next().getValue();
            if (living.isRemoved() || !living.isAlive() || !isBeingBarraged(living)) {
                var attribute = living.getAttribute(
                        net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                if (attribute != null) {
                    attribute.removeModifier(BARRAGED_SLOW_ID);
                }
                iterator.remove();
            }
        }
    }

    private static void tickBarrages(MinecraftServer server, int now) {
        lastServerTick = now;
        BARRAGED_UNTIL.values().removeIf(until -> until < now - 100);
        releaseBarragedTargets();

        Iterator<Map.Entry<UUID, BarrageState>> iterator = BARRAGING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BarrageState> entry = iterator.next();
            BarrageState state = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }
            int elapsed = now - state.startTick;
            if (elapsed >= BARRAGE_MAX_TICKS || !Stands.isSummoned(player)
                    || Stands.guardMode(player) == GuardMode.NONE) {
                iterator.remove();
                BARRAGE_COOLDOWNS.put(entry.getKey(), now + barrageCooldownFor(elapsed));
                removeBarrageSlow(player);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(player,
                        new net.noiilive.jojowor.network.StandBarrageEffectPayload(player.getId(), false));
                continue;
            }

            if (elapsed % BARRAGE_HIT_INTERVAL_TICKS == 0) {
                barrageStrike(player, state, now);
            }

            boolean clashing = isClashing(player, state, now);
            boolean holdingTarget = now - state.lastEntityHitTick <= BARRAGE_STALL_GRACE_TICKS;
            Entity target = holdingTarget ? player.level().getEntity(state.lastTargetId) : null;
            if (target != null && target.isAlive()) {
                suppressBarragedTarget(target);
            }

            if (clashing) {
                net.minecraft.world.phys.Vec3 velocity = player.getDeltaMovement();
                if (velocity.y < CLASH_RISE) {
                    player.setDeltaMovement(velocity.x, CLASH_RISE, velocity.z);
                    player.hurtMarked = true;
                }
                player.fallDistance = 0.0F;
            } else if (target != null && target.isAlive()) {
                net.minecraft.world.phys.Vec3 velocity = target.getDeltaMovement();
                if (velocity.y < -BARRAGE_STALL_DRIFT) {
                    target.setDeltaMovement(velocity.x, -BARRAGE_STALL_DRIFT, velocity.z);
                    target.hurtMarked = true;
                }
                target.fallDistance = 0.0F;
                player.fallDistance = 0.0F;
            }

            if (elapsed % 6 == 0) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.7F,
                        1.3F + player.getRandom().nextFloat() * 0.4F);
            }
        }
    }

    private static boolean isClashing(ServerPlayer player, BarrageState state, int now) {
        if (now - state.lastEntityHitTick > BARRAGED_GRACE_TICKS) {
            return false;
        }
        if (!(player.level().getEntity(state.lastTargetId) instanceof ServerPlayer other)) {
            return false;
        }
        BarrageState otherState = BARRAGING.get(other.getUUID());
        return otherState != null
                && otherState.lastTargetId == player.getId()
                && now - otherState.lastEntityHitTick <= BARRAGED_GRACE_TICKS;
    }

    private static void barrageStrike(ServerPlayer player, BarrageState state, int now) {
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 view = player.getViewVector(1.0F);
        net.minecraft.world.phys.Vec3 end = eye.add(view.scale(BARRAGE_RANGE));

        net.minecraft.world.phys.HitResult blockHit = player.level().clip(new net.minecraft.world.level.ClipContext(
                eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player.level(), player, eye, end,
                player.getBoundingBox().expandTowards(view.scale(BARRAGE_RANGE)).inflate(1.0D),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                        && (entity.isPickable() || entity instanceof net.minecraft.world.entity.projectile.Projectile));
        if (entityHit == null) {
            if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                strikeBlock(player, ((net.minecraft.world.phys.BlockHitResult) blockHit).getBlockPos(),
                        now, BARRAGE_BLOCK_HARDNESS_PER_HIT);
            }
            return;
        }
        if (entityHit.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            projectile.deflect(net.minecraft.world.entity.projectile.ProjectileDeflection.AIM_DEFLECT,
                    player, player, true);
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(DEFLECT_SPEED));
            player.level().playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.5F);
            return;
        }
        Entity target = entityHit.getEntity();
        if (!target.isAttackable()) {
            return;
        }

        state.lastEntityHitTick = now;
        state.lastTargetId = target.getId();
        BARRAGED_UNTIL.put(target.getUUID(), now + BARRAGED_GRACE_TICKS);

        if (target instanceof ServerPlayer otherPlayer && isClashing(player, state, now)) {
            player.level().playSound(null, otherPlayer.getX(), otherPlayer.getY(), otherPlayer.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3F, 1.6F);
            return;
        }

        target.invulnerableTime = 0;
        net.minecraft.world.phys.Vec3 before = target.getDeltaMovement();
        if (target.hurt(ModDamageTypes.stand(player), BARRAGE_DAMAGE)) {
            target.setDeltaMovement(before.x * BARRAGE_HOLD_FACTOR, before.y, before.z * BARRAGE_HOLD_FACTOR);
            target.hurtMarked = true;
            target.invulnerableTime = BARRAGE_INVULN_TICKS;
        }
    }

    private static void releaseThrow(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        ServerLevel level = player.serverLevel();

        double yaw = Math.toRadians(player.getYHeadRot());
        net.minecraft.world.phys.Vec3 forward = new net.minecraft.world.phys.Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        net.minecraft.world.phys.Vec3 right = new net.minecraft.world.phys.Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
        net.minecraft.world.phys.Vec3 spawn = player.position()
                .add(forward.scale(THROW_OFFSET_FORWARD))
                .add(right.scale(THROW_OFFSET_RIGHT))
                .add(0.0D, THROW_OFFSET_UP, 0.0D);

        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 end = eye.add(player.getViewVector(1.0F).scale(THROW_AIM_RANGE));
        net.minecraft.world.phys.HitResult clip = level.clip(new net.minecraft.world.level.ClipContext(
                eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        net.minecraft.world.phys.Vec3 target = clip.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                ? clip.getLocation()
                : end;
        net.minecraft.world.phys.Vec3 direction = target.subtract(spawn);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = player.getViewVector(1.0F);
        }
        direction = direction.normalize();

        if (stack.is(net.minecraft.world.item.Items.ENDER_PEARL)) {
            var pearl = new net.minecraft.world.entity.projectile.ThrownEnderpearl(level, player);
            pearl.setItem(stack);
            pearl.setPos(spawn);
            pearl.shoot(direction.x, direction.y, direction.z, PEARL_THROW_SPEED, 1.0F);
            level.addFreshEntity(pearl);
            level.playSound(null, spawn.x, spawn.y, spawn.z,
                    SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL, 0.5F, 0.5F);
        } else if (stack.getItem() instanceof net.minecraft.world.item.ArrowItem arrowItem) {
            var arrow = arrowItem.createArrow(level, stack, player, null);
            arrow.setPos(spawn);
            arrow.shoot(direction.x, direction.y, direction.z, ARROW_THROW_SPEED, 1.0F);
            arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED;
            level.addFreshEntity(arrow);
            level.playSound(null, spawn.x, spawn.y, spawn.z,
                    SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F);
        }
    }

    public static boolean canStrike(Player player, Entity target) {
        return target != player
                && target.isAlive()
                && target.isAttackable()
                && !target.isSpectator()
                && target.level() == player.level()
                && player.distanceToSqr(target) <= REACH * REACH
                && player.hasLineOfSight(target);
    }
}
