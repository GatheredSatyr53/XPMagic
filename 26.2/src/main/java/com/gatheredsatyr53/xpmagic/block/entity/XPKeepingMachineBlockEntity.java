package com.gatheredsatyr53.xpmagic.block.entity;

import com.gatheredsatyr53.xpmagic.block.XPKeepingMachineBlock;
import com.gatheredsatyr53.xpmagic.inventory.XPKeepingMachineMenu;
import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.nbt.PlayerOwner;
import com.gatheredsatyr53.xpmagic.nbt.StoredExp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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
    public static final int SLOT_MATRIX = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_KEY = 4;
    public static final int SLOT_COUNT = 5;

    /** Experience points drained per cocktail, as in 1.12 */
    public static final int COOK_TIME = 200;

    private final MachineInventory inventory = new MachineInventory();
    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> this.inventory);

    private int burnTime;
    private int burnTimeTotal;
    private int cookTime;
    private int cookTimeTotal;

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
            case SLOT_MATRIX -> stack.has(XPMagic.XP_CAPACITY.get());
            default -> false;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.xpmagic.xp_keeping_machine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new XPKeepingMachineMenu(containerId, playerInventory, this);
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    /** The online player whose key is in the machine, or null if no key / owner offline. */
    private @Nullable Player resolveOwner(Level level) {
        PlayerOwner owner = this.inventory.getStackInSlot(SLOT_KEY).get(XPMagic.PLAYER_OWNER.get());
        if (owner == null)
            return null;
        MinecraftServer server = level.getServer();
        return server == null ? null : server.getPlayerList().getPlayer(owner.id());
    }

    public static int getMatrixXPCapacity(XPKeepingMachineBlockEntity machine) {
        return machine.inventory.getStackInSlot(SLOT_MATRIX).getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, XPKeepingMachineBlockEntity machine) {
        boolean changed = false;
        boolean isLit;
        boolean wasLit;
        if (machine.isBurning()) {
            wasLit = true;
            machine.burnTime--;
            isLit = machine.burnTime > 0;
        } else {
            wasLit = false;
            isLit = false;
        }

        Player owner = machine.resolveOwner(level);
        boolean ownerReady = owner != null && owner.totalExperience >= getMatrixXPCapacity(machine);

        if (machine.isBurning() || machine.allInputsPresent()) {
            boolean canWork = machine.canProduce() && ownerReady;

            if (!machine.isBurning() && canWork) {
                ItemStack fuel = machine.inventory.getStackInSlot(SLOT_FUEL);
                int burnDuration = level.fuelValues().burnDuration(fuel);
                if (burnDuration > 0) {
                    machine.burnTime = burnDuration;
                    machine.burnTimeTotal = burnDuration;
                    machine.cookTimeTotal = COOK_TIME;
                    machine.consumeFuel();
                    isLit = true;
                    changed = true;
                }
            }

            if (machine.isBurning() && canWork) {
                ++machine.cookTime;
                if (machine.cookTime >= machine.cookTimeTotal) {
                    machine.cookTime = 0;
                    machine.cookTimeTotal = COOK_TIME;
                    machine.produce(owner);
                    changed = true;
                }
            } else if (!machine.canProduce()) {
                machine.cookTime = 0;
            }
            // canProduce but owner not ready: pause, progress is kept

        } else if (!machine.isBurning() && machine.cookTime > 0) {
            machine.cookTime = Mth.clamp(machine.cookTime - 2, 0, machine.cookTimeTotal);
        }

        if (wasLit != isLit) {
            changed = true;
            state = state.setValue(XPKeepingMachineBlock.LIT, isLit);
            level.setBlock(pos, state, 3);
        }

        if (changed) {
            setChanged(level, pos, state);
        }
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    private boolean allInputsPresent() {
        for (int slot = 0; slot < SLOT_OUTPUT; ++slot)
            if (this.inventory.getStackInSlot(slot).isEmpty())
                return false;
        return true;
    }

    private boolean canProduce() {
        ItemStack bottle = this.inventory.getStackInSlot(SLOT_BOTTLE);
        ItemStack matrix = this.inventory.getStackInSlot(SLOT_MATRIX);
        if (bottle.isEmpty() || matrix.isEmpty())
            return false;
        if (!isItemValid(SLOT_BOTTLE, bottle) || !isItemValid(SLOT_MATRIX, matrix))
            return false;
        return this.inventory.getStackInSlot(SLOT_OUTPUT).isEmpty();
    }

    private void consumeFuel() {
        ItemStack fuel = this.inventory.getStackInSlot(SLOT_FUEL);
        if (fuel.getCount() == 1) {
            ItemStackTemplate remainder = fuel.getCraftingRemainder();
            this.inventory.setStackInSlot(SLOT_FUEL, remainder != null ? remainder.create() : ItemStack.EMPTY);
        } else {
            fuel.shrink(1);
        }
    }

    private void produce(Player owner) {
        // Safeguard: never charge more XP than the owner actually has; store what was taken.
        int charged = Math.clamp(owner.totalExperience, 0, getMatrixXPCapacity(this));
        owner.giveExperiencePoints(-charged);

        ItemStack cocktail = new ItemStack(XPMagic.XP_COCKTAIL.get());
        cocktail.set(XPMagic.STORED_EXP.get(), new StoredExp(charged));
        this.inventory.setStackInSlot(SLOT_OUTPUT, cocktail);
        this.inventory.getStackInSlot(SLOT_BOTTLE).shrink(1);
        this.inventory.getStackInSlot(SLOT_MATRIX).shrink(1);
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
            // Removing (or swapping) the key resets cooking, so a half-done cycle
            // started by one owner can't be finished on — and charged to — another.
            if (slot == SLOT_KEY)
                XPKeepingMachineBlockEntity.this.cookTime = 0;
            XPKeepingMachineBlockEntity.this.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return XPKeepingMachineBlockEntity.this.isItemValid(slot, stack);
        }
    }
}
