package net.noiilive.jojowor.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleConsumer;

public class PoseSlider extends AbstractSliderButton {
    private static final int HANDLE_WIDTH = 5;

    private final ResourceLocation track;
    @Nullable
    private final Component label;
    private final double min;
    private final double max;
    private final String format;
    private final DoubleConsumer onChange;

    public PoseSlider(ResourceLocation track, int x, int y, int width, int height, @Nullable Component label,
                      double min, double max, double current, String format, DoubleConsumer onChange) {
        super(x, y, width, height, Component.empty(), Mth.clamp((current - min) / (max - min), 0.0D, 1.0D));
        this.track = track;
        this.label = label;
        this.min = min;
        this.max = max;
        this.format = format;
        this.onChange = onChange;
        updateMessage();
    }

    private double mappedValue() {
        return Mth.lerp(this.value, this.min, this.max);
    }

    @Override
    protected void updateMessage() {
        Component value = Component.literal(String.format(this.format, mappedValue()));
        setMessage(this.label == null
                ? value
                : Component.empty().append(this.label).append(": ").append(value));
    }

    @Override
    protected void applyValue() {
        this.onChange.accept(mappedValue());
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PosingTextures.blitFull(guiGraphics, this.track, getX(), getY(), getWidth(), getHeight());
        int handleX = getX() + (int) (this.value * (getWidth() - HANDLE_WIDTH));
        PosingTextures.blitSplit(guiGraphics, PosingTextures.HANDLE,
                handleX, getY(), HANDLE_WIDTH, getHeight(), isHoveredOrFocused());
        PosingTextures.drawFitText(guiGraphics, Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2, getY() + getHeight() / 2, getWidth() - 6, 0xFFFFFF);
    }
}
