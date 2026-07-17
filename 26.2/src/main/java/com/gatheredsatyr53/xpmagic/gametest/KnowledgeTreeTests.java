package com.gatheredsatyr53.xpmagic.gametest;

import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.datagen.XPMagicEnchantmentProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * End-to-end cover for the Grain-of-Truth chain: a Saturation Memory Crystal hoe tills Truth Farmland,
 * a Grain of Truth sown on it becomes a Knowledge Sapling, and the sapling grows the Tree of Knowledge.
 * Each test drives the real interaction paths ({@code ItemStack#useOn}, the tilling tool-modification
 * hook, {@code TreeGrower#growTree}) rather than the handlers in isolation.
 */
public final class KnowledgeTreeTests {

    private KnowledgeTreeTests() {}

    /** A Saturation hoe tilling dirt leaves Truth Farmland, not plain farmland. */
    public static final Consumer<GameTestHelper> SATURATION_HOE_TILLS_TRUTH = helper -> {
        BlockPos ground = tillSpot(helper);
        ItemStack hoe = new ItemStack(XPMagic.MEMORY_CRYSTAL_HOE.get());
        hoe.enchant(saturation(helper), 1);

        tillWith(helper, hoe, ground);

        helper.assertTrue(helper.getLevel().getBlockState(ground).is(XPMagic.TRUTH_FARMLAND.get()),
            "a Saturation crystal hoe should till Truth Farmland");
        helper.succeed();
    };

    /** The same hoe without Saturation tills ordinary farmland — the enchantment is the gate. */
    public static final Consumer<GameTestHelper> PLAIN_HOE_TILLS_VANILLA = helper -> {
        BlockPos ground = tillSpot(helper);
        ItemStack hoe = new ItemStack(XPMagic.MEMORY_CRYSTAL_HOE.get()); // unenchanted

        tillWith(helper, hoe, ground);

        BlockState result = helper.getLevel().getBlockState(ground);
        helper.assertTrue(result.is(Blocks.FARMLAND), "an unenchanted hoe should still till plain farmland");
        helper.assertTrue(!result.is(XPMagic.TRUTH_FARMLAND.get()),
            "without Saturation the hoe must not make Truth Farmland");
        helper.succeed();
    };

    /** A Grain of Truth sown on Truth Farmland plants a Knowledge Sapling above it. */
    public static final Consumer<GameTestHelper> GRAIN_PLANTS_ON_TRUTH = helper -> {
        BlockPos ground = tillSpot(helper);
        ServerLevel level = helper.getLevel();
        level.setBlockAndUpdate(ground, XPMagic.TRUTH_FARMLAND.get().defaultBlockState());

        ItemStack grain = new ItemStack(XPMagic.TRUTH_GRAIN.get());
        useOnTop(helper, grain, ground);

        helper.assertTrue(level.getBlockState(ground.above()).is(XPMagic.KNOWLEDGE_SAPLING.get()),
            "sowing a Grain of Truth on Truth Farmland should plant a Knowledge Sapling");
        helper.succeed();
    };

    /** A Grain of Truth on ordinary dirt does nothing — no sapling, grain untouched. */
    public static final Consumer<GameTestHelper> GRAIN_INERT_OFF_TRUTH = helper -> {
        BlockPos ground = tillSpot(helper);
        ServerLevel level = helper.getLevel();
        level.setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());

        ItemStack grain = new ItemStack(XPMagic.TRUTH_GRAIN.get());
        useOnTop(helper, grain, ground);

        helper.assertTrue(level.getBlockState(ground.above()).isAir(),
            "a Grain of Truth on plain dirt must not plant anything");
        helper.assertTrue(grain.getCount() == 1, "the grain should be spent only when it plants");
        helper.succeed();
    };

    /** A Knowledge Sapling on Truth Farmland grows a tree whose trunk is Knowledge Log. */
    public static final Consumer<GameTestHelper> SAPLING_GROWS_TREE = helper -> {
        ServerLevel level = helper.getLevel();
        BlockPos ground = tillSpot(helper);
        level.setBlockAndUpdate(ground, XPMagic.TRUTH_FARMLAND.get().defaultBlockState());

        BlockPos saplingPos = ground.above();
        BlockState saplingState = XPMagic.KNOWLEDGE_SAPLING.get().defaultBlockState();
        ChunkGenerator generator = level.getChunkSource().getGenerator();

        boolean grew = false;
        for (int attempt = 0; attempt < 64 && !grew; attempt++) {
            level.setBlockAndUpdate(saplingPos, saplingState);
            grew = XPMagic.KNOWLEDGE_TREE_GROWER.growTree(level, generator, saplingPos, saplingState, level.getRandom());
        }
        helper.assertTrue(grew, "the Knowledge Sapling never grew a tree in 64 attempts");

        boolean trunk = false;
        for (int y = 0; y < 10 && !trunk; y++) {
            if (level.getBlockState(saplingPos.above(y)).is(XPMagic.KNOWLEDGE_LOG.get())) {
                trunk = true;
            }
        }
        helper.assertTrue(trunk, "the grown Tree of Knowledge has no Knowledge Log trunk");
        helper.succeed();
    };

    // --- helpers ---

    // A clear cell above the structure floor: the ground block, with air above it to till/plant into.
    private static BlockPos tillSpot(GameTestHelper helper) {
        BlockPos ground = helper.absolutePos(new BlockPos(1, 2, 1));
        ServerLevel level = helper.getLevel();
        level.setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(ground.above(), Blocks.AIR.defaultBlockState());
        return ground;
    }

    private static void tillWith(GameTestHelper helper, ItemStack hoe, BlockPos ground) {
        hoe.useOn(topClick(helper, hoe, ground));
    }

    private static void useOnTop(GameTestHelper helper, ItemStack stack, BlockPos ground) {
        stack.useOn(topClick(helper, stack, ground));
    }

    // A use-on context for a right-click onto the top face of the given block.
    private static net.minecraft.world.item.context.UseOnContext topClick(GameTestHelper helper, ItemStack stack, BlockPos ground) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hitVec = new Vec3(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, ground, false);
        return new net.minecraft.world.item.context.UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
    }

    private static Holder<Enchantment> saturation(GameTestHelper helper) {
        return helper.getLevel().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(XPMagicEnchantmentProvider.SATURATION);
    }
}
