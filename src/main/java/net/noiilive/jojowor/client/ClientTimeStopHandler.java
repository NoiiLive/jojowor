package net.noiilive.jojowor.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.ability.TimeStopGrace;

@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public final class ClientTimeStopHandler {
    private static float lockedYRot;
    private static float lockedXRot;
    private static int lockedSlot;
    private static boolean locked;

    private ClientTimeStopHandler() {}

    static {
        net.noiilive.jojowor.stand.ability.TimeStopWorld.setClientCheck(
                (level, x, y, z) -> ClientTimeStop.coversPosition(level, x, y, z));
    }

    private static boolean localFrozen() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && ClientTimeStop.isFrozen(player);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!ClientTimeStop.isActive() || !entity.level().isClientSide()) {
            return;
        }
        if (!ClientTimeStop.covered(entity)) {
            ClientTimeStop.release(entity);
            return;
        }
        if (TimeStopGrace.tick(entity)) {
            return;
        }
        if (entity instanceof LivingEntity living) {
            ClientTimeStop.pose(living);
        }
        entity.tickCount--;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() || !ClientTimeStop.isActive()) {
            return;
        }
        if (ClientTimeStop.covered(event.getEntity())) {
            TimeStopGrace.grant(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !ClientTimeStop.isFrozen(player)) {
            locked = false;
            return;
        }

        if (!locked) {
            locked = true;
            lockedYRot = player.getYRot();
            lockedXRot = player.getXRot();
            lockedSlot = player.getInventory().selected;
        }

        if (blockedScreen(minecraft.screen)) {
            minecraft.setScreen(null);
        }

        drain(minecraft.options.keyInventory);
        drain(minecraft.options.keySwapOffhand);
        drain(minecraft.options.keyAttack);
        drain(minecraft.options.keyUse);
        drain(minecraft.options.keyPickItem);
        drain(minecraft.options.keyDrop);
        drain(minecraft.options.keyJump);
        for (KeyMapping hotbar : minecraft.options.keyHotbarSlots) {
            drain(hotbar);
        }

        player.getInventory().selected = lockedSlot;
        applyLock(player);
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && locked && ClientTimeStop.isFrozen(player)) {
            player.getInventory().selected = lockedSlot;
            applyLock(player);
        }
    }

    private static void applyLock(LocalPlayer player) {
        player.setYRot(lockedYRot);
        player.setXRot(lockedXRot);
        player.yRotO = lockedYRot;
        player.xRotO = lockedXRot;
        player.yHeadRot = lockedYRot;
        player.yHeadRotO = lockedYRot;
        player.yBodyRot = lockedYRot;
        player.yBodyRotO = lockedYRot;
    }

    private static boolean blockedScreen(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof AbstractContainerScreen
                || screen instanceof net.noiilive.jojowor.client.gui.PosingScreen
                || screen instanceof net.noiilive.jojowor.client.gui.StandSkinsScreen;
    }

    private static void drain(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            continue;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTimeStop.clear();
        locked = false;
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (localFrozen()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (localFrozen() && blockedScreen(event.getNewScreen())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!ClientTimeStop.isFrozen(event.getEntity())) {
            return;
        }
        var input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }
}
