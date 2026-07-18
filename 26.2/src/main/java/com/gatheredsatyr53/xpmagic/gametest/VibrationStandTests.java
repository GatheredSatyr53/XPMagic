package com.gatheredsatyr53.xpmagic.gametest;

import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.VibrationStandBlock;
import com.gatheredsatyr53.xpmagic.block.entity.VibrationStandBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Pins the {@link VibrationStandBlockEntity#fuelGauge(int)} quantisation and the furnace-model fuel
 * handling: the stand holds fuel in a slot, hoppers feed that slot (but can never drain it), and a
 * powered stand burns one piece at a time. The gauge checks are pure functions run on the first tick;
 * the hopper/burn checks place blocks and wait a few ticks for transfers and the burn cycle to settle.
 */
public final class VibrationStandTests {

    /** Ticks to let a hopper run a couple of transfer cycles, or the burn to catch, before asserting. */
    private static final int SETTLE_TICKS = 20;
    static final int MAX_TICKS = 40;

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

    /** A hopper above the stand must feed coal into its fuel slot through the top face. */
    public static final Consumer<GameTestHelper> HOPPER_FEEDS_STAND_FUEL = helper -> {
        BlockPos standPos = new BlockPos(0, 0, 0);
        BlockPos hopperPos = new BlockPos(0, 1, 0);
        helper.setBlock(standPos, XPMagic.VIBRATION_STAND.get());
        helper.setBlock(hopperPos, Blocks.HOPPER);

        VibrationStandBlockEntity stand = helper.getBlockEntity(standPos, VibrationStandBlockEntity.class);
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.COAL, 5));

        Container inv = stand.getInventory();
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(count(inv, Items.COAL) > 0,
                "the hopper should have fed coal into the stand's fuel slot");
            helper.succeed();
        });
    };

    /** A hopper below the stand must never pull fuel back out of the slot. */
    public static final Consumer<GameTestHelper> HOPPER_CANT_DRAIN_STAND_FUEL = helper -> {
        BlockPos standPos = new BlockPos(0, 1, 0);
        BlockPos hopperPos = new BlockPos(0, 0, 0);
        helper.setBlock(standPos, XPMagic.VIBRATION_STAND.get());
        helper.setBlock(hopperPos, Blocks.HOPPER);

        VibrationStandBlockEntity stand = helper.getBlockEntity(standPos, VibrationStandBlockEntity.class);
        Container inv = stand.getInventory();
        inv.setItem(VibrationStandBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 4));

        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            // Unpowered, so nothing is burned either: the four coal must all still be sitting in the slot.
            helper.assertTrue(count(inv, Items.COAL) == 4,
                "the stand's fuel must not be drained by a hopper, got " + count(inv, Items.COAL));
            helper.assertTrue(count(hopper, Items.COAL) == 0,
                "the hopper must not have pulled any fuel, got " + count(hopper, Items.COAL));
            helper.succeed();
        });
    };

    /**
     * The furnace step: a stand with fuel in its slot and a redstone signal must light one piece and run.
     * With a redstone block beside it the stand consumes exactly one coal into its burn buffer and turns
     * LIT, proving the slot -> burn path (not the old instant-burn on right-click).
     */
    public static final Consumer<GameTestHelper> STAND_BURNS_SLOT_FUEL = helper -> {
        BlockPos standPos = new BlockPos(0, 0, 0);
        helper.setBlock(standPos, XPMagic.VIBRATION_STAND.get());

        VibrationStandBlockEntity stand = helper.getBlockEntity(standPos, VibrationStandBlockEntity.class);
        stand.getInventory().setItem(VibrationStandBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 2));

        // A redstone block beside the stand is a constant signal — the stand's only power condition.
        helper.setBlock(new BlockPos(1, 0, 0), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(helper.getBlockState(standPos).getValue(VibrationStandBlock.LIT),
                "a powered stand with fuel should be lit and vibrating");
            int coal = count(stand.getInventory(), Items.COAL);
            helper.assertTrue(coal == 1,
                "the stand should have burned exactly one coal from the slot, leaving 1, but has " + coal);
            helper.succeed();
        });
    };

    private static int count(Container container, Item item) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }
}
