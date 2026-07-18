package com.gatheredsatyr53.xpmagic;

import com.gatheredsatyr53.xpmagic.item.ToolStats;
import com.gatheredsatyr53.xpmagic.nbt.StoredExp;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Client-side tooltip rendering driven by XPMagic data components. */
@EventBusSubscriber(modid = XPMagic.MODID, value = Dist.CLIENT)
public final class XPMagicClientEvents {

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Integer capacity = stack.get(XPMagic.XP_CAPACITY.get());
        if (capacity != null) {
            event.getToolTip().add(Component.translatable("tooltip.xpmagic.xp_capacity", capacity)
                                            .withStyle(ChatFormatting.GRAY));

            // Capacity above the item's baked-in base comes from separate sources, each tracked on its
            // own component: a lightning charge and the pearl-feeding fuels. Whatever is left over base
            // after subtracting all of those is an explosion's compaction. Show each as its own line.
            int base = stack.getItem().getDefaultInstance().getOrDefault(XPMagic.XP_CAPACITY.get(), capacity);
            int lightning = stack.getOrDefault(XPMagic.LIGHTNING_CHARGE.get(), 0);
            int fromFuels = 0;
            for (PearlFeedingHandler.PearlFuel fuel : PearlFeedingHandler.FUELS) {
                fromFuels += stack.getOrDefault(fuel.tally().get(), 0);
            }
            int compaction = capacity - base - lightning - fromFuels;
            if (compaction > 0) {
                event.getToolTip().add(Component.translatable("tooltip.xpmagic.compaction_bonus", compaction)
                                                .withStyle(ChatFormatting.AQUA));
            }
            if (lightning > 0) {
                event.getToolTip().add(Component.translatable("tooltip.xpmagic.lightning_charge", lightning)
                                                .withStyle(ChatFormatting.YELLOW));
            }
            for (PearlFeedingHandler.PearlFuel fuel : PearlFeedingHandler.FUELS) {
                int contributed = stack.getOrDefault(fuel.tally().get(), 0);
                if (contributed > 0) {
                    event.getToolTip().add(Component.translatable(fuel.tooltipKey(), contributed)
                                                    .withStyle(fuel.color()));
                }
            }
        }
        // Soul fire burns one point of capacity per soulFireTicksPerCapacity ticks in the flame, so
        // total time / interval is how much this crystal has lost to it.
        int burned = stack.getOrDefault(XPMagic.SOUL_FIRE_TIME.get(), 0) / Config.soulFireTicksPerCapacity;
        if (burned > 0) {
            event.getToolTip().add(Component.translatable("tooltip.xpmagic.burned_away", burned)
                                            .withStyle(ChatFormatting.DARK_AQUA));
        }
        // Evolution: the step count is what the player feels (vanilla already draws the attribute line
        // the steps bought), while the raw points show how close the next one is. A tool that has run
        // out of room reads as maxed rather than showing a bar that will never move again.
        int ceiling = stack.getOrDefault(XPMagic.MAX_EVOLUTION_POTENTIAL.get(), 0);
        if (ceiling > 0) {
            int steps = ToolStats.steps(stack);
            int maxSteps = ToolStats.maxSteps(stack);
            event.getToolTip().add(Component.translatable("tooltip.xpmagic.evolution", steps, maxSteps)
                                            .withStyle(ChatFormatting.LIGHT_PURPLE));
            int potential = stack.getOrDefault(XPMagic.EVOLUTION_POTENTIAL.get(), 0);
            if (steps < maxSteps) {
                event.getToolTip().add(Component.translatable("tooltip.xpmagic.evolution_progress",
                                                              potential, ceiling)
                                                .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        StoredExp storedExp = stack.get(XPMagic.STORED_EXP.get());
        if (storedExp != null) {
            event.getToolTip().add(Component.translatable("item.xpmagic.xp_cocktail.stored_exp", storedExp.amount())
                                            .withStyle(ChatFormatting.GRAY));
        }
    }
}
