package com.gatheredsatyr53.xpmagic.datagen;

import java.util.List;
import java.util.Set;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

public final class XPMagicBlockLoot extends BlockLootSubProvider {

    XPMagicBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        this.dropSelf(XPMagic.XP_KEEPING_MACHINE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(XPMagic.XP_KEEPING_MACHINE.get());
    }
}
