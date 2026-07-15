package com.gatheredsatyr53.xpmagic.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Output-only: players may take fractions but never insert into a result slot. */
public final class OutputSlot extends SlotItemHandler {
    public OutputSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
