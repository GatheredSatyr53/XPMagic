package com.gatheredsatyr53.xpmagic.gametest;

import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.entity.PowderSeparatorBlockEntity;
import com.gatheredsatyr53.xpmagic.block.entity.XPKeepingMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * End-to-end cover for hopper automation of the machines. NeoForge routes vanilla hoppers through the
 * {@code Capabilities.Item.BLOCK} capability, so these drive a real hopper and thereby exercise the whole
 * chain: the capability registration, the {@code WorldlyContainerWrapper}, and each machine's per-face
 * slot lists / {@code canTakeItemThroughFace} rules. The contract under test: hoppers feed inputs and
 * pull finished outputs, but never drain an input or the owner-bound key.
 *
 * <p>The hopper transfers on an 8-tick cooldown, so these wait {@link #SETTLE_TICKS} before asserting
 * (registered with maxTicks {@link #MAX_TICKS} in {@link XPMagicGameTests}).
 */
public final class MachineHopperTests {

    /** Ticks to let the hopper run a couple of transfer cycles before asserting. */
    private static final int SETTLE_TICKS = 20;
    static final int MAX_TICKS = 40;

    private MachineHopperTests() {}

    static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(XPMagic.MODID, name);
    }

    /**
     * A hopper below the separator must pull the sifted fractions and never the un-sifted powder still
     * sitting in the input. One assertion covers both sides of the rule: coarse leaves, powder stays put.
     */
    public static final Consumer<GameTestHelper> HOPPER_PULLS_FRACTION_NOT_POWDER = helper -> {
        BlockPos machinePos = new BlockPos(0, 1, 0);
        BlockPos hopperPos = new BlockPos(0, 0, 0);
        helper.setBlock(machinePos, XPMagic.POWDER_SEPARATOR.get());
        helper.setBlock(hopperPos, Blocks.HOPPER);

        PowderSeparatorBlockEntity separator = helper.getBlockEntity(machinePos, PowderSeparatorBlockEntity.class);
        Container inv = separator.getInventory();
        inv.setItem(PowderSeparatorBlockEntity.SLOT_INPUT, new ItemStack(XPMagic.MEMORY_POWDER.get(), 3));
        inv.setItem(PowderSeparatorBlockEntity.SLOT_COARSE, new ItemStack(XPMagic.COARSE_POWDER.get(), 2));

        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(count(hopper, XPMagic.COARSE_POWDER.get()) > 0,
                "the hopper should have pulled some coarse powder from the output");
            helper.assertTrue(count(hopper, XPMagic.MEMORY_POWDER.get()) == 0,
                "the hopper must never pull the un-sifted powder out of the input");
            helper.assertTrue(count(inv, XPMagic.MEMORY_POWDER.get()) == 3,
                "the separator input powder must still be there, got " + count(inv, XPMagic.MEMORY_POWDER.get()));
            helper.succeed();
        });
    };

    /** A hopper above the separator must feed powder into the input through the top face. */
    public static final Consumer<GameTestHelper> HOPPER_FEEDS_SEPARATOR_INPUT = helper -> {
        BlockPos machinePos = new BlockPos(0, 0, 0);
        BlockPos hopperPos = new BlockPos(0, 1, 0);
        helper.setBlock(machinePos, XPMagic.POWDER_SEPARATOR.get());
        helper.setBlock(hopperPos, Blocks.HOPPER);

        PowderSeparatorBlockEntity separator = helper.getBlockEntity(machinePos, PowderSeparatorBlockEntity.class);
        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(XPMagic.MEMORY_POWDER.get(), 5));

        Container inv = separator.getInventory();
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(count(inv, XPMagic.MEMORY_POWDER.get()) > 0,
                "the hopper should have fed powder into the separator input");
            helper.succeed();
        });
    };

    /**
     * A hopper below the XP Keeping Machine must pull only the finished cocktail: the bottle, fuel,
     * matrix and the owner-bound key all stay put. Without an online owner the machine produces nothing,
     * so the slots only change if the hopper drains them — which the rules must forbid.
     */
    public static final Consumer<GameTestHelper> HOPPER_LEAVES_MACHINE_INPUTS = helper -> {
        BlockPos machinePos = new BlockPos(0, 1, 0);
        BlockPos hopperPos = new BlockPos(0, 0, 0);
        helper.setBlock(machinePos, XPMagic.XP_KEEPING_MACHINE.get());
        helper.setBlock(hopperPos, Blocks.HOPPER);

        XPKeepingMachineBlockEntity machine = helper.getBlockEntity(machinePos, XPKeepingMachineBlockEntity.class);
        Container inv = machine.getInventory();
        inv.setItem(XPKeepingMachineBlockEntity.SLOT_BOTTLE, new ItemStack(Items.GLASS_BOTTLE));
        inv.setItem(XPKeepingMachineBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
        inv.setItem(XPKeepingMachineBlockEntity.SLOT_MATRIX, new ItemStack(XPMagic.MEMORY_PEARL.get()));
        inv.setItem(XPKeepingMachineBlockEntity.SLOT_KEY, new ItemStack(XPMagic.PLAYER_KEY.get()));
        inv.setItem(XPKeepingMachineBlockEntity.SLOT_OUTPUT, new ItemStack(XPMagic.XP_COCKTAIL.get()));

        HopperBlockEntity hopper = helper.getBlockEntity(hopperPos, HopperBlockEntity.class);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(count(hopper, XPMagic.XP_COCKTAIL.get()) > 0,
                "the hopper should have pulled the finished cocktail");
            helper.assertTrue(count(inv, Items.GLASS_BOTTLE) == 1, "the bottle input must not be drained");
            helper.assertTrue(count(inv, Items.COAL) == 1, "the fuel input must not be drained");
            helper.assertTrue(count(inv, XPMagic.MEMORY_PEARL.get()) == 1, "the matrix input must not be drained");
            helper.assertTrue(count(inv, XPMagic.PLAYER_KEY.get()) == 1, "the owner key must never be automatable");
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
