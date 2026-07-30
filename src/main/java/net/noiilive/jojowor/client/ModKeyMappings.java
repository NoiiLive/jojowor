package net.noiilive.jojowor.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.noiilive.jojowor.JoJoWoR;

import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories." + JoJoWoR.MODID;

    public static final KeyMapping SUMMON_STAND = new KeyMapping(
            "key." + JoJoWoR.MODID + ".summon_stand",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY);

    public static final KeyMapping MAIN_MENU = new KeyMapping(
            "key." + JoJoWoR.MODID + ".main_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY);

    private ModKeyMappings() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SUMMON_STAND);
        event.register(MAIN_MENU);
    }
}
