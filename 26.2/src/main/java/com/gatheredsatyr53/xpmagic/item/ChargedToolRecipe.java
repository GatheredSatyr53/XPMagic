package com.gatheredsatyr53.xpmagic.item;

import java.util.List;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

/**
 * A shaped recipe that pools the {@code lightning_charge} of everything in the grid onto its result —
 * forge a Memory Crystal Sword from crystals that drank a thunderbolt and the sword keeps the charge,
 * and with it a bonus to attack damage (see {@link LightningCharge}).
 *
 * <p>Vanilla's {@code shaped} recipes drop ingredient components on the floor, and {@code TransmuteRecipe}
 * — the one vanilla recipe that does carry them over — is shaped as "one input plus material", which a
 * multi-crystal pattern does not fit. Hence this type.
 *
 * <p>It mirrors {@link net.minecraft.world.item.crafting.ShapedRecipe} rather than extending it:
 * {@code ShapedRecipe.getSerializer()} is typed {@code RecipeSerializer<ShapedRecipe>}, and generics
 * being invariant, a subclass cannot narrow that to its own serializer. Extending
 * {@link NormalCraftingRecipe} — whose contract returns a wildcard — is the way out.
 */
public class ChargedToolRecipe extends NormalCraftingRecipe {

    public static final MapCodec<ChargedToolRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
            )
            .apply(i, ChargedToolRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ChargedToolRecipe> STREAM_CODEC = StreamCodec.composite(
        Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
        CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
        ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
        ItemStackTemplate.STREAM_CODEC, o -> o.result,
        ChargedToolRecipe::new
    );

    public static final RecipeSerializer<ChargedToolRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    public ChargedToolRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                             ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public RecipeSerializer<ChargedToolRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.pattern.matches(input);
    }

    /**
     * The charge is pooled here rather than in the handler, so the crafting grid's result slot shows the
     * finished weapon with its real damage before the player ever takes it.
     */
    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack tool = this.result.create();
        LightningCharge.applyTo(tool, totalCharge(input));
        return tool;
    }

    /**
     * Charge summed over the whole grid: every crystal that went into the weapon gives up what it holds,
     * so two half-charged crystals are worth one full one. {@link LightningCharge} caps what that buys.
     */
    private static int totalCharge(CraftingInput input) {
        int total = 0;
        for (ItemStack stack : input.items()) {
            total += stack.getOrDefault(XPMagic.LIGHTNING_CHARGE.get(), 0);
        }
        return total;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(this.pattern.ingredients());
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(
            new ShapedCraftingRecipeDisplay(
                this.pattern.width(),
                this.pattern.height(),
                this.pattern.ingredients().stream()
                    .map(e -> e.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE))
                    .toList(),
                new SlotDisplay.ItemStackSlotDisplay(this.result),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
            )
        );
    }
}
