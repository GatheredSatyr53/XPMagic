package com.gatheredsatyr53.xpmagic.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.Config;
import com.gatheredsatyr53.xpmagic.PearlFeedingHandler;
import com.gatheredsatyr53.xpmagic.PearlFeedingHandler.PearlFuel;
import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Feeding a Memory Pearl its three fuels (see {@link PearlFeedingHandler}). Exercises the pure
 * {@code feed} the anvil handler delegates to, plus one test that posts the real event on the bus.
 * Registered in {@link XPMagicGameTests}.
 */
public final class PearlFeedingTests {

    private PearlFeedingTests() {}

    /** Every fuel fills its own slice on a fresh pearl in one take, consuming only what the slice needs. */
    public static final Consumer<GameTestHelper> EACH_FUEL_FILLS_ITS_SLICE = helper -> {
        for (PearlFuel fuel : PearlFeedingHandler.FUELS) {
            int slice = fuel.sliceCap().getAsInt();
            int per = fuel.perItem().getAsInt();
            int expectedItems = (slice + per - 1) / per;

            PearlFeedingHandler.Result r = PearlFeedingHandler.feed(pearl(40), fuel, 1000);
            String who = fuel.tooltipKey();
            helper.assertTrue(r != null, who + ": feeding a base pearl should produce a result");
            helper.assertTrue(tally(r.output(), fuel) == slice,
                who + ": should give its whole slice " + slice + ", got " + tally(r.output(), fuel));
            helper.assertTrue(capacity(r.output()) == 40 + slice,
                who + ": a base pearl fed to the slice should read " + (40 + slice) + ", got " + capacity(r.output()));
            helper.assertTrue(r.materialCost() == expectedItems,
                who + ": should consume only " + expectedItems + " items, got " + r.materialCost());
            helper.assertTrue(r.xpCost() == expectedItems * fuel.xpCostPerItem().getAsInt(),
                who + ": xp cost should scale with items used, got " + r.xpCost());
        }
        helper.succeed();
    };

    /**
     * The headline: the three independent slices raise one pearl from 40 to a round 100, and to the same
     * 100 whatever order they are applied in — the whole reason each fuel caps its own tally rather than
     * absolute capacity.
     */
    public static final Consumer<GameTestHelper> FUELS_COMPOSE_TO_100 = helper -> {
        int expected = 40;
        for (PearlFuel fuel : PearlFeedingHandler.FUELS) {
            expected += fuel.sliceCap().getAsInt();
        }

        ItemStack forward = fillAll(PearlFeedingHandler.FUELS);
        helper.assertTrue(capacity(forward) == expected,
            "the three slices should compose to " + expected + ", got " + capacity(forward));

        List<PearlFuel> reversed = new ArrayList<>(PearlFeedingHandler.FUELS);
        java.util.Collections.reverse(reversed);
        ItemStack backward = fillAll(reversed);
        helper.assertTrue(capacity(backward) == expected,
            "order must not matter: reversed feeding should also reach " + expected + ", got " + capacity(backward));

        // Each source's own tally must be exactly its slice, whichever order it went in.
        for (PearlFuel fuel : PearlFeedingHandler.FUELS) {
            helper.assertTrue(tally(forward, fuel) == fuel.sliceCap().getAsInt()
                              && tally(backward, fuel) == fuel.sliceCap().getAsInt(),
                fuel.tooltipKey() + ": each fuel should hold its full slice in both orders");
        }
        helper.succeed();
    };

    /** Feeding one fuel does not touch another's tally, so a pearl grown by others still gets a full slice. */
    public static final Consumer<GameTestHelper> FUELS_ARE_INDEPENDENT = helper -> {
        PearlFuel first = PearlFeedingHandler.FUELS.get(0);
        PearlFuel second = PearlFeedingHandler.FUELS.get(1);

        // A pearl the second fuel has already filled to its slice; now feed the first.
        ItemStack grown = PearlFeedingHandler.feed(pearl(40), second, 1000).output();
        int secondSlice = tally(grown, second);

        PearlFeedingHandler.Result r = PearlFeedingHandler.feed(grown, first, 1000);
        helper.assertTrue(tally(r.output(), first) == first.sliceCap().getAsInt(),
            "the first fuel should give its full slice on an already-grown pearl, got " + tally(r.output(), first));
        helper.assertTrue(tally(r.output(), second) == secondSlice,
            "feeding the first fuel must not disturb the second's tally, got " + tally(r.output(), second));
        helper.assertTrue(capacity(r.output()) == 40 + first.sliceCap().getAsInt() + secondSlice,
            "both slices should stack: expected " + (40 + first.sliceCap().getAsInt() + secondSlice)
            + ", got " + capacity(r.output()));
        helper.succeed();
    };

