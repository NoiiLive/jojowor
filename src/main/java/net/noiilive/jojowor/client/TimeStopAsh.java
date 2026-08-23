package net.noiilive.jojowor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.noiilive.jojowor.JoJoWoR;

@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public final class TimeStopAsh {
    public static final int SPAWN_ATTEMPTS = 14;
    public static final double SPREAD_HORIZONTAL = 18.0D;
    public static final double SPREAD_VERTICAL = 11.0D;
    public static final double DRIFT = 0.006D;

    private TimeStopAsh() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || !ClientTimeStop.isActive()) {
            return;
        }

        Vec3 origin = minecraft.gameRenderer.getMainCamera().getPosition();
        RandomSource random = level.random;

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double x = origin.x + (random.nextDouble() * 2.0D - 1.0D) * SPREAD_HORIZONTAL;
            double y = origin.y + (random.nextDouble() * 2.0D - 1.0D) * SPREAD_VERTICAL;
            double z = origin.z + (random.nextDouble() * 2.0D - 1.0D) * SPREAD_HORIZONTAL;

            if (!ClientTimeStop.coversPosition(level, x, y, z)) {
                continue;
            }

            Particle particle = minecraft.particleEngine.createParticle(ParticleTypes.ASH, x, y, z,
                    (random.nextDouble() - 0.5D) * DRIFT,
                    -random.nextDouble() * DRIFT,
                    (random.nextDouble() - 0.5D) * DRIFT);
            if (particle instanceof FrozenParticle ash) {
                ash.jojowor$markTimeStopAsh();
            }
        }
    }
}
