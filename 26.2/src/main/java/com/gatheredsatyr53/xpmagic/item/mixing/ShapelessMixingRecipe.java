package com.gatheredsatyr53.xpmagic.item.mixing;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.RecipeMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shapeless mixing recipe: 1..3 order-independent powder components and an optional modifier.
 * The distinction between component slots and the modifier slot is enforced by the menu's slots;
 * this recipe only checks the component multiset and the modifier ingredient.
 */
public record ShapelessMixingRecipe(
        String group,
        CraftingBookCategory category,
        ItemStackTemplate result,
        List<Ingredient> components,
        Optional<Ingredient> modifier
) implements MixingRecipe {

    public static final MapCodec<ShapelessMixingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    net.minecraft.util.ExtraCodecs.NON_EMPTY_STRING.optionalFieldOf("group", "").forGetter(ShapelessMixingRecipe::group),
                    CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(ShapelessMixingRecipe::category),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(ShapelessMixingRecipe::result),
                    Ingredient.CODEC.listOf().fieldOf("components").forGetter(ShapelessMixingRecipe::components),
                    Ingredient.CODEC.optionalFieldOf("modifier").forGetter(ShapelessMixingRecipe::modifier)
            ).apply(instance, ShapelessMixingRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessMixingRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShapelessMixingRecipe::group,
            CraftingBookCategory.STREAM_CODEC, ShapelessMixingRecipe::category,
            ItemStackTemplate.STREAM_CODEC, ShapelessMixingRecipe::result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), ShapelessMixingRecipe::components,
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), ShapelessMixingRecipe::modifier,
            ShapelessMixingRecipe::new
    );

    public static final RecipeSerializer<ShapelessMixingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(final MixingInput input, final Level level) {
        // Modifier: present recipe requires a match; absent recipe requires an empty slot.
        if (this.modifier.isPresent()) {
            if (!this.modifier.get().test(input.modifier())) {
                return false;
            }
        } else if (!input.modifier().isEmpty()) {
            return false;
        }

        // Quantity-based, non-positional: expand every component stack into single-item units,
        // pooled across the component slots. A repeated ingredient in the recipe means that many
        // required items. Matching is an exact 1:1 assignment, so surplus items block the recipe.
        List<ItemStack> units = new ArrayList<>();
        for (ItemStack stack : input.components()) {
            for (int i = 0; i < stack.getCount(); i++) {
                units.add(stack);
            }
        }

        if (units.size() != this.components.size()) {
            return false;
        }

        return RecipeMatcher.findMatches(units, this.components) != null;
    }

    @Override
    public ItemStack assemble(final MixingInput input) {
        return this.result.create();
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public RecipeSerializer<? extends MixingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo() {
        List<Ingredient> all = new ArrayList<>(this.components);
        this.modifier.ifPresent(all::add);
        return PlacementInfo.create(all);
    }
}
