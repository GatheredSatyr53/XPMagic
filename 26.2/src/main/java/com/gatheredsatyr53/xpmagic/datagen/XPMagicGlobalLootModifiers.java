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

import java.util.Optional;
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
        swap("zombie_vindictive_flesh", "entities/zombie", Items.ROTTEN_FLESH, XPMagic.VINDICTIVE_FLESH.get(), 0.25F, lookup);
        swap("coal_ore_nostalgic_coal", "blocks/coal_ore", Items.COAL, XPMagic.NOSTALGIC_COAL.get(), 0.1F, lookup);
        swap("deepslate_coal_ore_nostalgic_coal", "blocks/deepslate_coal_ore", Items.COAL, XPMagic.NOSTALGIC_COAL.get(), 0.1F, lookup);

        // Truth grain is a bonus, not a swap: dig with Saturation and keep the block too. One entry per
        // loot table because a modifier's conditions are AND-ed, so tables cannot share a single file.
        // The dirt family and mud a shovel turns over, plus gravel. grass_block is in the list because
        // mining it runs blocks/grass_block (dropping a dirt item), not blocks/dirt.
        for (String block : new String[] {
            "dirt", "grass_block", "coarse_dirt", "podzol", "rooted_dirt", "mycelium", "dirt_path", "farmland", "mud"
        }) {
            bonus(block + "_truth_grain", "blocks/" + block, XPMagic.TRUTH_GRAIN.get(), 0.01F, lookup);
        }
        bonus("gravel_truth_grain", "blocks/gravel", XPMagic.TRUTH_GRAIN.get(), 0.01F, lookup);
    }

    /** Replace mode: each {@code from} drop has {@code chance} of coming back as {@code to} instead. */
    private void swap(String name, String lootTable, Item from, Item to, float chance, HolderLookup.Provider lookup) {
        register(name, lootTable, Optional.of(from), to, chance, false, lookup);
    }

    /** Add mode: the table's own drops stay, and {@code chance} appends one {@code to} on top. */
    private void bonus(String name, String lootTable, Item to, float chance, HolderLookup.Provider lookup) {
        register(name, lootTable, Optional.empty(), to, chance, true, lookup);
    }

    private void register(String name, String lootTable, Optional<Item> from, Item to, float chance,
                          boolean add, HolderLookup.Provider lookup) {
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
                chance,
                add
            )
        );
    }
}
