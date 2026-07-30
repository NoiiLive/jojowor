package net.noiilive.jojowor.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class SkinSlotButton extends AbstractButton {
    private final BooleanSupplier unlocked;
    private final BooleanSupplier selected;
    private final Runnable onSelect;

    public SkinSlotButton(int x, int y, Component narration,
                          BooleanSupplier unlocked, BooleanSupplier selected, Runnable onSelect) {
        super(x, y, SkinTextures.SLOT_SIZE, SkinTextures.SLOT_SIZE, narration);
        this.unlocked = unlocked;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    @Override
    public void onPress() {
        if (this.unlocked.getAsBoolean()) {
            this.onSelect.run();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int section;
        if (this.selected.getAsBoolean()) {
            section = SkinTextures.SLOT_SELECTED;
        } else if (this.unlocked.getAsBoolean()) {
            section = isHoveredOrFocused() ? SkinTextures.SLOT_UNLOCKED_HOVERED : SkinTextures.SLOT_UNLOCKED;
        } else {
            section = isHoveredOrFocused() ? SkinTextures.SLOT_LOCKED_HOVERED : SkinTextures.SLOT_LOCKED;
        }
        SkinTextures.blitSlot(guiGraphics, getX(), getY(), section);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
