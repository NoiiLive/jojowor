package net.noiilive.jojowor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.noiilive.jojowor.JoJoWoR;
import net.noiilive.jojowor.client.model.DummyStandModel;
import net.noiilive.jojowor.client.render.StandRenderers;
import net.noiilive.jojowor.client.render.StandTracker;
import net.noiilive.jojowor.stand.Stands;

@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public final class ModClientEvents {
    private static final int COOLDOWN_BAR_WIDTH = 16;
    private static final int COOLDOWN_BAR_HEIGHT = 2;
    private static final int COOLDOWN_BAR_OFFSET_Y = 14;

    private ModClientEvents() {}

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.DUMMY_STAND, DummyStandModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        StandRenderers.bake(event.getEntityModels());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "stand_cooldown"),
                (guiGraphics, deltaTracker) -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    LocalPlayer player = minecraft.player;
                    if (player == null || !Stands.isSummoned(player)
                            || !minecraft.options.getCameraType().isFirstPerson()) {
                        return;
                    }
                    float progress = StandTracker.punchCooldownProgress(player,
                            deltaTracker.getGameTimeDeltaPartialTick(true));
                    if (progress >= 1.0F) {
                        return;
                    }
                    int x = guiGraphics.guiWidth() / 2 - COOLDOWN_BAR_WIDTH / 2;
                    int y = guiGraphics.guiHeight() / 2 - COOLDOWN_BAR_OFFSET_Y;
                    guiGraphics.fill(x - 1, y - 1,
                            x + COOLDOWN_BAR_WIDTH + 1, y + COOLDOWN_BAR_HEIGHT + 1, 0x90000000);
                    guiGraphics.fill(x, y,
                            x + (int) (COOLDOWN_BAR_WIDTH * progress), y + COOLDOWN_BAR_HEIGHT, 0xFFFFFFFF);
                });
    }
}
