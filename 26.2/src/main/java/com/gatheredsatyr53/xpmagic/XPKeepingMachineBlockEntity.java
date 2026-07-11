package com.gatheredsatyr53.xpmagic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

public class XPKeepingMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_BOTTLE = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_POWDER = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_COUNT = 4;

    private final MachineInventory inventory = new MachineInventory();
    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> this.inventory);

    int burnTime;
    int burnTimeTotal;
    int cookTime;
    int cookTimeTotal;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case XPKeepingMachineMenu.DATA_BURN_TIME -> XPKeepingMachineBlockEntity.this.burnTime;
                case XPKeepingMachineMenu.DATA_BURN_TIME_TOTAL -> XPKeepingMachineBlockEntity.this.burnTimeTotal;
                case XPKeepingMachineMenu.DATA_COOK_TIME -> XPKeepingMachineBlockEntity.this.cookTime;
                case XPKeepingMachineMenu.DATA_COOK_TIME_TOTAL -> XPKeepingMachineBlockEntity.this.cookTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case XPKeepingMachineMenu.DATA_BURN_TIME -> XPKeepingMachineBlockEntity.this.burnTime = value;
                case XPKeepingMachineMenu.DATA_BURN_TIME_TOTAL -> XPKeepingMachineBlockEntity.this.burnTimeTotal = value;
                case XPKeepingMachineMenu.DATA_COOK_TIME -> XPKeepingMachineBlockEntity.this.cookTime = value;
                case XPKeepingMachineMenu.DATA_COOK_TIME_TOTAL -> XPKeepingMachineBlockEntity.this.cookTimeTotal = value;
            }
        }

        @Override
        public int getCount() {
            return XPKeepingMachineMenu.DATA_COUNT;
        }
    };

    public XPKeepingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(XPMagic.XP_KEEPING_MACHINE_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BOTTLE -> stack.is(Items.GLASS_BOTTLE);
            case SLOT_FUEL -> this.level != null && this.level.fuelValues().isFuel(stack);
            case SLOT_POWDER -> stack.is(XPMagic.MEMORY_POWDER.get());
            default -> false;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.xpmagic.xp_keeping_machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new XPKeepingMachineMenu(containerId, playerInventory, this.inventory, this.dataAccess,
            ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.inventory.items());
        output.putInt("burn_time", this.burnTime);
        output.putInt("burn_time_total", this.burnTimeTotal);
        output.putInt("cook_time", this.cookTime);
        output.putInt("cook_time_total", this.cookTimeTotal);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inventory.setSize(SLOT_COUNT);
        ContainerHelper.loadAllItems(input, this.inventory.items());
        this.burnTime = input.getIntOr("burn_time", 0);
        this.burnTimeTotal = input.getIntOr("burn_time_total", 0);
        this.cookTime = input.getIntOr("cook_time", 0);
        this.cookTimeTotal = input.getIntOr("cook_time_total", 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null)
            Containers.dropContents(this.level, pos, this.inventory.items());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return this.itemHandlerCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.itemHandlerCap.invalidate();
    }

    private final class MachineInventory extends ItemStackHandler {

        MachineInventory() {
            super(SLOT_COUNT);
        }

        NonNullList<ItemStack> items() {
            return this.stacks;
        }

        @Override
        protected void onContentsChanged(int slot) {
            XPKeepingMachineBlockEntity.this.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return XPKeepingMachineBlockEntity.this.isItemValid(slot, stack);
        }
    }
}
