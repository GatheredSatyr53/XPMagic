package com.gatheredsatyr53.xpmagic.block;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.entity.PowderSeparatorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class PowderSeparatorBlock extends OrientedMachineBlock {

    public static final MapCodec<PowderSeparatorBlock> CODEC = simpleCodec(PowderSeparatorBlock::new);

    public PowderSeparatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<PowderSeparatorBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new PowderSeparatorBlockEntity(worldPosition, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PowderSeparatorBlockEntity separator)
            player.openMenu(separator);
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : createTickerHelper(type, XPMagic.POWDER_SEPARATOR_BLOCK_ENTITY.get(), PowderSeparatorBlockEntity::serverTick);
    }
}
