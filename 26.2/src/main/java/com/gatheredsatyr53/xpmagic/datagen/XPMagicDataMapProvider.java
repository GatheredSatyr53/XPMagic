package com.gatheredsatyr53.xpmagic.datagen;

import com.gatheredsatyr53.xpmagic.XPMagic;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

public class XPMagicDataMapProvider extends DataMapProvider {

    protected XPMagicDataMapProvider(PackOutput packOutput,
                                     CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        this.builder(NeoForgeDataMaps.FURNACE_FUELS)
                .add(XPMagic.NOSTALGIC_COAL.getKey(), new FurnaceFuel(2400), false);
    }
}
