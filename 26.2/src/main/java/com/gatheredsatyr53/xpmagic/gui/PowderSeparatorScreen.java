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
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos,
            0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }
}
