package com.gatheredsatyr53.xpmagic.datagen;

import com.gatheredsatyr53.xpmagic.XPMagic;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = XPMagic.MODID)
public final class XPMagicDataGen {

    // NeoForge 26.2 reworked GatherDataEvent: providers are added through createProvider(...) factories
    // (given the PackOutput and lookup) instead of generator.addProvider(includeServer(), ...), and
    // ExistingFileHelper is gone, so the tag providers no longer take it.
    @SubscribeEvent
    static void gatherData(GatherDataEvent.Server event) {
        event.createProvider((GatherDataEvent.DataProviderFromOutputLookup<XPMagicRecipeProvider.Runner>)
            XPMagicRecipeProvider.Runner::new);

        event.createProvider((GatherDataEvent.DataProviderFromOutputLookup<LootTableProvider>)
            (output, lookup) -> new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(XPMagicBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));

        event.createProvider((GatherDataEvent.DataProviderFromOutputLookup<XPMagicBlockTagsProvider>)
            XPMagicBlockTagsProvider::new);

        event.createProvider((GatherDataEvent.DataProviderFromOutputLookup<XPMagicItemTagsProvider>)
            XPMagicItemTagsProvider::new);
    }
}
