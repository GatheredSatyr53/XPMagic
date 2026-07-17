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

    private static final ResourceKey<Consumer<GameTestHelper>> FLESH_FILLS_ITS_SLICE =
        register("flesh_fills_its_slice", VindictiveFleshTests.FLESH_FILLS_ITS_SLICE);

    private static final ResourceKey<Consumer<GameTestHelper>> FLESH_IS_INDEPENDENT =
        register("flesh_is_independent", VindictiveFleshTests.FLESH_IS_INDEPENDENT);

    private static final ResourceKey<Consumer<GameTestHelper>> FLESH_ADDS_PER_UNIT =
        register("flesh_adds_per_unit", VindictiveFleshTests.FLESH_ADDS_PER_UNIT);

    private static final ResourceKey<Consumer<GameTestHelper>> FLESH_RESPECTS_ITS_SLICE =
        register("flesh_respects_its_slice", VindictiveFleshTests.FLESH_RESPECTS_ITS_SLICE);

    private static final ResourceKey<Consumer<GameTestHelper>> ANVIL_WIRES_THE_FEED =
        register("anvil_wires_the_feed", VindictiveFleshTests.ANVIL_WIRES_THE_FEED);

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

        // The ore tests break a hundred coal ores apiece to sample a 25% swap, so they need room too.
        registerTest(event, environment, SATURATION_SWAPS_ORE_DROP, 200);
        registerTest(event, environment, PLAIN_PICKAXE_KEEPS_ORE_DROP, 200);

        registerTest(event, environment, FLESH_FILLS_ITS_SLICE);
        registerTest(event, environment, FLESH_IS_INDEPENDENT);
        registerTest(event, environment, FLESH_ADDS_PER_UNIT);
        registerTest(event, environment, FLESH_RESPECTS_ITS_SLICE);
        registerTest(event, environment, ANVIL_WIRES_THE_FEED);
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
