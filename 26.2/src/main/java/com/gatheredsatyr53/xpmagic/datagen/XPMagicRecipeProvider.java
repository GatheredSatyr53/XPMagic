package com.gatheredsatyr53.xpmagic.datagen;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.item.mixing.ShapelessMixingRecipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

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

        this.shaped(RecipeCategory.MISC, XPMagic.MEMORY_CHIP.get())
            .pattern("YZY")
            .define('Y', XPMagic.PROCESSING_CHIP.get())
            .define('Z', Items.COPPER_INGOT)
            .unlockedBy("has_processing_chip", this.has(XPMagic.PROCESSING_CHIP.get()))
            .save(this.output);

        this.shaped(RecipeCategory.MISC, XPMagic.PLAYER_KEY.get())
            .pattern(" X ")
            .pattern("YYY")
            .define('X', XPMagic.MEMORY_CHIP.get())
            .define('Y', Items.PAPER)
            .unlockedBy("has_memory_chip", this.has(XPMagic.MEMORY_CHIP.get()))
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

        // Mesh-bodied separator (string = filter mesh, matching its cobweb sound)
        this.shaped(RecipeCategory.MISC, XPMagic.POWDER_SEPARATOR.get())
            .pattern("ISI")
            .pattern("SYS")
            .pattern("ISI")
            .define('I', Items.IRON_INGOT)
            .define('S', Items.STRING)
            .define('Y', XPMagic.PROCESSING_CHIP.get())
            .unlockedBy("has_processing_chip", this.has(XPMagic.PROCESSING_CHIP.get()))
            .save(this.output);

        // Piston-driven vibration stand
        this.shaped(RecipeCategory.MISC, XPMagic.VIBRATION_STAND.get())
            .pattern("III")
            .pattern("RPR")
            .pattern("III")
            .define('I', Items.IRON_INGOT)
            .define('R', Items.REDSTONE)
            .define('P', Items.PISTON)
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);

        // Powder Mixer recipes (memory_powder_mixing type). Components are quantity-based and
        // non-positional: a repeated ingredient means that many required items, pooled across the
        // component slots (order/slot doesn't matter); only the modifier slot is fixed.
        // Example / placeholder — recombine fractions into full powder: 2x coarse + 1x medium + a
        // blaze rod. XP capacity in (5+5+2=12) must stay >= the result's (memory_powder = 10) so
        // mixing never creates XP. Tune freely.
        this.mixing("recombine_memory_powder",
            new ItemStackTemplate(XPMagic.MEMORY_POWDER.get()),
            Optional.of(Ingredient.of(Items.BLAZE_ROD)),
            Ingredient.of(XPMagic.COARSE_POWDER.get()),
            Ingredient.of(XPMagic.COARSE_POWDER.get()),
            Ingredient.of(XPMagic.MEDIUM_POWDER.get()));
    }

    private void mixing(String name, ItemStackTemplate result, Optional<Ingredient> modifier, Ingredient... components) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(XPMagic.MODID, name));
        this.output.accept(key, new ShapelessMixingRecipe("", CraftingBookCategory.MISC, result, List.of(components), modifier), null);
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
