package com.gatheredsatyr53.xpmagic;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(XPMagic.MODID)
public final class XPMagic {

    public static final String MODID = "xpmagic";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    // Experience stored inside an XP Cocktail; written by the XP Keeping Machine
    public static final RegistryObject<DataComponentType<StoredExp>> STORED_EXP = DATA_COMPONENTS.register("stored_exp",
        () -> DataComponentType.<StoredExp>builder()
            .persistent(StoredExp.CODEC)
            .networkSynchronized(StoredExp.STREAM_CODEC)
            .build()
    );

    public static final RegistryObject<Item> MEMORY_POWDER = ITEMS.register("memory_powder",
        () -> new Item(new Item.Properties().setId(ITEMS.key("memory_powder"))));

    public static final RegistryObject<Item> PROCESSING_CHIP = ITEMS.register("processing_chip",
        () -> new Item(new Item.Properties().setId(ITEMS.key("processing_chip"))));

    public static final RegistryObject<Item> XP_COCKTAIL = ITEMS.register("xp_cocktail",
        () -> new XPCocktailItem(new Item.Properties()
            .setId(ITEMS.key("xp_cocktail"))
            .stacksTo(1)
            .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            .usingConvertsTo(Items.GLASS_BOTTLE)
        ));

    public static final RegistryObject<CreativeModeTab> XPMAGIC_TAB = CREATIVE_MODE_TABS.register("xpmagic",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.xpmagic"))
            .icon(() -> XP_COCKTAIL.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(MEMORY_POWDER.get());
                output.accept(PROCESSING_CHIP.get());
                output.accept(XP_COCKTAIL.get());
            })
            .build());

    public XPMagic(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        BLOCKS.register(modBusGroup);
        ITEMS.register(modBusGroup);
        CREATIVE_MODE_TABS.register(modBusGroup);
        DATA_COMPONENTS.register(modBusGroup);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
