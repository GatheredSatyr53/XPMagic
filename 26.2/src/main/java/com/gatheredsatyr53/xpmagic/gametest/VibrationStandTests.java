package com.gatheredsatyr53.xpmagic.gametest;

import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.block.entity.VibrationStandBlockEntity;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Pins the {@link VibrationStandBlockEntity#fuelGauge(int)} quantisation, the only bit of numeric logic
 * behind the fuel indicator. It maps remaining burn time onto the 0–4 gauge shown on the front face:
 * empty means empty, any positive fuel shows at least one bar, one coal (1600t) tops it out, and anything
 * beyond a full load is capped rather than overflowing. These are pure-function checks, so nothing is
 * placed in the world — the test just asserts and succeeds on the first tick.
 */
public final class VibrationStandTests {

    private VibrationStandTests() {}

    /** The gauge steps at quarters of a coal (400t each): 0, 1..400, 401..800, 801..1200, 1201+. */
    public static final Consumer<GameTestHelper> FUEL_GAUGE_QUANTISES = helper -> {
        assertGauge(helper, 0, 0);        // empty reads empty
        assertGauge(helper, 1, 1);        // any fuel at all lights the first bar
        assertGauge(helper, 400, 1);      // top of the first quarter
        assertGauge(helper, 401, 2);      // first tick into the second
        assertGauge(helper, 800, 2);
        assertGauge(helper, 801, 3);
        assertGauge(helper, 1200, 3);
        assertGauge(helper, 1201, 4);
        assertGauge(helper, 1600, 4);     // exactly one coal is full
        assertGauge(helper, 16000, 4);    // a stack's worth still caps at full, never overflows
        assertGauge(helper, -50, 0);      // a nonsensical negative never dips below empty
        helper.succeed();
    };

    private static void assertGauge(GameTestHelper helper, int burnTime, int expected) {
        int actual = VibrationStandBlockEntity.fuelGauge(burnTime);
        helper.assertTrue(actual == expected,
            "fuel gauge for burnTime=" + burnTime + " should be " + expected + " but was " + actual);
    }
}
