package com.gatheredsatyr53.xpmagic.item;

import java.util.List;

import com.gatheredsatyr53.xpmagic.Config;
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
 * A shaped recipe that carries what the Memory Crystals in the grid hold onto the tool forged from
 * them. Two things cross over, and they are deliberately different in kind (see {@link ToolStats}):
 *
 * <ul>
 * <li>{@code lightning_charge} — pooled and paid out at once. Forge a sword from crystals that drank a
 *     thunderbolt and it swings harder from the first blow; a pickaxe bites into stone faster.</li>
 * <li>{@code xp_capacity} — pooled and spent on {@code max_evolution_potential}: not a bonus but a
 *     ceiling, the room the tool has to grow into through use. Crystals a good explosion packed dense
 *     make a tool that can go further, though it starts no stronger.</li>
 * </ul>
 *
 * <p>So the same crystal answers two questions at the forge — how good is this tool now, and how good
 * can it ever get — and a player can aim for either.
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
     * Both values are pooled here rather than in a handler, so the crafting grid's result slot shows the
     * finished tool with its real numbers before the player ever takes it.
     */
    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack tool = this.result.create();

        int charge = totalCharge(input);
        if (charge > 0) {
            tool.set(XPMagic.LIGHTNING_CHARGE.get(), charge);
        }
        tool.set(XPMagic.MAX_EVOLUTION_POTENTIAL.get(), totalCapacity(input) * Config.evolutionPerCapacity);

        ToolStats.recompute(tool);
        return tool;
    }

    /**
     * Charge summed over the whole grid: every crystal that went into the tool gives up what it holds,
     * so two half-charged crystals are worth one full one. {@link ToolStats} caps what that buys.
     */
    private static int totalCharge(CraftingInput input) {
        int total = 0;
        for (ItemStack stack : input.items()) {
            total += stack.getOrDefault(XPMagic.LIGHTNING_CHARGE.get(), 0);
        }
        return total;
    }

    /**
     * Capacity summed the same way, which decides how far the tool can evolve — minus the lightning
     * share, which must not count twice.
     *
     * <p>A bolt raises a crystal's {@code xp_capacity} rather than sitting beside it (see
     * {@link com.gatheredsatyr53.xpmagic.LightningChargingHandler}), and {@code lightning_charge} is
     * the record of how much of the capacity came from the sky. Summing capacity raw would let a
     * charged crystal buy both an immediate bonus and a higher ceiling off the same energy. Only what
     * the crystal owed to its own density — base plus the explosion's compaction — buys room to grow.
     *
     * <p>Note this reads the crystals' capacity but never copies the component onto the tool — see
     * {@link ToolStats} for why a tool must not carry {@code xp_capacity}.
     */
    private static int totalCapacity(CraftingInput input) {
        int total = 0;
        for (ItemStack stack : input.items()) {
            total += stack.getOrDefault(XPMagic.XP_CAPACITY.get(), 0)
                   - stack.getOrDefault(XPMagic.LIGHTNING_CHARGE.get(), 0);
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
