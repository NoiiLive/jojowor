package net.noiilive.jojowor;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.noiilive.jojowor.registry.ModBlocks;
import net.noiilive.jojowor.registry.ModCreativeTabs;
import net.noiilive.jojowor.registry.ModItems;

import org.slf4j.Logger;

/**
 * Main entrypoint for JoJo: Winds of Requiem.
 *
 * <p>The value passed to {@link Mod} must match the {@code modId} in
 * {@code META-INF/neoforge.mods.toml}.</p>
 */
@Mod(JoJoWoR.MODID)
public class JoJoWoR {
    /** Mod id, referenced everywhere a namespace is needed. */
    public static final String MODID = "jojowor";

    /** Shared logger for the mod. */
    public static final Logger LOGGER = LogUtils.getLogger();

    public JoJoWoR(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // Registries
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // Config
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("JoJo: Winds of Requiem loading.");
    }
}
