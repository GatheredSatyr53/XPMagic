package com.gatheredsatyr53.xpmagic.item;

import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Grain of Truth, as a seed. It only sprouts on {@link com.gatheredsatyr53.xpmagic.block.TruthFarmlandBlock
 * Truth Farmland} — the tilth a Saturation-enchanted Memory Crystal hoe leaves — where it plants a
 * {@link com.gatheredsatyr53.xpmagic.block.KnowledgeSaplingBlock Knowledge Sapling} that grows into the
 * Tree of Knowledge. Clicked on any other block it does nothing, so the grain stays a curio until the
 * player has prepared the ground for it.
 */
public class TruthGrainItem extends Item {

    public TruthGrainItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // Only the top face of Truth Farmland; the sapling grows in the space directly above.
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        if (!level.getBlockState(pos).is(XPMagic.TRUTH_FARMLAND.get())) return InteractionResult.PASS;

        BlockPos above = pos.above();
        if (!level.getBlockState(above).canBeReplaced()) return InteractionResult.PASS;

        BlockState sapling = XPMagic.KNOWLEDGE_SAPLING.get().defaultBlockState();
        if (!sapling.canSurvive(level, above)) return InteractionResult.PASS;

        if (level instanceof ServerLevel) {
            level.setBlock(above, sapling, Block.UPDATE_ALL);
            level.playSound(null, above, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
