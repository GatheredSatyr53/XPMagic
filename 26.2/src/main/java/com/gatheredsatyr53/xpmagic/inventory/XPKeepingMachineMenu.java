package com.gatheredsatyr53.xpmagic.inventory;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.entity.XPKeepingMachineBlockEntity;
import com.gatheredsatyr53.xpmagic.nbt.PlayerOwner;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import static com.gatheredsatyr53.xpmagic.block.entity.XPKeepingMachineBlockEntity.*;

public class XPKeepingMachineMenu extends AbstractContainerMenu {

    // Synced field ids, same layout as the 1.12 container window properties
    public static final int DATA_BURN_TIME = 0;
    public static final int DATA_BURN_TIME_TOTAL = 1;
    public static final int DATA_COOK_TIME = 2;
    public static final int DATA_COOK_TIME_TOTAL = 3;
    public static final int DATA_COUNT = 4;

    private static final int INV_START = SLOT_COUNT;
    private static final int HOTBAR_START = INV_START + 27;
    private static final int SLOTS_END = HOTBAR_START + 9;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Level level;
    private final Player player;

    public XPKeepingMachineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(SLOT_COUNT), new SimpleContainerData(DATA_COUNT),
            ContainerLevelAccess.NULL);
    }

    public XPKeepingMachineMenu(int containerId, Inventory playerInventory, XPKeepingMachineBlockEntity machine) {
        this(containerId, playerInventory, machine.getInventory(), machine.getDataAccess(),
            ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()));
    }

    private XPKeepingMachineMenu(int containerId, Inventory playerInventory, IItemHandler machineInventory,
                                 ContainerData data, ContainerLevelAccess access) {
        super(XPMagic.XP_KEEPING_MACHINE_MENU.get(), containerId);
        this.data = data;
        this.access = access;
        this.level = playerInventory.player.level();
        this.player = playerInventory.player;

        this.addSlot(new SlotItemHandler(machineInventory, SLOT_BOTTLE, 67, 19) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isBottle(stack);
            }
        });
        this.addSlot(new SlotItemHandler(machineInventory, SLOT_FUEL, 49, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        });
        this.addSlot(new SlotItemHandler(machineInventory, SLOT_MATRIX, 67, 59) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isMatrix(stack);
            }
        });
        this.addSlot(new SlotItemHandler(machineInventory, SLOT_OUTPUT, 116, 39) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(machineInventory, SLOT_KEY, 9, 9) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isOwnKey(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                // The handler reports isItemValid=false for the key slot (to block hopper
                // insertion), which would make SlotItemHandler simulate a capacity of 0 and
                // refuse GUI placement. Ownership is already enforced by mayPlace, so report
                // the real capacity here.
                return stack.getMaxStackSize();
            }
        });

        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, XPMagic.XP_KEEPING_MACHINE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();

            if (slotIndex == SLOT_OUTPUT) {
                if (!this.moveItemStackTo(stack, INV_START, SLOTS_END, true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex < SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, INV_START, SLOTS_END, false))
                    return ItemStack.EMPTY;
            } else {
                if (isOwnKey(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_KEY, SLOT_KEY + 1, false))
                        return ItemStack.EMPTY;
                } else if (isBottle(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_BOTTLE, SLOT_BOTTLE + 1, false))
                        return ItemStack.EMPTY;
                } else if (isFuel(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_FUEL, SLOT_FUEL + 1, false))
                        return ItemStack.EMPTY;
                } else if (isMatrix(stack)) {
                    if (!this.moveItemStackTo(stack, SLOT_MATRIX, SLOT_MATRIX + 1, false))
                        return ItemStack.EMPTY;
                } else if (slotIndex < HOTBAR_START) {
                    if (!this.moveItemStackTo(stack, HOTBAR_START, SLOTS_END, false))
                        return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty())
                slot.setByPlayer(ItemStack.EMPTY);
            else
                slot.setChanged();

            if (stack.getCount() == clicked.getCount())
                return ItemStack.EMPTY;

            slot.onTake(player, stack);
        }

        return clicked;
    }

    private boolean isBottle(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE);
    }

    private boolean isFuel(ItemStack stack) {
        return this.level.fuelValues().isFuel(stack);
    }

    /** Any XP-bearing item works as a matrix — memory powder and its separator fractions. */
    private boolean isMatrix(ItemStack stack) {
        return stack.has(XPMagic.XP_CAPACITY.get());
    }

    /** Only the player viewing the menu may place their own bound key. */
    private boolean isOwnKey(ItemStack stack) {
        PlayerOwner owner = stack.get(XPMagic.PLAYER_OWNER.get());
        return owner != null && owner.id().equals(this.player.getUUID());
    }

    public boolean isLit() {
        return this.data.get(DATA_BURN_TIME) > 0;
    }

    public int getLitScaled(int maxPixels) {
        int total = this.data.get(DATA_BURN_TIME_TOTAL);
        if (total == 0)
            total = 200;
        return this.data.get(DATA_BURN_TIME) * maxPixels / total;
    }

    public int getCookProgressScaled(int maxPixels) {
        int cook = this.data.get(DATA_COOK_TIME);
        int total = this.data.get(DATA_COOK_TIME_TOTAL);
        return total != 0 && cook != 0 ? cook * maxPixels / total : 0;
    }
}
