package com.gatheredsatyr53.xpmagic.item.mixing;

import com.gatheredsatyr53.xpmagic.XPMagic;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public interface MixingRecipe extends Recipe<MixingInput> {

    @Override
    default RecipeType<MixingRecipe> getType() {
        return XPMagic.MIXING_RECIPE.get();
    }

    @Override
    RecipeSerializer<? extends MixingRecipe> getSerializer();

    CraftingBookCategory category();

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return switch (this.category()) {
            case BUILDING -> RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
            case EQUIPMENT -> RecipeBookCategories.CRAFTING_EQUIPMENT;
            case REDSTONE -> RecipeBookCategories.CRAFTING_REDSTONE;
            case MISC -> RecipeBookCategories.CRAFTING_MISC;
        };
    }
}
