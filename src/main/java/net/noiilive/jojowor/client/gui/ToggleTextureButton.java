package net.noiilive.jojowor.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

public class ToggleTextureButton extends Button {
    private final ResourceLocation offTexture;
    private final ResourceLocation onTexture;
    private final BooleanSupplier state;

    public ToggleTextureButton(ResourceLocation offTexture, ResourceLocation onTexture,
                               int x, int y, int width, int height,
                               Component narration, BooleanSupplier state, OnPress onPress) {
        super(x, y, width, height, narration, onPress, DEFAULT_NARRATION);
        this.offTexture = offTexture;
        this.onTexture = onTexture;
        this.state = state;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PosingTextures.blitSplit(guiGraphics, this.state.getAsBoolean() ? this.onTexture : this.offTexture,
                getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused());
    }
}
