package com.gatheredsatyr53.xpmagic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Hands every player the guidebook once, the first time they log in, so the mod introduces itself
 * instead of staying invisible until its mechanics are stumbled upon. The "already given" flag lives
 * in the player's persistent data, so relogging never duplicates it.
 */
@EventBusSubscriber(modid = XPMagic.MODID)
public final class GuidebookGiveHandler {

    private static final String RECEIVED_KEY = "xpmagic_received_guidebook";

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        if (data.getBooleanOr(RECEIVED_KEY, false)) {
            return;
        }
        data.putBoolean(RECEIVED_KEY, true);
        player.getInventory().placeItemBackInInventory(new ItemStack(XPMagic.GUIDEBOOK.get()));
    }
}
