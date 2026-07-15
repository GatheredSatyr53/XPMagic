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
import com.gatheredsatyr53.xpmagic.item.ChargedToolRecipe;
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
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

@Mod(XPMagic.MODID)
public final class XPMagic {

    public static final String MODID = "xpmagic";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    // Loader-agnostic ResourceKey builders for the vanilla Properties.setId(...) calls below,
    // replacing Forge's DeferredRegister.key(String) convenience.
    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, name));
    }

    private static ResourceKey<Block> blockKey(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, name));
    }

    //<editor-fold desc="Data Components">

    // Experience stored inside an XP Cocktail; written by the XP Keeping Machine
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredExp>> STORED_EXP = DATA_COMPONENTS.register("stored_exp",
                                                                                                           () -> DataComponentType.<StoredExp>builder()
            .persistent(StoredExp.CODEC)
            .networkSynchronized(StoredExp.STREAM_CODEC)
            .build()
    );

    // Fixed XP capacity of a matrix item; baked in as a default component below
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> XP_CAPACITY = DATA_COMPONENTS.register("xp_capacity",
        () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    );

    // xp_capacity a Memory Crystal gained from absorbing lightning, tracked apart from the
    // explosion's compaction so both contributions can be shown separately. Also drives the glint.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LIGHTNING_CHARGE = DATA_COMPONENTS.register("lightning_charge",
        () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    );

    // Ticks an item has spent sitting in soul fire (blue flame), accumulated by SoulFireHandler to pace
    // how fast it burns xp_capacity off a transforming crystal. Doubles as the "this item was touched by
    // blue flame" flag: any stack carrying a non-zero value has been in soul fire.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SOUL_FIRE_TIME = DATA_COMPONENTS.register("soul_fire_time",
        () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> EVOLUTION_POTENTIAL = DATA_COMPONENTS.register("evolution_potential",
        () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MAX_EVOLUTION_POTENTIAL = DATA_COMPONENTS.register("max_evolution_potential",
                                                                                                                                            () -> DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    );

    // What one step of evolution is worth on this tool, in whatever stat its profile spends on (see
    // ToolStats): attack damage for a weapon, mining efficiency for a digger. A component rather than a
    // config key because the right value is a property of the individual tool — chiefly of how many
    // crystals it takes, since that sets how many steps it will ever reach — so a datapack retunes one
    // tool, or grants evolution to a tool from another mod, without this mod knowing it exists.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> EVOLUTION_GAIN = DATA_COMPONENTS.register("evolution_gain",
        () -> DataComponentType.<Float>builder()
            .persistent(Codec.FLOAT)
            .networkSynchronized(ByteBufCodecs.FLOAT)
            .build()
    );

    // Owner recorded on a Player Key; the machine drains XP from this player
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PlayerOwner>> PLAYER_OWNER = DATA_COMPONENTS.register("owner",
                                                                                                               () -> DataComponentType.<PlayerOwner>builder()
                                                                                                                                      .persistent(PlayerOwner.CODEC)
                                                                                                                                      .networkSynchronized(PlayerOwner.STREAM_CODEC)
                                                                                                                                      .build()
    );

    //</editor-fold>

    //<editor-fold desc="Items">

    public static final DeferredHolder<Item, Item> MEMORY_POWDER = ITEMS.register("memory_powder",
        () -> new Item(new Item.Properties()
            .setId(itemKey("memory_powder"))
            .component(XP_CAPACITY.get(), 10)));

    // Fractions produced by the Powder Separator. Their xp_capacity is both what the XP Keeping
    // Machine drains and what the separator's budget spends; the three must sum to <= MEMORY_POWDER's
    // capacity (10) so separating a portion can never create XP — no dupe.
    public static final DeferredHolder<Item, Item> COARSE_POWDER = ITEMS.register("coarse_powder",
        () -> new Item(new Item.Properties().setId(itemKey("coarse_powder")).component(XP_CAPACITY.get(), 5)));

    public static final DeferredHolder<Item, Item> MEDIUM_POWDER = ITEMS.register("medium_powder",
        () -> new Item(new Item.Properties().setId(itemKey("medium_powder")).component(XP_CAPACITY.get(), 2)));

    public static final DeferredHolder<Item, Item> FINE_POWDER = ITEMS.register("fine_powder",
        () -> new Item(new Item.Properties().setId(itemKey("fine_powder")).component(XP_CAPACITY.get(), 1)));

    // fireResistant so soul fire neither burns nor ignites it: SoulFireHandler transforms a crystal
    // that sits in soul fire, and vanilla fire mechanics would otherwise destroy it long before the
    // transformation finishes (also means it survives lava).
    public static final DeferredHolder<Item, Item> MEMORY_CRYSTAL = ITEMS.register("memory_crystal",
        () -> new Item(new Item.Properties()
            .setId(itemKey("memory_crystal"))
            .fireResistant()
            .component(XP_CAPACITY.get(), 20)));

    // Also fireResistant so the finished crystal doesn't burn up while still lying in the soul fire.
    public static final DeferredHolder<Item, Item> TIME_CRYSTAL = ITEMS.register("time_crystal",
        () -> new Item(new Item.Properties()
            .setId(itemKey("time_crystal"))
            .fireResistant()));

    public static final DeferredHolder<Item, Item> TIME_CRYSTAL_WAFER = ITEMS.register("time_crystal_wafer",
        () -> new Item(new Item.Properties()
            .setId(itemKey("time_crystal_wafer"))
            .fireResistant()));

    public static final DeferredHolder<Item, Item> TIME_CRYSTAL_ROD = ITEMS.register("time_crystal_rod",
        () -> new Item(new Item.Properties()
            .setId(itemKey("time_crystal_rod"))
            .fireResistant()));

    public static final DeferredHolder<Item, Item> PROCESSING_CHIP = ITEMS.register("processing_chip",
        () -> new Item(new Item.Properties().setId(itemKey("processing_chip"))));

    public static final DeferredHolder<Item, Item> MEMORY_CHIP = ITEMS.register("memory_chip",
        () -> new Item(new Item.Properties().setId(itemKey("memory_chip"))));

    public static final DeferredHolder<Item, Item> PLAYER_KEY = ITEMS.register("player_key",
        () -> new PlayerKeyItem(new Item.Properties().setId(itemKey("player_key")).stacksTo(1)));

    public static final DeferredHolder<Item, Item> XP_COCKTAIL = ITEMS.register("xp_cocktail",
        () -> new Item(new Item.Properties()
            .setId(itemKey("xp_cocktail"))
            .stacksTo(1)
            .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            .usingConvertsTo(Items.GLASS_BOTTLE)
        ));

    // Tools carved from a Memory Crystal. Repaired by the crystal itself (tag below).
    public static final TagKey<Item> MEMORY_CRYSTAL_TOOL_MATERIALS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "memory_crystal_tool_materials"));

    // The two evolution profiles (see ToolStats). Both lightning_charge and evolution_potential pay out
    // along whichever profile a tool belongs to: weapons grow attack damage and earn potential from
    // kills, diggers grow mining speed and earn it from blocks they are the correct tool for. A tool in
    // neither tag simply never evolves, so these tags are what makes the whole mechanic opt-in.
    public static final TagKey<Item> EVOLVING_WEAPONS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "evolving_weapons"));

    public static final TagKey<Item> EVOLVING_DIGGERS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "evolving_diggers"));

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
    //   sword 7 -> 8.75 (baseline 4.75), pickaxe 5 -> 6.25 (2.25), axe 9 -> 11.25 (7.25),
    //   shovel 5.5 -> 6.875 (2.875).
    // Attack speed baselines are the vanilla per-type values (-2.4 / -2.8 / -3.0).
    //
    // Each also carries an evolution_gain: what one step of growth pays out. The values are set against
    // how many steps each tool can reach, which follows from how many crystals its recipe takes (see
    // ChargedToolRecipe) — so the weapons land near +4.0 damage apiece at full growth and neither
    // out-evolves the other, and the diggers reach +12 mining efficiency, between Efficiency III and IV
    // on vanilla's scale, leaving the enchantment worth putting on top.
    public static final DeferredHolder<Item, Item> MEMORY_CRYSTAL_SWORD = ITEMS.register("memory_crystal_sword",
        () -> new Item(new Item.Properties()
            .setId(itemKey("memory_crystal_sword"))
            .sword(MEMORY_CRYSTAL_MATERIAL, 4.75F, -2.4F)
            .component(EVOLUTION_GAIN.get(), 0.5F) // 2 crystals -> ~8 steps -> ~+4.0 damage
            .fireResistant()));

    public static final DeferredHolder<Item, Item> MEMORY_CRYSTAL_PICKAXE = ITEMS.register("memory_crystal_pickaxe",
        () -> new Item(new Item.Properties()
            .setId(itemKey("memory_crystal_pickaxe"))
            .pickaxe(MEMORY_CRYSTAL_MATERIAL, 2.25F, -2.8F)
            .component(EVOLUTION_GAIN.get(), 1.0F) // 3 crystals -> ~12 steps -> ~+12 mining efficiency
            .fireResistant()));

    // AxeItem (not a plain Item) for the strip/scrape/wax-off behaviour; its constructor applies
    // .axe(...) to the Properties itself, so we pass bare Properties here.
    public static final DeferredHolder<Item, Item> MEMORY_CRYSTAL_AXE = ITEMS.register("memory_crystal_axe",
        () -> new AxeItem(MEMORY_CRYSTAL_MATERIAL, 7.25F, -3.0F, new Item.Properties()
            .setId(itemKey("memory_crystal_axe"))
            .component(EVOLUTION_GAIN.get(), 0.33F) // 3 crystals -> ~12 steps -> ~+4.0 damage, as the sword
            .fireResistant()));

    // ShovelItem for the same reason as AxeItem: the path-flattening and campfire-dousing
    // behaviour lives in the class, and its constructor applies .shovel(...) itself.
    // The +25% damage is dead weight on a shovel; fireResistant is what earns it its place.
    public static final DeferredHolder<Item, Item> MEMORY_CRYSTAL_SHOVEL = ITEMS.register("memory_crystal_shovel",
        () -> new ShovelItem(MEMORY_CRYSTAL_MATERIAL, 2.875F, -3.0F, new Item.Properties()
            .setId(itemKey("memory_crystal_shovel"))
            .component(EVOLUTION_GAIN.get(), 3.0F) // 1 crystal -> ~4 steps -> ~+12, level with the pickaxe
            .fireResistant()));

    //</editor-fold>

    //<editor-fold desc="Blocks">

    public static final DeferredHolder<Block, Block> XP_KEEPING_MACHINE = BLOCKS.register("xp_keeping_machine",
        () -> new XPKeepingMachineBlock(BlockBehaviour.Properties.of()
                                                                 .setId(blockKey("xp_keeping_machine"))
                                                                 .mapColor(MapColor.METAL)
                                                                 .sound(SoundType.METAL)
                                                                 .requiresCorrectToolForDrops()
                                                                 .strength(15.0F)
                                                                 .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0)
        ));

    public static final DeferredHolder<Block, Block> POWDER_SEPARATOR = BLOCKS.register("powder_separator",
        () -> new PowderSeparatorBlock(BlockBehaviour.Properties.of()
                                                                 .setId(blockKey("powder_separator"))
                                                                 .mapColor(MapColor.METAL)
                                                                 .sound(SoundType.COBWEB)
                                                                 .requiresCorrectToolForDrops()
                                                                 .strength(15.0F)
        ));

    public static final DeferredHolder<Block, Block> POWDER_MIXER = BLOCKS.register("powder_mixer",
        () -> new PowderMixerBlock(BlockBehaviour.Properties.of()
                                                            .setId(blockKey("powder_mixer"))
                                                            .mapColor(MapColor.METAL)
                                                            .sound(SoundType.METAL)
                                                            .requiresCorrectToolForDrops()
                                                            .strength(15.0F)
        ));

    public static final DeferredHolder<Block, Block> VIBRATION_STAND = BLOCKS.register("vibration_stand",
        () -> new VibrationStandBlock(BlockBehaviour.Properties.of()
                                                              .setId(blockKey("vibration_stand"))
                                                              .mapColor(MapColor.METAL)
                                                              .sound(SoundType.METAL)
                                                              .requiresCorrectToolForDrops()
                                                              .strength(15.0F)
                                                              .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 10 : 0)
        ));

    public static final DeferredHolder<Block, Block> TIME_CRYSTAL_BLOCK = BLOCKS.register("time_crystal_block",
        () -> new Block(BlockBehaviour.Properties.of()
                                                 .setId(blockKey("time_crystal_block"))
                                                 .mapColor(MapColor.DIAMOND)
                                                 .requiresCorrectToolForDrops()
                                                 .strength(6.25F, 7.5F)
                                                 .sound(SoundType.METAL)));

    //</editor-fold>

    //<editor-fold desc="BlockItems">

    public static final DeferredHolder<Item, Item> XP_KEEPING_MACHINE_ITEM = ITEMS.register("xp_keeping_machine",
        () -> new BlockItem(XP_KEEPING_MACHINE.get(), new Item.Properties().setId(itemKey("xp_keeping_machine"))
                                                                           .useBlockDescriptionPrefix()));

    public static final DeferredHolder<Item, Item> POWDER_SEPARATOR_ITEM = ITEMS.register("powder_separator",
        () -> new BlockItem(POWDER_SEPARATOR.get(), new Item.Properties()
            .setId(itemKey("powder_separator"))
            .useBlockDescriptionPrefix()));

    public static final DeferredHolder<Item, Item> POWDER_MIXER_ITEM = ITEMS.register("powder_mixer",
        () -> new BlockItem(POWDER_MIXER.get(), new Item.Properties()
            .setId(itemKey("powder_mixer"))
            .useBlockDescriptionPrefix()));

    public static final DeferredHolder<Item, Item> VIBRATION_STAND_ITEM = ITEMS.register("vibration_stand",
        () -> new BlockItem(VIBRATION_STAND.get(), new Item.Properties()
            .setId(itemKey("vibration_stand"))
            .useBlockDescriptionPrefix()));

    public static final DeferredHolder<Item, Item> TIME_CRYSTAL_BLOCK_ITEM = ITEMS.register("time_crystal_block",
         () -> new BlockItem(TIME_CRYSTAL_BLOCK.get(), new Item.Properties()
            .setId(itemKey("time_crystal_block"))
            .useBlockDescriptionPrefix()));

    //</editor-fold>

    //<editor-fold desc="BlockEntities">

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<XPKeepingMachineBlockEntity>> XP_KEEPING_MACHINE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("xp_keeping_machine",
            () -> new BlockEntityType<>(XPKeepingMachineBlockEntity::new, java.util.Set.of(XP_KEEPING_MACHINE.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowderSeparatorBlockEntity>> POWDER_SEPARATOR_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("powder_separator",
            () -> new BlockEntityType<>(PowderSeparatorBlockEntity::new, java.util.Set.of(POWDER_SEPARATOR.get())));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VibrationStandBlockEntity>> VIBRATION_STAND_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("vibration_stand",
            () -> new BlockEntityType<>(VibrationStandBlockEntity::new, java.util.Set.of(VIBRATION_STAND.get())));

    //</editor-fold>

    // Custom looping vibration sound played by the running Vibration Stand
    public static final DeferredHolder<SoundEvent, SoundEvent> VIBRATION_SOUND = SOUND_EVENTS.register("vibration",
        () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "vibration")));

    //<editor-fold desc="Menus">

    public static final DeferredHolder<MenuType<?>, MenuType<XPKeepingMachineMenu>> XP_KEEPING_MACHINE_MENU =
        MENU_TYPES.register("xp_keeping_machine",
            () -> new MenuType<>(XPKeepingMachineMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<PowderSeparatorMenu>> POWDER_SEPARATOR_MENU =
        MENU_TYPES.register("powder_separator",
            () -> new MenuType<>(PowderSeparatorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<PowderMixerMenu>> POWDER_MIXER_MENU =
            MENU_TYPES.register("powder_mixer",
                                () -> new MenuType<>(PowderMixerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    //</editor-fold>

    // Shaped crafting that carries lightning_charge from the crystals onto the weapon. No RecipeType of
    // our own: it is ordinary crafting-table crafting (RecipeType.CRAFTING), only the serializer is ours.
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChargedToolRecipe>> CHARGED_TOOL_RECIPE =
        RECIPE_SERIALIZERS.register("charged_tool", () -> ChargedToolRecipe.SERIALIZER);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> XPMAGIC_TAB = CREATIVE_MODE_TABS.register("xpmagic",
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
                output.accept(MEMORY_CRYSTAL_SHOVEL.get());
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

    public XPMagic(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        com.gatheredsatyr53.xpmagic.gametest.XPMagicGameTests.init(modEventBus);

        modEventBus.addListener(this::registerCapabilities);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // Expose each machine's Container inventory as an item-handler capability so hoppers and other
    // automation can insert/extract. NeoForge 26.2 uses the resource transfer API here:
    // VanillaContainerWrapper adapts the vanilla Container to a ResourceHandler<ItemResource>,
    // honouring the container's canPlaceItem rules for insertion.
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, XP_KEEPING_MACHINE_BLOCK_ENTITY.get(),
            (blockEntity, side) -> VanillaContainerWrapper.of(blockEntity.getInventory()));
        event.registerBlockEntity(Capabilities.Item.BLOCK, POWDER_SEPARATOR_BLOCK_ENTITY.get(),
            (blockEntity, side) -> VanillaContainerWrapper.of(blockEntity.getInventory()));
    }
}
