package com.gatheredsatyr53.xpmagic.datagen;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.loot.MemoryDropModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

/**
 * One entry per mob whose drop Saturation rewrites. Adding another pairing is a call here and a
 * lang line — the modifier itself stays untouched.
 */
public final class XPMagicGlobalLootModifiers extends GlobalLootModifierProvider {

    private final CompletableFuture<HolderLookup.Provider> registries;

    public XPMagicGlobalLootModifiers(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, XPMagic.MODID);
        this.registries = registries;
    }

    @Override
    protected void start() {
        HolderLookup.Provider lookup = this.registries.join();

        swap("enderman_memory_pearl", "entities/enderman", Items.ENDER_PEARL, XPMagic.MEMORY_PEARL.get(), 0.5F, lookup);
        swap("shulker_memory_pearl", "entities/shulker", Items.SHULKER_SHELL, XPMagic.MEMORY_PEARL.get(), 0.25F, lookup);
    }

    private void swap(String name, String lootTable, Item from, Item to, float chance, HolderLookup.Provider lookup) {
        this.add(
            name,
            new MemoryDropModifier(
                new LootItemCondition[] {
                    LootTableIdCondition.builder(Identifier.withDefaultNamespace(lootTable)).build()
                },
                net.neoforged.neoforge.common.loot.IGlobalLootModifier.DEFAULT_PRIORITY,
                lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(XPMagicEnchantmentProvider.SATURATION),
                from,
                to,
                chance
            )
        );
    }
}
