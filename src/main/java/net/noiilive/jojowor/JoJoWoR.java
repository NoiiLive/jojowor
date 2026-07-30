package net.noiilive.jojowor;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.noiilive.jojowor.registry.ModAttachments;
import net.noiilive.jojowor.registry.ModBlocks;
import net.noiilive.jojowor.registry.ModCreativeTabs;
import net.noiilive.jojowor.registry.ModEffects;
import net.noiilive.jojowor.registry.ModItems;
import net.noiilive.jojowor.registry.ModStands;

import org.slf4j.Logger;

@Mod(JoJoWoR.MODID)
public class JoJoWoR {
    public static final String MODID = "jojowor";

    public static final Logger LOGGER = LogUtils.getLogger();

    public JoJoWoR(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModStands.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModEffects.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("JoJo: Winds of Requiem loading.");
    }
}
