package com.gatheredsatyr53.xpmagic.datagen;

import java.util.List;
import java.util.Set;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class XPMagicBlockLoot extends BlockLootSubProvider {

    XPMagicBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        this.dropSelf(XPMagic.XP_KEEPING_MACHINE.get());
        this.dropSelf(XPMagic.POWDER_SEPARATOR.get());
        this.dropSelf(XPMagic.VIBRATION_STAND.get());
        this.dropSelf(XPMagic.POWDER_MIXER.get());

        this.dropSelf(XPMagic.KNOWLEDGE_LOG.get());
        this.dropSelf(XPMagic.KNOWLEDGE_SAPLING.get());
        // Like vanilla farmland, the seedbed yields dirt when dug — the magic is in the tilling, not the block.
        this.dropOther(XPMagic.TRUTH_FARMLAND.get(), Blocks.DIRT);
        // Leaves drop a Knowledge Sapling (and sticks) on the usual oak odds; shears/silk touch drop the leaf.
        this.add(XPMagic.KNOWLEDGE_LEAVES.get(),
            block -> this.createLeavesDrops(block, XPMagic.KNOWLEDGE_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(XPMagic.XP_KEEPING_MACHINE.get(), XPMagic.POWDER_SEPARATOR.get(), XPMagic.VIBRATION_STAND.get(),
            XPMagic.POWDER_MIXER.get(),
            XPMagic.KNOWLEDGE_LOG.get(), XPMagic.KNOWLEDGE_SAPLING.get(), XPMagic.TRUTH_FARMLAND.get(),
            XPMagic.KNOWLEDGE_LEAVES.get());
    }
}
