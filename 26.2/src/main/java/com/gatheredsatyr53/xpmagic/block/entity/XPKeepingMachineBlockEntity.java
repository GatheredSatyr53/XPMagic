package com.gatheredsatyr53.xpmagic.block.entity;

import com.gatheredsatyr53.xpmagic.block.XPKeepingMachineBlock;
import com.gatheredsatyr53.xpmagic.inventory.XPKeepingMachineMenu;
import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.nbt.PlayerOwner;
import com.gatheredsatyr53.xpmagic.nbt.StoredExp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
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

    private int burnTime;
    private int burnTimeTotal;
    private int cookTime;
    private int cookTimeTotal;
    /** Owner's live XP, refreshed each server tick and synced to the menu; -1 when no key / owner offline. */
    private int ownerXp = -1;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case XPKeepingMachineMenu.DATA_BURN_TIME -> XPKeepingMachineBlockEntity.this.burnTime;
                case XPKeepingMachineMenu.DATA_BURN_TIME_TOTAL -> XPKeepingMachineBlockEntity.this.burnTimeTotal;
                case XPKeepingMachineMenu.DATA_COOK_TIME -> XPKeepingMachineBlockEntity.this.cookTime;
                case XPKeepingMachineMenu.DATA_COOK_TIME_TOTAL -> XPKeepingMachineBlockEntity.this.cookTimeTotal;
                case XPKeepingMachineMenu.DATA_OWNER_XP -> XPKeepingMachineBlockEntity.this.ownerXp;
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
                case XPKeepingMachineMenu.DATA_OWNER_XP -> XPKeepingMachineBlockEntity.this.ownerXp = value;
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

    public Container getInventory() {
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

    public static @Nullable Player resolveOwner(ItemStack ownerStack, Level level) {
        PlayerOwner owner = ownerStack.get(XPMagic.PLAYER_OWNER.get());
        if (owner == null)
            return null;
        MinecraftServer server = level.getServer();
        return server == null ? null : server.getPlayerList().getPlayer(owner.id());
    }

    /** The online player whose key is in the machine, or null if no key / owner offline. */
    private @Nullable Player resolveOwner(Level level) {
        return resolveOwner(this.inventory.getItem(SLOT_KEY), level);
    }

    public static int getMatrixXPCapacity(XPKeepingMachineBlockEntity machine) {
        return machine.inventory.getItem(SLOT_MATRIX).getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
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
        machine.ownerXp = owner == null ? -1 : owner.totalExperience;
        boolean ownerReady = owner != null && owner.totalExperience >= getMatrixXPCapacity(machine);

        if (machine.isBurning() || machine.allInputsPresent()) {
            boolean canWork = machine.canProduce() && ownerReady;

            if (!machine.isBurning() && canWork) {
                ItemStack fuel = machine.inventory.getItem(SLOT_FUEL);
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
            if (this.inventory.getItem(slot).isEmpty())
                return false;
        return true;
    }

    private boolean canProduce() {
        ItemStack bottle = this.inventory.getItem(SLOT_BOTTLE);
        ItemStack matrix = this.inventory.getItem(SLOT_MATRIX);
        if (bottle.isEmpty() || matrix.isEmpty())
            return false;
        if (!isItemValid(SLOT_BOTTLE, bottle) || !isItemValid(SLOT_MATRIX, matrix))
            return false;
        return this.inventory.getItem(SLOT_OUTPUT).isEmpty();
    }

    private void consumeFuel() {
        ItemStack fuel = this.inventory.getItem(SLOT_FUEL);
        if (fuel.getCount() == 1) {
            ItemStackTemplate remainder = fuel.getCraftingRemainder();
            this.inventory.setItem(SLOT_FUEL, remainder != null ? remainder.create() : ItemStack.EMPTY);
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
        this.inventory.setItem(SLOT_OUTPUT, cocktail);
        this.inventory.getItem(SLOT_BOTTLE).shrink(1);
        this.inventory.getItem(SLOT_MATRIX).shrink(1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.inventory.getItems());
        output.putInt("burn_time", this.burnTime);
        output.putInt("burn_time_total", this.burnTimeTotal);
        output.putInt("cook_time", this.cookTime);
        output.putInt("cook_time_total", this.cookTimeTotal);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.inventory.getItems());
        // Seed the key snapshot from the loaded contents so the first post-load inventory change
        // (e.g. a hopper topping up fuel) isn't mistaken for a key swap and doesn't wipe cook progress.
        this.inventory.captureKeySnapshot();
        this.burnTime = input.getIntOr("burn_time", 0);
        this.burnTimeTotal = input.getIntOr("burn_time_total", 0);
        this.cookTime = input.getIntOr("cook_time", 0);
        this.cookTimeTotal = input.getIntOr("cook_time_total", 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null)
            Containers.dropContents(this.level, pos, this.inventory.getItems());
    }

    private final class MachineInventory extends SimpleContainer implements WorldlyContainer {

        /** Hoppers below extract the finished cocktail. */
        private static final int[] OUTPUT_SLOTS = {SLOT_OUTPUT};
        /** Any other face feeds the bottle/fuel/matrix inputs; the owner-bound key is never automatable. */
        private static final int[] INPUT_SLOTS = {SLOT_BOTTLE, SLOT_FUEL, SLOT_MATRIX};

        private ItemStack lastKey = ItemStack.EMPTY;

        MachineInventory() {
            super(SLOT_COUNT);
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return canPlaceItem(slot, stack);
        }

        // Only the finished cocktail leaves via automation; inputs and the key stay put.
        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return slot == SLOT_OUTPUT;
        }

        /** Re-baseline the key snapshot to the current key slot (called after NBT load). */
        void captureKeySnapshot() {
            this.lastKey = this.getItem(SLOT_KEY).copy();
        }

        // Gates hopper insertion (VanillaContainerWrapper.isValid consults this) and vanilla
        // container insertion; the output and key slots reject insertion (default false below).
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return XPKeepingMachineBlockEntity.this.isItemValid(slot, stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            // Removing (or swapping) the key resets cooking, so a half-done cycle started by one
            // owner can't be finished on — and charged to — another. setChanged() carries no slot,
            // so detect a key change by comparing against the last seen key stack.
            ItemStack key = this.getItem(SLOT_KEY);
            if (!ItemStack.matches(key, this.lastKey)) {
                XPKeepingMachineBlockEntity.this.cookTime = 0;
                this.lastKey = key.copy();
            }
            XPKeepingMachineBlockEntity.this.setChanged();
        }
    }
}
