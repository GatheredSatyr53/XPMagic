package com.gatheredsatyr53.xpmagic.datagen;

import java.util.List;
import java.util.Set;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = XPMagic.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class XPMagicDataGen {

    @SubscribeEvent
    static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        var lookup = event.getLookupProvider();

        generator.addProvider(event.includeServer(),
            (DataProvider.Factory<XPMagicRecipeProvider.Runner>) output -> new XPMagicRecipeProvider.Runner(output, lookup));

        generator.addProvider(event.includeServer(),
            (DataProvider.Factory<LootTableProvider>) output -> new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(XPMagicBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));

        generator.addProvider(event.includeServer(),
            (DataProvider.Factory<XPMagicBlockTagsProvider>) output -> new XPMagicBlockTagsProvider(output, lookup, event.getExistingFileHelper()));
    }
}
