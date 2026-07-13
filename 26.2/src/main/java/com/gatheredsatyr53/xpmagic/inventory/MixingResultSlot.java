package com.gatheredsatyr53.xpmagic.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Result of a mix: cannot be inserted into; taking it consumes one of each input. */
public final class MixingResultSlot extends Slot {

    private final PowderMixerMenu menu;

    public MixingResultSlot(Container container, int index, int x, int y, PowderMixerMenu menu) {
        super(container, index, x, y);
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        this.menu.onResultTaken();
        super.onTake(player, stack);
    }
}
