package net.noiilive.jojowor.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class PoseSlotButton extends Button {
    private final int slotNumber;
    private final BooleanSupplier selected;
    private final BooleanSupplier occupied;

    public PoseSlotButton(int x, int y, int width, int height, int slotNumber,
                          BooleanSupplier selected, BooleanSupplier occupied,
                          Component narration, OnPress onPress) {
        super(x, y, width, height, narration, onPress, DEFAULT_NARRATION);
        this.slotNumber = slotNumber;
        this.selected = selected;
        this.occupied = occupied;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PosingTextures.blitSplit(guiGraphics, PosingTextures.SAVESLOT,
                getX(), getY(), getWidth(), getHeight(), this.selected.getAsBoolean());
        int color = this.selected.getAsBoolean() ? 0xFFD770 : this.occupied.getAsBoolean() ? 0xFFFFFF : 0x707070;
        PosingTextures.drawFitText(guiGraphics, Minecraft.getInstance().font,
                Component.literal(String.valueOf(this.slotNumber)),
                getX() + getWidth() / 2, getY() + getHeight() / 2, getWidth() - 4, color);
    }
}
