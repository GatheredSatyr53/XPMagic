package com.gatheredsatyr53.xpmagic.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;

public class MachineShapelessCraftingContainer implements Container, StackedContentsCompatible {

    private final NonNullList<ItemStack> items;
    private final int size;
    private final AbstractContainerMenu menu;

    public MachineShapelessCraftingContainer(final AbstractContainerMenu menu, final int size) {
        this(menu, size, NonNullList.withSize(size, ItemStack.EMPTY));
    }

    private MachineShapelessCraftingContainer(final AbstractContainerMenu menu, final int size, final NonNullList<ItemStack> items) {
        this.items = items;
        this.menu = menu;
        this.size = size;
    }

    @Override
    public int getContainerSize() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot >= this.getContainerSize() ? ItemStack.EMPTY : this.items.get(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public ItemStack removeItem(final int slot, final int count) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, count);
        if (!result.isEmpty()) {
            this.menu.slotsChanged(this);
        }

        return result;
    }

    @Override
    public void setItem(final int slot, final ItemStack itemStack) {
        this.items.set(slot, itemStack);
        this.menu.slotsChanged(this);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public void fillStackedContents(final StackedItemContents contents) {
        for (ItemStack itemStack : this.items) {
            contents.accountSimpleStack(itemStack);
        }
    }
}
