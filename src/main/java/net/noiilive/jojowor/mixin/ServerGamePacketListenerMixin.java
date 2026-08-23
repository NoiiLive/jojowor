package net.noiilive.jojowor.mixin;

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.noiilive.jojowor.stand.ability.TimeStops;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleClientCommand", at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$holdRespawn(ServerboundClientCommandPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundClientCommandPacket.Action.PERFORM_RESPAWN) {
            return;
        }
        if (this.player != null && TimeStops.stopAffecting(this.player) != null) {
            ci.cancel();
        }
    }
}
