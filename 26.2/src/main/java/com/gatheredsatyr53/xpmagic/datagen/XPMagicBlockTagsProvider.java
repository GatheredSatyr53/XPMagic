package com.gatheredsatyr53.xpmagic.datagen;

import java.util.concurrent.CompletableFuture;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

public final class XPMagicBlockTagsProvider extends TagsProvider<Block> {

    public XPMagicBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.BLOCK, lookupProvider, XPMagic.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(XPMagic.XP_KEEPING_MACHINE.getKey())
            .add(XPMagic.POWDER_SEPARATOR.getKey())
            .add(XPMagic.VIBRATION_STAND.getKey());
        this.tag(BlockTags.NEEDS_STONE_TOOL)
            .add(XPMagic.XP_KEEPING_MACHINE.getKey())
            .add(XPMagic.POWDER_SEPARATOR.getKey())
            .add(XPMagic.VIBRATION_STAND.getKey());

        // The Tree of Knowledge's wood behaves like ordinary logs: axe-mined and part of the LOGS tag.
        this.tag(BlockTags.MINEABLE_WITH_AXE)
            .add(XPMagic.KNOWLEDGE_LOG.getKey());
        this.tag(BlockItemTags.LOGS_THAT_BURN.block())
            .add(XPMagic.KNOWLEDGE_LOG.getKey());

        // Leaves: the LEAVES tag (so they count as leaves everywhere), hoe-mineable, and sword-efficient.
        this.tag(BlockTags.LEAVES)
            .add(XPMagic.KNOWLEDGE_LEAVES.getKey());
        this.tag(BlockTags.MINEABLE_WITH_HOE)
            .add(XPMagic.KNOWLEDGE_LEAVES.getKey());
        this.tag(BlockTags.SWORD_EFFICIENT)
            .add(XPMagic.KNOWLEDGE_LEAVES.getKey());

        this.tag(BlockItemTags.SAPLINGS.block())
            .add(XPMagic.KNOWLEDGE_SAPLING.getKey());

        // The seedbed is dug with a shovel, like farmland.
        this.tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .add(XPMagic.TRUTH_FARMLAND.getKey());

        // So a growing Tree of Knowledge can push its trunk up through the sapling's own spot and its
        // foliage — exactly how vanilla trees treat saplings and leaves.
        this.tag(BlockTags.REPLACEABLE_BY_TREES)
            .add(XPMagic.KNOWLEDGE_SAPLING.getKey())
            .add(XPMagic.KNOWLEDGE_LEAVES.getKey());
    }
}
