package com.gatheredsatyr53.xpmagic;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.RegistryObject;

public class RegistrationHelper {

    private RegistrationHelper() {}

    public static <T extends Recipe<?>> RegistryObject<RecipeType<T>> registerRecipeType(final String name) {
        return XPMagic.RECIPE_TYPES.register(name, () -> new RecipeType<T>() {
            @Override
            public String toString() {
                return name;
            }
        });
    }
}
