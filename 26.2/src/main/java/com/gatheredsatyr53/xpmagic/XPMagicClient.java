package com.gatheredsatyr53.xpmagic;

import com.gatheredsatyr53.xpmagic.gui.PowderMixerScreen;
import com.gatheredsatyr53.xpmagic.gui.PowderSeparatorScreen;
import com.gatheredsatyr53.xpmagic.gui.XPKeepingMachineScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = XPMagic.MODID, value = Dist.CLIENT)
public final class XPMagicClient {

    // NeoForge 26.2 made MenuScreens.register private; menu screens are now bound through
    // RegisterMenuScreensEvent (fired on the mod bus) instead of during FMLClientSetupEvent.
    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(XPMagic.XP_KEEPING_MACHINE_MENU.get(), XPKeepingMachineScreen::new);
        event.register(XPMagic.POWDER_SEPARATOR_MENU.get(), PowderSeparatorScreen::new);
        event.register(XPMagic.POWDER_MIXER_MENU.get(), PowderMixerScreen::new);
    }
}
