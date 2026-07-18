package com.gatheredsatyr53.xpmagic.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Wires {@link EvolutionTests} into the game test framework.
 *
 * <p>Two registries are involved because 1.21.5 made game tests data-driven: the test <em>function</em>
 * (the Java that runs) lives in the built-in {@code test_function} registry and goes through the usual
 * {@link DeferredRegister}; the test <em>instance</em> (which function, which structure, how long) is a
 * dynamic registry entry and has to be added through {@link RegisterGameTestsEvent}.
 *
 * <p>The structure is vanilla's {@code minecraft:empty}, the same one {@code minecraft:always_pass}
 * uses. These tests only push item stacks around, so there is nothing to build in the world.
 */
@EventBusSubscriber(modid = XPMagic.MODID)
public final class XPMagicGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, XPMagic.MODID);

    private static final ResourceKey<Consumer<GameTestHelper>> CAP_EXCLUDES_LIGHTNING =
        register("cap_excludes_lightning", EvolutionTests.CAP_EXCLUDES_LIGHTNING);

    private static final ResourceKey<Consumer<GameTestHelper>> STEPS_RAISE_DAMAGE =
        register("steps_raise_damage", EvolutionTests.STEPS_RAISE_DAMAGE);

    private static final ResourceKey<Consumer<GameTestHelper>> RECOMPUTE_IS_PURE =
        register("recompute_is_pure", EvolutionTests.RECOMPUTE_IS_PURE);

    private static final ResourceKey<Consumer<GameTestHelper>> CEILING_IS_WHOLE_STEPS =
        register("ceiling_is_whole_steps", EvolutionTests.CEILING_IS_WHOLE_STEPS);

    private static final ResourceKey<Consumer<GameTestHelper>> MILESTONES_RAMP_IN_OWN_UNITS =
        register("milestones_ramp_in_own_units", EvolutionTests.MILESTONES_RAMP_IN_OWN_UNITS);

    private static final ResourceKey<Consumer<GameTestHelper>> KILL_GROWS_WEAPON =
        register("kill_grows_weapon", EvolutionTests.KILL_GROWS_WEAPON);

    private static final ResourceKey<Consumer<GameTestHelper>> MINING_GROWS_DIGGER =
        register("mining_grows_digger", EvolutionTests.MINING_GROWS_DIGGER);

    private static final ResourceKey<Consumer<GameTestHelper>> SATURATION_SWAPS_DROP =
        register("saturation_swaps_drop", LootTests.SATURATION_SWAPS_DROP);

    private static final ResourceKey<Consumer<GameTestHelper>> PLAIN_WEAPON_KEEPS_DROP =
        register("plain_weapon_keeps_drop", LootTests.PLAIN_WEAPON_KEEPS_DROP);

    private static final ResourceKey<Consumer<GameTestHelper>> PEARL_IS_AN_XP_STORE =
        register("pearl_is_an_xp_store", LootTests.PEARL_IS_AN_XP_STORE);

    private static final ResourceKey<Consumer<GameTestHelper>> SATURATION_SWAPS_ORE_DROP =
        register("saturation_swaps_ore_drop", LootTests.SATURATION_SWAPS_ORE_DROP);

    private static final ResourceKey<Consumer<GameTestHelper>> PLAIN_PICKAXE_KEEPS_ORE_DROP =
        register("plain_pickaxe_keeps_ore_drop", LootTests.PLAIN_PICKAXE_KEEPS_ORE_DROP);

    private static final ResourceKey<Consumer<GameTestHelper>> TRUTH_GRAIN_BONUS_ON_DIRT =
        register("truth_grain_bonus_on_dirt", LootTests.TRUTH_GRAIN_BONUS_ON_DIRT);

    private static final ResourceKey<Consumer<GameTestHelper>> TRUTH_GRAIN_BONUS_ON_GRASS =
        register("truth_grain_bonus_on_grass", LootTests.TRUTH_GRAIN_BONUS_ON_GRASS);

    private static final ResourceKey<Consumer<GameTestHelper>> TRUTH_GRAIN_BONUS_ON_GRAVEL =
        register("truth_grain_bonus_on_gravel", LootTests.TRUTH_GRAIN_BONUS_ON_GRAVEL);

    private static final ResourceKey<Consumer<GameTestHelper>> PLAIN_TOOL_NO_TRUTH_GRAIN =
        register("plain_tool_no_truth_grain", LootTests.PLAIN_TOOL_NO_TRUTH_GRAIN);

    private static final ResourceKey<Consumer<GameTestHelper>> EACH_FUEL_FILLS_ITS_SLICE =
        register("each_fuel_fills_its_slice", PearlFeedingTests.EACH_FUEL_FILLS_ITS_SLICE);

    private static final ResourceKey<Consumer<GameTestHelper>> FUELS_COMPOSE_TO_100 =
        register("fuels_compose_to_100", PearlFeedingTests.FUELS_COMPOSE_TO_100);

    private static final ResourceKey<Consumer<GameTestHelper>> FUELS_ARE_INDEPENDENT =
        register("fuels_are_independent", PearlFeedingTests.FUELS_ARE_INDEPENDENT);

    private static final ResourceKey<Consumer<GameTestHelper>> FUEL_RESPECTS_ITS_SLICE =
        register("fuel_respects_its_slice", PearlFeedingTests.FUEL_RESPECTS_ITS_SLICE);

    private static final ResourceKey<Consumer<GameTestHelper>> ANVIL_WIRES_EACH_FUEL =
        register("anvil_wires_each_fuel", PearlFeedingTests.ANVIL_WIRES_EACH_FUEL);

    private static final ResourceKey<Consumer<GameTestHelper>> SATURATION_HOE_TILLS_TRUTH =
        register("saturation_hoe_tills_truth", KnowledgeTreeTests.SATURATION_HOE_TILLS_TRUTH);

    private static final ResourceKey<Consumer<GameTestHelper>> PLAIN_HOE_TILLS_VANILLA =
        register("plain_hoe_tills_vanilla", KnowledgeTreeTests.PLAIN_HOE_TILLS_VANILLA);

    private static final ResourceKey<Consumer<GameTestHelper>> GRAIN_PLANTS_ON_TRUTH =
        register("grain_plants_on_truth", KnowledgeTreeTests.GRAIN_PLANTS_ON_TRUTH);

    private static final ResourceKey<Consumer<GameTestHelper>> GRAIN_INERT_OFF_TRUTH =
        register("grain_inert_off_truth", KnowledgeTreeTests.GRAIN_INERT_OFF_TRUTH);

    private static final ResourceKey<Consumer<GameTestHelper>> SAPLING_GROWS_TREE =
        register("sapling_grows_tree", KnowledgeTreeTests.SAPLING_GROWS_TREE);

    private static final ResourceKey<Consumer<GameTestHelper>> HOPPER_PULLS_FRACTION_NOT_POWDER =
        register("hopper_pulls_fraction_not_powder", MachineHopperTests.HOPPER_PULLS_FRACTION_NOT_POWDER);

    private static final ResourceKey<Consumer<GameTestHelper>> HOPPER_FEEDS_SEPARATOR_INPUT =
        register("hopper_feeds_separator_input", MachineHopperTests.HOPPER_FEEDS_SEPARATOR_INPUT);

    private static final ResourceKey<Consumer<GameTestHelper>> HOPPER_LEAVES_MACHINE_INPUTS =
        register("hopper_leaves_machine_inputs", MachineHopperTests.HOPPER_LEAVES_MACHINE_INPUTS);

    private static final ResourceKey<Consumer<GameTestHelper>> FUEL_GAUGE_QUANTISES =
        register("fuel_gauge_quantises", VibrationStandTests.FUEL_GAUGE_QUANTISES);

    private static final ResourceKey<Consumer<GameTestHelper>> HOPPER_FEEDS_STAND_FUEL =
        register("hopper_feeds_stand_fuel", VibrationStandTests.HOPPER_FEEDS_STAND_FUEL);

    private static final ResourceKey<Consumer<GameTestHelper>> HOPPER_CANT_DRAIN_STAND_FUEL =
        register("hopper_cant_drain_stand_fuel", VibrationStandTests.HOPPER_CANT_DRAIN_STAND_FUEL);

    private static final ResourceKey<Consumer<GameTestHelper>> STAND_BURNS_SLOT_FUEL =
        register("stand_burns_slot_fuel", VibrationStandTests.STAND_BURNS_SLOT_FUEL);

    private XPMagicGameTests() {}

    private static ResourceKey<Consumer<GameTestHelper>> register(String name, Consumer<GameTestHelper> function) {
        TEST_FUNCTIONS.register(name, () -> function);
        return ResourceKey.create(Registries.TEST_FUNCTION, EvolutionTests.id(name));
    }

    public static void init(net.neoforged.bus.api.IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        // Our own empty environment rather than minecraft:default: the event hands back a Holder for
        // what it registers, and TestData needs a Holder, not a key.
        Holder<TestEnvironmentDefinition<?>> environment =
            event.registerEnvironment(EvolutionTests.id("evolution"), new TestEnvironmentDefinition.AllOf(List.of()));

        registerTest(event, environment, CAP_EXCLUDES_LIGHTNING);
        registerTest(event, environment, STEPS_RAISE_DAMAGE);
        registerTest(event, environment, RECOMPUTE_IS_PURE);
        registerTest(event, environment, CEILING_IS_WHOLE_STEPS);
        registerTest(event, environment, MILESTONES_RAMP_IN_OWN_UNITS);
        registerTest(event, environment, KILL_GROWS_WEAPON);
        registerTest(event, environment, MINING_GROWS_DIGGER);

        // The loot tests kill a hundred endermen apiece to sample a 50% swap, so they need room to run.
        registerTest(event, environment, SATURATION_SWAPS_DROP, 200);
        registerTest(event, environment, PLAIN_WEAPON_KEEPS_DROP, 200);
        registerTest(event, environment, PEARL_IS_AN_XP_STORE);

        // The ore tests break hundreds of coal ores apiece to sample a 10% swap, so they need room too.
        registerTest(event, environment, SATURATION_SWAPS_ORE_DROP, 200);
        registerTest(event, environment, PLAIN_PICKAXE_KEEPS_ORE_DROP, 200);

        // The grain tests dig even more blocks apiece to sample a 5% bonus.
        registerTest(event, environment, TRUTH_GRAIN_BONUS_ON_DIRT, 200);
        registerTest(event, environment, TRUTH_GRAIN_BONUS_ON_GRASS, 200);
        registerTest(event, environment, TRUTH_GRAIN_BONUS_ON_GRAVEL, 200);
        registerTest(event, environment, PLAIN_TOOL_NO_TRUTH_GRAIN, 200);

        registerTest(event, environment, EACH_FUEL_FILLS_ITS_SLICE);
        registerTest(event, environment, FUELS_COMPOSE_TO_100);
        registerTest(event, environment, FUELS_ARE_INDEPENDENT);
        registerTest(event, environment, FUEL_RESPECTS_ITS_SLICE);
        registerTest(event, environment, ANVIL_WIRES_EACH_FUEL);

        registerTest(event, environment, SATURATION_HOE_TILLS_TRUTH);
        registerTest(event, environment, PLAIN_HOE_TILLS_VANILLA);
        registerTest(event, environment, GRAIN_PLANTS_ON_TRUTH);
        registerTest(event, environment, GRAIN_INERT_OFF_TRUTH);
        registerTest(event, environment, SAPLING_GROWS_TREE);

        // Hopper transfers run on an 8-tick cooldown, so these need room to settle before asserting.
        registerTest(event, environment, HOPPER_PULLS_FRACTION_NOT_POWDER, MachineHopperTests.MAX_TICKS);
        registerTest(event, environment, HOPPER_FEEDS_SEPARATOR_INPUT, MachineHopperTests.MAX_TICKS);
        registerTest(event, environment, HOPPER_LEAVES_MACHINE_INPUTS, MachineHopperTests.MAX_TICKS);

        // Pure quantisation check, runs to completion in the first tick.
        registerTest(event, environment, FUEL_GAUGE_QUANTISES);

        // The stand's furnace-model fuel handling: slot feeding, drain protection, and the burn cycle.
        registerTest(event, environment, HOPPER_FEEDS_STAND_FUEL, VibrationStandTests.MAX_TICKS);
        registerTest(event, environment, HOPPER_CANT_DRAIN_STAND_FUEL, VibrationStandTests.MAX_TICKS);
        registerTest(event, environment, STAND_BURNS_SLOT_FUEL, VibrationStandTests.MAX_TICKS);
    }

    private static void registerTest(RegisterGameTestsEvent event,
                                     Holder<TestEnvironmentDefinition<?>> environment,
                                     ResourceKey<Consumer<GameTestHelper>> function) {
        // maxTicks 1: these run to completion in the first tick
        registerTest(event, environment, function, 1);
    }

    private static void registerTest(RegisterGameTestsEvent event,
                                     Holder<TestEnvironmentDefinition<?>> environment,
                                     ResourceKey<Consumer<GameTestHelper>> function,
                                     int maxTicks) {
        Identifier name = function.identifier();
        event.registerTest(name, new FunctionGameTestInstance(function, new TestData<>(
            environment,
            Identifier.withDefaultNamespace("empty"), // nothing to build: these are item-stack tests
            maxTicks,
            1,             // setupTicks
            true,          // required: a failure should fail the run
            Rotation.NONE)));
    }
}
