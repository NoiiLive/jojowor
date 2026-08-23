package net.noiilive.jojowor.stand.ability;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeStopGrace {
    public static final int GRACE_TICKS = 40;
    public static final double DECAY = 0.88D;
    public static final double STOP_SPEED = 0.02D;

    private static final class Entry {
        private final Entity entity;
        private final boolean hadNoGravity;
        private int remaining;

        private Entry(Entity entity) {
            this.entity = entity;
            this.hadNoGravity = entity.isNoGravity();
            this.remaining = GRACE_TICKS;
        }

        private void restore() {
            this.entity.setNoGravity(this.hadNoGravity);
        }
    }

    private static final Map<UUID, Entry> CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Entry> SERVER = new ConcurrentHashMap<>();

    private TimeStopGrace() {}

    private static Map<UUID, Entry> pool(Entity entity) {
        return entity.level().isClientSide() ? CLIENT : SERVER;
    }

    public static void grant(Entity entity) {
        Entry entry = new Entry(entity);
        pool(entity).put(entity.getUUID(), entry);
        entity.setNoGravity(true);
    }

    public static boolean active(Entity entity) {
        return pool(entity).containsKey(entity.getUUID());
    }

    public static boolean tick(Entity entity) {
        Map<UUID, Entry> pool = pool(entity);
        Entry entry = pool.get(entity.getUUID());
        if (entry == null) {
            return false;
        }

        Vec3 velocity = entity.getDeltaMovement().scale(DECAY);
        if (entry.remaining <= 1 || velocity.lengthSqr() < STOP_SPEED * STOP_SPEED) {
            pool.remove(entity.getUUID());
            entry.restore();
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = true;
            return false;
        }

        entry.remaining--;
        entity.setNoGravity(true);
        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true;
        return true;
    }

    public static void clear(boolean clientSide) {
        Map<UUID, Entry> pool = clientSide ? CLIENT : SERVER;
        pool.values().forEach(Entry::restore);
        pool.clear();
    }
}
