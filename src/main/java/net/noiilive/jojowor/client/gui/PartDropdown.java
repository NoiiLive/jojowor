package net.noiilive.jojowor.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class PartDropdown extends AbstractWidget {
    private static final int ENTRY_HEIGHT = 13;

    private final net.minecraft.resources.ResourceLocation dropdownTexture;
    private final net.minecraft.resources.ResourceLocation entryTexture;
    private final List<String> options;
    private final Function<String, Component> labeler;
    private final Consumer<String> onSelect;
    private String selected;
    private boolean open;

    public PartDropdown(net.minecraft.resources.ResourceLocation dropdownTexture,
                        net.minecraft.resources.ResourceLocation entryTexture,
                        int x, int y, int width, int height, List<String> options, String selected,
                        Function<String, Component> labeler, Consumer<String> onSelect) {
        super(x, y, width, height, Component.empty());
        this.dropdownTexture = dropdownTexture;
        this.entryTexture = entryTexture;
        this.options = options;
        this.selected = selected;
        this.labeler = labeler;
        this.onSelect = onSelect;
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean handleGlobalClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (overMain(mouseX, mouseY)) {
            this.open = !this.open;
            return true;
        }
        if (this.open) {
            int index = entryIndexAt(mouseX, mouseY);
            this.open = false;
            if (index >= 0) {
                this.selected = this.options.get(index);
                this.onSelect.accept(this.selected);
            }
            return index >= 0;
        }
        return false;
    }

    private boolean overMain(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight();
    }

    private int entryIndexAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX >= getX() + getWidth()) {
            return -1;
        }
        int listTop = getY() + getHeight();
        int index = (int) ((mouseY - listTop) / ENTRY_HEIGHT);
        return mouseY >= listTop && index >= 0 && index < this.options.size() ? index : -1;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PosingTextures.blitSplit(guiGraphics, this.dropdownTexture,
                getX(), getY(), getWidth(), getHeight(), overMain(mouseX, mouseY));
        PosingTextures.drawFitText(guiGraphics, Minecraft.getInstance().font,
                Component.empty().append(this.labeler.apply(this.selected)).append(this.open ? " ▲" : " ▼"),
                getX() + getWidth() / 2, getY() + getHeight() / 2 + 1, getWidth() - 6, 0xFFFFFF);

        if (!this.open) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        int y = getY() + getHeight();
        for (int i = 0; i < this.options.size(); i++) {
            boolean entryHovered = entryIndexAt(mouseX, mouseY) == i;
            PosingTextures.blitSplit(guiGraphics, this.entryTexture,
                    getX(), y, getWidth(), ENTRY_HEIGHT, entryHovered);
            PosingTextures.drawFitText(guiGraphics, Minecraft.getInstance().font,
                    this.labeler.apply(this.options.get(i)),
                    getX() + getWidth() / 2, y + ENTRY_HEIGHT / 2, getWidth() - 6,
                    this.options.get(i).equals(this.selected) ? 0xFFD770 : 0xE0E0E0);
            y += ENTRY_HEIGHT;
        }
        guiGraphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
