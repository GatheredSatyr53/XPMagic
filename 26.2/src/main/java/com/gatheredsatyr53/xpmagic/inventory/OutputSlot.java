package com.gatheredsatyr53.xpmagic.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Output-only: players may take fractions but never insert into a result slot. */
public final class OutputSlot extends Slot {
    public OutputSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
