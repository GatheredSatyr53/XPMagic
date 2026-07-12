package com.gatheredsatyr53.xpmagic.gui;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.inventory.PowderSeparatorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class PowderSeparatorScreen extends AbstractContainerScreen<PowderSeparatorMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(XPMagic.MODID, "textures/gui/powder_separator.png");

    // Lit-mesh indicator: overlay sprite at u=176 drawn over the dim mesh in the panel.
    private static final int MESH_X = 66;
    private static final int MESH_Y = 41;
    private static final int MESH_U = 176;
    private static final int MESH_V = 0;
    private static final int MESH_W = 43;
    private static final int MESH_H = 16;

    public PowderSeparatorScreen(PowderSeparatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo,
            0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        int progress = this.menu.getSeparationProgressScaled(MESH_H);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo + MESH_X, yo + MESH_Y,
                      MESH_U, MESH_V, MESH_W, progress, 256, 256);
    }
}
