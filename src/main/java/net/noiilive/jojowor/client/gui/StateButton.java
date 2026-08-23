package net.noiilive.jojowor.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

public class StateButton extends Button {
    private final ResourceLocation texture;
    private final BooleanSupplier secondHalf;

    public StateButton(ResourceLocation texture, int x, int y, int width, int height,
                       Component narration, BooleanSupplier secondHalf, OnPress onPress) {
        super(x, y, width, height, narration, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.secondHalf = secondHalf;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PosingTextures.blitSplit(guiGraphics, this.texture, getX(), getY(), getWidth(), getHeight(),
                this.secondHalf.getAsBoolean());
    }
}
