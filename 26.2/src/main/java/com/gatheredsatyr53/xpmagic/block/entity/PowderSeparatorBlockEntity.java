package com.gatheredsatyr53.xpmagic.block.entity;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.VibrationStandBlock;
import com.gatheredsatyr53.xpmagic.inventory.PowderSeparatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
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

public class PowderSeparatorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_COARSE = 1;
    public static final int SLOT_MEDIUM = 2;
    public static final int SLOT_FINE = 3;
    public static final int SLOT_COUNT = 4;

    /** XP capacity of one source portion; the fraction budget for rule B. */
    private static final int SOURCE_CAPACITY = 10;

    /** Output slots and the capacity of the fraction produced there. Ordered largest-first for rule B. */
    private static final int[] FRACTION_SLOTS = {SLOT_COARSE, SLOT_MEDIUM, SLOT_FINE};
    private static final int[] FRACTION_CAPS = {5, 2, 1};

    /** Ticks of vibration needed to process one portion. */
    private static final int PROCESS_INTERVAL = 40;

    private final SeparatorInventory inventory = new SeparatorInventory();
    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> this.inventory);

    /** Vibration accumulated from the stand below; one portion is processed per PROCESS_INTERVAL. */
    private int vibrationTicks;

    public PowderSeparatorBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(XPMagic.POWDER_SEPARATOR_BLOCK_ENTITY.get(), worldPosition, blockState);
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        // Only the input accepts insertion (from GUI or hopper); outputs are extract-only.
        return slot == SLOT_INPUT && stack.is(XPMagic.MEMORY_POWDER.get());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.xpmagic.powder_separator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PowderSeparatorMenu(containerId, playerInventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowderSeparatorBlockEntity be) {
        // Powered only by a running vibration stand directly below.
        BlockState below = level.getBlockState(pos.below());
        boolean vibrating = below.getBlock() instanceof VibrationStandBlock && below.getValue(VibrationStandBlock.LIT);
        if (!vibrating)
            return;

        if (++be.vibrationTicks >= PROCESS_INTERVAL) {
            be.vibrationTicks = 0;
            if (be.processPortion()) {
                SoundType sound = state.getSoundType();
                level.playSound(null, pos, sound.getHitSound(), SoundSource.BLOCKS,
                    0.6F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            }
        }
        setChanged(level, pos, state);
    }

    /** Which fractions are emitted, walking largest-first while capacity fits the budget (rule B). */
    private static boolean[] computeEmissions() {
        int budget = SOURCE_CAPACITY;
        boolean[] emit = new boolean[FRACTION_SLOTS.length];
        for (int i = 0; i < FRACTION_SLOTS.length; ++i) {
            if (FRACTION_CAPS[i] <= budget) {
                emit[i] = true;
                budget -= FRACTION_CAPS[i];
            }
        }
        return emit;
    }

    private static Item[] fractionItems() {
        return new Item[]{XPMagic.COARSE_POWDER.get(), XPMagic.MEDIUM_POWDER.get(), XPMagic.FINE_POWDER.get()};
    }

    /** True if the input holds powder and every fraction that would be produced has room. */
    public boolean canProcess() {
        ItemStack in = this.inventory.getStackInSlot(SLOT_INPUT);
        if (!in.is(XPMagic.MEMORY_POWDER.get()))
            return false;

        boolean[] emit = computeEmissions();
        Item[] fractions = fractionItems();
        for (int i = 0; i < FRACTION_SLOTS.length; ++i) {
            if (!emit[i])
                continue;
            ItemStack out = this.inventory.getStackInSlot(FRACTION_SLOTS[i]);
            if (!out.isEmpty() && (!out.is(fractions[i]) || out.getCount() >= out.getMaxStackSize()))
                return false;
        }
        return true;
    }

    /** Consumes one source portion and emits the fractions. No-op unless {@link #canProcess()}. */
    private boolean processPortion() {
        if (!canProcess())
            return false;

        boolean[] emit = computeEmissions();
        Item[] fractions = fractionItems();

        this.inventory.getStackInSlot(SLOT_INPUT).shrink(1);
        for (int i = 0; i < FRACTION_SLOTS.length; ++i) {
            if (!emit[i])
                continue;
            ItemStack out = this.inventory.getStackInSlot(FRACTION_SLOTS[i]);
            if (out.isEmpty())
                this.inventory.setStackInSlot(FRACTION_SLOTS[i], new ItemStack(fractions[i]));
            else
                out.grow(1);
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.inventory.items());
        output.putInt("vibration_ticks", this.vibrationTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inventory.setSize(SLOT_COUNT);
        ContainerHelper.loadAllItems(input, this.inventory.items());
        this.vibrationTicks = input.getIntOr("vibration_ticks", 0);
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

    private final class SeparatorInventory extends ItemStackHandler {

        SeparatorInventory() {
            super(SLOT_COUNT);
        }

        NonNullList<ItemStack> items() {
            return this.stacks;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PowderSeparatorBlockEntity.this.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return PowderSeparatorBlockEntity.this.isItemValid(slot, stack);
        }
    }
}
