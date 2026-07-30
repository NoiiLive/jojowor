package net.noiilive.jojowor.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.jojowor.JoJoWoR;

public final class SkinTextures {
    public static final ResourceLocation MENU = tex("standskins_menu");
    public static final ResourceLocation VIEWPORT = tex("standskins_viewport");
    public static final ResourceLocation SLOTS = tex("standskins_slots");
    public static final ResourceLocation DROPDOWN = tex("standskins_dropdown");
    public static final ResourceLocation DROPDOWN_ENTRY = tex("standskins_dropdown_entry");
    public static final ResourceLocation BUTTON = tex("standskins_button");
    public static final ResourceLocation TRACK = tex("standskins_slidertrack");
    public static final ResourceLocation HANDLE = tex("standskins_sliderhandle");
    public static final ResourceLocation TAB_LEFT = tex("gui_tab_left");

    public static final int SLOT_SIZE = 16;
    public static final int SLOT_LOCKED = 0;
    public static final int SLOT_LOCKED_HOVERED = 1;
    public static final int SLOT_UNLOCKED = 2;
    public static final int SLOT_UNLOCKED_HOVERED = 3;
    public static final int SLOT_SELECTED = 4;

    private SkinTextures() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(JoJoWoR.MODID, "textures/gui/" + name + ".png");
    }

    public static void blitSlot(GuiGraphics guiGraphics, int x, int y, int section) {
        guiGraphics.blit(SLOTS, x, y, SLOT_SIZE, SLOT_SIZE,
                0.0F, section * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE * 5);
    }
}
