package com.gatheredsatyr53.xpmagic;

import com.gatheredsatyr53.xpmagic.datagen.XPMagicEnchantmentProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Turns a Saturation hoe's tilling into Truth Farmland. When a Memory Crystal hoe enchanted with
 * {@code xpmagic:saturation} tills ground that would become plain farmland, the resulting soil is
 * {@link XPMagic#TRUTH_FARMLAND} instead — the only tilth a Grain of Truth roots in.
 *
 * <p>Riding NeoForge's tool-modification hook rather than replacing {@link net.minecraft.world.item.HoeItem}
 * keeps every vanilla tilling rule (which blocks are tillable, sounds, durability) intact; we only
 * swap the block the hoe was already about to leave, and only for our own enchanted hoe.
 */
@EventBusSubscriber(modid = XPMagic.MODID)
public final class TruthFarmlandHandler {

    // LOWEST so we have the final say over any other handler that reacts to the same till.
    //
    // NeoForge fires this event with the block's ORIGINAL state as the final state; vanilla's own
    // dirt -> farmland conversion only runs afterwards, and only if no handler changed anything (see
    // IBlockExtension#getToolModifiedState). So we can't wait to see "farmland" here — we recognise the
    // inputs vanilla would have turned into farmland (grass / dirt / dirt path, with air above) and set
    // Truth Farmland ourselves. Coarse and rooted dirt, which vanilla only turns to plain dirt, are
    // deliberately excluded: they were never a seedbed.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onToolModify(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated()) return; // a prediction pass; make no world changes
        if (event.getItemAbility() != ItemAbilities.HOE_TILL) return;

        BlockState original = event.getState();
        if (!original.is(Blocks.GRASS_BLOCK) && !original.is(Blocks.DIRT) && !original.is(Blocks.DIRT_PATH)) return;

        // Mirror vanilla's onlyIfAirAbove precondition for these tills.
        if (!event.getLevel().getBlockState(event.getPos().above()).isAir()) return;

        ItemStack tool = event.getHeldItemStack();
        if (!tool.is(XPMagic.MEMORY_CRYSTAL_HOE.get())) return;
        if (!hasSaturation(event, tool)) return;

        event.setFinalState(XPMagic.TRUTH_FARMLAND.get().defaultBlockState());
    }

    private static boolean hasSaturation(BlockEvent.BlockToolModificationEvent event, ItemStack tool) {
        Holder<Enchantment> saturation = event.getContext().getLevel().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(XPMagicEnchantmentProvider.SATURATION);
        return EnchantmentHelper.getItemEnchantmentLevel(saturation, tool) > 0;
    }
}
