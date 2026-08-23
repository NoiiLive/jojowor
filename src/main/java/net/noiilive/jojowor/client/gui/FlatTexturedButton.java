package net.noiilive.jojowor.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FlatTexturedButton extends Button {
    private final ResourceLocation texture;

    public FlatTexturedButton(ResourceLocation texture, int x, int y, int width, int height,
                              Component narration, OnPress onPress) {
        super(x, y, width, height, narration, onPress, DEFAULT_NARRATION);
        this.texture = texture;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PosingTextures.blitFull(guiGraphics, this.texture, getX(), getY(), getWidth(), getHeight());
    }
}
