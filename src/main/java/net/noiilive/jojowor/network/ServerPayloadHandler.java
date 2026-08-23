package net.noiilive.jojowor.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.noiilive.jojowor.Config;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.registry.ModAttachments;
import net.noiilive.jojowor.registry.ModEffects;
import net.noiilive.jojowor.stand.GuardMode;
import net.noiilive.jojowor.stand.Stand;
import net.noiilive.jojowor.stand.StandAttacks;
import net.noiilive.jojowor.stand.StandGuards;
import net.noiilive.jojowor.stand.Stands;

public final class ServerPayloadHandler {
    private ServerPayloadHandler() {}

    private static boolean frozen(ServerPlayer player) {
        return net.noiilive.jojowor.stand.ability.TimeStops.stopAffecting(player) != null;
    }

    private static int serverTick(ServerPlayer player) {
        return player.getServer() == null ? 0 : player.getServer().getTickCount();
    }

    public static void handleSummonStand(SummonStandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || frozen(player)) {
                return;
            }

            Stand stand = Stands.get(player);
            if (stand == null) {
                return;
            }

            Stands.toggleSummoned(player);

            if (Config.DEBUG_LOGGING.getAsBoolean()) {
                JoJoWoR.LOGGER.info("{} {} {}", player.getGameProfile().getName(),
                        Stands.isSummoned(player) ? "summoned" : "desummoned", stand.getId());
            }
        });
    }

    public static void handleSetStandPose(SetStandPosePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Stands.has(player) || frozen(player)) {
                return;
            }
            player.setData(ModAttachments.STAND, Stands.getData(player)
                    .withPose(payload.pose().sanitized())
                    .withOffset(payload.offset().clamped()));
        });
    }

    public static void handleSetStandSkin(SetStandSkinPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Stand stand = Stands.get(player);
            if (stand == null || stand.getSkins().isEmpty() || frozen(player)) {
                return;
            }
            java.util.List<net.noiilive.jojowor.stand.skin.StandSkin> skins = stand.getSkins();
            int skin = net.minecraft.util.Mth.clamp(payload.skin(), 0, skins.size() - 1);
            java.util.List<net.noiilive.jojowor.stand.skin.StandSkin.Layer> recolorable =
                    skins.get(skin).recolorableLayers();
            java.util.List<Integer> colors = new java.util.ArrayList<>(recolorable.size());
            for (int i = 0; i < recolorable.size(); i++) {
                colors.add(i < payload.colors().size()
                        ? payload.colors().get(i) & 0xFFFFFF
                        : recolorable.get(i).defaultColor());
            }
            player.setData(ModAttachments.STAND,
                    Stands.getData(player).withSkin(skin, java.util.List.copyOf(colors), payload.colored()));
        });
    }

    public static void handleStandAbility(StandAbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Stands.isSummoned(player) || player.hasEffect(ModEffects.STUN)) {
                return;
            }
            net.noiilive.jojowor.stand.ability.StandAbilities.activate(player, payload.slot());
        });
    }

    public static void handleStandThrow(StandThrowPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Stands.isSummoned(player)
                    || Stands.guardMode(player) != GuardMode.NONE
                    || player.hasEffect(ModEffects.STUN)
                    || frozen(player)) {
                return;
            }

            net.minecraft.world.item.ItemStack stack = player.getItemInHand(payload.hand());
            if (player.getCooldowns().isOnCooldown(stack.getItem())) {
                return;
            }
            if (!stack.is(net.minecraft.world.item.Items.ENDER_PEARL)
                    && !(stack.getItem() instanceof net.minecraft.world.item.ArrowItem)) {
                return;
            }

            net.minecraft.world.item.ItemStack thrown = stack.copyWithCount(1);
            int cooldown = stack.is(net.minecraft.world.item.Items.ENDER_PEARL)
                    ? StandAttacks.PEARL_THROW_COOLDOWN_TICKS
                    : StandAttacks.ARROW_THROW_COOLDOWN_TICKS;
            player.getCooldowns().addCooldown(stack.getItem(), cooldown);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 0.8F);

            StandAttacks.scheduleThrow(player, thrown);
            PacketDistributor.sendToPlayersTrackingEntity(player,
                    new StandThrowEffectPayload(player.getId(), thrown));
        });
    }

    public static void handleStandUppercut(StandUppercutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Stands.isSummoned(player)
                    || Stands.guardMode(player) != GuardMode.NONE
                    || player.hasEffect(ModEffects.STUN)
                    || frozen(player)) {
                return;
            }
            Entity candidate = player.level().getEntity(payload.targetEntityId());
            if (candidate == null || !StandAttacks.canStrike(player, candidate)) {
                return;
            }
            if (!StandAttacks.tryBeginUppercut(player)) {
                return;
            }
            StandAttacks.scheduleUppercut(player, candidate.getId());
            PacketDistributor.sendToPlayersTrackingEntity(player,
                    new StandUppercutEffectPayload(player.getId(), candidate.getId()));
        });
    }

    public static void handleStandSlam(StandSlamPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Stands.isSummoned(player)
                    || Stands.guardMode(player) != GuardMode.NONE
                    || player.hasEffect(ModEffects.STUN)
                    || frozen(player)) {
                return;
            }
            Entity candidate = player.level().getEntity(payload.targetEntityId());
            if (candidate == null || !StandAttacks.canStrike(player, candidate)) {
                return;
            }
            if (!StandAttacks.tryBeginSlam(player)) {
                return;
            }
            StandAttacks.scheduleSlam(player, candidate.getId());
            PacketDistributor.sendToPlayersTrackingEntity(player,
                    new StandSlamEffectPayload(player.getId(), candidate.getId()));
        });
    }

    public static void handleStandBarrage(StandBarragePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!payload.active()) {
                StandAttacks.stopBarrage(player);
                return;
            }
            if (!Stands.isSummoned(player)
                    || Stands.guardMode(player) == GuardMode.NONE
                    || player.hasEffect(ModEffects.STUN)
                    || frozen(player)) {
                return;
            }
            StandAttacks.startBarrage(player);
        });
    }

    public static void handleStandGuard(StandGuardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Stands.isSummoned(player) || frozen(player)) {
                return;
            }

            GuardMode mode = payload.mode();
            int now = serverTick(player);
            if (mode != GuardMode.NONE
                    && (StandGuards.isBroken(player, now) || player.hasEffect(ModEffects.STUN))) {
                mode = GuardMode.NONE;
            }

            if (mode == GuardMode.GUARD && Stands.guardMode(player) != GuardMode.GUARD) {
                StandGuards.onGuardStart(player, now);
            }
            Stands.setGuardMode(player, mode);
        });
    }

    public static void handleStandAttack(StandAttackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!Stands.isSummoned(player)
                    || Stands.guardMode(player) != GuardMode.NONE
                    || player.hasEffect(ModEffects.STUN)
                    || frozen(player)) {
                return;
            }

            int targetId = StandAttackPayload.NO_TARGET;
            if (payload.targetEntityId() != StandAttackPayload.NO_TARGET) {
                Entity candidate = player.level().getEntity(payload.targetEntityId());
                if (candidate != null && StandAttacks.canStrike(player, candidate)) {
                    targetId = candidate.getId();
                }
            }

            if (!StandAttacks.tryBeginPunch(player, targetId != StandAttackPayload.NO_TARGET)) {
                return;
            }

            java.util.Optional<net.minecraft.core.BlockPos> blockTarget = targetId == StandAttackPayload.NO_TARGET
                    ? payload.blockTarget()
                    : java.util.Optional.empty();
            if (targetId != StandAttackPayload.NO_TARGET || blockTarget.isPresent()) {
                StandAttacks.schedule(player, targetId, blockTarget);
            }
            PacketDistributor.sendToPlayersTrackingEntity(player,
                    new StandAttackEffectPayload(player.getId(), targetId, blockTarget));
        });
    }
}
