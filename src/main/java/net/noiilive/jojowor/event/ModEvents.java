package net.noiilive.jojowor.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.GuardMode;
import net.noiilive.jojowor.stand.StandAttacks;
import net.noiilive.jojowor.stand.StandGuards;
import net.noiilive.jojowor.stand.Stands;

@EventBusSubscriber(modid = JoJoWoR.MODID)
public final class ModEvents {
    private ModEvents() {}

    @SubscribeEvent
    public static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        net.minecraft.world.entity.Entity entity = event.getEntity();
        if (entity.level().isClientSide()
                || net.noiilive.jojowor.stand.ability.TimeStops.stopAffecting(entity) == null) {
            return;
        }
        if (net.noiilive.jojowor.stand.ability.TimeStopGrace.tick(entity)) {
            return;
        }
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
            item.setNoPickUpDelay();
        }
        entity.tickCount--;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (net.noiilive.jojowor.stand.ability.TimeStops.stopAffecting(event.getEntity()) != null) {
            net.noiilive.jojowor.stand.ability.TimeStopGrace.grant(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        StandAttacks.tick(event.getServer());
        StandGuards.tick(event.getServer());
        net.noiilive.jojowor.stand.ability.TimeStops.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var sourceEntity = event.getSource().getEntity();
        if (sourceEntity != null && StandAttacks.isBeingBarraged(sourceEntity)
                && !event.getSource().is(net.noiilive.jojowor.registry.ModDamageTypes.STAND)) {
            event.setCanceled(true);
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        GuardMode mode = Stands.guardMode(player);
        if (mode == GuardMode.NONE || StandAttacks.isBarraging(player)) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_SHIELD)) {
            return;
        }
        Vec3 sourcePos = event.getSource().getSourcePosition();
        if (sourcePos == null || !isFrontal(player, sourcePos)) {
            return;
        }

        if (mode == GuardMode.BLOCK) {
            event.setAmount(event.getAmount() * (1.0F - StandGuards.BLOCK_REDUCTION));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6F, 0.8F);
            return;
        }

        int now = player.getServer() == null ? 0 : player.getServer().getTickCount();
        float amount = event.getAmount();
        event.setCanceled(true);

        if (StandGuards.inParryWindow(player, now)
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != player) {
            StandGuards.parry(player, amount, attacker);
            return;
        }

        if (!StandGuards.absorb(player, amount, now)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F,
                    0.8F + player.getRandom().nextFloat() * 0.4F);
        }
    }

    private static boolean isFrontal(ServerPlayer player, Vec3 sourcePos) {
        double yaw = Math.toRadians(player.getYHeadRot());
        Vec3 view = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 toPlayer = player.position().subtract(sourcePos);
        toPlayer = new Vec3(toPlayer.x, 0.0D, toPlayer.z).normalize();
        return toPlayer.dot(view) < 0.0D;
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (net.noiilive.jojowor.stand.ability.TimeStops.stopAffecting(player) == null) {
                Stands.setSummoned(player, false);
            }
            StandGuards.clear(player);
            net.noiilive.jojowor.stand.ability.TimeStops.stop(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Stands.setSummoned(player, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Stands.setSummoned(player, false);
            StandGuards.clear(player);
            net.noiilive.jojowor.stand.ability.TimeStops.stop(player);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isStandControlling(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isGuardHeld(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isGuardHeld(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isGuardHeld(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isGuardHeld(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean isGuardHeld(Player player) {
        return Stands.guardMode(player) != GuardMode.NONE;
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isStandControlling(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean isStandControlling(Player player) {
        return Stands.isSummoned(player);
    }
}
