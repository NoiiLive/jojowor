package net.noiilive.jojowor.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class PoseDropdown extends AbstractWidget {
    private static final int ENTRY_HEIGHT = 21;
    private static final int TEXT_BOTTOM_OFFSET = 13;

    private final List<String> options;
    private final Function<String, Component> labeler;
    private final Consumer<String> onSelect;
    private String selected;
    private boolean open;
    private int scrollOffset;

    public PoseDropdown(int x, int y, int width, int height, List<String> options, String selected,
                        Function<String, Component> labeler, Consumer<String> onSelect) {
        super(x, y, width, height, Component.empty());
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
            if (this.open) {
                int selectedIndex = Math.max(0, this.options.indexOf(this.selected));
                this.scrollOffset = clampScroll(selectedIndex - visibleCount() / 2);
            }
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

    public boolean handleGlobalScroll(double mouseX, double mouseY, double scrollY) {
        if (!this.open || mouseX < getX() || mouseX >= getX() + getWidth()) {
            return false;
        }
        int listTop = getY() + getHeight();
        if (mouseY < listTop || mouseY >= listTop + visibleCount() * ENTRY_HEIGHT) {
            return false;
        }
        this.scrollOffset = clampScroll(this.scrollOffset - (int) Math.signum(scrollY));
        return true;
    }

    private int visibleCount() {
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int available = (screenHeight - (getY() + getHeight())) / ENTRY_HEIGHT;
        return Math.max(1, Math.min(available, this.options.size()));
    }

    private int clampScroll(int offset) {
        return Math.max(0, Math.min(offset, this.options.size() - visibleCount()));
    }

    private int entryIndexAt(double mouseX, double mouseY) {
        if (mouseX < getX() || mouseX >= getX() + getWidth()) {
            return -1;
        }
        int listTop = getY() + getHeight();
        int row = (int) ((mouseY - listTop) / ENTRY_HEIGHT);
        if (mouseY < listTop || row < 0 || row >= visibleCount()) {
            return -1;
        }
        int index = row + this.scrollOffset;
        return index < this.options.size() ? index : -1;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        float centerX = getX() + getWidth() / 2.0F - 0.5F;
        PosingTextures.blitSplit(guiGraphics, PosingTextures.DROPDOWN_BAR,
                getX(), getY(), getWidth(), getHeight(), this.open);
        PosingTextures.drawHeader(guiGraphics, font, this.labeler.apply(this.selected),
                centerX, getY() + TEXT_BOTTOM_OFFSET);

        if (!this.open) {
            return;
        }

        this.scrollOffset = clampScroll(this.scrollOffset);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        int y = getY() + getHeight();
        int visible = visibleCount();
        for (int row = 0; row < visible; row++) {
            int index = row + this.scrollOffset;
            boolean entryHovered = entryIndexAt(mouseX, mouseY) == index;
            PosingTextures.blitSplit(guiGraphics, PosingTextures.DROPDOWN_ENTRY,
                    getX(), y, getWidth(), ENTRY_HEIGHT, entryHovered);
            PosingTextures.drawHeader(guiGraphics, font, this.labeler.apply(this.options.get(index)),
                    centerX, y + TEXT_BOTTOM_OFFSET,
                    this.options.get(index).equals(this.selected) ? 0xFFFFD770 : 0xFFFFFFFF);
            y += ENTRY_HEIGHT;
        }
        guiGraphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
