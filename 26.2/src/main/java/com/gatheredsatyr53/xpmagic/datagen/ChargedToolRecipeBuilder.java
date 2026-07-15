package com.gatheredsatyr53.xpmagic.datagen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gatheredsatyr53.xpmagic.item.ChargedToolRecipe;

import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

import org.jspecify.annotations.Nullable;

/**
 * Datagen builder for {@link ChargedToolRecipe}, mirroring vanilla's {@code ShapedRecipeBuilder} — which
 * is hardcoded to emit {@code minecraft:crafting_shaped} and so cannot emit our serializer.
 *
 * <p>Only the subset the provider actually uses is here: item ingredients, a pattern, and an unlock
 * criterion. No tag ingredients (hence no {@code HolderGetter<Item>}), no group, no notification toggle.
 */
public final class ChargedToolRecipeBuilder implements RecipeBuilder {

    private final RecipeCategory category;
    private final ItemStackTemplate result;
    private final List<String> rows = new ArrayList<>();
    private final Map<Character, Ingredient> key = new LinkedHashMap<>();
    private final RecipeUnlockAdvancementBuilder advancementBuilder = new RecipeUnlockAdvancementBuilder();
    private @Nullable String group;

    private ChargedToolRecipeBuilder(RecipeCategory category, ItemStackTemplate result) {
        this.category = category;
        this.result = result;
    }

    public static ChargedToolRecipeBuilder chargedTool(RecipeCategory category, ItemLike result) {
        return new ChargedToolRecipeBuilder(category, new ItemStackTemplate(result.asItem(), 1));
    }

    public ChargedToolRecipeBuilder define(Character symbol, ItemLike item) {
        if (this.key.containsKey(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
        }

        this.key.put(symbol, Ingredient.of(item));
        return this;
    }

    public ChargedToolRecipeBuilder pattern(String row) {
        if (!this.rows.isEmpty() && row.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        }

        this.rows.add(row);
        return this;
    }

    @Override
    public ChargedToolRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.advancementBuilder.unlockedBy(name, criterion);
        return this;
    }

    @Override
    public ChargedToolRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    @Override
    public ResourceKey<Recipe<?>> defaultId() {
        return RecipeBuilder.getDefaultRecipeId(this.result);
    }

    @Override
    public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
        ChargedToolRecipe recipe = new ChargedToolRecipe(
            RecipeBuilder.createCraftingCommonInfo(true),
            RecipeBuilder.createCraftingBookInfo(this.category, this.group),
            ShapedRecipePattern.of(this.key, this.rows),
            this.result
        );
        output.accept(id, recipe, this.advancementBuilder.build(output, id, this.category));
    }
}
