package com.gatheredsatyr53.xpmagic.block.entity;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.block.VibrationStandBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class VibrationStandBlockEntity extends BlockEntity {

    /** Peak per-tick jitter added to entities on top. Symmetric, so it trembles in place. */
    private static final double SHAKE_HORIZONTAL = 0.06;
    private static final double SHAKE_VERTICAL = 0.04;

    /** Remaining burn time; spent while the stand is powered by redstone. */
    private int burnTime;

    public VibrationStandBlockEntity(BlockPos pos, BlockState state) {
        super(XPMagic.VIBRATION_STAND_BLOCK_ENTITY.get(), pos, state);
    }

    public void addFuel(int burnDuration) {
        this.burnTime += burnDuration;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VibrationStandBlockEntity stand) {
        boolean wasLit = state.getValue(VibrationStandBlock.LIT);

        // Driven purely by redstone (independent of the separator above) as long as it has fuel.
        boolean vibrating = stand.burnTime > 0 && level.hasNeighborSignal(pos);

        if (vibrating) {
            --stand.burnTime;
            stand.setChanged();
            shakeEntitiesAbove(level, pos);
        }

        // Flags 3 = notify neighbours (1) + sync to clients (2).
        if (vibrating != wasLit)
            level.setBlock(pos, state.setValue(VibrationStandBlock.LIT, vibrating), 3);
    }

    /** Jolts entities standing on the stand so they visibly shake. */
    private static void shakeEntitiesAbove(Level level, BlockPos pos) {
        AABB box = new AABB(pos).move(0.0, 1.0, 0.0);
        RandomSource random = level.getRandom();
        for (Entity entity : level.getEntities((Entity) null, box, EntitySelector.NO_SPECTATORS)) {
            Vec3 jitter = new Vec3(
                (random.nextDouble() - 0.5) * 2.0 * SHAKE_HORIZONTAL,
                (random.nextDouble() - 0.5) * 2.0 * SHAKE_VERTICAL,
                (random.nextDouble() - 0.5) * 2.0 * SHAKE_HORIZONTAL);
            entity.setDeltaMovement(entity.getDeltaMovement().add(jitter));
            // Force the server to push the new velocity to the client (works for players too).
            entity.hurtMarked = true;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burn_time", this.burnTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.burnTime = input.getIntOr("burn_time", 0);
    }
}
