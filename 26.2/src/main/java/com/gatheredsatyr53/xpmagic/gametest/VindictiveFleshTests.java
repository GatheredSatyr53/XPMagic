package com.gatheredsatyr53.xpmagic.gametest;

import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.Config;
import com.gatheredsatyr53.xpmagic.VindictiveFleshHandler;
import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Feeding Vindictive Flesh to a Memory Pearl (see {@link VindictiveFleshHandler}). Exercises the pure
 * {@code feed} the anvil handler delegates to, so no anvil menu need be stood up. Registered in
 * {@link XPMagicGameTests}.
 */
public final class VindictiveFleshTests {

    private VindictiveFleshTests() {}

    /** A full stack fills flesh's whole slice on a fresh pearl — never past it — in one take. */
    public static final Consumer<GameTestHelper> FLESH_FILLS_ITS_SLICE = helper -> {
        ItemStack pearl = pearl(40); // base, no flesh yet
        VindictiveFleshHandler.Result r = VindictiveFleshHandler.feed(pearl, 100);
        helper.assertTrue(r != null, "feeding a base pearl should produce a result");

        int slice = Config.vindictiveCapacityCap;
        int expectedFlesh = (slice + Config.vindictiveCapacityPerFlesh - 1) / Config.vindictiveCapacityPerFlesh;

        helper.assertTrue(vindictive(r.output()) == slice,
            "flesh should give its whole slice " + slice + ", got " + vindictive(r.output()));
        helper.assertTrue(capacity(r.output()) == 40 + slice,
            "a base pearl fed to the slice should read " + (40 + slice) + ", got " + capacity(r.output()));
        helper.assertTrue(r.fleshUsed() == expectedFlesh,
            "should consume only the flesh the slice needed: expected " + expectedFlesh + ", got " + r.fleshUsed());
        helper.assertTrue(r.xpCost() == expectedFlesh * Config.vindictiveXpCostPerFlesh,
            "xp cost should scale with flesh used, got " + r.xpCost());

        // The input must be untouched — feed builds a copy, it does not mutate the stack in the slot.
        helper.assertTrue(capacity(pearl) == 40, "feed must not mutate the input pearl");

        helper.succeed();
    };

    /**
     * The point of the slice: flesh caps on what flesh has added, not on absolute capacity. A pearl
     * another source has already grown still gets its full flesh slice on top — so three independent
     * sources of {@code slice} compose to a round 100 whatever order they run in.
     */
    public static final Consumer<GameTestHelper> FLESH_IS_INDEPENDENT = helper -> {
        int slice = Config.vindictiveCapacityCap;

        // A pearl some other source raised to 80, with no flesh in it yet.
        ItemStack grown = pearl(80, 0);
        VindictiveFleshHandler.Result r = VindictiveFleshHandler.feed(grown, 100);
        helper.assertTrue(r != null, "flesh should still have room even on an already-grown pearl");
        helper.assertTrue(vindictive(r.output()) == slice,
            "flesh should give its full slice regardless of other sources, got " + vindictive(r.output()));
        helper.assertTrue(capacity(r.output()) == 80 + slice,
            "flesh should add on top of the other source: expected " + (80 + slice)
            + ", got " + capacity(r.output()) + " (capping absolute capacity would starve it here)");

        helper.succeed();
    };

    /** A partial handful raises capacity by exactly perFlesh each, and stacks onto prior feedings. */
    public static final Consumer<GameTestHelper> FLESH_ADDS_PER_UNIT = helper -> {
        int per = Config.vindictiveCapacityPerFlesh;

        ItemStack pearl = pearl(40);
        VindictiveFleshHandler.Result r = VindictiveFleshHandler.feed(pearl, 3);
        helper.assertTrue(r != null && r.fleshUsed() == 3, "three flesh should all be used on a fresh pearl");
        helper.assertTrue(capacity(r.output()) == 40 + 3 * per,
            "three flesh should add 3x" + per + ", got " + (capacity(r.output()) - 40));

        // Feeding the already-fed pearl again accumulates rather than resets.
        VindictiveFleshHandler.Result again = VindictiveFleshHandler.feed(r.output(), 2);
        helper.assertTrue(vindictive(again.output()) == (3 + 2) * per,
            "a second feeding should add to the first, got " + vindictive(again.output()));

        helper.succeed();
    };

