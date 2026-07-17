package com.gatheredsatyr53.xpmagic.block;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The sprout a Grain of Truth becomes. A plain {@link SaplingBlock} in every way but one: it will only
 * root on {@link TruthFarmlandBlock}, so the Tree of Knowledge can only ever be raised on soil a
 * Saturation hoe prepared. Growth (random tick and bonemeal alike) runs through the shared
 * {@link XPMagic#KNOWLEDGE_TREE_GROWER}, which resolves the {@code xpmagic:knowledge_tree} configured
 * feature.
 */
public class KnowledgeSaplingBlock extends SaplingBlock {

    public KnowledgeSaplingBlock(TreeGrower treeGrower, Properties properties) {
        super(treeGrower, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(XPMagic.TRUTH_FARMLAND.get());
    }
}
