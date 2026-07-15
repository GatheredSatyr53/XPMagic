package com.gatheredsatyr53.xpmagic;

import com.gatheredsatyr53.xpmagic.gui.PowderMixerScreen;
import com.gatheredsatyr53.xpmagic.gui.PowderSeparatorScreen;
import com.gatheredsatyr53.xpmagic.gui.XPKeepingMachineScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = XPMagic.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class XPMagicClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(XPMagic.XP_KEEPING_MACHINE_MENU.get(), XPKeepingMachineScreen::new);
            MenuScreens.register(XPMagic.POWDER_SEPARATOR_MENU.get(), PowderSeparatorScreen::new);
            MenuScreens.register(XPMagic.POWDER_MIXER_MENU.get(), PowderMixerScreen::new);
        });
    }
}
