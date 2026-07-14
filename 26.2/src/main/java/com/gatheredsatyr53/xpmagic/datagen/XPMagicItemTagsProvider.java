package com.gatheredsatyr53.xpmagic.datagen;

import java.util.concurrent.CompletableFuture;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jspecify.annotations.Nullable;

public final class XPMagicItemTagsProvider extends TagsProvider<Item> {

    public XPMagicItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                   @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.ITEM, lookupProvider, XPMagic.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Repair material for the Memory Crystal tools (referenced by MEMORY_CRYSTAL_MATERIAL).
        this.tag(XPMagic.MEMORY_CRYSTAL_TOOL_MATERIALS)
            .add(XPMagic.MEMORY_CRYSTAL.getKey());
    }
}
