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

    /** A full stack tops a fresh pearl off to exactly the ceiling — never past it — in one take. */
    public static final Consumer<GameTestHelper> FLESH_FILLS_TO_CAP = helper -> {
        ItemStack pearl = pearl(40); // base
        VindictiveFleshHandler.Result r = VindictiveFleshHandler.feed(pearl, 100);
        helper.assertTrue(r != null, "feeding a base pearl should produce a result");

        int cap = Config.vindictivePearlMaxCapacity;
        int gained = cap - 40;
        int expectedFlesh = (gained + Config.vindictiveCapacityPerFlesh - 1) / Config.vindictiveCapacityPerFlesh;

        helper.assertTrue(capacity(r.output()) == cap,
            "a fed pearl should reach the ceiling " + cap + ", got " + capacity(r.output()));
        helper.assertTrue(vindictive(r.output()) == gained,
            "the whole gain should be tracked as vindictive: expected " + gained + ", got " + vindictive(r.output()));
        helper.assertTrue(r.fleshUsed() == expectedFlesh,
            "should consume only the flesh the room needed: expected " + expectedFlesh + ", got " + r.fleshUsed());
        helper.assertTrue(r.xpCost() == expectedFlesh * Config.vindictiveXpCostPerFlesh,
            "xp cost should scale with flesh used, got " + r.xpCost());

        // The input must be untouched — feed builds a copy, it does not mutate the stack in the slot.
        helper.assertTrue(capacity(pearl) == 40, "feed must not mutate the input pearl");

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

    /** At the ceiling there is nothing to buy, and a pearl one point short never overshoots it. */
    public static final Consumer<GameTestHelper> FLESH_RESPECTS_THE_CEILING = helper -> {
        int cap = Config.vindictivePearlMaxCapacity;

        helper.assertTrue(VindictiveFleshHandler.feed(pearl(cap), 64) == null,
            "a pearl already at the ceiling should yield nothing");

        // One below the ceiling with a whole stack offered: takes exactly one flesh, lands exactly on cap.
        VindictiveFleshHandler.Result r = VindictiveFleshHandler.feed(pearl(cap - 1), 64);
        helper.assertTrue(r != null && capacity(r.output()) == cap,
            "a nearly-full pearl should land on the cap, got " + (r == null ? "null" : capacity(r.output())));
        helper.assertTrue(r.fleshUsed() == 1,
            "filling a single point of room should cost a single flesh, not a whole stack, got " + r.fleshUsed());

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

        // A pearl with nothing to gain must leave the anvil to vanilla — output stays empty.
        var noop = new net.neoforged.neoforge.event.AnvilUpdateEvent(
            pearl(Config.vindictivePearlMaxCapacity), flesh, null, ItemStack.EMPTY, 0, 0, player);
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

    private static int capacity(ItemStack stack) {
        return stack.getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
    }

    private static int vindictive(ItemStack stack) {
        return stack.getOrDefault(XPMagic.VINDICTIVE_CAPACITY.get(), 0);
    }
}
