package net.noiilive.jojowor.stand.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.jojowor.Config;
import net.noiilive.jojowor.network.TimeStopEffectPayload;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeStops {
    public static final double CHEST_HEIGHT_FACTOR = 0.62D;
    public static final float INSIDE_VOLUME = 1.0F;
    public static final float OUTSIDE_VOLUME = 4.0F;

    public record ActiveStop(UUID owner, int ownerEntityId, ResourceKey<Level> dimension, Vec3 center,
                             double radius, boolean global) {
        public Vec3 currentCenter(Level level) {
            Player owner = level.getPlayerByUUID(this.owner);
            return owner == null ? this.center : chestPosition(owner);
        }

        public boolean covers(Entity entity) {
            return coversPosition(entity.level(), entity.getX(), entity.getY(), entity.getZ());
        }

        public boolean coversPosition(Level level, double x, double y, double z) {
            if (!level.dimension().equals(this.dimension)) {
                return false;
            }
            if (this.global) {
                return true;
            }
            return currentCenter(level).distanceToSqr(x, y, z) <= this.radius * this.radius;
        }
    }

    private static final Map<UUID, ActiveStop> ACTIVE = new ConcurrentHashMap<>();

    private TimeStops() {}

    public static Vec3 chestPosition(Player player) {
        return new Vec3(player.getX(), player.getY() + player.getBbHeight() * CHEST_HEIGHT_FACTOR, player.getZ());
    }

    public static void toggle(ServerPlayer player) {
        if (isActive(player)) {
            stop(player);
        } else {
            start(player);
        }
    }

    public static void start(ServerPlayer player) {
        boolean global = Config.GLOBAL_TIMESTOP.getAsBoolean();
        double radius = global ? 0.0D : Config.TIMESTOP_RADIUS.getAsInt();
        Vec3 center = chestPosition(player);
        boolean silent = stopAffecting(player) != null;

        ACTIVE.put(player.getUUID(), new ActiveStop(player.getUUID(), player.getId(),
                player.level().dimension(), center, radius, global));

        ActiveStop started = ACTIVE.get(player.getUUID());
        broadcast(player.serverLevel(), new TimeStopEffectPayload(
                player.getId(), center.x, center.y, center.z, (float) radius, global, true, silent));

        if (silent) {
            return;
        }
        net.noiilive.jojowor.stand.Stand stand = net.noiilive.jojowor.stand.Stands.get(player);
        playSound(player, stand == null
                ? net.noiilive.jojowor.registry.ModSounds.TW_TIMESTOP.get()
                : stand.getTimeStopSound(), started);
    }

    public static void stop(ServerPlayer player) {
        ActiveStop removed = ACTIVE.remove(player.getUUID());
        if (removed == null) {
            return;
        }
        broadcast(player.serverLevel(), new TimeStopEffectPayload(
                player.getId(), removed.center().x, removed.center().y, removed.center().z,
                (float) removed.radius(), removed.global(), false, false));

        if (!ACTIVE.isEmpty()) {
            return;
        }
        TimeStopGrace.clear(false);
        playSound(player, net.noiilive.jojowor.registry.ModSounds.TIME_RESUME.get(), removed);
        if (player.getServer() != null) {
            TimeStopWorld.release(player.getServer());
        }
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static boolean anyActive() {
        return !ACTIVE.isEmpty();
    }

    public static boolean coversPosition(Level level, double x, double y, double z) {
        if (ACTIVE.isEmpty()) {
            return false;
        }
        for (ActiveStop stop : ACTIVE.values()) {
            if (stop.coversPosition(level, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ActiveStop stopAffecting(Entity entity) {
        if (ACTIVE.isEmpty() || ACTIVE.containsKey(entity.getUUID())) {
            return null;
        }
        for (ActiveStop stop : ACTIVE.values()) {
            if (!stop.owner().equals(entity.getUUID()) && stop.covers(entity)) {
                return stop;
            }
        }
        return null;
    }

    public static void tick(MinecraftServer server) {
        boolean had = !ACTIVE.isEmpty();
        ACTIVE.keySet().removeIf(owner -> server.getPlayerList().getPlayer(owner) == null);
        if (had && ACTIVE.isEmpty()) {
            TimeStopWorld.release(server);
        }
    }

    public static void clear(UUID owner) {
        ACTIVE.remove(owner);
    }

    private static void playSound(ServerPlayer caster, SoundEvent sound, @Nullable ActiveStop stop) {
        Vec3 origin = chestPosition(caster);
        for (ServerPlayer listener : caster.serverLevel().players()) {
            if (listener == caster || stop == null || stop.covers(listener)) {
                listener.playNotifySound(sound, SoundSource.PLAYERS, INSIDE_VOLUME, 1.0F);
            } else {
                listener.connection.send(new ClientboundSoundPacket(
                        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS,
                        origin.x, origin.y, origin.z, OUTSIDE_VOLUME, 1.0F,
                        listener.getRandom().nextLong()));
            }
        }
    }

    private static void broadcast(ServerLevel level, TimeStopEffectPayload payload) {
        PacketDistributor.sendToPlayersInDimension(level, payload);
    }
}
