package net.noiilive.jojowor.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public final class PosingTextures {
    public static final ResourceLocation MENU = tex("standposing_menu");
    public static final ResourceLocation VIEWPORT = tex("standposing_viewport");
    public static final ResourceLocation DROPDOWN_BAR = tex("standposing_dropdown_bar");
    public static final ResourceLocation DROPDOWN_ENTRY = tex("standposing_dropdown_entry");
    public static final ResourceLocation CONFIRM = tex("standposing_confirm");
    public static final ResourceLocation RESET_PART = tex("standposing_reset_part");
    public static final ResourceLocation RESET_POSE = tex("standposing_reset_pose");
    public static final ResourceLocation SHOWPLAYER_OFF = tex("standposing_showplayer_off");
    public static final ResourceLocation SHOWPLAYER_ON = tex("standposing_showplayer_on");
    public static final ResourceLocation TRACK_LONG = tex("standposing_slidertrack_long");
    public static final ResourceLocation TRACK_SMALL = tex("standposing_slidertrack_small");
    public static final ResourceLocation HANDLE = tex("standposing_slidertrack_handle");
    public static final ResourceLocation CHECKBOX = tex("standposing_checkbox");
    public static final ResourceLocation SAVEBAR = tex("standposing_savebar");
    public static final ResourceLocation UPLOAD = tex("standposing_upload");
    public static final ResourceLocation DOWNLOAD = tex("standposing_download");
    public static final ResourceLocation SAVESLOT = tex("standposing_saveslot");
    public static final ResourceLocation SAVESLOT_SAVE = tex("standposing_saveslot_save");
    public static final ResourceLocation SAVESLOT_LOAD = tex("standposing_saveslot_load");
    public static final ResourceLocation SAVESLOT_CLEAR = tex("standposing_saveslot_clear");
    public static final ResourceLocation BUTTON_SAVE = tex("standposing_button_save");
    public static final ResourceLocation BUTTON_CLEAR = tex("standposing_button_clear");

    private PosingTextures() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "textures/gui/" + name + ".png");
    }

    public static void blitSplit(GuiGraphics guiGraphics, ResourceLocation texture,
                                 int x, int y, int width, int height, boolean second) {
        guiGraphics.blit(texture, x, y, width, height, 0.0F, second ? height : 0.0F, width, height, width, height * 2);
    }

    public static void blitFull(GuiGraphics guiGraphics, ResourceLocation texture,
                                int x, int y, int width, int height) {
        guiGraphics.blit(texture, x, y, width, height, 0.0F, 0.0F, width, height, width, height);
    }

    public static String prettyName(String name) {
        String[] words = name.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(word.substring(0, 1).toUpperCase(java.util.Locale.ROOT)).append(word.substring(1));
        }
        return builder.toString();
    }

    public static void drawHeader(GuiGraphics guiGraphics, Font font, Component text,
                                  float centerX, float bottomY) {
        drawHeader(guiGraphics, font, text, centerX, bottomY, 0xFFFFFFFF);
    }

    public static void drawHeader(GuiGraphics guiGraphics, Font font, Component text,
                                  float centerX, float bottomY, int color) {
        var sequence = text.getVisualOrderText();
        float x = centerX - font.width(sequence) / 2.0F;
        float y = bottomY - 6.0F;
        guiGraphics.drawString(font, sequence, x + 1.0F, y + 1.0F, 0xFF121316, false);
        guiGraphics.drawString(font, sequence, x, y, color, false);
    }

    public static void drawFitText(GuiGraphics guiGraphics, Font font, Component text,
                                   int centerX, int centerY, int maxWidth, int color) {
        int width = font.width(text);
        float scale = width > maxWidth ? (float) maxWidth / width : 1.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawCenteredString(font, text, 0, -font.lineHeight / 2, color);
        guiGraphics.pose().popPose();
    }
}
