package net.noiilive.jojowor.stand.ability;

import net.minecraft.server.level.ServerPlayer;

public class TimeStopAbility extends StandAbility {
    @Override
    public boolean usableWhileFrozen() {
        return true;
    }

    @Override
    public void activate(ServerPlayer player) {
        TimeStops.toggle(player);
    }
}
