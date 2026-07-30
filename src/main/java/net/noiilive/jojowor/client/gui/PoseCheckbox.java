package net.noiilive.jojowor.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class PoseCheckbox extends AbstractWidget {
    private final Consumer<Boolean> onChange;
    private boolean checked;

    public PoseCheckbox(int x, int y, int width, int height, Component narration,
                        boolean checked, Consumer<Boolean> onChange) {
        super(x, y, width, height, narration);
        this.checked = checked;
        this.onChange = onChange;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.checked = !this.checked;
        this.onChange.accept(this.checked);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PosingTextures.blitSplit(guiGraphics, PosingTextures.CHECKBOX,
                getX(), getY(), getWidth(), getHeight(), this.checked);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
