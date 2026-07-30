package net.noiilive.jojowor.stand;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.noiilive.jojowor.registry.ModEffects;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StandGuards {
    public static final float BLOCK_REDUCTION = 0.5F;
    public static final float GUARD_MAX_HEALTH = 12.0F;
    public static final float GUARD_REGEN_PER_TICK = 0.2F;
    public static final int PARRY_WINDOW_TICKS = 5;
    public static final float PARRY_REFLECT_FRACTION = 0.5F;
    public static final int PARRY_STUN_TICKS = 60;
    public static final int GUARD_BREAK_TICKS = 100;
    public static final int GUARD_BREAK_STUN_TICKS = 60;

    private static final Map<UUID, GuardState> STATES = new ConcurrentHashMap<>();

    private static final class GuardState {
        float health = GUARD_MAX_HEALTH;
        int guardStartTick = Integer.MIN_VALUE / 2;
        int brokenUntilTick = Integer.MIN_VALUE / 2;
    }

    private StandGuards() {}

    private static GuardState state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), uuid -> new GuardState());
    }

    public static void onGuardStart(ServerPlayer player, int now) {
        state(player).guardStartTick = now;
    }

    public static boolean isBroken(Player player, int now) {
        return now < state(player).brokenUntilTick;
    }

    public static boolean inParryWindow(Player player, int now) {
        return now - state(player).guardStartTick < PARRY_WINDOW_TICKS;
    }

    public static boolean absorb(ServerPlayer player, float amount, int now) {
        GuardState state = state(player);
        state.health -= amount;
        if (state.health > 0.0F) {
            return false;
        }
        state.health = GUARD_MAX_HEALTH;
        state.brokenUntilTick = now + GUARD_BREAK_TICKS;
        Stands.setGuardMode(player, GuardMode.NONE);
        player.addEffect(new MobEffectInstance(ModEffects.STUN, GUARD_BREAK_STUN_TICKS, 0));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    public static void parry(ServerPlayer player, float amount, LivingEntity attacker) {
        attacker.hurt(net.noiilive.jojowor.registry.ModDamageTypes.stand(player), amount * PARRY_REFLECT_FRACTION);
        attacker.addEffect(new MobEffectInstance(ModEffects.STUN, PARRY_STUN_TICKS, 0));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.6F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 1.8F);
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GuardState state = STATES.get(player.getUUID());
            if (state == null) {
                continue;
            }
            if (Stands.guardMode(player) != GuardMode.GUARD && state.health < GUARD_MAX_HEALTH) {
                state.health = Math.min(GUARD_MAX_HEALTH, state.health + GUARD_REGEN_PER_TICK);
            }
        }
    }

    public static void clear(Player player) {
        STATES.remove(player.getUUID());
    }
}
