package com.gatheredsatyr53.xpmagic.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.entity.XPKeepingMachineBlockEntity;
import com.gatheredsatyr53.xpmagic.datagen.XPMagicEnchantmentProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * End-to-end cover for the Saturation drop swap. These go through the real loot table and the real
 * global loot modifier json, so they fail if the modifier stops being discovered, the serializer
 * stops being registered, or the enchantment stops resolving — the wiring, not just the arithmetic.
 */
public final class LootTests {

    /** Enderman drops 0-1 pearls, each swapped at 50%, so a single kill proves nothing. */
    private static final int KILLS = 100;

    /**
     * Coal ore drops one coal, swapped at only 10% (veins are large), so this needs more samples than
     * the pearls to keep the same safety margin: at p=0.10, 0.9^300 is a ~1-in-10^13 chance of no swap.
     */
    private static final int ORE_BREAKS = 300;

    /**
     * Truth Grain is a 1% bonus, the rarest of these, so it needs the most samples: at p=0.01, 0.99^3000
     * is a ~1-in-10^13 chance of no grain in {@link #DIG_COUNT} digs.
     */
    private static final int DIG_COUNT = 3000;

    private LootTests() {}

    static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(XPMagic.MODID, name);
    }

    /**
     * With the enchantment on the weapon, memory pearls must appear. Chance-based, so this asserts
     * over many kills rather than one: at p=0.25 per kill, a hundred kills yielding nothing is a
     * 1-in-10^12 event, i.e. a real failure rather than bad luck.
     */
    public static final Consumer<GameTestHelper> SATURATION_SWAPS_DROP = helper -> {
        ServerPlayer player = player(helper);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(
            helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(XPMagicEnchantmentProvider.SATURATION),
            1
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);

        killEndermen(helper, player);

        int memory = count(helper, XPMagic.MEMORY_PEARL.get());
        helper.assertTrue(memory > 0, "a Saturation weapon should have swapped some pearls, got none in " + KILLS + " kills");

        helper.succeed();
    };

    /**
     * The other side, and the deterministic one: without the enchantment not a single pearl may be
     * swapped. Also asserts ordinary pearls did drop, so a silent failure to run the loot table at
     * all cannot masquerade as a pass.
     */
    public static final Consumer<GameTestHelper> PLAIN_WEAPON_KEEPS_DROP = helper -> {
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));

        killEndermen(helper, player);

        int memory = count(helper, XPMagic.MEMORY_PEARL.get());
        helper.assertTrue(memory == 0, "an unenchanted weapon must never swap a pearl, got " + memory);

        int plain = count(helper, Items.ENDER_PEARL);
        helper.assertTrue(plain > 0, "the vanilla drop should still happen, got no ender pearls in " + KILLS + " kills");

        helper.succeed();
    };

    /**
     * The pearl is the third fundamental XP store, so the claim to check is not "it has a component"
     * but "the machine takes it": xp_capacity is the whole contract, and this asserts the real
     * XP Keeping Machine accepts a pearl as a matrix and reads back the capacity it was given.
     */
    public static final Consumer<GameTestHelper> PEARL_IS_AN_XP_STORE = helper -> {
        BlockPos pos = BlockPos.ZERO;
        helper.setBlock(pos, XPMagic.XP_KEEPING_MACHINE.get());
        XPKeepingMachineBlockEntity machine = helper.getBlockEntity(pos, XPKeepingMachineBlockEntity.class);

        ItemStack pearl = new ItemStack(XPMagic.MEMORY_PEARL.get());
        helper.assertTrue(
            machine.isItemValid(XPKeepingMachineBlockEntity.SLOT_MATRIX, pearl),
            "the machine should accept a Memory Pearl as a matrix"
        );

        machine.getInventory().setItem(XPKeepingMachineBlockEntity.SLOT_MATRIX, pearl);
        int capacity = XPKeepingMachineBlockEntity.getMatrixXPCapacity(machine);
        helper.assertTrue(capacity == 40, "a Memory Pearl should hold 40 XP, got " + capacity);

        helper.succeed();
    };

    /**
     * The block half of the swap, and the one that caught the bug: an ore's loot has no attacking
     * entity at all — the enchantment rides the pickaxe as {@code LootContextParams.TOOL}. This drives
     * a real {@code destroyBlock} through {@code ServerPlayerGameMode} so the block loot context, the
     * global loot modifier json and the tool-side enchantment read all run for real. At p=0.10 per
     * break, {@link #ORE_BREAKS} breaks yielding nothing is a ~1-in-10^13 event, so this is a genuine
     * failure rather than bad luck.
     */
    public static final Consumer<GameTestHelper> SATURATION_SWAPS_ORE_DROP = helper -> {
        ServerPlayer player = player(helper);
        // The real evolving pickaxe, not a vanilla stand-in: Saturation's supported_items is the
        // evolving_tools tag, so enchanting the crystal pickaxe also proves the tag still admits it.
        ItemStack pickaxe = EvolutionTests.craftPickaxe(helper);
        pickaxe.enchant(saturation(helper), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

        mineCoalOre(helper, player);

        int nostalgic = count(helper, XPMagic.NOSTALGIC_COAL.get());
        helper.assertTrue(nostalgic > 0,
            "a Saturation pickaxe should have swapped some coal, got none in " + ORE_BREAKS + " breaks");

        helper.succeed();
    };

    /**
     * The deterministic counterpart: a plain pickaxe must never mint nostalgic coal, and ordinary coal
     * must still drop — so a silent failure to run the ore loot at all cannot pass for a clean swap.
     */
    public static final Consumer<GameTestHelper> PLAIN_PICKAXE_KEEPS_ORE_DROP = helper -> {
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, EvolutionTests.craftPickaxe(helper));

        mineCoalOre(helper, player);

        int nostalgic = count(helper, XPMagic.NOSTALGIC_COAL.get());
        helper.assertTrue(nostalgic == 0, "an unenchanted pickaxe must never swap coal, got " + nostalgic);

        int plain = count(helper, Items.COAL);
        helper.assertTrue(plain > 0,
            "the vanilla drop should still happen, got no coal in " + ORE_BREAKS + " breaks");

        helper.succeed();
    };

    /**
     * The add-mode side: Truth Grain does not replace anything, it rides on top. Digging dirt with a
     * Saturation tool must still yield dirt and, over enough digs, some grain — so this asserts both,
     * which also pins that "add" leaves the original loot whole rather than swapping it.
     */
    public static final Consumer<GameTestHelper> TRUTH_GRAIN_BONUS_ON_DIRT = helper -> {
        ServerPlayer player = player(helper);
        ItemStack tool = EvolutionTests.craftPickaxe(helper); // any Saturation-carrying digger will do
        tool.enchant(saturation(helper), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);

        mine(helper, player, Blocks.DIRT);

        int grain = count(helper, XPMagic.TRUTH_GRAIN.get());
        helper.assertTrue(grain > 0,
            "digging dirt with Saturation should drop some Truth Grain, got none in " + DIG_COUNT + " digs");
        int dirt = count(helper, Items.DIRT);
        helper.assertTrue(dirt > 0, "add mode must leave the dirt itself: got none back in " + DIG_COUNT + " digs");

        helper.succeed();
    };

    /**
     * Grass block is the one a shovel actually meets most, and the reason the pairing is per-table:
     * mining it runs blocks/grass_block (dropping a dirt item), never blocks/dirt, so its own bonus
     * entry has to fire for the grain to appear here.
     */
    public static final Consumer<GameTestHelper> TRUTH_GRAIN_BONUS_ON_GRASS = helper -> {
        ServerPlayer player = player(helper);
        ItemStack tool = EvolutionTests.craftPickaxe(helper);
        tool.enchant(saturation(helper), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);

        mine(helper, player, Blocks.GRASS_BLOCK);

        int grain = count(helper, XPMagic.TRUTH_GRAIN.get());
        helper.assertTrue(grain > 0,
            "digging grass block with Saturation should drop some Truth Grain, got none in " + DIG_COUNT + " digs");

        helper.succeed();
    };

    /**
     * Gravel is the case that proves the roll keys off the table, not off a drop item: gravel sometimes
     * rolls flint instead of gravel, and the grain must still come either way.
     */
    public static final Consumer<GameTestHelper> TRUTH_GRAIN_BONUS_ON_GRAVEL = helper -> {
        ServerPlayer player = player(helper);
        ItemStack tool = EvolutionTests.craftPickaxe(helper);
        tool.enchant(saturation(helper), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);

        mine(helper, player, Blocks.GRAVEL);

        int grain = count(helper, XPMagic.TRUTH_GRAIN.get());
        helper.assertTrue(grain > 0,
            "digging gravel with Saturation should drop some Truth Grain, got none in " + DIG_COUNT + " digs");
        int gravelOrFlint = count(helper, Items.GRAVEL) + count(helper, Items.FLINT);
        helper.assertTrue(gravelOrFlint > 0, "the gravel table should still have dropped its own item");

        helper.succeed();
    };

    /** The deterministic guard: no Saturation, no grain — while the block itself still drops. */
    public static final Consumer<GameTestHelper> PLAIN_TOOL_NO_TRUTH_GRAIN = helper -> {
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, EvolutionTests.craftPickaxe(helper));

        mine(helper, player, Blocks.DIRT);

        int grain = count(helper, XPMagic.TRUTH_GRAIN.get());
        helper.assertTrue(grain == 0, "an unenchanted tool must never mint Truth Grain, got " + grain);
        int dirt = count(helper, Items.DIRT);
        helper.assertTrue(dirt > 0, "the dirt should still drop, got none in " + DIG_COUNT + " digs");

        helper.succeed();
    };

    private static ServerPlayer player(GameTestHelper helper) {
        // GameType.SURVIVAL, not creative: a creative player's preventsBlockDrops short-circuits
        // destroyBlock before any loot runs, so the ore tests would pass by testing nothing.
        return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    }

    private static Holder<Enchantment> saturation(GameTestHelper helper) {
        return helper.getLevel().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(XPMagicEnchantmentProvider.SATURATION);
    }

    private static void mineCoalOre(GameTestHelper helper, ServerPlayer player) {
        mine(helper, player, Blocks.COAL_ORE, ORE_BREAKS);
    }

    private static void mine(GameTestHelper helper, ServerPlayer player, Block block) {
        mine(helper, player, block, DIG_COUNT);
    }

    private static void mine(GameTestHelper helper, ServerPlayer player, Block block, int times) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        for (int i = 0; i < times; i++) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            helper.assertTrue(player.gameMode.destroyBlock(pos), "the block should have been destroyed");
        }
    }

    private static void killEndermen(GameTestHelper helper, ServerPlayer player) {
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < KILLS; i++) {
            LivingEntity victim = helper.spawn(EntityTypes.ENDERMAN, BlockPos.ZERO);
            victim.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
            helper.assertTrue(victim.isDeadOrDying(), "the enderman should have died");
        }
    }

    private static int count(GameTestHelper helper, Item item) {
        // Kept tight on purpose: every test in a run shares one plot, and a radius wide enough to
        // reach the neighbouring test would let its drops answer for ours. The mobs die where they
        // spawn, so the drops never travel far.
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(6.0);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
        int total = 0;
        for (ItemEntity drop : drops) {
            if (drop.getItem().is(item)) total += drop.getItem().getCount();
        }
        return total;
    }
}
