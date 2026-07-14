package com.gatheredsatyr53.xpmagic;

import com.gatheredsatyr53.xpmagic.block.PowderMixerBlock;
import com.gatheredsatyr53.xpmagic.block.PowderSeparatorBlock;
import com.gatheredsatyr53.xpmagic.block.VibrationStandBlock;
import com.gatheredsatyr53.xpmagic.block.XPKeepingMachineBlock;
import com.gatheredsatyr53.xpmagic.block.entity.PowderSeparatorBlockEntity;
import com.gatheredsatyr53.xpmagic.block.entity.VibrationStandBlockEntity;
import com.gatheredsatyr53.xpmagic.block.entity.XPKeepingMachineBlockEntity;
import com.gatheredsatyr53.xpmagic.inventory.PowderMixerMenu;
import com.gatheredsatyr53.xpmagic.inventory.PowderSeparatorMenu;
import com.gatheredsatyr53.xpmagic.inventory.XPKeepingMachineMenu;
import com.gatheredsatyr53.xpmagic.item.PlayerKeyItem;
import com.gatheredsatyr53.xpmagic.nbt.PlayerOwner;
import com.gatheredsatyr53.xpmagic.nbt.StoredExp;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
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
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    //<editor-fold desc="Data Components">

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

    // xp_capacity a Memory Crystal gained from absorbing lightning, tracked apart from the
    // explosion's compaction so both contributions can be shown separately. Also drives the glint.
    public static final RegistryObject<DataComponentType<Integer>> LIGHTNING_CHARGE = DATA_COMPONENTS.register("lightning_charge",
        () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    );

    // Ticks an item has spent sitting in soul fire (blue flame), accumulated by SoulFireHandler to pace
    // how fast it burns xp_capacity off a transforming crystal. Doubles as the "this item was touched by
    // blue flame" flag: any stack carrying a non-zero value has been in soul fire.
    public static final RegistryObject<DataComponentType<Integer>> SOUL_FIRE_TIME = DATA_COMPONENTS.register("soul_fire_time",
        () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    );

    // Owner recorded on a Player Key; the machine drains XP from this player
    public static final RegistryObject<DataComponentType<PlayerOwner>> PLAYER_OWNER = DATA_COMPONENTS.register("owner",
                                                                                                               () -> DataComponentType.<PlayerOwner>builder()
                                                                                                                                      .persistent(PlayerOwner.CODEC)
                                                                                                                                      .networkSynchronized(PlayerOwner.STREAM_CODEC)
                                                                                                                                      .build()
    );

    //</editor-fold>

    //<editor-fold desc="Items">

    public static final RegistryObject<Item> MEMORY_POWDER = ITEMS.register("memory_powder",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("memory_powder"))
            .component(XP_CAPACITY.get(), 10)));

    // Fractions produced by the Powder Separator. Their xp_capacity is both what the XP Keeping
    // Machine drains and what the separator's budget spends; the three must sum to <= MEMORY_POWDER's
    // capacity (10) so separating a portion can never create XP — no dupe.
    public static final RegistryObject<Item> COARSE_POWDER = ITEMS.register("coarse_powder",
        () -> new Item(new Item.Properties().setId(ITEMS.key("coarse_powder")).component(XP_CAPACITY.get(), 5)));

    public static final RegistryObject<Item> MEDIUM_POWDER = ITEMS.register("medium_powder",
        () -> new Item(new Item.Properties().setId(ITEMS.key("medium_powder")).component(XP_CAPACITY.get(), 2)));

    public static final RegistryObject<Item> FINE_POWDER = ITEMS.register("fine_powder",
        () -> new Item(new Item.Properties().setId(ITEMS.key("fine_powder")).component(XP_CAPACITY.get(), 1)));

    // fireResistant so soul fire neither burns nor ignites it: SoulFireHandler transforms a crystal
    // that sits in soul fire, and vanilla fire mechanics would otherwise destroy it long before the
    // transformation finishes (also means it survives lava).
    public static final RegistryObject<Item> MEMORY_CRYSTAL = ITEMS.register("memory_crystal",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("memory_crystal"))
            .fireResistant()
            .component(XP_CAPACITY.get(), 20)));

    // Also fireResistant so the finished crystal doesn't burn up while still lying in the soul fire.
    public static final RegistryObject<Item> TIME_CRYSTAL = ITEMS.register("time_crystal",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("time_crystal"))
            .fireResistant()));

    public static final RegistryObject<Item> TIME_CRYSTAL_WAFER = ITEMS.register("time_crystal_wafer",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("time_crystal_wafer"))
            .fireResistant()));

    public static final RegistryObject<Item> TIME_CRYSTAL_ROD = ITEMS.register("time_crystal_rod",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("time_crystal_rod"))
            .fireResistant()));

    public static final RegistryObject<Item> PROCESSING_CHIP = ITEMS.register("processing_chip",
        () -> new Item(new Item.Properties().setId(ITEMS.key("processing_chip"))));

    public static final RegistryObject<Item> MEMORY_CHIP = ITEMS.register("memory_chip",
        () -> new Item(new Item.Properties().setId(ITEMS.key("memory_chip"))));

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

    // Tools carved from a Memory Crystal. Repaired by the crystal itself (tag below).
    public static final TagKey<Item> MEMORY_CRYSTAL_TOOL_MATERIALS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "memory_crystal_tool_materials"));

    // Diamond-grade material, but half the durability (780 vs 1561) — see attackDamageBaseline
    // per tool below for the +25% damage over diamond. attackDamageBonus stays at diamond's 3.0F;
    // the extra damage is added through each tool's baseline instead.
    public static final ToolMaterial MEMORY_CRYSTAL_MATERIAL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL, // mines everything diamond can
        780,                                  // durability: half of diamond's 1561
        8.0F,                                 // mining speed (diamond)
        3.0F,                                 // attackDamageBonus (diamond)
        10,                                   // enchantment value (diamond)
        MEMORY_CRYSTAL_TOOL_MATERIALS         // repair items
    );

    // Damage tooltip = 1 (player base) + baseline + material bonus (3.0). Targets are diamond +25%:
    //   sword 7 -> 8.75 (baseline 4.75), pickaxe 5 -> 6.25 (2.25), axe 9 -> 11.25 (7.25).
    // Attack speed baselines are the vanilla per-type values (-2.4 / -2.8 / -3.0).
    public static final RegistryObject<Item> MEMORY_CRYSTAL_SWORD = ITEMS.register("memory_crystal_sword",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("memory_crystal_sword"))
            .sword(MEMORY_CRYSTAL_MATERIAL, 4.75F, -2.4F)
            .fireResistant()));

    public static final RegistryObject<Item> MEMORY_CRYSTAL_PICKAXE = ITEMS.register("memory_crystal_pickaxe",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("memory_crystal_pickaxe"))
            .pickaxe(MEMORY_CRYSTAL_MATERIAL, 2.25F, -2.8F)
            .fireResistant()));

    // AxeItem (not a plain Item) for the strip/scrape/wax-off behaviour; its constructor applies
    // .axe(...) to the Properties itself, so we pass bare Properties here.
    public static final RegistryObject<Item> MEMORY_CRYSTAL_AXE = ITEMS.register("memory_crystal_axe",
        () -> new AxeItem(MEMORY_CRYSTAL_MATERIAL, 7.25F, -3.0F, new Item.Properties()
            .setId(ITEMS.key("memory_crystal_axe"))
            .fireResistant()));

    //</editor-fold>

    //<editor-fold desc="Blocks">

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

    public static final RegistryObject<Block> POWDER_MIXER = BLOCKS.register("powder_mixer",
        () -> new PowderMixerBlock(BlockBehaviour.Properties.of()
                                                            .setId(BLOCKS.key("powder_mixer"))
                                                            .mapColor(MapColor.METAL)
                                                            .sound(SoundType.METAL)
                                                            .requiresCorrectToolForDrops()
                                                            .strength(15.0F)
        ));

    public static final RegistryObject<Block> VIBRATION_STAND = BLOCKS.register("vibration_stand",
        () -> new VibrationStandBlock(BlockBehaviour.Properties.of()
                                                              .setId(BLOCKS.key("vibration_stand"))
                                                              .mapColor(MapColor.METAL)
                                                              .sound(SoundType.METAL)
                                                              .requiresCorrectToolForDrops()
                                                              .strength(15.0F)
                                                              .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 10 : 0)
        ));

    public static final RegistryObject<Block> TIME_CRYSTAL_BLOCK = BLOCKS.register("time_crystal_block",
        () -> new Block(BlockBehaviour.Properties.of()
                                                 .setId(BLOCKS.key("time_crystal_block"))
                                                 .mapColor(MapColor.DIAMOND)
                                                 .requiresCorrectToolForDrops()
                                                 .strength(6.25F, 7.5F)
                                                 .sound(SoundType.METAL)));

    //</editor-fold>

    //<editor-fold desc="BlockItems">

    public static final RegistryObject<Item> XP_KEEPING_MACHINE_ITEM = ITEMS.register("xp_keeping_machine",
        () -> new BlockItem(XP_KEEPING_MACHINE.get(), new Item.Properties().setId(ITEMS.key("xp_keeping_machine"))
                                                                           .useBlockDescriptionPrefix()));

    public static final RegistryObject<Item> POWDER_SEPARATOR_ITEM = ITEMS.register("powder_separator",
        () -> new BlockItem(POWDER_SEPARATOR.get(), new Item.Properties()
            .setId(ITEMS.key("powder_separator"))
            .useBlockDescriptionPrefix()));

    public static final RegistryObject<Item> POWDER_MIXER_ITEM = ITEMS.register("powder_mixer",
        () -> new BlockItem(POWDER_MIXER.get(), new Item.Properties()
            .setId(ITEMS.key("powder_mixer"))
            .useBlockDescriptionPrefix()));

    public static final RegistryObject<Item> VIBRATION_STAND_ITEM = ITEMS.register("vibration_stand",
        () -> new BlockItem(VIBRATION_STAND.get(), new Item.Properties()
            .setId(ITEMS.key("vibration_stand"))
            .useBlockDescriptionPrefix()));

    public static final RegistryObject<Item> TIME_CRYSTAL_BLOCK_ITEM = ITEMS.register("time_crystal_block",
         () -> new BlockItem(TIME_CRYSTAL_BLOCK.get(), new Item.Properties()
            .setId(ITEMS.key("time_crystal_block"))
            .useBlockDescriptionPrefix()));

    //</editor-fold>

    //<editor-fold desc="BlockEntities">

    public static final RegistryObject<BlockEntityType<XPKeepingMachineBlockEntity>> XP_KEEPING_MACHINE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("xp_keeping_machine",
            () -> new BlockEntityType<>(XPKeepingMachineBlockEntity::new, java.util.Set.of(XP_KEEPING_MACHINE.get())));

    public static final RegistryObject<BlockEntityType<PowderSeparatorBlockEntity>> POWDER_SEPARATOR_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("powder_separator",
            () -> new BlockEntityType<>(PowderSeparatorBlockEntity::new, java.util.Set.of(POWDER_SEPARATOR.get())));

    public static final RegistryObject<BlockEntityType<VibrationStandBlockEntity>> VIBRATION_STAND_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("vibration_stand",
            () -> new BlockEntityType<>(VibrationStandBlockEntity::new, java.util.Set.of(VIBRATION_STAND.get())));

    //</editor-fold>

    // Custom looping vibration sound played by the running Vibration Stand
    public static final RegistryObject<SoundEvent> VIBRATION_SOUND = SOUND_EVENTS.register("vibration",
        () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "vibration")));

    //<editor-fold desc="Menus">

    public static final RegistryObject<MenuType<XPKeepingMachineMenu>> XP_KEEPING_MACHINE_MENU =
        MENU_TYPES.register("xp_keeping_machine",
            () -> new MenuType<>(XPKeepingMachineMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<PowderSeparatorMenu>> POWDER_SEPARATOR_MENU =
        MENU_TYPES.register("powder_separator",
            () -> new MenuType<>(PowderSeparatorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<PowderMixerMenu>> POWDER_MIXER_MENU =
            MENU_TYPES.register("powder_mixer",
                                () -> new MenuType<>(PowderMixerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    //</editor-fold>

    public static final RegistryObject<CreativeModeTab> XPMAGIC_TAB = CREATIVE_MODE_TABS.register("xpmagic",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.xpmagic"))
            .icon(() -> XP_COCKTAIL.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(MEMORY_POWDER.get());
                output.accept(COARSE_POWDER.get());
                output.accept(MEDIUM_POWDER.get());
                output.accept(FINE_POWDER.get());
                output.accept(MEMORY_CRYSTAL.get());
                output.accept(TIME_CRYSTAL.get());
                output.accept(TIME_CRYSTAL_WAFER.get());
                output.accept(TIME_CRYSTAL_ROD.get());
                output.accept(MEMORY_CRYSTAL_SWORD.get());
                output.accept(MEMORY_CRYSTAL_PICKAXE.get());
                output.accept(MEMORY_CRYSTAL_AXE.get());
                output.accept(PROCESSING_CHIP.get());
                output.accept(MEMORY_CHIP.get());
                output.accept(XP_COCKTAIL.get());
                output.accept(PLAYER_KEY.get());
                output.accept(XP_KEEPING_MACHINE_ITEM.get());
                output.accept(POWDER_SEPARATOR_ITEM.get());
                output.accept(VIBRATION_STAND_ITEM.get());
                output.accept(POWDER_MIXER_ITEM.get());
                output.accept(TIME_CRYSTAL_BLOCK_ITEM.get());
            })
            .build());

    public XPMagic(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        BLOCKS.register(modBusGroup);
        ITEMS.register(modBusGroup);
        CREATIVE_MODE_TABS.register(modBusGroup);
        DATA_COMPONENTS.register(modBusGroup);
        BLOCK_ENTITIES.register(modBusGroup);
        SOUND_EVENTS.register(modBusGroup);
        MENU_TYPES.register(modBusGroup);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
