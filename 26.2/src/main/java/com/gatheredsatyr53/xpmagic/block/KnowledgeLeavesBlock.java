package com.gatheredsatyr53.xpmagic.block;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The foliage of the Tree of Knowledge. Ordinary leaves in look and decay, but they bear fruit: a leaf
 * that is not already bearing has, on each random tick, a chance to grow a Fruit of Knowledge (tracked
 * by {@link #BEARING}). Right-clicking a bearing leaf picks the fruit and the leaf goes bare again —
 * the same harvest loop as sweet berries or glow berries, kept to one self-contained block so the
 * mechanic never depends on fragile worldgen.
 *
 * <p>The fruit is what the whole chain — Saturation hoe, Truth Farmland, Grain of Truth — is grown for;
 * it is destined to be fed to a Memory Pearl.
 */
public class KnowledgeLeavesBlock extends LeavesBlock {

    public static final BooleanProperty BEARING = BooleanProperty.create("bearing");

    // A bare leaf's odds, per random tick, of setting fruit. Healthy leaves tick seldom (see
    // isRandomlyTicking), so this is deliberately generous rather than rare.
    private static final int GROW_ONE_IN = 12;

    public static final MapCodec<KnowledgeLeavesBlock> CODEC = simpleCodec(KnowledgeLeavesBlock::new);

    public KnowledgeLeavesBlock(Properties properties) {
        super(0.0F, properties); // 0 particle chance: the Tree of Knowledge does not shed leaves
        this.registerDefaultState(this.defaultBlockState().setValue(BEARING, Boolean.FALSE));
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        // Deliberately none — see the 0 particle chance above.
    }

    // Vanilla only ticks leaves that are decaying; we want grown, healthy leaves to keep bearing fruit,
    // so tick them all. Decay is still handled by super.randomTick below.
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random); // preserve vanilla leaf decay

        // super.randomTick may have removed a decaying leaf; only bear fruit on a leaf still standing.
        BlockState current = level.getBlockState(pos);
        if (!current.is(this) || current.getValue(BEARING)) return;

        if (random.nextInt(GROW_ONE_IN) == 0) {
            level.setBlock(pos, current.setValue(BEARING, Boolean.TRUE), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!state.getValue(BEARING)) return InteractionResult.PASS;

        if (level instanceof ServerLevel) {
            Block.popResource(level, pos, new ItemStack(XPMagic.FRUIT_OF_KNOWLEDGE.get()));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.setBlock(pos, state.setValue(BEARING, Boolean.FALSE), Block.UPDATE_CLIENTS);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BEARING);
    }
}
