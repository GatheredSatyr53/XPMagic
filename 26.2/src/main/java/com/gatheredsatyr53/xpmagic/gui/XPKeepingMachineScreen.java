package com.gatheredsatyr53.xpmagic.gui;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.inventory.ConditionalInputSlot;
import com.gatheredsatyr53.xpmagic.inventory.PowderMixerMenu;
import com.gatheredsatyr53.xpmagic.inventory.XPKeepingMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class XPKeepingMachineScreen extends AbstractContainerScreen<XPKeepingMachineMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(XPMagic.MODID, "textures/gui/xp_keeping_machine.png");

    // Texture layout from the 1.12 XPKMGuiBounds: GUI at (0,0), animation sector at u=176
    private static final int ANIMATION_U = 176;
    private static final int BURN_X = 31;
    private static final int BURN_Y = 39;
    private static final int BURN_WIDTH = 14;
    private static final int BURN_HEIGHT = 13;
    private static final int PROGRESS_X = 77;
    private static final int PROGRESS_Y = 41;
    private static final int PROGRESS_V = 14;
    private static final int PROGRESS_WIDTH = 29;
    private static final int PROGRESS_HEIGHT = 12;

    public XPKeepingMachineScreen(XPKeepingMachineMenu menu, Inventory inventory, Component title) {
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
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.isLit()) {
            int lit = this.menu.getLitScaled(BURN_HEIGHT);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                xo + BURN_X, yo + BURN_Y + 12 - lit, ANIMATION_U, 12 - lit, BURN_WIDTH, lit + 1, 256, 256);
        }

        int progress = this.menu.getCookProgressScaled(PROGRESS_WIDTH);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
            xo + PROGRESS_X, yo + PROGRESS_Y, ANIMATION_U, PROGRESS_V, progress, PROGRESS_HEIGHT, 256, 256);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        List<Component> lines = super.getTooltipFromContainerItem(itemStack);
        Slot slot = this.hoveredSlot;
        if (slot == null || itemStack.isEmpty()) {
            return lines;
        }

        if (itemStack.has(XPMagic.XP_CAPACITY.get())) {
            Integer reserve = this.menu.getXPReserve();
            if (reserve != null && reserve < 0) {
                lines.add(Component.translatable("tooltip.xpmagic.keeper.insufficient_xp", -reserve)
                                   .withStyle(ChatFormatting.RED));
            }
        }

        return lines;
    }
}
