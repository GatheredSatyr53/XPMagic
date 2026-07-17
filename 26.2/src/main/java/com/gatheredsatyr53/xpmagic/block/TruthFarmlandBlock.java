package com.gatheredsatyr53.xpmagic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Truth Farmland — the special tilth a Memory Crystal hoe carrying {@code xpmagic:saturation}
 * leaves behind (see {@link com.gatheredsatyr53.xpmagic.TruthFarmlandHandler}). It is the only soil a
 * {@link com.gatheredsatyr53.xpmagic.item.TruthGrainItem Grain of Truth} will take root in, so it must
 * be as permanent as the effort that made it: unlike vanilla farmland it stays fully moist forever,
 * never reverts to dirt, and is not trampled flat — not even by the solid trunk the Tree of Knowledge
 * grows on top of it.
 */
public class TruthFarmlandBlock extends FarmlandBlock {

    public TruthFarmlandBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(MOISTURE, MAX_MOISTURE));
    }

    // Magic soil is self-watering: hold it at full moisture and never dry out to the point of reverting.
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MOISTURE) != MAX_MOISTURE) {
            level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), Block.UPDATE_CLIENTS);
        }
    }

    // Vanilla farmland reverts to dirt when a solid block sits on it; the Tree of Knowledge's own trunk
    // would trip that. The Truth Farmland endures instead.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    // Skip FarmlandBlock's trample-to-dirt entirely; keep only the vanilla Block fall-damage default.
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }
}
