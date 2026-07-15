package com.gatheredsatyr53.xpmagic.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class ConditionalInputSlot extends Slot {
    private final Predicate<ItemStack> condition;

    public ConditionalInputSlot(Container container, int index, int xPosition,
                                int yPosition, Predicate<ItemStack> condition) {
        super(container, index, xPosition, yPosition);

        this.condition = condition;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return condition.test(stack);
    }
}
