package net.noiilive.jojowor.stand;

import net.minecraft.world.entity.player.Player;
import net.noiilive.jojowor.registry.ModAttachments;

import org.jetbrains.annotations.Nullable;

public final class Stands {
    private Stands() {}

    public static StandData getData(Player player) {
        return player.getData(ModAttachments.STAND);
    }

    @Nullable
    public static Stand get(Player player) {
        return getData(player).stand();
    }

    public static boolean has(Player player) {
        return get(player) != null;
    }

    public static void set(Player player, @Nullable Stand stand) {
        player.setData(ModAttachments.STAND, StandData.of(stand));
    }

    public static void clear(Player player) {
        set(player, null);
    }

    public static boolean isSummoned(Player player) {
        return getData(player).summoned();
    }

    public static void setSummoned(Player player, boolean summoned) {
        StandData data = getData(player);
        StandData updated = data.withSummoned(summoned && data.stand() != null);
        if (data.equals(updated)) {
            return;
        }
        player.setData(ModAttachments.STAND, updated);
    }

    public static void toggleSummoned(Player player) {
        setSummoned(player, !isSummoned(player));
    }

    public static GuardMode guardMode(Player player) {
        return getData(player).guardMode();
    }

    public static void setGuardMode(Player player, GuardMode mode) {
        StandData data = getData(player);
        StandData updated = data.withGuardMode(mode);
        if (data.equals(updated)) {
            return;
        }
        player.setData(ModAttachments.STAND, updated);
    }
}
