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
        this.dropSelf(XPMagic.POWDER_SEPARATOR.get());
        this.dropSelf(XPMagic.VIBRATION_STAND.get());
        this.dropSelf(XPMagic.POWDER_MIXER.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(XPMagic.XP_KEEPING_MACHINE.get(), XPMagic.POWDER_SEPARATOR.get(), XPMagic.VIBRATION_STAND.get(),
            XPMagic.POWDER_MIXER.get());
    }
}
