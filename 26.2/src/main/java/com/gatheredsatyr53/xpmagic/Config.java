package com.gatheredsatyr53.xpmagic;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = XPMagic.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // Explosion crafting: Memory Powders caught in a blast fuse into Memory Crystals.
    private static final ForgeConfigSpec.DoubleValue EXPLOSION_MIN_RADIUS = BUILDER
            .comment("Minimum explosion radius that can fuse Memory Crystals (TNT is 4.0, a creeper is 3.0).")
            .defineInRange("explosionMinRadius", 4.0, 0.0, 128.0);

    private static final ForgeConfigSpec.IntValue POWDER_PER_CRYSTAL = BUILDER
            .comment("Memory Powders consumed per Memory Crystal produced in an explosion.")
            .defineInRange("powderPerCrystal", 2, 1, 64);

    private static final ForgeConfigSpec.IntValue CRYSTAL_BONUS_MAX = BUILDER
            .comment("Maximum random bonus xp_capacity added to a fused Memory Crystal, rolled 0..max on top of its base capacity.")
            .defineInRange("crystalBonusMax", 4, 0, 64);

    // Lightning charging: a Memory Crystal struck by lightning absorbs the bolt's energy, gaining
    // xp_capacity up to a cap. Like the explosion's compaction, this is genuine external energy —
    // so the crystal legitimately ends up holding more than it did.
    private static final ForgeConfigSpec.IntValue LIGHTNING_CHARGE_PER_STRIKE = BUILDER
            .comment("xp_capacity a Memory Crystal gains from a single lightning strike.")
            .defineInRange("lightningChargePerStrike", 10, 0, 1024);

    private static final ForgeConfigSpec.IntValue LIGHTNING_MAX_CAPACITY = BUILDER
            .comment("Maximum xp_capacity a Memory Crystal can reach by lightning charging.")
            .defineInRange("lightningMaxCapacity", 40, 0, 4096);

    // Anvil crushing: a Memory Crystal an anvil lands on is shattered back into Memory Powder.
    // Compaction (and any lightning charge) scatters on impact, so this is deliberately lossy — the
    // reverse of the explosion's clean compaction — which keeps crystals worth fusing in the first place.
    private static final ForgeConfigSpec.BooleanValue CRUSH_CRYSTALS = BUILDER
            .comment("Whether an anvil landing on a Memory Crystal shatters it back into Memory Powder.")
            .define("crushCrystals", true);

    private static final ForgeConfigSpec.IntValue SHATTER_CAPACITY = BUILDER
            .comment("xp_capacity budget released per Memory Crystal an anvil shatters, then broken at random into powder fractions (largest favoured). Keep below a crystal's own xp_capacity so crushing stays a lossy recycle, never a dupe.")
            .defineInRange("shatterCapacity", 10, 0, 4096);

    // Soul-fire transformation: a Memory Crystal left in soul fire (blue flame) has its xp_capacity
    // burned away one point at a time; when it hits 0 the crystal transforms into a Time Crystal. The
    // soul_fire_time component also marks any item that has touched blue flame, for detection elsewhere.
    private static final ForgeConfigSpec.IntValue SOUL_FIRE_TICKS_PER_CAPACITY = BUILDER
            .comment("Ticks in soul fire to burn one point of xp_capacity off a Memory Crystal; at 0 capacity it becomes a Time Crystal (20 ticks = 1 second).")
            .defineInRange("soulFireTicksPerCapacity", 20, 1, 72000);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;
    public static double explosionMinRadius;
    public static int powderPerCrystal;
    public static int crystalBonusMax;
    public static int lightningChargePerStrike;
    public static int lightningMaxCapacity;
    public static boolean crushCrystals;
    public static int shatterCapacity;
    public static int soulFireTicksPerCapacity;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(Identifier.tryParse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
        explosionMinRadius = EXPLOSION_MIN_RADIUS.get();
        powderPerCrystal = POWDER_PER_CRYSTAL.get();
        crystalBonusMax = CRYSTAL_BONUS_MAX.get();
        lightningChargePerStrike = LIGHTNING_CHARGE_PER_STRIKE.get();
        lightningMaxCapacity = LIGHTNING_MAX_CAPACITY.get();
        crushCrystals = CRUSH_CRYSTALS.get();
        shatterCapacity = SHATTER_CAPACITY.get();
        soulFireTicksPerCapacity = SOUL_FIRE_TICKS_PER_CAPACITY.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(Identifier.tryParse(itemName)))
                .collect(Collectors.toSet());
    }
}
