package com.gatheredsatyr53.xpmagic.block;

import com.gatheredsatyr53.xpmagic.inventory.PowderMixerMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PowderMixerBlock extends Block {

    public static final MapCodec<PowderMixerBlock> CODEC = simpleCodec(PowderMixerBlock::new);
    private static final Component CONTAINER_TITLE = Component.translatable("block.xpmagic.powder_mixer");

    @Override
    public MapCodec<? extends PowderMixerBlock> codec() {
        return CODEC;
    }

    public PowderMixerBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
    ) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(final BlockState state, final Level level, final BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new PowderMixerMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), CONTAINER_TITLE
        );
    }
    
}
