package com.gatheredsatyr53.xpmagic.datagen;

import java.util.concurrent.CompletableFuture;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;

public final class XPMagicItemTagsProvider extends TagsProvider<Item> {

    public XPMagicItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.ITEM, lookupProvider, XPMagic.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Repair material for the Memory Crystal tools (referenced by MEMORY_CRYSTAL_MATERIAL).
        this.tag(XPMagic.MEMORY_CRYSTAL_TOOL_MATERIALS)
            .add(XPMagic.MEMORY_CRYSTAL.getKey());

        this.tag(XPMagic.TIME_CRYSTAL_ARMOR_MATERIALS)
            .add(XPMagic.TIME_CRYSTAL.getKey());

        // Which stat a tool's lightning charge and evolution buy. The axe sits with the weapons: it
        // does mine wood, but its reason to exist is the damage.
        this.tag(XPMagic.EVOLVING_WEAPONS)
            .add(XPMagic.MEMORY_CRYSTAL_SWORD.getKey())
            .add(XPMagic.MEMORY_CRYSTAL_AXE.getKey());

        this.tag(XPMagic.EVOLVING_DIGGERS)
            .add(XPMagic.MEMORY_CRYSTAL_PICKAXE.getKey())
            .add(XPMagic.MEMORY_CRYSTAL_SHOVEL.getKey());

        // The hoe joins EVOLVING_TOOLS directly, not through the two profile sub-tags: it must accept
        // the Saturation enchantment (supported_items = EVOLVING_TOOLS) yet never evolve, so it stays
        // out of EVOLVING_WEAPONS and EVOLVING_DIGGERS.
        this.tag(XPMagic.EVOLVING_TOOLS)
            .addTag(XPMagic.EVOLVING_WEAPONS)
            .addTag(XPMagic.EVOLVING_DIGGERS)
            .add(XPMagic.MEMORY_CRYSTAL_HOE.getKey());
    }
}
