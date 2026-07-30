package net.noiilive.jojowor.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class TexturedButton extends Button {
    private final ResourceLocation texture;
    @Nullable
    private final Component label;

    public TexturedButton(ResourceLocation texture, int x, int y, int width, int height,
                          Component narration, @Nullable Component label, OnPress onPress) {
        super(x, y, width, height, narration, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.label = label;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PosingTextures.blitSplit(guiGraphics, this.texture, getX(), getY(), getWidth(), getHeight(),
                isHoveredOrFocused());
        if (this.label != null) {
            PosingTextures.drawFitText(guiGraphics, Minecraft.getInstance().font, this.label,
                    getX() + getWidth() / 2, getY() + getHeight() / 2, getWidth() - 6, 0xFFFFFF);
        }
    }
}
