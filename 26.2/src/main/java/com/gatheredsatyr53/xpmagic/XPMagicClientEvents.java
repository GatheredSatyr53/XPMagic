package com.gatheredsatyr53.xpmagic;

import com.gatheredsatyr53.xpmagic.nbt.StoredExp;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-side tooltip rendering driven by XPMagic data components. */
@Mod.EventBusSubscriber(modid = XPMagic.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class XPMagicClientEvents {

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Integer capacity = stack.get(XPMagic.XP_CAPACITY.get());
        if (capacity != null) {
            event.getToolTip().add(Component.translatable("tooltip.xpmagic.xp_capacity", capacity)
                                            .withStyle(ChatFormatting.GRAY));

            // Capacity above the item's baked-in base means the powder was compacted (e.g. fused
            // by an explosion) — surface the surplus so the denser crystal reads as such.
            int base = stack.getItem().getDefaultInstance().getOrDefault(XPMagic.XP_CAPACITY.get(), capacity);
            int bonus = capacity - base;
            if (bonus > 0) {
                event.getToolTip().add(Component.translatable("tooltip.xpmagic.compaction_bonus", bonus)
                                                .withStyle(ChatFormatting.AQUA));
            }
        }
        StoredExp storedExp = stack.get(XPMagic.STORED_EXP.get());
        if (storedExp != null) {
            event.getToolTip().add(Component.translatable("item.xpmagic.xp_cocktail.stored_exp", storedExp.amount())
                                            .withStyle(ChatFormatting.GRAY));
        }
    }
}
