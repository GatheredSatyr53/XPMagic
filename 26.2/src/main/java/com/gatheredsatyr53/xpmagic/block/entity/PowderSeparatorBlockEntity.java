package com.gatheredsatyr53.xpmagic.block.entity;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.VibrationStandBlock;
import com.gatheredsatyr53.xpmagic.inventory.PowderSeparatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

public class PowderSeparatorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_COARSE = 1;
    public static final int SLOT_MEDIUM = 2;
    public static final int SLOT_FINE = 3;
    public static final int SLOT_COUNT = 4;

    /** XP capacity of one source portion; the fraction budget for rule B. */
    private static final int SOURCE_CAPACITY = 10;

    /** Output slots, ordered largest-capacity-first for rule B. */
    private static final int[] FRACTION_SLOTS = {SLOT_COARSE, SLOT_MEDIUM, SLOT_FINE};

    /** Fraction capacities, cached from the items' xp_capacity so the budget rule and the XP Keeping
     *  Machine can never disagree on a fraction's worth (keeps the sum &le; source: no XP dupe). */
    private static int[] fractionCaps;

    /** Independent drop chance for each fraction (coarse, medium, fine). */
    private static final float[] FRACTION_CHANCES = {0.80F, 0.60F, 0.40F};

    /** Each portion yields at least this much fraction capacity; only coarse (cap 5) can reach it,
     *  so a short cycle is topped up with a coarse fraction. */
    private static final int MIN_YIELD = 5;

    /** Ticks of vibration needed to process one portion. */
    private static final int PROCESS_INTERVAL = 40;

    private final SeparatorInventory inventory = new SeparatorInventory();

    /** Vibration accumulated from the stand below; one portion is processed per PROCESS_INTERVAL. */
    private int vibrationTicks;
    private int vibrationTicksTotal = PROCESS_INTERVAL;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case PowderSeparatorMenu.DATA_VIBRATION_TICKS -> PowderSeparatorBlockEntity.this.vibrationTicks;
                case PowderSeparatorMenu.DATA_VIBRATION_TICKS_TOTAL -> PowderSeparatorBlockEntity.this.vibrationTicksTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case PowderSeparatorMenu.DATA_VIBRATION_TICKS -> PowderSeparatorBlockEntity.this.vibrationTicks = value;
                case PowderSeparatorMenu.DATA_VIBRATION_TICKS_TOTAL -> PowderSeparatorBlockEntity.this.vibrationTicksTotal = value;
            }
        }

        @Override
        public int getCount() {
            return PowderSeparatorMenu.DATA_COUNT;
        }
    };

    public PowderSeparatorBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(XPMagic.POWDER_SEPARATOR_BLOCK_ENTITY.get(), worldPosition, blockState);
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
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

        // Only advance the sifting cycle when there is actually something to sift; otherwise
        // hold it at zero so a full cycle can't be pre-charged while the input is empty.
        if (!vibrating || !be.canProcess()) {
            if (be.vibrationTicks != 0) {
                be.vibrationTicks = 0;
                setChanged(level, pos, state);
            }
            return;
        }

        if (++be.vibrationTicks >= PROCESS_INTERVAL) {
            be.vibrationTicks = 0;
            if (be.processPortion(level.getRandom())) {
                SoundType sound = state.getSoundType();
                level.playSound(null, pos, sound.getHitSound(), SoundSource.BLOCKS,
                    0.6F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            }
        }
        setChanged(level, pos, state);
    }

    /** Which fractions are emitted, walking largest-first while capacity fits the budget (rule B). */
    private static boolean[] computeEmissions() {
        int[] caps = fractionCaps();
        int budget = SOURCE_CAPACITY;
        boolean[] emit = new boolean[FRACTION_SLOTS.length];
        for (int i = 0; i < FRACTION_SLOTS.length; ++i) {
            if (caps[i] <= budget) {
                emit[i] = true;
                budget -= caps[i];
            }
        }
        return emit;
    }

    private static Item[] fractionItems() {
        return new Item[]{XPMagic.COARSE_POWDER.get(), XPMagic.MEDIUM_POWDER.get(), XPMagic.FINE_POWDER.get()};
    }

    /** Each fraction's capacity, read once from its item's xp_capacity component, then cached. */
    private static int[] fractionCaps() {
        int[] caps = fractionCaps;
        if (caps == null) {
            Item[] items = fractionItems();
            caps = new int[items.length];
            for (int i = 0; i < items.length; ++i)
                caps[i] = items[i].getDefaultInstance().getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
            fractionCaps = caps;
        }
        return caps;
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

    /**
     * Consumes one source portion and rolls each fraction independently: a fraction drops only if
     * it still fits the remaining budget (rule B) and passes its {@link #FRACTION_CHANCES} roll.
     * If the rolled fractions fall short of {@link #MIN_YIELD} capacity, a coarse fraction is added
     * so every portion yields at least that much.
     * No-op unless {@link #canProcess()}.
     */
    private boolean processPortion(RandomSource random) {
        if (!canProcess())
            return false;

        Item[] fractions = fractionItems();
        int[] caps = fractionCaps();
        this.inventory.getStackInSlot(SLOT_INPUT).shrink(1);

        int budget = SOURCE_CAPACITY;
        int yield = 0;
        for (int i = 0; i < FRACTION_SLOTS.length; ++i) {
            if (caps[i] > budget || random.nextFloat() >= FRACTION_CHANCES[i])
                continue;
            putFraction(i, fractions[i]);   // canProcess() guaranteed room this cycle
            budget -= caps[i];
            yield += caps[i];
        }

        // Guarantee the minimum yield: coarse (index 0, cap 5) is the only fraction big enough.
        if (yield < MIN_YIELD)
            putFraction(0, fractions[0]);
        return true;
    }

    /** Adds one of the given fraction to its output slot (room is assumed, checked by canProcess). */
    private void putFraction(int index, Item fraction) {
        ItemStack out = this.inventory.getStackInSlot(FRACTION_SLOTS[index]);
        if (out.isEmpty())
            this.inventory.setStackInSlot(FRACTION_SLOTS[index], new ItemStack(fraction));
        else
            out.grow(1);
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
