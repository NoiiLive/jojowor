package net.noiilive.jojowor.stand.ability;

import net.minecraft.server.level.ServerPlayer;
import net.noiilive.jojowor.registry.ModAbilities;
import net.noiilive.jojowor.stand.Stands;

import org.jetbrains.annotations.Nullable;

public final class StandAbilities {
    public static final int SLOT_COUNT = 3;

    private StandAbilities() {}

    @Nullable
    public static StandAbility slotAbility(net.minecraft.world.entity.player.Player player, int slot) {
        if (slot < 0 || slot >= SLOT_COUNT || !Stands.has(player)) {
            return null;
        }
        return slot == 0 ? ModAbilities.TIME_STOP.get() : null;
    }

    public static void activate(ServerPlayer player, int slot) {
        StandAbility ability = slotAbility(player, slot);
        if (ability == null || !ability.canActivate(player)) {
            return;
        }
        if (!ability.usableWhileFrozen() && TimeStops.stopAffecting(player) != null) {
            return;
        }
        ability.activate(player);
    }
}
