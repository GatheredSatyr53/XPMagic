package com.gatheredsatyr53.xpmagic.datagen;

import com.gatheredsatyr53.xpmagic.XPMagic;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class XPMagicItemTagsProvider extends TagsProvider<Item> {

    public XPMagicItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                    @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.ITEM, lookupProvider, XPMagic.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(XPMagic.POWDER_COMPONENT)
            .add(XPMagic.COARSE_POWDER.getKey())
            .add(XPMagic.MEDIUM_POWDER.getKey())
            .add(XPMagic.FINE_POWDER.getKey());
        this.tag(XPMagic.POWDER_MODIFIER)
            .add(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", "blaze_rod")))
            .add(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", "iron_nugget")))
            .add(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", "copper_nugget")));
    }
}
