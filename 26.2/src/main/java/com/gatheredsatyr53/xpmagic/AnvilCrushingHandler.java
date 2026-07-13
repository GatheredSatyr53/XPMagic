package com.gatheredsatyr53.xpmagic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Anvil crushing: the reverse of the explosion's compaction. A falling anvil that lands on Memory
 * Crystals shatters them back into loose powder. The blast that fused a crystal packed it denser
 * than its inputs (and lightning may have charged it further); the anvil's blow scatters all of
 * that — so recovery is deliberately lossy: each crystal releases only a {@link Config#shatterCapacity}
 * xp_capacity budget (kept below a crystal's own capacity), and its bonus and lightning charge are
 * simply lost on impact.
 *
 * <p>Where the Powder Separator sifts a whole powder into fractions cleanly and deterministically,
 * the anvil is a blunt instrument: the budget is broken into <em>random</em> fractions, weighted
 * toward the coarse grains, so the prized fine dust stays rare. Fractions read their xp_capacity from
 * the same components the separator and XP Keeping Machine use, so a shatter can never mint capacity
 * that wasn't in the budget — no dupe.
 *
 * <p>There is no "anvil landed" event, so we hook the moment the {@link FallingBlockEntity} is
 * removed: a real landing discards it ({@link Entity.RemovalReason#DISCARDED}) while on the ground.
 * Items never block or absorb a falling anvil — it drops straight through them onto the floor — so
 * any crystals it "crushed" are still sitting in the block cell it settled into.
 */
@Mod.EventBusSubscriber(modid = XPMagic.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AnvilCrushingHandler {

    /** Coarse, medium, fine — largest first. Draw weights favour the coarse grains so fine stays rare. */
    private static final int[] FRACTION_WEIGHTS = {6, 3, 1};

    private static Item[] fractionItems() {
        return new Item[]{XPMagic.COARSE_POWDER.get(), XPMagic.MEDIUM_POWDER.get(), XPMagic.FINE_POWDER.get()};
    }

    @SubscribeEvent
    static void onLeave(EntityLeaveLevelEvent event) {
        if (!Config.crushCrystals || Config.shatterCapacity == 0) return;
        if (!(event.getEntity() instanceof FallingBlockEntity anvil)) return;

        // Only a genuine landing, not a chunk unload / dimension change / mid-air auto-expire.
        if (anvil.getRemovalReason() != Entity.RemovalReason.DISCARDED) return;
        if (!anvil.onGround()) return;
        if (!(anvil.getBlockState().getBlock() instanceof AnvilBlock)) return;

        Level level = event.getLevel();
        if (level.isClientSide()) return; // authoritative item changes happen server-side only

        // Crystals sitting in the cell the anvil settled into (inflated a touch for items nudged out
        // of the way when the block was placed).
        BlockPos pos = anvil.blockPosition();
        AABB box = new AABB(pos).inflate(0.3);
        List<ItemEntity> crystals = level.getEntitiesOfClass(ItemEntity.class, box,
            item -> item.isAlive() && item.getItem().is(XPMagic.MEMORY_CRYSTAL.get()));
        if (crystals.isEmpty()) return;

        Item[] fractions = fractionItems();
        int[] caps = new int[fractions.length];
        for (int i = 0; i < fractions.length; ++i)
            caps[i] = fractions[i].getDefaultInstance().getOrDefault(XPMagic.XP_CAPACITY.get(), 0);

        // Shatter every crystal unit under the anvil; each spends its own budget on random fractions.
        RandomSource random = level.getRandom();
        int[] produced = new int[fractions.length];
        int shattered = 0;
        for (ItemEntity item : crystals) {
            ItemStack stack = item.getItem();
            int count = stack.getCount();
            shattered += count;
            if (stack.has(XPMagic.LIGHTNING_CHARGE.get())) {
                for (int i = 0; i < count; i++) {
                    level.explode(item,
                                  item.getX(),
                                  item.getY(),
                                  item.getZ(),
                                  Math.ceilDiv(stack.get(XPMagic.LIGHTNING_CHARGE.get()), 7),
                                  Level.ExplosionInteraction.MOB);
                }
            }
            item.discard();
            for (int c = 0; c < count; ++c)
                drawFractions(Config.shatterCapacity, caps, random, produced);
        }
        if (shattered == 0) return;

        // Drop the recovered fractions at the anvil's centre, each split into stackable piles.
        Vec3 center = Vec3.atCenterOf(pos);
        boolean any = false;
        for (int i = 0; i < fractions.length; ++i) {
            if (produced[i] == 0) continue;
            any = true;
            spawnStacked(level, center, fractions[i], produced[i]);
        }
        if (any) playShatterEffects(level, center);
    }

    /**
     * Spend a capacity {@code budget} on random fractions, adding the drawn counts into {@code out}.
     * Each step picks — weighted toward the coarse grains — among the fractions that still fit the
     * remaining budget, then subtracts that fraction's capacity. When nothing fits, the leftover
     * budget is lost (the crush wasted it), which is what keeps the recycle lossy.
     */
    private static void drawFractions(int budget, int[] caps, RandomSource random, int[] out) {
        while (true) {
            int totalWeight = 0;
            for (int i = 0; i < caps.length; ++i)
                if (caps[i] > 0 && caps[i] <= budget) totalWeight += FRACTION_WEIGHTS[i];
            if (totalWeight == 0) return; // nothing left that fits the remaining budget

            int roll = random.nextInt(totalWeight);
            for (int i = 0; i < caps.length; ++i) {
                if (caps[i] <= 0 || caps[i] > budget) continue;
                roll -= FRACTION_WEIGHTS[i];
                if (roll < 0) {
                    out[i]++;
                    budget -= caps[i];
                    break;
                }
            }
        }
    }

    private static void spawnStacked(Level level, Vec3 center, Item item, int count) {
        int maxStack = new ItemStack(item).getMaxStackSize();
        while (count > 0) {
            int take = Math.min(count, maxStack);
            count -= take;
            ItemEntity result = new ItemEntity(level, center.x, center.y, center.z, new ItemStack(item, take));
            result.setDefaultPickUpDelay();
            level.addFreshEntity(result);
        }
    }

    /** A sharp crack plus a scatter of crystalline shards marking a crushed crystal. */
    private static void playShatterEffects(Level level, Vec3 center) {
        level.playSound(null, center.x, center.y, center.z,
            SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                center.x, center.y + 0.3, center.z, 30, 0.4, 0.2, 0.4, 0.15);
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                center.x, center.y + 0.3, center.z, 20, 0.4, 0.3, 0.4, 0.1);
        }
    }
}
