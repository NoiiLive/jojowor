package net.noiilive.jojowor.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

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
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 188;

    private static final int VIEWPORT_X = 7;
    private static final int VIEWPORT_Y = 32;
    private static final int VIEWPORT_WIDTH = 85;
    private static final int VIEWPORT_HEIGHT = 92;

    private static final int DROPDOWN_X = 96;
    private static final int DROPDOWN_Y = 32;
    private static final int DROPDOWN_WIDTH = 73;
    private static final int DROPDOWN_HEIGHT = 15;

    private static final int TRACK_X = 97;
    private static final int[] TRACK_YS = {51, 70, 89, 108, 127, 146};
    private static final int TRACK_LONG_WIDTH = 71;
    private static final int TRACK_HEIGHT = 13;

    private static final int BUTTON_SMALL_WIDTH = 41;
    private static final int BUTTON_HEIGHT = 15;
    private static final int RESET_PART_X = 7;
    private static final int RESET_POSE_X = 51;
    private static final int RESET_Y = 127;
    private static final int SAVE_X = 7;
    private static final int SAVE_Y = 145;
    private static final int BUTTON_LARGE_WIDTH = 85;

    private static final int CHECKBOX_X = 9;
    private static final int CHECKBOX_Y = 169;
    private static final int CHECKBOX_SIZE = 15;

    private static final int TRACK_SMALL_WIDTH = 29;
    private static final int OFFSET_Y = 170;
    private static final int OFFSET_X_SLIDER_X = 43;
    private static final int OFFSET_Y_SLIDER_X = 90;
    private static final int OFFSET_Z_SLIDER_X = 137;

    private static final int SAVEBAR_WIDTH = 34;
    private static final int SAVEBAR_HEIGHT = 126;
    private static final int UPLOAD_X = 0;
    private static final int DOWNLOAD_X = 14;
    private static final int TRANSFER_Y = 2;
    private static final int TRANSFER_WIDTH = 13;
    private static final int TRANSFER_HEIGHT = 14;
    private static final int SLOT_X = 5;
    private static final int SLOT_FIRST_Y = 24;
    private static final int SLOT_PITCH = 20;
    private static final int SLOT_SIZE = 17;
    private static final int TAB_X = 27;
    private static final int TAB_SAVE_FIRST_Y = 25;
    private static final int TAB_CLEAR_FIRST_Y = 33;
    private static final int TAB_WIDTH = 7;
    private static final int TAB_HEIGHT = 7;

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

    private final Map<String, float[]> working = new LinkedHashMap<>();
    private final float[] offsetWorking = new float[3];
    private boolean showPlayer;
    private int selectedSlot = -1;
    private int savebarX;
    private int savebarY;
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
    private PartDropdown dropdown;

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

        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        this.viewportLeft = this.leftPos + VIEWPORT_X;
        this.viewportTop = this.topPos + VIEWPORT_Y;
        this.viewportRight = this.viewportLeft + VIEWPORT_WIDTH;
        this.viewportBottom = this.viewportTop + VIEWPORT_HEIGHT;

        float[] values = this.working.get(this.selectedPart);
        String[] sliderKeys = {"rot_x", "rot_y", "rot_z", "off_x", "off_y", "off_z"};
        for (int i = 0; i < 6; i++) {
            final int index = i;
            boolean rotation = i < 3;
            addRenderableWidget(new PoseSlider(PosingTextures.TRACK_LONG,
                    this.leftPos + TRACK_X, this.topPos + TRACK_YS[i], TRACK_LONG_WIDTH, TRACK_HEIGHT,
                    Component.translatable("jojowor.screen.posing." + sliderKeys[i]),
                    rotation ? -MAX_ROTATION_DEGREES : -MAX_OFFSET,
                    rotation ? MAX_ROTATION_DEGREES : MAX_OFFSET,
                    values[index],
                    rotation ? "%.0f" : "%.1f",
                    value -> this.working.get(this.selectedPart)[index] = (float) value));
        }

        addRenderableWidget(new TexturedButton(PosingTextures.BUTTON_SMALL,
                this.leftPos + RESET_PART_X, this.topPos + RESET_Y, BUTTON_SMALL_WIDTH, BUTTON_HEIGHT,
                Component.translatable("jojowor.screen.posing.reset"),
                Component.translatable("jojowor.screen.posing.reset_short"), button -> {
            this.working.put(this.selectedPart, new float[6]);
            rebuildWidgets();
        }));

        addRenderableWidget(new TexturedButton(PosingTextures.BUTTON_SMALL,
                this.leftPos + RESET_POSE_X, this.topPos + RESET_Y, BUTTON_SMALL_WIDTH, BUTTON_HEIGHT,
                Component.translatable("jojowor.screen.posing.reset_all"),
                Component.translatable("jojowor.screen.posing.reset_all_short"), button -> {
            this.working.replaceAll((name, old) -> new float[6]);
            this.offsetWorking[0] = StandOffset.DEFAULT.forward();
            this.offsetWorking[1] = StandOffset.DEFAULT.right();
            this.offsetWorking[2] = StandOffset.DEFAULT.up();
            rebuildWidgets();
        }));

        addRenderableWidget(new TexturedButton(PosingTextures.BUTTON_LARGE,
                this.leftPos + SAVE_X, this.topPos + SAVE_Y, BUTTON_LARGE_WIDTH, BUTTON_HEIGHT,
                Component.translatable("jojowor.screen.posing.save"),
                Component.translatable("jojowor.screen.posing.save"), button -> {
            sendPose();
            if (this.selectedSlot >= 0) {
                net.noiilive.jojowor.client.pose.PoseSlots.save(this.selectedSlot, buildPose());
            }
            onClose();
        }));

        addRenderableWidget(new PoseCheckbox(
                this.leftPos + CHECKBOX_X, this.topPos + CHECKBOX_Y, CHECKBOX_SIZE, CHECKBOX_SIZE,
                Component.translatable("jojowor.screen.posing.show_player"),
                this.showPlayer, value -> this.showPlayer = value));

        addRenderableWidget(new PoseSlider(PosingTextures.TRACK_SMALL,
                this.leftPos + OFFSET_X_SLIDER_X, this.topPos + OFFSET_Y, TRACK_SMALL_WIDTH, TRACK_HEIGHT,
                null, -StandOffset.MAX_HORIZONTAL, StandOffset.MAX_HORIZONTAL,
                this.offsetWorking[1], "%.1f",
                value -> this.offsetWorking[1] = (float) value));

        addRenderableWidget(new PoseSlider(PosingTextures.TRACK_SMALL,
                this.leftPos + OFFSET_Y_SLIDER_X, this.topPos + OFFSET_Y, TRACK_SMALL_WIDTH, TRACK_HEIGHT,
                null, StandOffset.MIN_UP, StandOffset.MAX_UP,
                this.offsetWorking[2], "%.1f",
                value -> this.offsetWorking[2] = (float) value));

        addRenderableWidget(new PoseSlider(PosingTextures.TRACK_SMALL,
                this.leftPos + OFFSET_Z_SLIDER_X, this.topPos + OFFSET_Y, TRACK_SMALL_WIDTH, TRACK_HEIGHT,
                null, -StandOffset.MAX_HORIZONTAL, StandOffset.MAX_HORIZONTAL,
                this.offsetWorking[0], "%.1f",
                value -> this.offsetWorking[0] = (float) value));

        this.savebarX = this.leftPos + IMAGE_WIDTH + 2;
        this.savebarY = this.topPos + (IMAGE_HEIGHT - SAVEBAR_HEIGHT) / 2;

        addRenderableWidget(new TexturedButton(PosingTextures.UPLOAD,
                this.savebarX + UPLOAD_X, this.savebarY + TRANSFER_Y, TRANSFER_WIDTH, TRANSFER_HEIGHT,
                Component.translatable("jojowor.screen.posing.upload"), null, button ->
                net.noiilive.jojowor.client.pose.PoseShare.openUpload(pose -> {
                    if (this.minecraft != null && this.minecraft.screen == this) {
                        applyImportedPose(pose);
                        sendPose();
                    }
                })));

        addRenderableWidget(new TexturedButton(PosingTextures.DOWNLOAD,
                this.savebarX + DOWNLOAD_X, this.savebarY + TRANSFER_Y, TRANSFER_WIDTH, TRANSFER_HEIGHT,
                Component.translatable("jojowor.screen.posing.download"), null, button ->
                net.noiilive.jojowor.client.pose.PoseShare.openDownload(buildPose())));

        for (int i = 0; i < net.noiilive.jojowor.client.pose.PoseSlots.SLOT_COUNT; i++) {
            final int slot = i;
            addRenderableWidget(new PoseSlotButton(
                    this.savebarX + SLOT_X, this.savebarY + SLOT_FIRST_Y + SLOT_PITCH * i,
                    SLOT_SIZE, SLOT_SIZE, i + 1,
                    () -> this.selectedSlot == slot,
                    () -> !net.noiilive.jojowor.client.pose.PoseSlots.isEmpty(slot),
                    Component.translatable("jojowor.screen.posing.slot", i + 1), button ->
                    this.selectedSlot = slot));

            addRenderableWidget(new TexturedButton(PosingTextures.BUTTON_SAVE,
                    this.savebarX + TAB_X, this.savebarY + TAB_SAVE_FIRST_Y + SLOT_PITCH * i,
                    TAB_WIDTH, TAB_HEIGHT,
                    Component.translatable("jojowor.screen.posing.slot_load", i + 1), null, button -> {
                if (!net.noiilive.jojowor.client.pose.PoseSlots.isEmpty(slot)) {
                    this.selectedSlot = slot;
                    applyImportedPose(net.noiilive.jojowor.client.pose.PoseSlots.get(slot));
                    sendPose();
                }
            }));

            addRenderableWidget(new TexturedButton(PosingTextures.BUTTON_CLEAR,
                    this.savebarX + TAB_X, this.savebarY + TAB_CLEAR_FIRST_Y + SLOT_PITCH * i,
                    TAB_WIDTH, TAB_HEIGHT,
                    Component.translatable("jojowor.screen.posing.slot_clear", i + 1), null, button ->
                    net.noiilive.jojowor.client.pose.PoseSlots.clear(slot)));
        }

        addRenderableWidget(new TabButton(SkinTextures.TAB_LEFT,
                this.leftPos - 32 + 4, this.topPos + 10, 32, 26,
                Component.translatable("jojowor.screen.skins.title"), () -> false,
                button -> this.minecraft.setScreen(new StandSkinsScreen())));

        addRenderableWidget(new TabButton(SkinTextures.TAB_LEFT,
                this.leftPos - 32 + 4, this.topPos + 10 + 26 + 2, 32, 26,
                Component.translatable("jojowor.screen.posing.title"), () -> true,
                button -> {}));

        this.dropdown = addRenderableWidget(new PartDropdown(
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

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        PosingTextures.blitFull(guiGraphics, PosingTextures.MENU,
                this.leftPos, this.topPos, IMAGE_WIDTH, IMAGE_HEIGHT);
        PosingTextures.blitFull(guiGraphics, PosingTextures.VIEWPORT,
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
