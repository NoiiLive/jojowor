package net.noiilive.jojowor.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.noiilive.jojowor.client.render.StandTracker;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleStandThrowEffect(StandThrowEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            Entity owner = level.getEntity(payload.ownerEntityId());
            if (owner instanceof Player player) {
                StandTracker.startThrow(player, payload.stack());
            }
        });
    }

    public static void handleStandUppercutEffect(StandUppercutEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            Entity owner = level.getEntity(payload.ownerEntityId());
            if (owner instanceof Player player) {
                StandTracker.startUppercut(player, payload.targetEntityId());
            }
        });
    }

    public static void handleStandSlamEffect(StandSlamEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            Entity owner = level.getEntity(payload.ownerEntityId());
            if (owner instanceof Player player) {
                StandTracker.startSlam(player, payload.targetEntityId());
            }
        });
    }

    public static void handleStandBarrageEffect(StandBarrageEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            Entity owner = level.getEntity(payload.ownerEntityId());
            if (owner instanceof Player player) {
                StandTracker.setBarraging(player, payload.active());
            }
        });
    }

    public static void handleStandAttackEffect(StandAttackEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            Entity owner = level.getEntity(payload.ownerEntityId());
            if (owner instanceof Player player) {
                StandTracker.startPunch(player, payload.targetEntityId(), payload.blockTarget());
            }
        });
    }
}
