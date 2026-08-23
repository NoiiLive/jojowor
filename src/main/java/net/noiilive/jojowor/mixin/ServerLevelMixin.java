package net.noiilive.jojowor.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.noiilive.jojowor.stand.ability.TimeStopWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$freezeBlockTick(BlockPos pos, Block block, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!TimeStopWorld.frozen(level, pos)) {
            return;
        }
        if (!level.getBlockTicks().hasScheduledTick(pos, block)) {
            level.scheduleTick(pos, block, 1);
        }
        ci.cancel();
    }

    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true, require = 0)
    private void jojowor$freezeFluidTick(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!TimeStopWorld.frozen(level, pos)) {
            return;
        }
        if (!level.getFluidTicks().hasScheduledTick(pos, fluid)) {
            level.scheduleTick(pos, fluid, 1);
        }
        ci.cancel();
    }

    @Redirect(
            method = "tickChunk",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"),
            require = 0)
    private void jojowor$freezeBlockRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (TimeStopWorld.frozen(level, pos)) {
            return;
        }
        state.randomTick(level, pos, random);
    }

    @Redirect(
            method = "tickChunk",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"),
            require = 0)
    private void jojowor$freezeFluidRandomTick(FluidState state, net.minecraft.world.level.Level level,
                                               BlockPos pos, RandomSource random) {
        if (TimeStopWorld.frozen(level, pos)) {
            return;
        }
        state.randomTick(level, pos, random);
    }
}
