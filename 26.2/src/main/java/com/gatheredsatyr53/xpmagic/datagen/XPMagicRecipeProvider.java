package com.gatheredsatyr53.xpmagic.datagen;

import java.util.concurrent.CompletableFuture;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

public final class XPMagicRecipeProvider extends RecipeProvider {

    private XPMagicRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        this.shaped(RecipeCategory.MISC, XPMagic.MEMORY_POWDER.get(), 4)
            .pattern(" # ")
            .pattern("XYZ")
            .pattern(" $ ")
            .define('#', Items.REDSTONE)
            .define('X', Items.IRON_INGOT)
            .define('Y', Items.LAPIS_LAZULI)
            .define('Z', Items.GOLD_INGOT)
            .define('$', Items.GUNPOWDER)
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);

        this.shaped(RecipeCategory.MISC, XPMagic.PROCESSING_CHIP.get())
            .pattern("XXX")
            .pattern("YZY")
            .pattern("XXX")
            .define('X', Items.IRON_NUGGET)
            .define('Y', Items.GOLD_NUGGET)
            .define('Z', Items.REDSTONE)
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);

        this.shaped(RecipeCategory.MISC, XPMagic.XP_KEEPING_MACHINE.get())
            .pattern("GXG")
            .pattern("XYX")
            .pattern("XGX")
            .define('X', Items.IRON_INGOT)
            .define('Y', XPMagic.PROCESSING_CHIP.get())
            .define('G', Items.GLASS)
            .unlockedBy("has_processing_chip", this.has(XPMagic.PROCESSING_CHIP.get()))
            .save(this.output);
    }

    public static final class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new XPMagicRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "XPMagic Recipes";
        }
    }
}
