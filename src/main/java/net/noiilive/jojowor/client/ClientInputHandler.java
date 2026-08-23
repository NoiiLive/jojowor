package net.noiilive.jojowor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.client.gui.PosingScreen;
import net.noiilive.jojowor.client.render.StandTracker;
import net.noiilive.jojowor.network.StandAbilityPayload;
import net.noiilive.jojowor.network.StandAttackPayload;
import net.noiilive.jojowor.network.StandBarragePayload;
import net.noiilive.jojowor.network.StandGuardPayload;
import net.noiilive.jojowor.network.StandSlamPayload;
import net.noiilive.jojowor.network.StandThrowPayload;
import net.noiilive.jojowor.network.StandUppercutPayload;
import net.noiilive.jojowor.network.SummonStandPayload;

import org.jetbrains.annotations.Nullable;
import net.noiilive.jojowor.registry.ModEffects;
import net.noiilive.jojowor.stand.GuardMode;
import net.noiilive.jojowor.stand.StandAttacks;
import net.noiilive.jojowor.stand.ability.StandAbilities;
import net.noiilive.jojowor.stand.Stands;

import java.util.Optional;

@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public final class ClientInputHandler {
    private static GuardMode sentGuardMode = GuardMode.NONE;
    private static boolean barrageSent;
    private static boolean barrageExhausted;

    private ClientInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            sentGuardMode = GuardMode.NONE;
            return;
        }

        boolean frozen = net.noiilive.jojowor.client.ClientTimeStop.isFrozen(player);
        boolean stunned = player.hasEffect(ModEffects.STUN);

        while (ModKeyMappings.SUMMON_STAND.consumeClick()) {
            if (!stunned && !frozen) {
                PacketDistributor.sendToServer(SummonStandPayload.INSTANCE);
            }
        }

        while (ModKeyMappings.MAIN_MENU.consumeClick()) {
            if (!frozen && minecraft.screen == null && Stands.has(player)) {
                minecraft.setScreen(new PosingScreen());
            }
        }

        for (int slot = 0; slot < ModKeyMappings.ABILITIES.length; slot++) {
            final int abilitySlot = slot;
            while (ModKeyMappings.ABILITIES[slot].consumeClick()) {
                net.noiilive.jojowor.stand.ability.StandAbility ability =
                        StandAbilities.slotAbility(player, abilitySlot);
                if (!stunned && Stands.isSummoned(player) && ability != null
                        && (!frozen || ability.usableWhileFrozen())) {
                    PacketDistributor.sendToServer(new StandAbilityPayload(abilitySlot));
                }
            }
        }

        if (frozen) {
            sentGuardMode = GuardMode.NONE;
            return;
        }

        if (!Stands.isSummoned(player)) {
            sentGuardMode = GuardMode.NONE;
            return;
        }

        boolean attackHeld = minecraft.screen == null && minecraft.options.keyAttack.isDown();
        if (!attackHeld) {
            barrageExhausted = false;
        }

        GuardMode wanted = GuardMode.NONE;
        if (minecraft.screen == null && minecraft.options.keyUse.isDown() && !stunned
                && (barrageSent || attackHeld || !vanillaUsePriority(minecraft, player))) {
            wanted = player.isShiftKeyDown() ? GuardMode.GUARD : GuardMode.BLOCK;
        }

        if (wanted != sentGuardMode
                || (wanted != Stands.guardMode(player) && player.tickCount % 10 == 0)) {
            sentGuardMode = wanted;
            PacketDistributor.sendToServer(new StandGuardPayload(wanted));
        }
        boolean wantsBarrage = attackHeld && wanted != GuardMode.NONE && !stunned && !barrageExhausted
                && StandTracker.barrageCooldownRemaining(player) <= 0;
        if (wantsBarrage && StandTracker.barrageDuration(player) >= StandAttacks.BARRAGE_MAX_TICKS) {
            barrageExhausted = true;
            wantsBarrage = false;
        }
        if (wantsBarrage != barrageSent) {
            barrageSent = wantsBarrage;
            PacketDistributor.sendToServer(new StandBarragePayload(wantsBarrage));
            StandTracker.setBarraging(player, wantsBarrage);
        }

        if (attackHeld && wanted == GuardMode.NONE && !stunned && !StandTracker.specialActive(player)) {
            StandTracker.queuePunch(player);
        }

        tryFirePunch(minecraft, player);
    }

    @SubscribeEvent
    public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !Stands.isSummoned(player)) {
            return;
        }
        if (net.noiilive.jojowor.client.ClientTimeStop.isFrozen(player)) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }

        if (event.isAttack()) {
            if (sentGuardMode != GuardMode.NONE) {
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }
            if (player.isShiftKeyDown()) {
                PunchTarget found = findTarget(minecraft, player);
                if (found.entityId() != StandAttackPayload.NO_TARGET) {
                    event.setSwingHand(false);
                    event.setCanceled(true);
                    if (!player.hasEffect(ModEffects.STUN) && StandTracker.attackReady(player)) {
                        if (!player.onGround()) {
                            if (StandTracker.slamReady(player)) {
                                StandTracker.startSlam(player, found.entityId());
                                PacketDistributor.sendToServer(new StandSlamPayload(found.entityId()));
                            }
                        } else if (StandTracker.uppercutReady(player)) {
                            StandTracker.startUppercut(player, found.entityId());
                            PacketDistributor.sendToServer(new StandUppercutPayload(found.entityId()));
                        }
                    }
                    return;
                }
            }
            if (!hasStandTarget(minecraft, player)) {
                return;
            }
            event.setSwingHand(false);
            event.setCanceled(true);
            if (sentGuardMode == GuardMode.NONE && !player.hasEffect(ModEffects.STUN)
                    && !StandTracker.specialActive(player)) {
                StandTracker.queuePunch(player);
                tryFirePunch(minecraft, player);
            }
            return;
        }

        if (event.isUseItem()) {
            if (barrageSent) {
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }
            InteractionHand throwHand = standThrowHand(player);
            if (throwHand != null) {
                event.setSwingHand(false);
                event.setCanceled(true);
                if (!player.hasEffect(ModEffects.STUN) && sentGuardMode == GuardMode.NONE
                        && !player.getCooldowns().isOnCooldown(player.getItemInHand(throwHand).getItem())) {
                    PacketDistributor.sendToServer(new StandThrowPayload(throwHand));
                    StandTracker.startThrow(player, player.getItemInHand(throwHand));
                }
                return;
            }
            if (!vanillaUsePriority(minecraft, player)) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        }
    }

    @Nullable
    private static InteractionHand standThrowHand(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Items.ENDER_PEARL) || stack.getItem() instanceof ArrowItem) {
                return hand;
            }
        }
        return null;
    }

    private static boolean hasStandTarget(Minecraft minecraft, LocalPlayer player) {
        if (StandTracker.hasValidLockTarget(player)) {
            return true;
        }
        PunchTarget found = findTarget(minecraft, player);
        return found.entityId() != StandAttackPayload.NO_TARGET || found.blockPos().isPresent();
    }

    private static boolean vanillaUsePriority(Minecraft minecraft, LocalPlayer player) {
        if (player.isUsingItem()) {
            return true;
        }

        HitResult hit = minecraft.hitResult;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity().isAlive()
                && !(entityHit.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)
                && !(entityHit.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            return true;
        }

        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                && minecraft.level != null) {
            if (player.getMainHandItem().getItem() instanceof BlockItem
                    || player.getOffhandItem().getItem() instanceof BlockItem) {
                return true;
            }
            BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
            if (state.getBlock() instanceof EntityBlock
                    || state.getBlock() instanceof LeverBlock
                    || state.is(BlockTags.DOORS)
                    || state.is(BlockTags.TRAPDOORS)
                    || state.is(BlockTags.BUTTONS)
                    || state.is(BlockTags.FENCE_GATES)
                    || state.is(BlockTags.BEDS)) {
                return true;
            }
        }

        return !inertForGuard(player.getMainHandItem()) || !inertForGuard(player.getOffhandItem());
    }

    private static boolean inertForGuard(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (stack.getUseAnimation() != UseAnim.NONE) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem) {
            return true;
        }
        boolean[] weapon = new boolean[1];
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == Attributes.ATTACK_DAMAGE.value()) {
                weapon[0] = true;
            }
        });
        return weapon[0];
    }

    private record PunchTarget(int entityId, Optional<BlockPos> blockPos) {}

    private static void tryFirePunch(Minecraft minecraft, LocalPlayer player) {
        if (sentGuardMode != GuardMode.NONE || player.hasEffect(ModEffects.STUN)) {
            return;
        }
        PunchTarget found = findTarget(minecraft, player);
        boolean entityIntent = found.entityId() != StandAttackPayload.NO_TARGET
                || StandTracker.hasValidLockTarget(player);
        if (!entityIntent && found.blockPos().isEmpty()) {
            StandTracker.clearQueuedPunch(player);
            return;
        }
        if (!StandTracker.tryConsumeQueuedPunch(player, entityIntent)) {
            return;
        }
        int targetId = StandTracker.resolveTarget(player, found.entityId());
        Optional<BlockPos> blockTarget = targetId == StandAttackPayload.NO_TARGET
                ? found.blockPos()
                : Optional.empty();
        StandTracker.startPunch(player, targetId, blockTarget);
        PacketDistributor.sendToServer(new StandAttackPayload(targetId, blockTarget));
    }

    private static PunchTarget findTarget(Minecraft minecraft, LocalPlayer player) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return new PunchTarget(StandAttackPayload.NO_TARGET, Optional.empty());
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = eye.add(view.scale(StandAttacks.TARGET_RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Optional<BlockPos> blockTarget = Optional.empty();
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            end = blockHit.getLocation();
            blockTarget = Optional.of(blockHit.getBlockPos());
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level, player, eye, end,
                player.getBoundingBox().expandTowards(view.scale(StandAttacks.TARGET_RANGE)).inflate(1.0D),
                entity -> entity != player && entity.isPickable() && entity.isAlive() && !entity.isSpectator());

        return entityHit == null
                ? new PunchTarget(StandAttackPayload.NO_TARGET, blockTarget)
                : new PunchTarget(entityHit.getEntity().getId(), Optional.empty());
    }
}