    /** When flesh has already given its slice there is nothing more to buy, and it never overshoots. */
    public static final Consumer<GameTestHelper> FLESH_RESPECTS_ITS_SLICE = helper -> {
        int slice = Config.vindictiveCapacityCap;

        // A pearl that flesh has already filled to its slice: no further gain.
        helper.assertTrue(VindictiveFleshHandler.feed(pearl(40 + slice, slice), 64) == null,
            "a pearl whose flesh slice is full should yield nothing");

        // One point of slice left, a whole stack offered: takes exactly one flesh, lands on the slice.
        VindictiveFleshHandler.Result r = VindictiveFleshHandler.feed(pearl(40 + slice - 1, slice - 1), 64);
        helper.assertTrue(r != null && vindictive(r.output()) == slice,
            "a nearly-full slice should land on the slice, got "
            + (r == null ? "null" : vindictive(r.output())));
        helper.assertTrue(r.fleshUsed() == 1,
            "filling a single point of slice should cost a single flesh, not a whole stack, got " + r.fleshUsed());

        // A stack of pearls is ambiguous to raise, so feed declines it.
        ItemStack two = pearl(40);
        two.setCount(2);
        helper.assertTrue(VindictiveFleshHandler.feed(two, 10) == null,
            "feed should decline a stack of more than one pearl");

        helper.succeed();
    };

    /**
     * The other tests probe the pure arithmetic; this one drives the actual subscriber by posting an
     * {@link net.neoforged.neoforge.event.AnvilUpdateEvent} on the bus, so the {@code @EventBusSubscriber}
     * wiring, the type guard, and the three setters are all exercised end to end — not just {@code feed}.
     */
    public static final Consumer<GameTestHelper> ANVIL_WIRES_THE_FEED = helper -> {
        var player = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pearl = pearl(40);
        ItemStack flesh = new ItemStack(XPMagic.VINDICTIVE_FLESH.get(), 3);

        var event = new net.neoforged.neoforge.event.AnvilUpdateEvent(pearl, flesh, null, ItemStack.EMPTY, 0, 0, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);

        helper.assertTrue(capacity(event.getOutput()) == 40 + 3 * Config.vindictiveCapacityPerFlesh,
            "the anvil handler should raise the output pearl's capacity, got " + capacity(event.getOutput()));
        helper.assertTrue(event.getMaterialCost() == 3,
            "the handler should set the material cost to the flesh consumed, got " + event.getMaterialCost());

        // A pearl whose flesh slice is already full must leave the anvil to vanilla — output stays empty.
        int slice = Config.vindictiveCapacityCap;
        var noop = new net.neoforged.neoforge.event.AnvilUpdateEvent(
            pearl(40 + slice, slice), flesh, null, ItemStack.EMPTY, 0, 0, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(noop);
        helper.assertTrue(noop.getOutput().isEmpty(),
            "a full pearl should produce no anvil output, got " + noop.getOutput());

        helper.succeed();
    };

    private static ItemStack pearl(int capacity) {
        ItemStack stack = new ItemStack(XPMagic.MEMORY_PEARL.get());
        stack.set(XPMagic.XP_CAPACITY.get(), capacity);
        return stack;
    }

    /** A pearl at {@code capacity} of which {@code vindictive} was contributed by flesh already. */
    private static ItemStack pearl(int capacity, int vindictive) {
        ItemStack stack = pearl(capacity);
        stack.set(XPMagic.VINDICTIVE_CAPACITY.get(), vindictive);
        return stack;
    }

    private static int capacity(ItemStack stack) {
        return stack.getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
    }

    private static int vindictive(ItemStack stack) {
        return stack.getOrDefault(XPMagic.VINDICTIVE_CAPACITY.get(), 0);
    }
}
