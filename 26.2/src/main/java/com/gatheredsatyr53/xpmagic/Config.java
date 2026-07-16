package com.gatheredsatyr53.xpmagic;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@EventBusSubscriber(modid = XPMagic.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    // Explosion crafting: Memory Powders caught in a blast fuse into Memory Crystals.
    private static final ModConfigSpec.DoubleValue EXPLOSION_MIN_RADIUS = BUILDER
            .comment("Minimum explosion radius that can fuse Memory Crystals (TNT is 4.0, a creeper is 3.0).")
            .defineInRange("explosionMinRadius", 4.0, 0.0, 128.0);

    private static final ModConfigSpec.IntValue POWDER_PER_CRYSTAL = BUILDER
            .comment("Memory Powders consumed per Memory Crystal produced in an explosion.")
            .defineInRange("powderPerCrystal", 2, 1, 64);

    private static final ModConfigSpec.IntValue CRYSTAL_BONUS_MAX = BUILDER
            .comment("Maximum random bonus xp_capacity added to a fused Memory Crystal, rolled 0..max on top of its base capacity.")
            .defineInRange("crystalBonusMax", 4, 0, 64);

    // Lightning charging: a Memory Crystal struck by lightning absorbs the bolt's energy, gaining
    // xp_capacity up to a cap. Like the explosion's compaction, this is genuine external energy —
    // so the crystal legitimately ends up holding more than it did.
    private static final ModConfigSpec.IntValue LIGHTNING_CHARGE_PER_STRIKE = BUILDER
            .comment("xp_capacity a Memory Crystal gains from a single lightning strike.")
            .defineInRange("lightningChargePerStrike", 10, 0, 1024);

    private static final ModConfigSpec.IntValue LIGHTNING_MAX_CAPACITY = BUILDER
            .comment("Maximum xp_capacity a Memory Crystal can reach by lightning charging.")
            .defineInRange("lightningMaxCapacity", 40, 0, 4096);

    // Charged tools: a tool forged from charged crystals pools their lightning_charge and spends it
    // immediately — on attack damage for a weapon, on mining speed for a digging tool. A crystal tops
    // out near 20 charge (base capacity 20 against a 40 cap), so a two-crystal sword reaches roughly
    // +2.0 damage and a three-crystal axe runs into the cap below.
    private static final ModConfigSpec.DoubleValue LIGHTNING_DAMAGE_PER_CHARGE = BUILDER
            .comment("Attack damage a weapon gains per point of lightning_charge carried over from the Memory Crystals it was forged from.")
            .defineInRange("lightningDamagePerCharge", 0.05, 0.0, 100.0);

    private static final ModConfigSpec.DoubleValue LIGHTNING_MAX_DAMAGE_BONUS = BUILDER
            .comment("Ceiling on the attack damage lightning_charge can buy a single weapon, however much charge went into it.")
            .defineInRange("lightningMaxDamageBonus", 3.0, 0.0, 1024.0);

    // The digging counterpart of the two above, deliberately given the same defaults: mining_efficiency
    // adds onto a base mining speed of 8, attack damage onto a base of 8.75, so +3.0 lands at roughly
    // the same +35% on either profile.
    private static final ModConfigSpec.DoubleValue LIGHTNING_MINING_EFFICIENCY_PER_CHARGE = BUILDER
            .comment("Mining efficiency a digging tool gains per point of lightning_charge carried over from the Memory Crystals it was forged from.")
            .defineInRange("lightningMiningEfficiencyPerCharge", 0.05, 0.0, 100.0);

    private static final ModConfigSpec.DoubleValue LIGHTNING_MAX_MINING_EFFICIENCY_BONUS = BUILDER
            .comment("Ceiling on the mining efficiency lightning_charge can buy a single digging tool, however much charge went into it.")
            .defineInRange("lightningMaxMiningEfficiencyBonus", 3.0, 0.0, 1024.0);

    // Tool evolution: a tool grows with use. Every kill (weapons) or correctly-mined block (digging
    // tools) adds a point of evolution_potential; each evolutionStepCost points is one step of growth.
    // How far a tool can grow is decided at the forge: max_evolution_potential is the summed xp_capacity
    // of the crystals it was made from, times evolutionPerCapacity. Denser crystals — the ones a good
    // explosion compacted, or a bolt of lightning charged — therefore make a tool with more room to grow.
    // Note the two mechanics stay separate on purpose: lightning_charge pays out at once and does not
    // touch the cap, while capacity buys potential and never pays out at once.
    private static final ModConfigSpec.IntValue EVOLUTION_PER_CAPACITY = BUILDER
            .comment("Points of max_evolution_potential granted per point of summed xp_capacity of the crystals a tool was forged from.")
            .defineInRange("evolutionPerCapacity", 20, 0, 4096);

    private static final ModConfigSpec.IntValue EVOLUTION_STEP_COST = BUILDER
            .comment("Points of evolution_potential per step of growth. The number of steps a tool can ever reach is max_evolution_potential / this.")
            .defineInRange("evolutionStepCost", 100, 1, 100000);

    // What the PRIMARY stat's step is worth is NOT configured here: it rides on each tool as the
    // evolution_gain component (see XPMagic's item registrations), because it has to differ per tool
    // rather than per profile. The axe is forged from three crystals and the sword from two, so the axe
    // reaches ~12 steps against the sword's ~8; paying them the same per step would have evolution
    // quietly widen the damage gap the two were balanced around. Being a component, a datapack can
    // retune any single tool — or give evolution to a tool this mod never registered — without a config
    // key per item.

    // The two MILESTONE stats, on the other hand, are per-profile and live here. A weapon unlocks
    // knockback then luck; a digger unlocks reach then luck. Each is a target bonus reached at full
    // growth, in the milestone attribute's OWN units — which is exactly why it cannot be derived from
    // evolution_gain: +2 blocks of reach and +12 mining efficiency are both "a lot", and one rate
    // cannot price both (pricing reach off the primary gain once sent it past +8 blocks). The
    // thresholds at which they start (0.3 and 0.7 of full growth) live in ToolStats.
    private static final ModConfigSpec.DoubleValue EVOLUTION_REACH_BONUS = BUILDER
            .comment("Block reach a fully grown digging tool gains at its first milestone, on top of the base 4.5.")
            .defineInRange("evolutionReachBonus", 2.0, 0.0, 59.5);

    private static final ModConfigSpec.DoubleValue EVOLUTION_KNOCKBACK_BONUS = BUILDER
            .comment("Attack knockback a fully grown weapon gains at its first milestone (1.0 is about one level of the Knockback enchantment).")
            .defineInRange("evolutionKnockbackBonus", 1.0, 0.0, 1024.0);

    private static final ModConfigSpec.DoubleValue EVOLUTION_LUCK_BONUS = BUILDER
            .comment("Luck a fully grown tool gains at its second milestone (shared by weapons and diggers).")
            .defineInRange("evolutionLuckBonus", 1.0, 0.0, 1024.0);

    // Anvil crushing: a Memory Crystal an anvil lands on is shattered back into Memory Powder.
    // Compaction (and any lightning charge) scatters on impact, so this is deliberately lossy — the
    // reverse of the explosion's clean compaction — which keeps crystals worth fusing in the first place.
    private static final ModConfigSpec.BooleanValue CRUSH_CRYSTALS = BUILDER
            .comment("Whether an anvil landing on a Memory Crystal shatters it back into Memory Powder.")
            .define("crushCrystals", true);

    private static final ModConfigSpec.IntValue SHATTER_CAPACITY = BUILDER
            .comment("xp_capacity budget released per Memory Crystal an anvil shatters, then broken at random into powder fractions (largest favoured). Keep below a crystal's own xp_capacity so crushing stays a lossy recycle, never a dupe.")
            .defineInRange("shatterCapacity", 10, 0, 4096);

    // Soul-fire transformation: a Memory Crystal left in soul fire (blue flame) has its xp_capacity
    // burned away one point at a time; when it hits 0 the crystal transforms into a Time Crystal. The
    // soul_fire_time component also marks any item that has touched blue flame, for detection elsewhere.
    private static final ModConfigSpec.IntValue SOUL_FIRE_TICKS_PER_CAPACITY = BUILDER
            .comment("Ticks in soul fire to burn one point of xp_capacity off a Memory Crystal; at 0 capacity it becomes a Time Crystal (20 ticks = 1 second).")
            .defineInRange("soulFireTicksPerCapacity", 20, 1, 72000);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;
    public static double explosionMinRadius;
    public static int powderPerCrystal;
    public static int crystalBonusMax;
    public static int lightningChargePerStrike;
    public static int lightningMaxCapacity;
    public static double lightningDamagePerCharge;
    public static double lightningMaxDamageBonus;
    public static double lightningMiningEfficiencyPerCharge;
    public static double lightningMaxMiningEfficiencyBonus;
    public static int evolutionPerCapacity;
    public static int evolutionStepCost;
    public static double evolutionReachBonus;
    public static double evolutionKnockbackBonus;
    public static double evolutionLuckBonus;
    public static boolean crushCrystals;
    public static int shatterCapacity;
    public static int soulFireTicksPerCapacity;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.tryParse(itemName));
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
        lightningDamagePerCharge = LIGHTNING_DAMAGE_PER_CHARGE.get();
        lightningMaxDamageBonus = LIGHTNING_MAX_DAMAGE_BONUS.get();
        lightningMiningEfficiencyPerCharge = LIGHTNING_MINING_EFFICIENCY_PER_CHARGE.get();
        lightningMaxMiningEfficiencyBonus = LIGHTNING_MAX_MINING_EFFICIENCY_BONUS.get();
        evolutionPerCapacity = EVOLUTION_PER_CAPACITY.get();
        evolutionStepCost = EVOLUTION_STEP_COST.get();
        evolutionReachBonus = EVOLUTION_REACH_BONUS.get();
        evolutionKnockbackBonus = EVOLUTION_KNOCKBACK_BONUS.get();
        evolutionLuckBonus = EVOLUTION_LUCK_BONUS.get();
        crushCrystals = CRUSH_CRYSTALS.get();
        shatterCapacity = SHATTER_CAPACITY.get();
        soulFireTicksPerCapacity = SOUL_FIRE_TICKS_PER_CAPACITY.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> BuiltInRegistries.ITEM.getValue(Identifier.tryParse(itemName)))
                .collect(Collectors.toSet());
    }
}
