package com.gatheredsatyr53.xpmagic.inventory;

import com.gatheredsatyr53.xpmagic.XPMagic;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class PowderMixerMenu extends AbstractContainerMenu {

    public static final int INPUT_COUNT = 4; // 3 components + 1 modifier
    public static final int RESULT_SLOT = 4;

    private static final int COMPONENT_SLOTS = 3;
    private static final int MODIFIER_SLOT = 3;
    private static final int MACHINE_SLOTS = 5; // inputs + result
    private static final int INV_START = MACHINE_SLOTS;
    private static final int HOTBAR_START = INV_START + 27;
    private static final int SLOTS_END = HOTBAR_START + 9;

    /** Hard ceiling on a single output unit's capacity; above it the mix yields nothing. */
    public static final int MAX_OUTPUT_CAPACITY = 20;

    private final Player player;
    private final ContainerLevelAccess access;
    protected final MachineShapelessCraftingContainer craftSlots = new MachineShapelessCraftingContainer(this, INPUT_COUNT);
    protected final ResultContainer resultSlots = new ResultContainer();

    public PowderMixerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public PowderMixerMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(XPMagic.POWDER_MIXER_MENU.get(), containerId);
        this.access = access;
        this.player = inventory.player;

        IItemHandler inputs = new InvWrapper(this.craftSlots);

        // Three interchangeable component slots (top row) + one fixed catalyst slot below, per the GUI texture.
        this.addSlot(new ConditionalInputSlot(inputs, 0, 25, 25, PowderMixerMenu::isFraction));
        this.addSlot(new ConditionalInputSlot(inputs, 1, 43, 25, PowderMixerMenu::isFraction));
        this.addSlot(new ConditionalInputSlot(inputs, 2, 61, 25, PowderMixerMenu::isFraction));
        this.addSlot(new ConditionalInputSlot(inputs, 3, 43, 43, PowderMixerMenu::isCatalyst));
        this.addSlot(new MixingResultSlot(this.resultSlots, 0, 123, 34, this));

        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 7 + col * 18, 83 + row * 18));

        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(inventory, col, 7 + col * 18, 141));
    }

    private static boolean isFraction(ItemStack stack) {
        return stack.is(XPMagic.COARSE_POWDER.get())
            || stack.is(XPMagic.MEDIUM_POWDER.get())
            || stack.is(XPMagic.FINE_POWDER.get());
    }

    private static boolean isCatalyst(ItemStack stack) {
        return catalystPercent(stack) > 0;
    }

    /**
     * Catalyst bonus as a percentage added to the final specific capacity. Catalysts are rare,
     * hard-to-obtain items; rarer ones give a bigger bonus. They are never consumed. Tune freely.
     */
    private static int catalystPercent(ItemStack stack) {
        if (stack.is(Items.BLAZE_ROD)) return 10;
        if (stack.is(Items.ECHO_SHARD)) return 15;
        if (stack.is(Items.NETHERITE_INGOT)) return 30;
        if (stack.is(Items.NETHER_STAR)) return 50;
        return 0;
    }

    @Override
    public void slotsChanged(final Container container) {
        this.access.execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                this.updateResult(serverLevel);
            }
        });
    }

    /** Server-side: recompute the mixing result and push it (or empty) to the client. */
    private void updateResult(final ServerLevel level) {
        if (!(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack result = this.computeResult();
        this.resultSlots.setItem(0, result);
        this.setRemoteSlot(RESULT_SLOT, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), RESULT_SLOT, result));
    }

    /**
     * Recombine fractions into memory powder. Coarse (loose) and fine (dense) compensate each other
     * by count: paired fractions count at their nominal capacity (coarse 5, medium 2, fine 1), the
     * unpaired surplus of coarse is worth -25% and the surplus of fine +25%. The output is a single
     * stack of gcd(coarse, medium, fine) powders; a catalyst adds a percentage to each one's capacity.
     * Everything floors, so a fine premium must be accumulated before it materialises.
     */
    private ItemStack computeResult() {
        MixSummary mix = this.summarize();
        if (!mix.hasOutput()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(XPMagic.MEMORY_POWDER.get(), mix.outputCount());
        // Only override the baked default so full-capacity output still stacks with ordinary powder.
        Integer defaultCapacity = result.get(XPMagic.XP_CAPACITY.get());
        if (defaultCapacity == null || defaultCapacity != mix.outputCapacity()) {
            result.set(XPMagic.XP_CAPACITY.get(), mix.outputCapacity());
        }
        return result;
    }

    /** The current mix as read from the (synced) component and modifier slots. Safe on the client. */
    public MixSummary currentMix() {
        return this.summarize();
    }

    private MixSummary summarize() {
        int coarse = 0, medium = 0, fine = 0;
        for (int slot = 0; slot < COMPONENT_SLOTS; slot++) {
            ItemStack stack = this.craftSlots.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            } else if (stack.is(XPMagic.COARSE_POWDER.get())) {
                coarse += stack.getCount();
            } else if (stack.is(XPMagic.MEDIUM_POWDER.get())) {
                medium += stack.getCount();
            } else if (stack.is(XPMagic.FINE_POWDER.get())) {
                fine += stack.getCount();
            } else {
                return MixSummary.EMPTY; // an unexpected component blocks the mix (slots filter this anyway)
            }
        }

        if (coarse == 0 && medium == 0 && fine == 0) {
            return MixSummary.EMPTY;
        }

        int pairs = Math.min(coarse, fine);
        int surplusCoarse = coarse - pairs;
        int surplusFine = fine - pairs;

        // Total capacity in quarter-units (x4) to keep exact integers:
        // medium 2 -> 8, paired coarse+fine 6 -> 24, surplus coarse 3.75 -> 15, surplus fine 1.25 -> 5.
        int totalTimes4 = 8 * medium + 24 * pairs + 15 * surplusCoarse + 5 * surplusFine;

        int count = gcd(gcd(coarse, medium), fine); // gcd of the component stack sizes; catalyst excluded
        int percent = catalystPercent(this.craftSlots.getItem(MODIFIER_SLOT));

        // Per-item capacity, catalyst applied, floored: floor( totalTimes4 / (4*count) * (100+percent)/100 ).
        int capacity = Math.max(1, (totalTimes4 * (100 + percent)) / (400 * count));
        boolean exceeded = capacity > MAX_OUTPUT_CAPACITY;

        return new MixSummary(coarse, medium, fine, pairs, surplusCoarse, surplusFine,
            totalTimes4, percent, count, capacity, exceeded);
    }

    /** Breakdown of the current mix, shared by result computation and the screen's tooltips. */
    public record MixSummary(int coarse, int medium, int fine, int pairs, int surplusCoarse, int surplusFine,
                             int mixCapacityTimes4, int catalystPercent, int outputCount, int outputCapacity,
                             boolean exceeded) {
        public static final MixSummary EMPTY = new MixSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);

        /** True when fractions are present, regardless of the ceiling. */
        public boolean present() {
            return this.outputCount > 0;
        }

        /** True when there is a valid result (present and within the capacity ceiling). */
        public boolean hasOutput() {
            return this.present() && !this.exceeded;
        }
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    /**
     * Called when the player removes the result. The whole component pool feeds the mix, so it is
     * consumed entirely; the catalyst is never consumed.
     */
    public void onResultTaken() {
        for (int slot = 0; slot < COMPONENT_SLOTS; slot++) {
            if (!this.craftSlots.getItem(slot).isEmpty()) {
                this.craftSlots.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
    }

    @Override
    public boolean stillValid(final Player player) {
        return stillValid(this.access, player, XPMagic.POWDER_MIXER.get());
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int slotIndex) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();

            if (slotIndex < MACHINE_SLOTS) {
                // machine -> player inventory
                if (!this.moveItemStackTo(stack, INV_START, SLOTS_END, true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(stack, moved);
            } else if (!this.moveItemStackTo(stack, 0, INPUT_COUNT, false)) {
                // player -> machine inputs failed; shuffle within player inventory
                if (slotIndex < HOTBAR_START) {
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

            if (stack.getCount() == moved.getCount())
                return ItemStack.EMPTY;

            slot.onTake(player, stack);
        }

        return moved;
    }
}
