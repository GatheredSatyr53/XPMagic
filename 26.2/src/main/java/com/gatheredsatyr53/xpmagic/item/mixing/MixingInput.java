package com.gatheredsatyr53.xpmagic.item.mixing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * Input for the Powder Mixer: up to three shapeless powder components plus one modifier.
 * Slots 0..2 are components (order-independent), slot 3 is the modifier.
 */
public record MixingInput(List<ItemStack> components, ItemStack modifier) implements RecipeInput {

    public static final int COMPONENT_SLOTS = 3;
    public static final int MODIFIER_SLOT = COMPONENT_SLOTS;

    @Override
    public ItemStack getItem(final int index) {
        return index < this.components.size() ? this.components.get(index) : this.modifier;
    }

    @Override
    public int size() {
        return this.components.size() + 1;
    }

    @Override
    public boolean isEmpty() {
        return this.modifier.isEmpty() && this.components.stream().allMatch(ItemStack::isEmpty);
    }
}
