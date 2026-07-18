package com.gatheredsatyr53.xpmagic.inventory;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.entity.PowderSeparatorBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static com.gatheredsatyr53.xpmagic.block.entity.PowderSeparatorBlockEntity.*;

public class PowderSeparatorMenu extends AbstractContainerMenu {

    public static final int DATA_VIBRATION_TICKS = 0;
    public static final int DATA_VIBRATION_TICKS_TOTAL = 1;
    public static final int DATA_COUNT = 2;

    private static final int INV_START = SLOT_COUNT;
    private static final int HOTBAR_START = INV_START + 27;
    private static final int SLOTS_END = HOTBAR_START + 9;

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public PowderSeparatorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(DATA_COUNT),
            ContainerLevelAccess.NULL);
    }

    public PowderSeparatorMenu(int containerId, Inventory playerInventory, PowderSeparatorBlockEntity separator) {
        this(containerId, playerInventory, separator.getInventory(), separator.getDataAccess(),
            ContainerLevelAccess.create(separator.getLevel(), separator.getBlockPos()));
    }

    private PowderSeparatorMenu(int containerId, Inventory playerInventory, Container inventory,
                                ContainerData data, ContainerLevelAccess access) {
        super(XPMagic.POWDER_SEPARATOR_MENU.get(), containerId);
        this.access = access;
        this.data = data;

        this.addSlot(new Slot(inventory, SLOT_INPUT, 80, 18));
        this.addSlot(new OutputSlot(inventory, SLOT_COARSE, 62, 55));
        this.addSlot(new OutputSlot(inventory, SLOT_MEDIUM, 80, 55));
        this.addSlot(new OutputSlot(inventory, SLOT_FINE, 98, 55));

        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, XPMagic.POWDER_SEPARATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();

            if (slotIndex < SLOT_COUNT) {
                // machine -> player inventory
                if (!this.moveItemStackTo(stack, INV_START, SLOTS_END, true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(stack, moved);
            } else if (stack.is(XPMagic.MEMORY_POWDER.get())) {
                // powder -> input
                if (!this.moveItemStackTo(stack, SLOT_INPUT, SLOT_INPUT + 1, false))
                    return ItemStack.EMPTY;
            } else if (slotIndex < HOTBAR_START) {
                // inventory -> hotbar
                if (!this.moveItemStackTo(stack, HOTBAR_START, SLOTS_END, false))
                    return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
                // hotbar -> inventory
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty())
                slot.setByPlayer(ItemStack.EMPTY);
            else
                slot.setChanged();

            if (stack.getCount() == moved.getCount())
                return ItemStack.EMPTY;

            slot.onTake(player, stack);
        }

        return moved;
    }

    public int getSeparationProgressScaled(int maxPixels) {
        int separation = this.data.get(DATA_VIBRATION_TICKS);
        int total = this.data.get(DATA_VIBRATION_TICKS_TOTAL);
        return total != 0 && separation != 0 ? separation * maxPixels / total : 0;
    }
}
