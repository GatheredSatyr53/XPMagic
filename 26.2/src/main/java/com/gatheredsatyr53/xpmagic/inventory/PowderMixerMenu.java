package com.gatheredsatyr53.xpmagic.inventory;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.item.mixing.MixingInput;
import com.gatheredsatyr53.xpmagic.item.mixing.MixingRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.List;
import java.util.Optional;

public class PowderMixerMenu extends AbstractContainerMenu {

    public static final int INPUT_COUNT = 4;
    public static final int RESULT_SLOT = 4;

    private static final int MACHINE_SLOTS = 5; // 4 inputs + result
    private static final int INV_START = MACHINE_SLOTS;
    private static final int HOTBAR_START = INV_START + 27;
    private static final int SLOTS_END = HOTBAR_START + 9;

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

        // Three interchangeable component slots (top row) + one fixed modifier slot below, per the GUI texture.
        this.addSlot(new ConditionalInputSlot(inputs, 0, 26, 26, this::isPowderComponent));
        this.addSlot(new ConditionalInputSlot(inputs, 1, 44, 26, this::isPowderComponent));
        this.addSlot(new ConditionalInputSlot(inputs, 2, 62, 26, this::isPowderComponent));
        this.addSlot(new ConditionalInputSlot(inputs, 3, 44, 44, this::isPowderModifier));
        this.addSlot(new MixingResultSlot(this.resultSlots, 0, 124, 35, this));

        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }

    private boolean isPowderComponent(ItemStack stack) {
        return stack.tags().anyMatch(tagKey -> tagKey.equals(XPMagic.POWDER_COMPONENT));
    }

    private boolean isPowderModifier(ItemStack stack) {
        return stack.tags().anyMatch(tagKey -> tagKey.equals(XPMagic.POWDER_MODIFIER));
    }

    @Override
    public void slotsChanged(final Container container) {
        this.access.execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                this.updateResult(serverLevel);
            }
        });
    }

    private MixingInput currentInput() {
        return new MixingInput(
            List.of(this.craftSlots.getItem(0), this.craftSlots.getItem(1), this.craftSlots.getItem(2)),
            this.craftSlots.getItem(3));
    }

    /** Server-side: match the inputs against a mixing recipe and push the result (or empty) to the client. */
    private void updateResult(final ServerLevel level) {
        if (!(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MixingInput input = this.currentInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<MixingRecipe>> recipe = level.recipeAccess().getRecipeFor(XPMagic.MIXING_RECIPE.get(), input, level);
        if (recipe.isPresent()) {
            this.resultSlots.setRecipeUsed(recipe.get());
            result = recipe.get().value().assemble(input);
        }

        this.resultSlots.setItem(0, result);
        this.setRemoteSlot(RESULT_SLOT, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), RESULT_SLOT, result));
    }

    /**
     * Called when the player removes the result. Matching is exact, so the whole component pool is
     * exactly one recipe's worth — consume it entirely. The modifier is a single catalyst per craft.
     */
    public void onResultTaken() {
        for (int slot = 0; slot < MixingInput.COMPONENT_SLOTS; slot++) {
            if (!this.craftSlots.getItem(slot).isEmpty()) {
                this.craftSlots.setItem(slot, ItemStack.EMPTY);
            }
        }
        if (!this.craftSlots.getItem(MixingInput.MODIFIER_SLOT).isEmpty()) {
            this.craftSlots.removeItem(MixingInput.MODIFIER_SLOT, 1);
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
