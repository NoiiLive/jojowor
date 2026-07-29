package net.noiilive.jojowor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only entrypoint. This class is never loaded on a dedicated server,
 * so client-only code is safe to touch from here.
 */
@Mod(value = JoJoWoR.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public class JoJoWoRClient {
    public JoJoWoRClient(ModContainer container) {
        // Lets NeoForge build a config screen from Config.SPEC
        // (Mods screen > JoJo: Winds of Requiem > Config).
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Client-side setup: renderers, key mappings, screens, etc.
    }
}
