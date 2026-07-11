package com.gatheredsatyr53.xpmagic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
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

public class XPKeepingMachineBlockEntity extends BlockEntity {

    public static final int SLOT_BOTTLE = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_POWDER = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_COUNT = 4;

    private final MachineInventory inventory = new MachineInventory();
    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> this.inventory);

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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.inventory.items());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inventory.setSize(SLOT_COUNT);
        ContainerHelper.loadAllItems(input, this.inventory.items());
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