    /** A fuel at its slice yields nothing, one point short never overshoots, and a pearl stack is declined. */
    public static final Consumer<GameTestHelper> FUEL_RESPECTS_ITS_SLICE = helper -> {
        PearlFuel fuel = PearlFeedingHandler.FUELS.get(0);
        int slice = fuel.sliceCap().getAsInt();

        helper.assertTrue(PearlFeedingHandler.feed(withTally(pearl(40 + slice), fuel, slice), fuel, 64) == null,
            "a pearl whose slice is full should yield nothing");

        PearlFeedingHandler.Result r =
            PearlFeedingHandler.feed(withTally(pearl(40 + slice - 1), fuel, slice - 1), fuel, 64);
        helper.assertTrue(r != null && tally(r.output(), fuel) == slice,
            "a nearly-full slice should land on the slice, got " + (r == null ? "null" : tally(r.output(), fuel)));
        helper.assertTrue(r.materialCost() == 1,
            "filling a single point of slice should cost a single item, not a whole stack, got " + r.materialCost());

        ItemStack twoPearls = pearl(40);
        twoPearls.setCount(2);
        helper.assertTrue(PearlFeedingHandler.feed(twoPearls, fuel, 10) == null,
            "feed should decline a stack of more than one pearl");
        helper.succeed();
    };

    /**
     * Drives the real subscriber for every fuel by posting an
     * {@link net.neoforged.neoforge.event.AnvilUpdateEvent} on the bus, so the {@code @EventBusSubscriber}
     * wiring, the per-fuel match, and the three setters are all exercised — not just {@code feed}.
     */
    public static final Consumer<GameTestHelper> ANVIL_WIRES_EACH_FUEL = helper -> {
        var player = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);

        for (PearlFuel fuel : PearlFeedingHandler.FUELS) {
            int per = fuel.perItem().getAsInt();
            ItemStack right = new ItemStack(fuel.ingredient().get(), 1);

            var event = new net.neoforged.neoforge.event.AnvilUpdateEvent(pearl(40), right, null, ItemStack.EMPTY, 0, 0, player);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(capacity(event.getOutput()) == 40 + per,
                fuel.tooltipKey() + ": the anvil should raise capacity by one item's worth, got " + capacity(event.getOutput()));
            helper.assertTrue(event.getMaterialCost() == 1,
                fuel.tooltipKey() + ": one item should be consumed, got " + event.getMaterialCost());

            // A pearl whose slice for this fuel is full must leave the anvil to vanilla — output empty.
            int slice = fuel.sliceCap().getAsInt();
            var noop = new net.neoforged.neoforge.event.AnvilUpdateEvent(
                withTally(pearl(40 + slice), fuel, slice), right, null, ItemStack.EMPTY, 0, 0, player);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(noop);
            helper.assertTrue(noop.getOutput().isEmpty(),
                fuel.tooltipKey() + ": a full-slice pearl should produce no anvil output, got " + noop.getOutput());
        }
        helper.succeed();
    };

    /** Chains a full feeding of every fuel in {@code order} onto one fresh pearl, returning the result. */
    private static ItemStack fillAll(List<PearlFuel> order) {
        ItemStack pearl = pearl(40);
        for (PearlFuel fuel : order) {
            PearlFeedingHandler.Result r = PearlFeedingHandler.feed(pearl, fuel, 1000);
            if (r == null) {
                throw new AssertionError(fuel.tooltipKey()
                    + " gave no room while composing — a fuel that caps absolute capacity instead of its"
                    + " own slice starves later ones");
            }
            pearl = r.output();
        }
        return pearl;
    }

    private static ItemStack pearl(int capacity) {
        ItemStack stack = new ItemStack(XPMagic.MEMORY_PEARL.get());
        stack.set(XPMagic.XP_CAPACITY.get(), capacity);
        return stack;
    }

    private static ItemStack withTally(ItemStack pearl, PearlFuel fuel, int tally) {
        pearl.set(fuel.tally().get(), tally);
        return pearl;
    }

    private static int capacity(ItemStack stack) {
        return stack.getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
    }

    private static int tally(ItemStack stack, PearlFuel fuel) {
        return stack.getOrDefault(fuel.tally().get(), 0);
    }
}
