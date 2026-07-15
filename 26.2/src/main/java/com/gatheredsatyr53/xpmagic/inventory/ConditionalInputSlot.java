package com.gatheredsatyr53.xpmagic.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class ConditionalInputSlot extends SlotItemHandler {
    private final Predicate<ItemStack> condition;

    public ConditionalInputSlot(IItemHandler itemHandler, int index, int xPosition,
                                int yPosition, Predicate<ItemStack> condition) {
        super(itemHandler, index, xPosition, yPosition);

        this.condition = condition;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return condition.test(stack);
    }
}
