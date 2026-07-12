package com.gatheredsatyr53.xpmagic;

import com.gatheredsatyr53.xpmagic.block.PowderSeparatorBlock;
import com.gatheredsatyr53.xpmagic.block.VibrationStandBlock;
import com.gatheredsatyr53.xpmagic.block.XPKeepingMachineBlock;
import com.gatheredsatyr53.xpmagic.block.entity.PowderSeparatorBlockEntity;
import com.gatheredsatyr53.xpmagic.block.entity.VibrationStandBlockEntity;
import com.gatheredsatyr53.xpmagic.block.entity.XPKeepingMachineBlockEntity;
import com.gatheredsatyr53.xpmagic.inventory.PowderSeparatorMenu;
import com.gatheredsatyr53.xpmagic.inventory.XPKeepingMachineMenu;
import com.gatheredsatyr53.xpmagic.item.PlayerKeyItem;
import com.gatheredsatyr53.xpmagic.nbt.PlayerOwner;
import com.gatheredsatyr53.xpmagic.nbt.StoredExp;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
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
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    // Experience stored inside an XP Cocktail; written by the XP Keeping Machine
    public static final RegistryObject<DataComponentType<StoredExp>> STORED_EXP = DATA_COMPONENTS.register("stored_exp",
                                                                                                           () -> DataComponentType.<StoredExp>builder()
            .persistent(StoredExp.CODEC)
            .networkSynchronized(StoredExp.STREAM_CODEC)
            .build()
    );

    // Fixed XP capacity of a matrix item; baked in as a default component below
    public static final RegistryObject<DataComponentType<Integer>> XP_CAPACITY = DATA_COMPONENTS.register("xp_capacity",
        () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    );

    public static final RegistryObject<Item> MEMORY_POWDER = ITEMS.register("memory_powder",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("memory_powder"))
            .component(XP_CAPACITY.get(), 10)));

    // Fractions produced by the Powder Separator; capacity lives in the recipe, not on the item
    public static final RegistryObject<Item> COARSE_POWDER = ITEMS.register("coarse_powder",
        () -> new Item(new Item.Properties().setId(ITEMS.key("coarse_powder"))));

    public static final RegistryObject<Item> MEDIUM_POWDER = ITEMS.register("medium_powder",
        () -> new Item(new Item.Properties().setId(ITEMS.key("medium_powder"))));

    public static final RegistryObject<Item> FINE_POWDER = ITEMS.register("fine_powder",
        () -> new Item(new Item.Properties().setId(ITEMS.key("fine_powder"))));

    public static final RegistryObject<Item> PROCESSING_CHIP = ITEMS.register("processing_chip",
        () -> new Item(new Item.Properties().setId(ITEMS.key("processing_chip"))));

    public static final RegistryObject<Item> MEMORY_CHIP = ITEMS.register("memory_chip",
        () -> new Item(new Item.Properties().setId(ITEMS.key("memory_chip"))));

    // Owner recorded on a Player Key; the machine drains XP from this player
    public static final RegistryObject<DataComponentType<PlayerOwner>> PLAYER_OWNER = DATA_COMPONENTS.register("owner",
        () -> DataComponentType.<PlayerOwner>builder()
            .persistent(PlayerOwner.CODEC)
            .networkSynchronized(PlayerOwner.STREAM_CODEC)
            .build()
    );

    public static final RegistryObject<Item> PLAYER_KEY = ITEMS.register("player_key",
        () -> new PlayerKeyItem(new Item.Properties().setId(ITEMS.key("player_key")).stacksTo(1)));

    public static final RegistryObject<Item> XP_COCKTAIL = ITEMS.register("xp_cocktail",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("xp_cocktail"))
            .stacksTo(1)
            .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            .usingConvertsTo(Items.GLASS_BOTTLE)
        ));

    public static final RegistryObject<Block> XP_KEEPING_MACHINE = BLOCKS.register("xp_keeping_machine",
        () -> new XPKeepingMachineBlock(BlockBehaviour.Properties.of()
                                                                 .setId(BLOCKS.key("xp_keeping_machine"))
                                                                 .mapColor(MapColor.METAL)
                                                                 .sound(SoundType.METAL)
                                                                 .requiresCorrectToolForDrops()
                                                                 .strength(15.0F)
                                                                 .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0)
        ));

    public static final RegistryObject<Block> POWDER_SEPARATOR = BLOCKS.register("powder_separator",
        () -> new PowderSeparatorBlock(BlockBehaviour.Properties.of()
                                                                 .setId(BLOCKS.key("powder_separator"))
                                                                 .mapColor(MapColor.METAL)
                                                                 .sound(SoundType.COBWEB)
                                                                 .requiresCorrectToolForDrops()
                                                                 .strength(15.0F)
        ));

    public static final RegistryObject<Item> XP_KEEPING_MACHINE_ITEM = ITEMS.register("xp_keeping_machine",
        () -> new BlockItem(XP_KEEPING_MACHINE.get(), new Item.Properties()
            .setId(ITEMS.key("xp_keeping_machine"))
            .useBlockDescriptionPrefix()));

    public static final RegistryObject<Item> POWDER_SEPARATOR_ITEM = ITEMS.register("powder_separator",
        () -> new BlockItem(POWDER_SEPARATOR.get(), new Item.Properties()
            .setId(ITEMS.key("powder_separator"))
            .useBlockDescriptionPrefix()));

    public static final RegistryObject<Block> VIBRATION_STAND = BLOCKS.register("vibration_stand",
        () -> new VibrationStandBlock(BlockBehaviour.Properties.of()
                                                              .setId(BLOCKS.key("vibration_stand"))
                                                              .mapColor(MapColor.METAL)
                                                              .sound(SoundType.METAL)
                                                              .requiresCorrectToolForDrops()
                                                              .strength(15.0F)
                                                              .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 10 : 0)
        ));

    public static final RegistryObject<Item> VIBRATION_STAND_ITEM = ITEMS.register("vibration_stand",
        () -> new BlockItem(VIBRATION_STAND.get(), new Item.Properties()
            .setId(ITEMS.key("vibration_stand"))
            .useBlockDescriptionPrefix()));

    public static final RegistryObject<BlockEntityType<XPKeepingMachineBlockEntity>> XP_KEEPING_MACHINE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("xp_keeping_machine",
            () -> new BlockEntityType<>(XPKeepingMachineBlockEntity::new, java.util.Set.of(XP_KEEPING_MACHINE.get())));

    public static final RegistryObject<BlockEntityType<PowderSeparatorBlockEntity>> POWDER_SEPARATOR_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("powder_separator",
            () -> new BlockEntityType<>(PowderSeparatorBlockEntity::new, java.util.Set.of(POWDER_SEPARATOR.get())));

    public static final RegistryObject<BlockEntityType<VibrationStandBlockEntity>> VIBRATION_STAND_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("vibration_stand",
            () -> new BlockEntityType<>(VibrationStandBlockEntity::new, java.util.Set.of(VIBRATION_STAND.get())));

    public static final RegistryObject<MenuType<XPKeepingMachineMenu>> XP_KEEPING_MACHINE_MENU =
        MENU_TYPES.register("xp_keeping_machine",
            () -> new MenuType<>(XPKeepingMachineMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<PowderSeparatorMenu>> POWDER_SEPARATOR_MENU =
        MENU_TYPES.register("powder_separator",
            () -> new MenuType<>(PowderSeparatorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<CreativeModeTab> XPMAGIC_TAB = CREATIVE_MODE_TABS.register("xpmagic",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.xpmagic"))
            .icon(() -> XP_COCKTAIL.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(MEMORY_POWDER.get());
                output.accept(COARSE_POWDER.get());
                output.accept(MEDIUM_POWDER.get());
                output.accept(FINE_POWDER.get());
                output.accept(PROCESSING_CHIP.get());
                output.accept(MEMORY_CHIP.get());
                output.accept(XP_COCKTAIL.get());
                output.accept(PLAYER_KEY.get());
                output.accept(XP_KEEPING_MACHINE_ITEM.get());
                output.accept(POWDER_SEPARATOR_ITEM.get());
                output.accept(VIBRATION_STAND_ITEM.get());
            })
            .build());

    public XPMagic(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        BLOCKS.register(modBusGroup);
        ITEMS.register(modBusGroup);
        CREATIVE_MODE_TABS.register(modBusGroup);
        DATA_COMPONENTS.register(modBusGroup);
        BLOCK_ENTITIES.register(modBusGroup);
        MENU_TYPES.register(modBusGroup);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
