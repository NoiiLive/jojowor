package net.noiilive.jojowor.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.noiilive.jojowor.stand.ability.TimeStopWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public class LevelMixin {
    @Redirect(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"),
            require = 0)
    private void jojowor$freezeBlockEntity(TickingBlockEntity ticker) {
        if (TimeStopWorld.frozen((Level) (Object) this, ticker.getPos())) {
            return;
        }
        ticker.tick();
    }
}
