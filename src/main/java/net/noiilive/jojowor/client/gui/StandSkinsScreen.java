package net.noiilive.jojowor.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.jojowor.client.model.StandAnimation;
import net.noiilive.jojowor.client.render.StandRenderers;
import net.noiilive.jojowor.network.SetStandSkinPayload;
import net.noiilive.jojowor.stand.Stand;
import net.noiilive.jojowor.stand.StandData;
import net.noiilive.jojowor.stand.Stands;
import net.noiilive.jojowor.stand.skin.StandSkin;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class StandSkinsScreen extends Screen {
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 188;
    private static final int POSING_IMAGE_HEIGHT = 188;

    private static final int VIEWPORT_X = 7;
    private static final int VIEWPORT_Y = 32;
    private static final int VIEWPORT_WIDTH = 60;
    private static final int VIEWPORT_HEIGHT = 83;

    private static final int SLOT_FIRST_X = 72;
    private static final int SLOT_FIRST_Y = 33;
    private static final int SLOT_COLUMNS = 6;
    private static final int SLOT_ROWS = 4;

    private static final int DROPDOWN_X = 71;
    private static final int DROPDOWN_Y = 102;
    private static final int DROPDOWN_WIDTH = 98;
    private static final int DROPDOWN_HEIGHT = 13;

    private static final int TRACK_WIDTH = 77;
    private static final int TRACK_HEIGHT = 11;
    private static final int RGB_X = 8;
    private static final int RED_Y = 119;
    private static final int GREEN_Y = 134;
    private static final int BLUE_Y = 149;
    private static final int HSV_X = 91;
    private static final int SATURATION_Y = 119;
    private static final int LIGHTNESS_Y = 134;

    private static final int BUTTON_WIDTH = 38;
    private static final int BUTTON_HEIGHT = 13;
    private static final int RESET_X = 90;
    private static final int SAVE_X = 131;
    private static final int BUTTON_Y = 148;

    private static final int SAVEBAR_WIDTH = 34;
    private static final int SAVEBAR_HEIGHT = 126;
    private static final int SAVEBAR_GAP = 2;
    private static final int SLOT_X = 5;
    private static final int SAVE_SLOT_FIRST_Y = 24;
    private static final int SLOT_PITCH = 20;
    private static final int SAVE_SLOT_SIZE = 17;
    private static final int TAB_X = 27;
    private static final int TAB_LOAD_FIRST_Y = 25;
    private static final int TAB_CLEAR_FIRST_Y = 33;
    private static final int SIDE_TAB_WIDTH = 7;
    private static final int SIDE_TAB_HEIGHT = 7;

    private static final int TAB_WIDTH = 32;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_OVERLAP = 3;
    private static final int TAB_Y = 10;

    private static final int CHECKBOX_X = 33;
    private static final int CHECKBOX_Y = 169;
    private static final int CHECKBOX_SIZE = 15;
    private static final int HEX_BOX_X = 51;
    private static final int HEX_BOX_Y = 169;
    private static final int HEX_BOX_WIDTH = 91;
    private static final int HEX_BOX_HEIGHT = 14;

    private List<StandSkin> skins = List.of();
    private int selectedSkin;
    private int selectedLayer;
    private List<Integer> workingColors = new ArrayList<>();
    private boolean workingColored;
    private boolean initialized;
    private int selectedSlot = -1;
    private int savebarX;
    private int savebarY;

    private float viewYaw = 20.0F;
    private float viewPitch = -10.0F;
    private float zoom = 1.0F;

    private int leftPos;
    private int topPos;
    private int viewportLeft;
    private int viewportTop;
    private int viewportRight;
    private int viewportBottom;

    @Nullable
    private StandRenderers.Baked baked;
    @Nullable
    private PartDropdown dropdown;
    @Nullable
    private ColorSlider redSlider;
    @Nullable
    private ColorSlider greenSlider;
    @Nullable
    private ColorSlider blueSlider;
    @Nullable
    private ColorSlider saturationSlider;
    @Nullable
    private ColorSlider lightnessSlider;
    @Nullable
    private net.minecraft.client.gui.components.EditBox hexBox;

    public StandSkinsScreen() {
        super(Component.translatable("jojowor.screen.skins.title"));
    }

    @Override
    protected void init() {
        if (this.minecraft == null || this.minecraft.player == null) {
            onClose();
            return;
        }
        Stand stand = Stands.get(this.minecraft.player);
        this.baked = stand == null ? null : StandRenderers.get(stand.getId());
        if (stand == null || this.baked == null || stand.getSkins().isEmpty()) {
            onClose();
            return;
        }
        this.skins = stand.getSkins();

        StandData data = Stands.getData(this.minecraft.player);
        if (!this.initialized) {
            this.initialized = true;
            this.selectedSkin = Mth.clamp(data.skin(), 0, this.skins.size() - 1);
            this.workingColors = savedColors(this.selectedSkin, data);
            this.workingColored = data.skinColored();
        }
        List<StandSkin.Layer> recolorable = currentSkin().recolorableLayers();
        this.selectedLayer = Mth.clamp(this.selectedLayer, 0, Math.max(0, recolorable.size() - 1));

        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - POSING_IMAGE_HEIGHT) / 2;

        this.viewportLeft = this.leftPos + VIEWPORT_X;
        this.viewportTop = this.topPos + VIEWPORT_Y;
        this.viewportRight = this.viewportLeft + VIEWPORT_WIDTH;
        this.viewportBottom = this.viewportTop + VIEWPORT_HEIGHT;

        addRenderableWidget(new TabButton(SkinTextures.TAB_LEFT,
                this.leftPos - TAB_WIDTH + TAB_OVERLAP + 1, this.topPos + TAB_Y, TAB_WIDTH, TAB_HEIGHT,
                Component.translatable("jojowor.screen.skins.title"), () -> true,
                button -> {}));

        addRenderableWidget(new TabButton(SkinTextures.TAB_LEFT,
                this.leftPos - TAB_WIDTH + TAB_OVERLAP + 1, this.topPos + TAB_Y + TAB_HEIGHT + 2,
                TAB_WIDTH, TAB_HEIGHT,
                Component.translatable("jojowor.screen.posing.title"), () -> false,
                button -> this.minecraft.setScreen(new PosingScreen())));

        for (int i = 0; i < Math.min(this.skins.size(), SLOT_COLUMNS * SLOT_ROWS); i++) {
            final int slot = i;
            addRenderableWidget(new SkinSlotButton(
                    this.leftPos + SLOT_FIRST_X + (i % SLOT_COLUMNS) * SkinTextures.SLOT_SIZE,
                    this.topPos + SLOT_FIRST_Y + (i / SLOT_COLUMNS) * SkinTextures.SLOT_SIZE,
                    Component.literal(this.skins.get(slot).name()),
                    () -> true,
                    () -> slot == this.selectedSkin,
                    () -> selectSkin(slot)));
        }

        int color = currentColor();
        this.redSlider = addRenderableWidget(new ColorSlider(
                this.leftPos + RGB_X, this.topPos + RED_Y, TRACK_WIDTH, TRACK_HEIGHT,
                "R", 255, color >> 16 & 0xFF, value -> setChannel(16, value)));
        this.greenSlider = addRenderableWidget(new ColorSlider(
                this.leftPos + RGB_X, this.topPos + GREEN_Y, TRACK_WIDTH, TRACK_HEIGHT,
                "G", 255, color >> 8 & 0xFF, value -> setChannel(8, value)));
        this.blueSlider = addRenderableWidget(new ColorSlider(
                this.leftPos + RGB_X, this.topPos + BLUE_Y, TRACK_WIDTH, TRACK_HEIGHT,
                "B", 255, color & 0xFF, value -> setChannel(0, value)));

        float[] hsv = rgbToHsv(color);
        this.saturationSlider = addRenderableWidget(new ColorSlider(
                this.leftPos + HSV_X, this.topPos + SATURATION_Y, TRACK_WIDTH, TRACK_HEIGHT,
                "S", 100, Math.round(hsv[1] * 100.0F), value -> setSaturation(value)));
        this.lightnessSlider = addRenderableWidget(new ColorSlider(
                this.leftPos + HSV_X, this.topPos + LIGHTNESS_Y, TRACK_WIDTH, TRACK_HEIGHT,
                "L", 100, Math.round(hsv[2] * 100.0F), value -> setLightness(value)));

        addRenderableWidget(new TexturedButton(SkinTextures.BUTTON,
                this.leftPos + RESET_X, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("jojowor.screen.skins.reset"),
                Component.translatable("jojowor.screen.skins.reset"), button -> {
            List<Integer> defaults = currentSkin().defaultColors();
            if (!this.workingColors.isEmpty() && this.selectedLayer < defaults.size()) {
                this.workingColors.set(this.selectedLayer, defaults.get(this.selectedLayer));
            }
            rebuildWidgets();
        }));

        addRenderableWidget(new TexturedButton(SkinTextures.BUTTON,
                this.leftPos + SAVE_X, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("jojowor.screen.skins.save"),
                Component.translatable("jojowor.screen.skins.save"), button -> {
            PacketDistributor.sendToServer(new SetStandSkinPayload(
                    this.selectedSkin, List.copyOf(this.workingColors), this.workingColored));
            if (this.selectedSlot >= 0) {
                net.noiilive.jojowor.client.skin.SkinColorSlots.save(this.selectedSlot,
                        new net.noiilive.jojowor.client.skin.SkinColorSlots.SavedColors(
                                this.selectedSkin, List.copyOf(this.workingColors)));
            }
            onClose();
        }));

        this.savebarX = this.leftPos + IMAGE_WIDTH + SAVEBAR_GAP;
        this.savebarY = this.topPos + (IMAGE_HEIGHT - SAVEBAR_HEIGHT) / 2;

        for (int i = 0; i < net.noiilive.jojowor.client.skin.SkinColorSlots.SLOT_COUNT; i++) {
            final int slot = i;
            addRenderableWidget(new PoseSlotButton(
                    this.savebarX + SLOT_X, this.savebarY + SAVE_SLOT_FIRST_Y + SLOT_PITCH * i,
                    SAVE_SLOT_SIZE, SAVE_SLOT_SIZE, i + 1,
                    () -> this.selectedSlot == slot,
                    () -> !net.noiilive.jojowor.client.skin.SkinColorSlots.isEmpty(slot),
                    Component.translatable("jojowor.screen.skins.slot", i + 1), button ->
                    this.selectedSlot = slot));

            addRenderableWidget(new TexturedButton(PosingTextures.BUTTON_SAVE,
                    this.savebarX + TAB_X, this.savebarY + TAB_LOAD_FIRST_Y + SLOT_PITCH * i,
                    SIDE_TAB_WIDTH, SIDE_TAB_HEIGHT,
                    Component.translatable("jojowor.screen.skins.slot_load", i + 1), null, button -> {
                net.noiilive.jojowor.client.skin.SkinColorSlots.SavedColors saved =
                        net.noiilive.jojowor.client.skin.SkinColorSlots.get(slot);
                if (saved != null) {
                    this.selectedSlot = slot;
                    applySavedColors(saved);
                }
            }));

            addRenderableWidget(new TexturedButton(PosingTextures.BUTTON_CLEAR,
                    this.savebarX + TAB_X, this.savebarY + TAB_CLEAR_FIRST_Y + SLOT_PITCH * i,
                    SIDE_TAB_WIDTH, SIDE_TAB_HEIGHT,
                    Component.translatable("jojowor.screen.skins.slot_clear", i + 1), null, button ->
                    net.noiilive.jojowor.client.skin.SkinColorSlots.clear(slot)));
        }

        addRenderableWidget(new PoseCheckbox(
                this.leftPos + CHECKBOX_X, this.topPos + CHECKBOX_Y, CHECKBOX_SIZE, CHECKBOX_SIZE,
                Component.translatable("jojowor.screen.skins.recolor"),
                this.workingColored, value -> this.workingColored = value));

        this.hexBox = addRenderableWidget(new net.minecraft.client.gui.components.EditBox(this.font,
                this.leftPos + HEX_BOX_X + 3, this.topPos + HEX_BOX_Y + 3,
                HEX_BOX_WIDTH - 6, HEX_BOX_HEIGHT - 4,
                Component.translatable("jojowor.screen.skins.hex")));
        this.hexBox.setBordered(false);
        this.hexBox.setMaxLength(7);
        this.hexBox.setFilter(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
        this.hexBox.setValue(String.format("#%06X", currentColor()));

        List<String> layerNames = recolorable.stream().map(StandSkin.Layer::name).toList();
        if (!layerNames.isEmpty()) {
            this.dropdown = addRenderableWidget(new PartDropdown(
                    SkinTextures.DROPDOWN, SkinTextures.DROPDOWN_ENTRY,
                    this.leftPos + DROPDOWN_X, this.topPos + DROPDOWN_Y, DROPDOWN_WIDTH, DROPDOWN_HEIGHT,
                    layerNames, layerNames.get(this.selectedLayer),
                    name -> Component.literal(PosingTextures.prettyName(name)),
                    name -> {
                        this.selectedLayer = layerNames.indexOf(name);
                        rebuildWidgets();
                    }));
        }
    }

    private StandSkin currentSkin() {
        return this.skins.get(this.selectedSkin);
    }

    private List<Integer> savedColors(int skinIndex, StandData data) {
        StandSkin skin = this.skins.get(skinIndex);
        List<Integer> colors = new ArrayList<>(skin.defaultColors());
        if (skinIndex == Mth.clamp(data.skin(), 0, this.skins.size() - 1)) {
            for (int i = 0; i < colors.size(); i++) {
                colors.set(i, skin.colorAt(i, data.skinColors()));
            }
        }
        return colors;
    }

    private void applySavedColors(net.noiilive.jojowor.client.skin.SkinColorSlots.SavedColors saved) {
        this.selectedSkin = Mth.clamp(saved.skin(), 0, this.skins.size() - 1);
        this.selectedLayer = 0;
        List<Integer> colors = new ArrayList<>(currentSkin().defaultColors());
        for (int i = 0; i < colors.size() && i < saved.colors().size(); i++) {
            colors.set(i, saved.colors().get(i) & 0xFFFFFF);
        }
        this.workingColors = colors;
        rebuildWidgets();
    }

    private void selectSkin(int slot) {
        if (slot == this.selectedSkin || this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        this.selectedSkin = slot;
        this.selectedLayer = 0;
        this.workingColors = savedColors(slot, Stands.getData(this.minecraft.player));
        rebuildWidgets();
    }

    private int currentColor() {
        return this.workingColors.isEmpty() ? 0xFFFFFF : this.workingColors.get(this.selectedLayer);
    }

    private void setCurrentColor(int color) {
        if (!this.workingColors.isEmpty()) {
            this.workingColors.set(this.selectedLayer, color & 0xFFFFFF);
        }
    }

    private void setChannel(int shift, int value) {
        setCurrentColor(currentColor() & ~(0xFF << shift) | (value & 0xFF) << shift);
        syncHsvSliders();
        syncHexBox();
    }

    private void setSaturation(int value) {
        float[] hsv = rgbToHsv(currentColor());
        setCurrentColor(Mth.hsvToRgb(hsv[0], value / 100.0F, hsv[2]));
        syncRgbSliders();
        syncHexBox();
    }

    private void setLightness(int value) {
        float[] hsv = rgbToHsv(currentColor());
        setCurrentColor(Mth.hsvToRgb(hsv[0], hsv[1], value / 100.0F));
        syncRgbSliders();
        syncHexBox();
    }

    private void syncHexBox() {
        if (this.hexBox != null && !this.hexBox.isFocused()) {
            this.hexBox.setValue(String.format("#%06X", currentColor()));
        }
    }

    private void applyHexInput() {
        if (this.hexBox == null) {
            return;
        }
        String text = this.hexBox.getValue().trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.length() == 3) {
            StringBuilder expanded = new StringBuilder();
            for (char digit : text.toCharArray()) {
                expanded.append(digit).append(digit);
            }
            text = expanded.toString();
        }
        if (!text.matches("[0-9a-fA-F]{6}")) {
            return;
        }
        setCurrentColor(Integer.parseInt(text, 16));
        syncRgbSliders();
        syncHsvSliders();
        this.hexBox.setValue(String.format("#%06X", currentColor()));
        this.hexBox.setFocused(false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.hexBox != null && this.hexBox.isFocused()
                && (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)) {
            applyHexInput();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void syncRgbSliders() {
        int color = currentColor();
        if (this.redSlider != null) {
            this.redSlider.setIntValue(color >> 16 & 0xFF);
        }
        if (this.greenSlider != null) {
            this.greenSlider.setIntValue(color >> 8 & 0xFF);
        }
        if (this.blueSlider != null) {
            this.blueSlider.setIntValue(color & 0xFF);
        }
    }

    private void syncHsvSliders() {
        float[] hsv = rgbToHsv(currentColor());
        if (this.saturationSlider != null) {
            this.saturationSlider.setIntValue(Math.round(hsv[1] * 100.0F));
        }
        if (this.lightnessSlider != null) {
            this.lightnessSlider.setIntValue(Math.round(hsv[2] * 100.0F));
        }
    }

    private static float[] rgbToHsv(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255.0F;
        float g = (rgb >> 8 & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta <= 0.0F) {
            hue = 0.0F;
        } else if (max == r) {
            hue = (((g - b) / delta % 6.0F) + 6.0F) % 6.0F / 6.0F;
        } else if (max == g) {
            hue = ((b - r) / delta + 2.0F) / 6.0F;
        } else {
            hue = ((r - g) / delta + 4.0F) / 6.0F;
        }
        float saturation = max <= 0.0F ? 0.0F : delta / max;
        return new float[]{hue, saturation, max};
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        PosingTextures.blitFull(guiGraphics, SkinTextures.MENU,
                this.leftPos, this.topPos, IMAGE_WIDTH, IMAGE_HEIGHT);
        PosingTextures.blitFull(guiGraphics, SkinTextures.VIEWPORT,
                this.viewportLeft, this.viewportTop, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        PosingTextures.blitFull(guiGraphics, PosingTextures.SAVEBAR,
                this.savebarX, this.savebarY, SAVEBAR_WIDTH, SAVEBAR_HEIGHT);
        PosingTextures.drawHeader(guiGraphics, this.font, getTitle(),
                this.leftPos + 87.5F, this.topPos + 20.0F);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderViewport(guiGraphics, partialTick);
    }

    private void renderViewport(GuiGraphics guiGraphics, float partialTick) {
        if (this.baked == null || this.minecraft == null || this.minecraft.level == null
                || this.minecraft.player == null) {
            return;
        }

        double ageTicks = this.minecraft.level.getGameTime() + partialTick;
        this.baked.model().setupAnim(new StandAnimation(
                ageTicks, 0.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, -1.0F, -1.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F,
                Stands.getData(this.minecraft.player).pose()));

        float centerX = (this.viewportLeft + this.viewportRight) / 2.0F;
        float centerY = (this.viewportTop + this.viewportBottom) / 2.0F;
        float scale = VIEWPORT_HEIGHT / 3.2F * this.zoom;

        guiGraphics.enableScissor(this.viewportLeft, this.viewportTop, this.viewportRight, this.viewportBottom);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 100.0F);
        poseStack.scale(scale, scale, -scale);
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.PI));
        poseStack.mulPose(Axis.XP.rotationDegrees(this.viewPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(this.viewYaw));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        guiGraphics.flush();
        Lighting.setupForEntityInInventory();
        if (this.workingColored) {
            StandSkin skin = currentSkin();
            int recolorIndex = 0;
            for (StandSkin.Layer layer : skin.layers()) {
                int color = 0xFFFFFF;
                if (layer.recolorable()) {
                    color = recolorIndex < this.workingColors.size()
                            ? this.workingColors.get(recolorIndex)
                            : layer.defaultColor();
                    recolorIndex++;
                }
                VertexConsumer buffer = guiGraphics.bufferSource()
                        .getBuffer(this.baked.model().renderType(layer.texture()));
                this.baked.model().renderToBuffer(poseStack, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY,
                        0xFF000000 | (color & 0xFFFFFF));
            }
        } else {
            VertexConsumer buffer = guiGraphics.bufferSource()
                    .getBuffer(this.baked.model().renderType(this.baked.texture()));
            this.baked.model().renderToBuffer(poseStack, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        }
        guiGraphics.flush();
        Lighting.setupFor3DItems();

        poseStack.popPose();
        guiGraphics.disableScissor();
    }

    private boolean inViewport(double mouseX, double mouseY) {
        return mouseX >= this.viewportLeft && mouseX < this.viewportRight
                && mouseY >= this.viewportTop && mouseY < this.viewportBottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.dropdown != null && this.dropdown.handleGlobalClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && inViewport(mouseX, mouseY)) {
            this.viewYaw += (float) dragX;
            this.viewPitch = Mth.clamp(this.viewPitch + (float) dragY * 0.5F, -90.0F, 90.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inViewport(mouseX, mouseY)) {
            this.zoom = Mth.clamp(this.zoom + (float) scrollY * 0.15F, 0.4F, 3.0F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
