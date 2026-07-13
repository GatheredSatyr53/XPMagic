package com.gatheredsatyr53.xpmagic.gui;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.inventory.ConditionalInputSlot;
import com.gatheredsatyr53.xpmagic.inventory.MixingResultSlot;
import com.gatheredsatyr53.xpmagic.inventory.PowderMixerMenu;
import com.gatheredsatyr53.xpmagic.inventory.PowderMixerMenu.MixSummary;
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

public class PowderMixingScreen extends AbstractContainerScreen<PowderMixerMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(XPMagic.MODID, "textures/gui/powder_mixer.png");
    private static final int PANEL_WIDTH = 174;
    private static final int PANEL_HEIGHT = 164;
    // Warning overlay sprite in the texture's free space (to the right of the panel), drawn on the
    // panel background near the top-right corner when the mix is over the ceiling.
    private static final int WARN_U = 174;
    private static final int WARN_V = 0;
    private static final int WARN_SIZE = 16;
    private static final int WARN_X = 123;
    private static final int WARN_Y = 60;

    public PowderMixingScreen(final PowderMixerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        // When the mix exceeds the ceiling there is no result — show the warning overlay instead.
        if (this.menu.currentMix().exceeded()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, WARN_X, WARN_Y, WARN_U, WARN_V, WARN_SIZE, WARN_SIZE, 256, 256);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(final ItemStack stack) {
        List<Component> lines = super.getTooltipFromContainerItem(stack);
        Slot slot = this.hoveredSlot;
        if (slot == null || stack.isEmpty()) {
            return lines;
        }

        if (slot instanceof ConditionalInputSlot) {
            MixSummary mix = this.menu.currentMix();
            lines = new ArrayList<>(lines);
            Component hint = fractionHint(stack, mix);
            if (hint != null) {
                lines.add(hint);
            }
            if (mix.exceeded()) {
                lines.add(Component.translatable("tooltip.xpmagic.mixer.exceeded", mix.outputCapacity(), PowderMixerMenu.MAX_OUTPUT_CAPACITY)
                                   .withStyle(ChatFormatting.RED));
                lines.add(Component.translatable("tooltip.xpmagic.mixer.blocked").withStyle(ChatFormatting.RED));
            }
        } else if (slot instanceof MixingResultSlot) {
            MixSummary mix = this.menu.currentMix();
            if (mix.hasOutput()) {
                lines = new ArrayList<>(lines);
                appendResultBreakdown(lines, mix);
            }
        }

        return lines;
    }

    /** A one-line note on how the hovered fraction behaves in the current mix. */
    private static Component fractionHint(final ItemStack stack, final MixSummary mix) {
        if (stack.is(XPMagic.COARSE_POWDER.get())) {
            return mix.surplusCoarse() > 0
                ? Component.translatable("tooltip.xpmagic.mixer.coarse_surplus", mix.surplusCoarse()).withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tooltip.xpmagic.mixer.coarse_balanced").withStyle(ChatFormatting.DARK_GRAY);
        }
        if (stack.is(XPMagic.FINE_POWDER.get())) {
            return mix.surplusFine() > 0
                ? Component.translatable("tooltip.xpmagic.mixer.fine_surplus", mix.surplusFine()).withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tooltip.xpmagic.mixer.fine_balanced").withStyle(ChatFormatting.DARK_GRAY);
        }
        if (stack.is(XPMagic.MEDIUM_POWDER.get())) {
            return Component.translatable("tooltip.xpmagic.mixer.medium").withStyle(ChatFormatting.DARK_GRAY);
        }
        return null;
    }

    /** The calculation behind the result: compensated capacity, catalyst, and how it splits into units. */
    private static void appendResultBreakdown(final List<Component> lines, final MixSummary mix) {
        if (mix.pairs() > 0) {
            lines.add(Component.translatable("tooltip.xpmagic.mixer.pairs", mix.pairs()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (mix.surplusCoarse() > 0) {
            lines.add(Component.translatable("tooltip.xpmagic.mixer.surplus_coarse", mix.surplusCoarse()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (mix.surplusFine() > 0) {
            lines.add(Component.translatable("tooltip.xpmagic.mixer.surplus_fine", mix.surplusFine()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (mix.medium() > 0) {
            lines.add(Component.translatable("tooltip.xpmagic.mixer.filler", mix.medium()).withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.add(Component.translatable("tooltip.xpmagic.mixer.mix_capacity", quarters(mix.mixCapacityTimes4())).withStyle(ChatFormatting.GRAY));
        if (mix.catalystPercent() > 0) {
            lines.add(Component.translatable("tooltip.xpmagic.mixer.catalyst", mix.catalystPercent()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (mix.outputCount() > 1) {
            lines.add(Component.translatable("tooltip.xpmagic.mixer.units", mix.outputCount()).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Format a quarter-unit fixed-point capacity (x4) as a decimal, e.g. 31 -> "7.75". */
    private static String quarters(final int times4) {
        int whole = times4 / 4;
        return switch (times4 % 4) {
            case 1 -> whole + ".25";
            case 2 -> whole + ".5";
            case 3 -> whole + ".75";
            default -> Integer.toString(whole);
        };
    }
}
