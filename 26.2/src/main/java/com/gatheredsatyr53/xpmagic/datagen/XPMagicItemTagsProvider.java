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

        // Which stat a tool's lightning charge and evolution buy. The axe sits with the weapons: it
        // does mine wood, but its reason to exist is the damage.
        this.tag(XPMagic.EVOLVING_WEAPONS)
            .add(XPMagic.MEMORY_CRYSTAL_SWORD.getKey())
            .add(XPMagic.MEMORY_CRYSTAL_AXE.getKey());

        this.tag(XPMagic.EVOLVING_DIGGERS)
            .add(XPMagic.MEMORY_CRYSTAL_PICKAXE.getKey())
            .add(XPMagic.MEMORY_CRYSTAL_SHOVEL.getKey());
    }
}
