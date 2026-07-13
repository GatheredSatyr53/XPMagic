package com.gatheredsatyr53.xpmagic;

import com.gatheredsatyr53.xpmagic.gui.PowderMixingScreen;
import com.gatheredsatyr53.xpmagic.gui.PowderSeparatorScreen;
import com.gatheredsatyr53.xpmagic.gui.XPKeepingMachineScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = XPMagic.MODID, value = Dist.CLIENT)
public final class XPMagicClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(XPMagic.XP_KEEPING_MACHINE_MENU.get(), XPKeepingMachineScreen::new);
            MenuScreens.register(XPMagic.POWDER_SEPARATOR_MENU.get(), PowderSeparatorScreen::new);
            MenuScreens.register(XPMagic.POWDER_MIXER_MENU.get(), PowderMixingScreen::new);
        });
    }
}
