package net.noiilive.jojowor.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.noiilive.jojowor.client.ClientTimeStop;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$blockFrozenTurn(double movementTime, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && ClientTimeStop.isFrozen(player)) {
            ci.cancel();
        }
    }
}
