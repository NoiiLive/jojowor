package net.noiilive.jojowor.stand.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TimeStopWorld {
    public interface PositionCheck {
        boolean frozen(Level level, double x, double y, double z);
    }

    private record DeferredExplosion(ResourceKey<Level> dimension, Explosion explosion) {}

    private static final Map<ResourceKey<Level>, Set<BlockPos>> PENDING_UPDATES = new ConcurrentHashMap<>();
    private static final List<DeferredExplosion> PENDING_EXPLOSIONS = new CopyOnWriteArrayList<>();

    private static PositionCheck clientCheck = (level, x, y, z) -> false;

    private TimeStopWorld() {}

    public static void setClientCheck(PositionCheck check) {
        clientCheck = check;
    }

    public static boolean frozen(@Nullable Level level, double x, double y, double z) {
        if (level == null) {
            return false;
        }
        return level.isClientSide()
                ? clientCheck.frozen(level, x, y, z)
                : TimeStops.coversPosition(level, x, y, z);
    }

    public static boolean frozen(@Nullable Level level, BlockPos pos) {
        return frozen(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    public static void deferUpdate(Level level, BlockPos pos) {
        PENDING_UPDATES.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet())
                .add(pos.immutable());
    }

    public static void deferExplosion(Level level, Explosion explosion) {
        PENDING_EXPLOSIONS.add(new DeferredExplosion(level.dimension(), explosion));
    }

    public static void release(MinecraftServer server) {
        List<DeferredExplosion> explosions = new ArrayList<>(PENDING_EXPLOSIONS);
        PENDING_EXPLOSIONS.clear();

        Map<ResourceKey<Level>, Set<BlockPos>> updates = Map.copyOf(PENDING_UPDATES);
        PENDING_UPDATES.clear();

        updates.forEach((dimension, positions) -> {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                return;
            }
            for (BlockPos pos : positions) {
                level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
            }
        });

        for (DeferredExplosion deferred : explosions) {
            if (server.getLevel(deferred.dimension()) == null) {
                continue;
            }
            deferred.explosion().explode();
            deferred.explosion().finalizeExplosion(true);
        }
    }

    public static void clear() {
        PENDING_UPDATES.clear();
        PENDING_EXPLOSIONS.clear();
    }
}
