package net.noiilive.jojowor.mixin.client;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.noiilive.jojowor.client.ClientTimeStop;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {
    @Inject(method = "cycleAnimationFrames", at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$freezeAnimatedTextures(CallbackInfo ci) {
        if (ClientTimeStop.affectsView()) {
            ci.cancel();
        }
    }
}
