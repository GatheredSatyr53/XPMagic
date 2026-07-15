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

        this.shapeless(RecipeCategory.DECORATIONS, XPMagic.TIME_CRYSTAL_BLOCK.get())
            .requires(XPMagic.TIME_CRYSTAL.get(), 9)
            .unlockedBy("has_time_crystal", this.has(XPMagic.TIME_CRYSTAL.get()))
            .save(this.output);

        this.stonecutterResultFromBase(RecipeCategory.MISC, XPMagic.TIME_CRYSTAL_WAFER.get(), XPMagic.TIME_CRYSTAL_BLOCK.get(), 2);
        this.stonecutterResultFromBase(RecipeCategory.MISC, XPMagic.TIME_CRYSTAL_ROD.get(), XPMagic.TIME_CRYSTAL_WAFER.get(), 4);

        // Sword and axe are built as charged tools: whatever lightning_charge the crystals carry pools
        // onto the weapon as bonus attack damage. The pickaxe and shovel below stay plain shaped — the
        // charge only buys attack damage, which is dead weight on a digging tool.
        ChargedToolRecipeBuilder.chargedTool(RecipeCategory.COMBAT, XPMagic.MEMORY_CRYSTAL_SWORD.get())
            .pattern(" X ")
            .pattern(" X ")
            .pattern(" Y ")
            .define('X', XPMagic.MEMORY_CRYSTAL.get())
            .define('Y', XPMagic.TIME_CRYSTAL_ROD.get())
            .unlockedBy("has_memory_crystal", this.has(XPMagic.MEMORY_CRYSTAL.get()))
            .save(this.output);

        this.shaped(RecipeCategory.COMBAT, XPMagic.MEMORY_CRYSTAL_PICKAXE.get())
            .pattern("XXX")
            .pattern(" Y ")
            .pattern(" Y ")
            .define('X', XPMagic.MEMORY_CRYSTAL.get())
            .define('Y', XPMagic.TIME_CRYSTAL_ROD.get())
            .unlockedBy("has_memory_crystal", this.has(XPMagic.MEMORY_CRYSTAL.get()))
            .save(this.output);

        ChargedToolRecipeBuilder.chargedTool(RecipeCategory.COMBAT, XPMagic.MEMORY_CRYSTAL_AXE.get())
            .pattern("XX ")
            .pattern("XY ")
            .pattern(" Y ")
            .define('X', XPMagic.MEMORY_CRYSTAL.get())
            .define('Y', XPMagic.TIME_CRYSTAL_ROD.get())
            .unlockedBy("has_memory_crystal", this.has(XPMagic.MEMORY_CRYSTAL.get()))
            .save(this.output);

        this.shaped(RecipeCategory.COMBAT, XPMagic.MEMORY_CRYSTAL_SHOVEL.get())
            .pattern(" X ")
            .pattern(" Y ")
            .pattern(" Y ")
            .define('X', XPMagic.MEMORY_CRYSTAL.get())
            .define('Y', XPMagic.TIME_CRYSTAL_ROD.get())
            .unlockedBy("has_memory_crystal", this.has(XPMagic.MEMORY_CRYSTAL.get()))
            .save(this.output);

        // Note: powder mixing is not a data recipe — it's a fixed proportional formula computed
        // directly in PowderMixerMenu (N x 1:2:1 fractions -> N memory powder).
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
