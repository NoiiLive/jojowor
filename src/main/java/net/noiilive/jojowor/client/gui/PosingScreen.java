package net.noiilive.jojowor.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noiilive.jojowor.client.model.StandAnimation;
import net.noiilive.jojowor.client.render.StandRenderers;
import net.noiilive.jojowor.network.SetStandPosePayload;
import net.noiilive.jojowor.stand.Stand;
import net.noiilive.jojowor.stand.StandOffset;
import net.noiilive.jojowor.stand.StandPose;
import net.noiilive.jojowor.stand.Stands;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PosingScreen extends Screen {
    private static final int IMAGE_WIDTH = 267;
    private static final int IMAGE_HEIGHT = 241;

    private static final int VIEWPORT_X = 21;
    private static final int VIEWPORT_Y = 36;
    private static final int VIEWPORT_WIDTH = 88;
    private static final int VIEWPORT_HEIGHT = 122;

    private static final int DROPDOWN_X = 121;
    private static final int DROPDOWN_Y = 28;
    private static final int DROPDOWN_WIDTH = 133;
    private static final int DROPDOWN_HEIGHT = 21;

    private static final int SLIDER_COL_X = 143;
    private static final int OFFSET_COL_X = 211;
    private static final int[] ROT_TRACK_YS = {56, 79, 102};
    private static final int[] POS_TRACK_YS = {125, 148, 171};
    private static final int TRACK_LONG_WIDTH = 108;
    private static final int TRACK_SMALL_WIDTH = 40;
    private static final int TRACK_HEIGHT = 12;

    private static final int BIG_BUTTON_SIZE = 31;
    private static final int RESET_PART_X = 13;
    private static final int RESET_POSE_X = 50;
    private static final int SHOWPLAYER_X = 87;
    private static final int BIG_BUTTON_Y = 169;

    private static final int CONFIRM_X = 13;
    private static final int CONFIRM_Y = 202;
    private static final int CONFIRM_WIDTH = 105;
    private static final int CONFIRM_HEIGHT = 26;
    private static final float CONFIRM_TEXT_CENTER_X = 64.5F;
    private static final float CONFIRM_TEXT_BOTTOM_Y = 217.0F;

    private static final int[] SLOT_XS = {123, 161, 199};
    private static final int SLOT_Y = 190;
    private static final int SLOT_WIDTH = 36;
    private static final int SLOT_HEIGHT = 40;
    private static final float[] SLOT_TEXT_CENTER_XS = {140.0F, 178.0F, 216.0F};
    private static final float SLOT_TEXT_BOTTOM_Y = 207.0F;

    private static final int MINI_BUTTON_Y = 217;
    private static final int MINI_BUTTON_SIZE = 9;
    private static final int MINI_SAVE_OFFSET = 3;
    private static final int MINI_LOAD_OFFSET = 13;
    private static final int MINI_CLEAR_OFFSET = 23;

    private static final int TRANSFER_X = 235;
    private static final int UPLOAD_Y = 190;
    private static final int DOWNLOAD_Y = 210;
    private static final int TRANSFER_SIZE = 20;

    private static final float TITLE_CENTER_X = 133.0F;
    private static final float TITLE_BOTTOM_Y = 16.0F;

    private static final int ROT_X = 0;
    private static final int ROT_Y = 1;
    private static final int ROT_Z = 2;
    private static final int OFF_X = 3;
    private static final int OFF_Y = 4;
    private static final int OFF_Z = 5;

    private static final double MAX_ROTATION_DEGREES = 180.0D;
    private static final double MAX_OFFSET = StandPose.MAX_OFFSET;

    private static final float GRID_EXTENT = 1.5F;
    private static final float GRID_STEP = 0.5F;
    private static final float GRID_LINE_HALF_WIDTH = 0.012F;
    private static final int GRID_COLOR = 0x50FFFFFF;
    private static final int GRID_AXIS_COLOR = 0x90FFFFFF;

    @Nullable
    private static List<int[]> viewportMask;

    private final List<TooltipZone> tooltipZones = new ArrayList<>();
    private final Map<String, float[]> working = new LinkedHashMap<>();
    private final float[] offsetWorking = new float[3];
    private boolean showPlayer;
    private int selectedSlot = -1;
    private List<String> partNames = List.of();
    private String selectedPart = "";

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
    private PoseDropdown dropdown;

    public PosingScreen() {
        super(Component.translatable("jojowor.screen.posing.title"));
    }

    @Override
    protected void init() {
        if (this.minecraft == null || this.minecraft.player == null) {
            onClose();
            return;
        }
        Stand stand = Stands.get(this.minecraft.player);
        this.baked = stand == null ? null : StandRenderers.get(stand.getId());
        if (this.baked == null) {
            onClose();
            return;
        }

        this.partNames = new ArrayList<>(this.baked.model().posableParts().keySet());
        if (this.selectedPart.isEmpty() || !this.partNames.contains(this.selectedPart)) {
            this.selectedPart = this.partNames.get(0);
        }

        if (this.working.isEmpty()) {
            StandPose current = Stands.getData(this.minecraft.player).pose();
            for (String name : this.partNames) {
                float[] values = new float[6];
                StandPose.Part part = current.parts().get(name);
                if (part != null) {
                    values[ROT_X] = (float) Math.toDegrees(part.rx());
                    values[ROT_Y] = (float) Math.toDegrees(part.ry());
                    values[ROT_Z] = (float) Math.toDegrees(part.rz());
                    values[OFF_X] = part.ox();
                    values[OFF_Y] = part.oy();
                    values[OFF_Z] = part.oz();
                }
                this.working.put(name, values);
            }
            StandOffset offset = Stands.getData(this.minecraft.player).offset();
            this.offsetWorking[0] = offset.forward();
            this.offsetWorking[1] = offset.right();
            this.offsetWorking[2] = offset.up();
        }

        this.tooltipZones.clear();
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        this.viewportLeft = this.leftPos + VIEWPORT_X;
        this.viewportTop = this.topPos + VIEWPORT_Y;
        this.viewportRight = this.viewportLeft + VIEWPORT_WIDTH;
        this.viewportBottom = this.viewportTop + VIEWPORT_HEIGHT;

        addRenderableWidget(new TabButton(SkinTextures.TAB_LEFT,
                this.leftPos - 32 + 4, this.topPos + 10, 32, 26,
                Component.translatable("jojowor.screen.skins.title"), () -> false,
                button -> this.minecraft.setScreen(new StandSkinsScreen())));

        addRenderableWidget(new TabButton(SkinTextures.TAB_LEFT,
                this.leftPos - 32 + 4, this.topPos + 10 + 26 + 2, 32, 26,
                Component.translatable("jojowor.screen.posing.title"), () -> true,
                button -> {}));

        float[] values = this.working.get(this.selectedPart);
        String[] sliderKeys = {"rot_x", "rot_y", "rot_z", "off_x", "off_y", "off_z"};
        for (int i = 0; i < 6; i++) {
            final int index = i;
            boolean rotation = i < 3;
            addRenderableWidget(new PoseSlider(
                    rotation ? PosingTextures.TRACK_LONG : PosingTextures.TRACK_SMALL,
                    this.leftPos + SLIDER_COL_X,
                    this.topPos + (rotation ? ROT_TRACK_YS[i] : POS_TRACK_YS[i - 3]),
                    rotation ? TRACK_LONG_WIDTH : TRACK_SMALL_WIDTH, TRACK_HEIGHT,
                    null,
                    rotation ? -MAX_ROTATION_DEGREES : -MAX_OFFSET,
                    rotation ? MAX_ROTATION_DEGREES : MAX_OFFSET,
                    values[index],
                    rotation ? "%.0f" : "%.1f",
                    value -> this.working.get(this.selectedPart)[index] = (float) value));
        }

        addRenderableWidget(new PoseSlider(PosingTextures.TRACK_SMALL,
                this.leftPos + OFFSET_COL_X, this.topPos + POS_TRACK_YS[0], TRACK_SMALL_WIDTH, TRACK_HEIGHT,
                null, -StandOffset.MAX_HORIZONTAL, StandOffset.MAX_HORIZONTAL,
                this.offsetWorking[1], "%.1f",
                value -> this.offsetWorking[1] = (float) value));

        addRenderableWidget(new PoseSlider(PosingTextures.TRACK_SMALL,
                this.leftPos + OFFSET_COL_X, this.topPos + POS_TRACK_YS[1], TRACK_SMALL_WIDTH, TRACK_HEIGHT,
                null, StandOffset.MIN_UP, StandOffset.MAX_UP,
                this.offsetWorking[2], "%.1f",
                value -> this.offsetWorking[2] = (float) value));

        addRenderableWidget(new PoseSlider(PosingTextures.TRACK_SMALL,
                this.leftPos + OFFSET_COL_X, this.topPos + POS_TRACK_YS[2], TRACK_SMALL_WIDTH, TRACK_HEIGHT,
                null, -StandOffset.MAX_HORIZONTAL, StandOffset.MAX_HORIZONTAL,
                this.offsetWorking[0], "%.1f",
                value -> this.offsetWorking[0] = (float) value));

        addRenderableWidget(tooltip(new TexturedButton(PosingTextures.RESET_PART,
                this.leftPos + RESET_PART_X, this.topPos + BIG_BUTTON_Y, BIG_BUTTON_SIZE, BIG_BUTTON_SIZE,
                Component.translatable("jojowor.screen.posing.reset"), null, button -> {
            this.working.put(this.selectedPart, new float[6]);
            rebuildWidgets();
        }), Component.translatable("jojowor.screen.posing.reset")));

        addRenderableWidget(tooltip(new TexturedButton(PosingTextures.RESET_POSE,
                this.leftPos + RESET_POSE_X, this.topPos + BIG_BUTTON_Y, BIG_BUTTON_SIZE, BIG_BUTTON_SIZE,
                Component.translatable("jojowor.screen.posing.reset_all"), null, button -> {
            this.working.replaceAll((name, old) -> new float[6]);
            this.offsetWorking[0] = StandOffset.DEFAULT.forward();
            this.offsetWorking[1] = StandOffset.DEFAULT.right();
            this.offsetWorking[2] = StandOffset.DEFAULT.up();
            rebuildWidgets();
        }), Component.translatable("jojowor.screen.posing.reset_all")));

        addRenderableWidget(tooltip(new ToggleTextureButton(
                PosingTextures.SHOWPLAYER_OFF, PosingTextures.SHOWPLAYER_ON,
                this.leftPos + SHOWPLAYER_X, this.topPos + BIG_BUTTON_Y, BIG_BUTTON_SIZE, BIG_BUTTON_SIZE,
                Component.translatable("jojowor.screen.posing.show_player"),
                () -> this.showPlayer, button -> this.showPlayer = !this.showPlayer),
                Component.translatable("jojowor.screen.posing.show_player")));

        addRenderableWidget(new TexturedButton(PosingTextures.CONFIRM,
                this.leftPos + CONFIRM_X, this.topPos + CONFIRM_Y, CONFIRM_WIDTH, CONFIRM_HEIGHT,
                Component.translatable("jojowor.screen.posing.confirm"), null, button -> {
            sendPose();
            onClose();
        }));

        for (int i = 0; i < net.noiilive.jojowor.client.pose.PoseSlots.SLOT_COUNT; i++) {
            final int slot = i;
            int slotX = this.leftPos + SLOT_XS[i];
            addRenderableWidget(tooltip(new StateButton(PosingTextures.SAVESLOT,
                    slotX, this.topPos + SLOT_Y, SLOT_WIDTH, SLOT_HEIGHT,
                    Component.translatable("jojowor.screen.posing.slot", i + 1),
                    () -> this.selectedSlot == slot, button -> this.selectedSlot = slot) {
                @Override
                protected boolean clicked(double mouseX, double mouseY) {
                    return super.clicked(mouseX, mouseY)
                            && mouseY < getY() + MINI_BUTTON_Y - SLOT_Y;
                }
            }, Component.translatable("jojowor.screen.posing.slot", i + 1), MINI_BUTTON_Y - SLOT_Y));

            addRenderableWidget(tooltip(new TexturedButton(PosingTextures.SAVESLOT_SAVE,
                    slotX + MINI_SAVE_OFFSET, this.topPos + MINI_BUTTON_Y, MINI_BUTTON_SIZE, MINI_BUTTON_SIZE,
                    Component.translatable("jojowor.screen.posing.slot_save", i + 1), null, button -> {
                this.selectedSlot = slot;
                net.noiilive.jojowor.client.pose.PoseSlots.save(slot, buildPose());
            }), Component.translatable("jojowor.screen.posing.slot_save", i + 1)));

            addRenderableWidget(tooltip(new TexturedButton(PosingTextures.SAVESLOT_LOAD,
                    slotX + MINI_LOAD_OFFSET, this.topPos + MINI_BUTTON_Y, MINI_BUTTON_SIZE, MINI_BUTTON_SIZE,
                    Component.translatable("jojowor.screen.posing.slot_load", i + 1), null, button -> {
                if (!net.noiilive.jojowor.client.pose.PoseSlots.isEmpty(slot)) {
                    this.selectedSlot = slot;
                    applyImportedPose(net.noiilive.jojowor.client.pose.PoseSlots.get(slot));
                    sendPose();
                }
            }), Component.translatable("jojowor.screen.posing.slot_load", i + 1)));

            addRenderableWidget(tooltip(new TexturedButton(PosingTextures.SAVESLOT_CLEAR,
                    slotX + MINI_CLEAR_OFFSET, this.topPos + MINI_BUTTON_Y, MINI_BUTTON_SIZE, MINI_BUTTON_SIZE,
                    Component.translatable("jojowor.screen.posing.slot_clear", i + 1), null, button ->
                    net.noiilive.jojowor.client.pose.PoseSlots.clear(slot)),
                    Component.translatable("jojowor.screen.posing.slot_clear", i + 1)));
        }

        addRenderableWidget(tooltip(new TexturedButton(PosingTextures.UPLOAD,
                this.leftPos + TRANSFER_X, this.topPos + UPLOAD_Y, TRANSFER_SIZE, TRANSFER_SIZE,
                Component.translatable("jojowor.screen.posing.upload"), null, button ->
                net.noiilive.jojowor.client.pose.PoseShare.openUpload(pose -> {
                    if (this.minecraft != null && this.minecraft.screen == this) {
                        applyImportedPose(pose);
                        sendPose();
                    }
                })), Component.translatable("jojowor.screen.posing.upload")));

        addRenderableWidget(tooltip(new TexturedButton(PosingTextures.DOWNLOAD,
                this.leftPos + TRANSFER_X, this.topPos + DOWNLOAD_Y, TRANSFER_SIZE, TRANSFER_SIZE,
                Component.translatable("jojowor.screen.posing.download"), null, button ->
                net.noiilive.jojowor.client.pose.PoseShare.openDownload(buildPose())),
                Component.translatable("jojowor.screen.posing.download")));

        this.dropdown = addRenderableWidget(new PoseDropdown(
                this.leftPos + DROPDOWN_X, this.topPos + DROPDOWN_Y, DROPDOWN_WIDTH, DROPDOWN_HEIGHT,
                this.partNames, this.selectedPart,
                name -> Component.literal(prettyName(name)),
                name -> {
                    this.selectedPart = name;
                    rebuildWidgets();
                }));
    }

    private void sendPose() {
        PacketDistributor.sendToServer(new SetStandPosePayload(buildPose(), new StandOffset(
                this.offsetWorking[0], this.offsetWorking[1], this.offsetWorking[2]).clamped()));
    }

    private void applyImportedPose(StandPose pose) {
        for (Map.Entry<String, float[]> entry : this.working.entrySet()) {
            float[] values = new float[6];
            StandPose.Part part = pose.parts().get(entry.getKey());
            if (part != null) {
                values[ROT_X] = (float) Math.toDegrees(part.rx());
                values[ROT_Y] = (float) Math.toDegrees(part.ry());
                values[ROT_Z] = (float) Math.toDegrees(part.rz());
                values[OFF_X] = part.ox();
                values[OFF_Y] = part.oy();
                values[OFF_Z] = part.oz();
            }
            entry.setValue(values);
        }
        rebuildWidgets();
    }

    private StandPose buildPose() {
        Map<String, StandPose.Part> parts = new LinkedHashMap<>();
        this.working.forEach((name, values) -> {
            StandPose.Part part = new StandPose.Part(
                    (float) Math.toRadians(values[ROT_X]),
                    (float) Math.toRadians(values[ROT_Y]),
                    (float) Math.toRadians(values[ROT_Z]),
                    values[OFF_X], values[OFF_Y], values[OFF_Z]);
            if (!part.isZero()) {
                parts.put(name, part);
            }
        });
        return parts.isEmpty() ? StandPose.EMPTY : new StandPose(Map.copyOf(parts));
    }

    private static String prettyName(String name) {
        return PosingTextures.prettyName(name);
    }

    private record TooltipZone(int x, int y, int width, int height, Component text) {}

    private <T extends net.minecraft.client.gui.components.AbstractWidget> T tooltip(T widget, Component text) {
        this.tooltipZones.add(new TooltipZone(
                widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), text));
        return widget;
    }

    private <T extends net.minecraft.client.gui.components.AbstractWidget> T tooltip(T widget, Component text,
                                                                                     int zoneHeight) {
        this.tooltipZones.add(new TooltipZone(
                widget.getX(), widget.getY(), widget.getWidth(), zoneHeight, text));
        return widget;
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.dropdown != null && this.dropdown.isOpen()) {
            return;
        }
        for (TooltipZone zone : this.tooltipZones) {
            if (mouseX >= zone.x() && mouseX < zone.x() + zone.width()
                    && mouseY >= zone.y() && mouseY < zone.y() + zone.height()) {
                guiGraphics.renderTooltip(this.font, zone.text(), mouseX, mouseY);
                return;
            }
        }
    }

    private static List<int[]> viewportMask() {
        if (viewportMask == null) {
            List<int[]> runs = new ArrayList<>();
            try (var resource = Minecraft.getInstance().getResourceManager().open(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            net.noiilive.jojowor.JoJoWoR.MODID, "textures/gui/standposing_viewport.png"));
                 NativeImage image = NativeImage.read(resource)) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int runStart = -1;
                    for (int x = 0; x <= image.getWidth(); x++) {
                        boolean transparent = x < image.getWidth()
                                && (image.getPixelRGBA(x, y) >>> 24) == 0;
                        if (transparent && runStart < 0) {
                            runStart = x;
                        } else if (!transparent && runStart >= 0) {
                            runs.add(new int[]{runStart, y, x - runStart});
                            runStart = -1;
                        }
                    }
                }
            } catch (Exception exception) {
                net.noiilive.jojowor.JoJoWoR.LOGGER.warn("Failed to read viewport mask", exception);
            }
            viewportMask = runs;
        }
        return viewportMask;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        PosingTextures.blitFull(guiGraphics, PosingTextures.MENU,
                this.leftPos, this.topPos, IMAGE_WIDTH, IMAGE_HEIGHT);
        PosingTextures.blitFull(guiGraphics, PosingTextures.VIEWPORT,
                this.viewportLeft, this.viewportTop, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderViewport(guiGraphics, partialTick);
        renderViewportMask(guiGraphics);
        renderTexts(guiGraphics);
        renderTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderTexts(GuiGraphics guiGraphics) {
        PosingTextures.drawHeader(guiGraphics, this.font, getTitle(),
                this.leftPos + TITLE_CENTER_X, this.topPos + TITLE_BOTTOM_Y);
        PosingTextures.drawHeader(guiGraphics, this.font,
                Component.translatable("jojowor.screen.posing.confirm"),
                this.leftPos + CONFIRM_TEXT_CENTER_X, this.topPos + CONFIRM_TEXT_BOTTOM_Y);
        for (int i = 0; i < net.noiilive.jojowor.client.pose.PoseSlots.SLOT_COUNT; i++) {
            boolean occupied = !net.noiilive.jojowor.client.pose.PoseSlots.isEmpty(i);
            PosingTextures.drawHeader(guiGraphics, this.font, Component.literal(String.valueOf(i + 1)),
                    this.leftPos + SLOT_TEXT_CENTER_XS[i], this.topPos + SLOT_TEXT_BOTTOM_Y,
                    occupied ? 0xFFFFFFFF : 0xFFA0A0A0);
        }
    }

    private void renderViewportMask(GuiGraphics guiGraphics) {
        List<int[]> runs = viewportMask();
        if (runs.isEmpty()) {
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 600.0F);
        for (int[] run : runs) {
            guiGraphics.blit(PosingTextures.MENU,
                    this.viewportLeft + run[0], this.viewportTop + run[1],
                    VIEWPORT_X + run[0], VIEWPORT_Y + run[1],
                    run[2], 1, IMAGE_WIDTH, IMAGE_HEIGHT);
        }
        guiGraphics.pose().popPose();
    }

    private void renderViewport(GuiGraphics guiGraphics, float partialTick) {
        if (this.baked == null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        double ageTicks = this.minecraft.level.getGameTime() + partialTick;
        this.baked.model().setupAnim(new StandAnimation(
                ageTicks, 0.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, -1.0F, -1.0F, -1.0F, 0.0F, 0.0F, -1.0F, -1.0F,
                buildPose()));

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

        renderGrid(guiGraphics, poseStack);
        renderNorthMarker(guiGraphics, poseStack);
        if (this.showPlayer) {
            renderPlayer(guiGraphics, poseStack);
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        guiGraphics.flush();
        Lighting.setupForEntityInInventory();
        Stand stand = this.minecraft == null || this.minecraft.player == null
                ? null : Stands.get(this.minecraft.player);
        java.util.List<StandRenderers.SkinLayer> layers = stand == null
                ? java.util.List.of(new StandRenderers.SkinLayer(this.baked.texture(), 0xFFFFFF))
                : StandRenderers.skinLayers(stand, Stands.getData(this.minecraft.player), this.baked);
        for (StandRenderers.SkinLayer layer : layers) {
            VertexConsumer buffer = guiGraphics.bufferSource()
                    .getBuffer(this.baked.model().renderType(layer.texture()));
            this.baked.model().renderToBuffer(poseStack, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY,
                    0xFF000000 | (layer.color() & 0xFFFFFF));
        }
        guiGraphics.flush();
        Lighting.setupFor3DItems();

        poseStack.popPose();
        guiGraphics.disableScissor();
    }

    private void renderGrid(GuiGraphics guiGraphics, PoseStack poseStack) {
        VertexConsumer buffer = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        for (float line = -GRID_EXTENT; line <= GRID_EXTENT + 1.0E-4F; line += GRID_STEP) {
            int color = Math.abs(line) < 1.0E-4F ? GRID_AXIS_COLOR : GRID_COLOR;
            gridQuad(buffer, poseStack, -GRID_EXTENT, GRID_EXTENT, line - GRID_LINE_HALF_WIDTH,
                    line + GRID_LINE_HALF_WIDTH, false, color);
            gridQuad(buffer, poseStack, -GRID_EXTENT, GRID_EXTENT, line - GRID_LINE_HALF_WIDTH,
                    line + GRID_LINE_HALF_WIDTH, true, color);
        }
    }

    private static void gridQuad(VertexConsumer buffer, PoseStack poseStack,
                                 float from, float to, float lineMin, float lineMax,
                                 boolean alongZ, int color) {
        PoseStack.Pose pose = poseStack.last();
        float y = -0.004F;
        if (alongZ) {
            buffer.addVertex(pose, lineMin, y, from).setColor(color);
            buffer.addVertex(pose, lineMin, y, to).setColor(color);
            buffer.addVertex(pose, lineMax, y, to).setColor(color);
            buffer.addVertex(pose, lineMax, y, from).setColor(color);
            buffer.addVertex(pose, lineMax, y, from).setColor(color);
            buffer.addVertex(pose, lineMax, y, to).setColor(color);
            buffer.addVertex(pose, lineMin, y, to).setColor(color);
            buffer.addVertex(pose, lineMin, y, from).setColor(color);
        } else {
            buffer.addVertex(pose, from, y, lineMin).setColor(color);
            buffer.addVertex(pose, to, y, lineMin).setColor(color);
            buffer.addVertex(pose, to, y, lineMax).setColor(color);
            buffer.addVertex(pose, from, y, lineMax).setColor(color);
            buffer.addVertex(pose, from, y, lineMax).setColor(color);
            buffer.addVertex(pose, to, y, lineMax).setColor(color);
            buffer.addVertex(pose, to, y, lineMin).setColor(color);
            buffer.addVertex(pose, from, y, lineMin).setColor(color);
        }
    }

    private void renderNorthMarker(GuiGraphics guiGraphics, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.01F, -(GRID_EXTENT + 0.35F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        float textScale = 0.05F;
        poseStack.scale(-textScale, textScale, textScale);
        String text = "N";
        this.font.drawInBatch(text, -this.font.width(text) / 2.0F, -this.font.lineHeight / 2.0F,
                0xFFFFFFFF, false, poseStack.last().pose(), guiGraphics.bufferSource(),
                net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        poseStack.popPose();
    }

    private void renderPlayer(GuiGraphics guiGraphics, PoseStack poseStack) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        net.minecraft.client.player.LocalPlayer player = this.minecraft.player;
        float bodyYaw = player.yBodyRot;
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float headYawO = player.yHeadRotO;
        float headYaw = player.yHeadRot;
        player.yBodyRot = 180.0F;
        player.setYRot(180.0F);
        player.setXRot(0.0F);
        player.yHeadRot = 180.0F;
        player.yHeadRotO = 180.0F;

        double px = -this.offsetWorking[1];
        double py = -(this.offsetWorking[2] + 0.5D);
        double pz = this.offsetWorking[0];

        net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher =
                this.minecraft.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        dispatcher.render(player, px, py, pz, 0.0F, 1.0F, poseStack, guiGraphics.bufferSource(), 0xF000F0);
        guiGraphics.flush();
        dispatcher.setRenderShadow(true);

        player.yBodyRot = bodyYaw;
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.yHeadRotO = headYawO;
        player.yHeadRot = headYaw;
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
        if (this.dropdown != null && this.dropdown.handleGlobalScroll(mouseX, mouseY, scrollY)) {
            return true;
        }
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
