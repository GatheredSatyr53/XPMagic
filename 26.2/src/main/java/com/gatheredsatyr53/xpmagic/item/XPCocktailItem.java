package com.gatheredsatyr53.xpmagic.item;

import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.XPMagic;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Vanilla only queries TooltipProvider for its own hardcoded component list
 * (see ItemStack#addDetailsToTooltip), so the item has to surface StoredExp itself.
 */
public class XPCocktailItem extends Item {

    public XPCocktailItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        stack.addToTooltip(XPMagic.STORED_EXP.get(), context, display, builder, flag);
    }
}
