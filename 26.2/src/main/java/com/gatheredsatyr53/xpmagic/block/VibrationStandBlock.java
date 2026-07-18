package com.gatheredsatyr53.xpmagic.block;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.entity.VibrationStandBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class VibrationStandBlock extends OrientedMachineBlock {

    public static final MapCodec<VibrationStandBlock> CODEC = simpleCodec(VibrationStandBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Quantised remaining-fuel gauge (0 = empty … 4 = full) shown on the front face. */
    public static final IntegerProperty FUEL = IntegerProperty.create("fuel", 0, 4);

    public VibrationStandBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(FUEL, 0));
    }

    @Override
    protected MapCodec<VibrationStandBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT, FUEL);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VibrationStandBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : createTickerHelper(type, XPMagic.VIBRATION_STAND_BLOCK_ENTITY.get(), VibrationStandBlockEntity::serverTick);
    }

    // Right-click with any furnace fuel to load it into the stand's fuel slot (furnace model): as much
    // of the held stack as fits is moved in, and the stand burns it a piece at a time while powered.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.fuelValues().burnDuration(stack) <= 0)
            return InteractionResult.PASS;

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VibrationStandBlockEntity stand) {
            Container inventory = stand.getInventory();
            ItemStack slot = inventory.getItem(VibrationStandBlockEntity.SLOT_FUEL);
            // A different fuel already occupies the slot, or it is already full: leave the hand alone.
            if (!slot.isEmpty() && !ItemStack.isSameItemSameComponents(slot, stack))
                return InteractionResult.PASS;
            int capacity = slot.isEmpty() ? stack.getMaxStackSize() : slot.getMaxStackSize();
            int moved = Math.min(capacity - slot.getCount(), stack.getCount());
            if (moved <= 0)
                return InteractionResult.PASS;

            ItemStack loaded = slot.isEmpty() ? stack.copyWithCount(moved) : slot.copy();
            if (!slot.isEmpty())
                loaded.grow(moved);
            inventory.setItem(VibrationStandBlockEntity.SLOT_FUEL, loaded);
            stack.consume(moved, player);
            level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.95;
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.4) {
            double spread = (random.nextDouble() - 0.5) * 0.4;
            level.addParticle(ParticleTypes.CRIT, x + spread, y, z + spread, 0.0, 0.03, 0.0);
        }
    }
}
