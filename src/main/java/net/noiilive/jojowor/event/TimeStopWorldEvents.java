package net.noiilive.jojowor.event;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.stand.ability.TimeStopWorld;

@EventBusSubscriber(modid = JoJoWoR.MODID)
public final class TimeStopWorldEvents {
    private TimeStopWorldEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (TimeStopWorld.frozen(level, event.getPos())) {
            TimeStopWorld.deferUpdate(level, event.getPos());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosionStart(ExplosionEvent.Start event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        var center = event.getExplosion().center();
        if (TimeStopWorld.frozen(level, center.x, center.y, center.z)) {
            TimeStopWorld.deferExplosion(level, event.getExplosion());
            event.setCanceled(true);
        }
    }
}
