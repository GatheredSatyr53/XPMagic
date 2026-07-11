package com.gatheredsatyr53.xpmagic.datagen;

import java.util.concurrent.CompletableFuture;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jspecify.annotations.Nullable;

public final class XPMagicBlockTagsProvider extends TagsProvider<Block> {

    public XPMagicBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                    @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.BLOCK, lookupProvider, XPMagic.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(XPMagic.XP_KEEPING_MACHINE.getKey());
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(XPMagic.XP_KEEPING_MACHINE.getKey());
    }
}
