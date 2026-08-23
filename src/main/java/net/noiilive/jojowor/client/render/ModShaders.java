package net.noiilive.jojowor.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.noiilive.jojowor.JoJoWoR;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@EventBusSubscriber(modid = JoJoWoR.MODID, value = Dist.CLIENT)
public final class ModShaders {
    @Nullable
    private static ShaderInstance timeStop;

    private ModShaders() {}

    @Nullable
    public static ShaderInstance timeStop() {
        return timeStop;
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "timestop"),
                        DefaultVertexFormat.POSITION),
                shader -> timeStop = shader);
    }
}
