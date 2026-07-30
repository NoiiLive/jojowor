package net.noiilive.jojowor.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

public class ColorSlider extends AbstractSliderButton {
    private static final int HANDLE_WIDTH = 5;

    private final String label;
    private final int max;
    private final IntConsumer onChange;

    public ColorSlider(int x, int y, int width, int height, String label, int max, int current,
                       IntConsumer onChange) {
        super(x, y, width, height, Component.empty(),
                Mth.clamp(current / (double) max, 0.0D, 1.0D));
        this.label = label;
        this.max = max;
        this.onChange = onChange;
        updateMessage();
    }

    public int intValue() {
        return (int) Math.round(this.value * this.max);
    }

    public void setIntValue(int value) {
        this.value = Mth.clamp(value / (double) this.max, 0.0D, 1.0D);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(this.label + " " + intValue()));
    }

    @Override
    protected void applyValue() {
        this.onChange.accept(intValue());
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PosingTextures.blitFull(guiGraphics, SkinTextures.TRACK, getX(), getY(), getWidth(), getHeight());
        int handleX = getX() + (int) (this.value * (getWidth() - HANDLE_WIDTH));
        PosingTextures.blitSplit(guiGraphics, SkinTextures.HANDLE,
                handleX, getY(), HANDLE_WIDTH, getHeight(), isHoveredOrFocused());
        PosingTextures.drawFitText(guiGraphics, Minecraft.getInstance().font, getMessage(),
                getX() + getWidth() / 2, getY() + getHeight() / 2, getWidth() - 6, 0xFFFFFF);
    }
}
