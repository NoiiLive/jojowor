package net.noiilive.jojowor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.noiilive.jojowor.stand.ability.TimeStopGrace;
import net.noiilive.jojowor.stand.ability.TimeStops;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientTimeStop {
    public record FrozenPose(float walkPosition, float walkSpeed, float yRot, float xRot,
                             float yBodyRot, float yHeadRot, float attackAnim) {}

    private record Stop(int ownerEntityId, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                       Vec3 center, double radius, boolean global) {}

    private static final Map<UUID, FrozenPose> POSES = new HashMap<>();
    private static final Map<Integer, Stop> STOPS = new HashMap<>();

    private ClientTimeStop() {}

    public static void start(int ownerEntityId, Vec3 center, double radius, boolean global) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        STOPS.put(ownerEntityId, new Stop(ownerEntityId, minecraft.level.dimension(), center, radius, global));
    }

    public static boolean coversPosition(net.minecraft.world.level.Level level, double x, double y, double z) {
        if (STOPS.isEmpty()) {
            return false;
        }
        for (Stop stop : STOPS.values()) {
            if (!level.dimension().equals(stop.dimension())) {
                continue;
            }
            if (stop.global() || centerOf(stop).distanceToSqr(x, y, z) <= stop.radius() * stop.radius()) {
                return true;
            }
        }
        return false;
    }

    public static void stop(int ownerEntityId) {
        STOPS.remove(ownerEntityId);
        if (STOPS.isEmpty()) {
            clear();
        }
    }

    public static void clear() {
        STOPS.clear();
        POSES.clear();
        TimeStopGrace.clear(true);
    }

    public static boolean isActive() {
        return !STOPS.isEmpty();
    }

    public static boolean affectsView() {
        if (STOPS.isEmpty()) {
            return false;
        }
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        return player != null && (ownsStop(player) || covered(player));
    }

    public static java.util.Set<Integer> owners() {
        return STOPS.keySet();
    }

    public static boolean ownsStop(@Nullable Entity entity) {
        return entity != null && STOPS.containsKey(entity.getId());
    }

    private static Vec3 centerOf(Stop stop) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity owner = minecraft.level == null ? null : minecraft.level.getEntity(stop.ownerEntityId());
        if (owner == null) {
            return stop.center();
        }
        return new Vec3(owner.getX(),
                owner.getY() + owner.getBbHeight() * TimeStops.CHEST_HEIGHT_FACTOR,
                owner.getZ());
    }

    public static boolean covered(@Nullable Entity entity) {
        if (STOPS.isEmpty() || entity == null || ownsStop(entity)) {
            return false;
        }
        return coversPosition(entity.level(), entity.getX(), entity.getY(), entity.getZ());
    }

    public static boolean isFrozen(@Nullable Entity entity) {
        return covered(entity) && !TimeStopGrace.active(entity);
    }

    public static FrozenPose pose(LivingEntity entity) {
        return POSES.computeIfAbsent(entity.getUUID(), uuid -> new FrozenPose(
                entity.walkAnimation.position(),
                entity.walkAnimation.speed(),
                entity.getYRot(),
                entity.getXRot(),
                entity.yBodyRot,
                entity.yHeadRot,
                entity.attackAnim));
    }

    public static void release(Entity entity) {
        POSES.remove(entity.getUUID());
    }
}
