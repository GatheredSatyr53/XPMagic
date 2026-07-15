package com.gatheredsatyr53.xpmagic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Soul-fire transformation: a Memory Crystal item entity resting in soul fire (blue flame) has its
 * {@code xp_capacity} slowly burned away — one point every {@link Config#soulFireTicksPerCapacity}
 * ticks the item spends in the flame. When the capacity reaches 0 the crystal is spent and finally
 * transforms into a Time Crystal. The whole stack drains together and converts at once.
 *
 * <p>Both crystals are {@code fireResistant} (see their registration), so soul fire never ignites or
 * damages them — only this handler acts on them, and the drain can safely take its full time.
 *
 * <p>Forge has no per-entity tick event, so we hook the server level tick and query the level's item
 * entities, pre-filtered to live crystals actually standing in soul fire.
 *
 * <p>{@code soul_fire_time} paces the drain (ticks accrued toward the next point) and doubles as the
 * "this item was touched by blue flame" flag: any stack carrying it has spent time in soul fire.
 */
@EventBusSubscriber(modid = XPMagic.MODID)
public final class SoulFireHandler {

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return; // server-authoritative

        // getEntities collects into a fresh list, so mutating the entity while iterating is safe.
        for (ItemEntity item : level.getEntities(EntityTypes.ITEM, SoulFireHandler::isTransforming)) {
            tick(level, item);
        }
    }

    /** A live item entity touching a soul-fire block and holding something we can transform. */
    private static boolean isTransforming(ItemEntity item) {
        return item.isAlive()
            // Which items soul fire transforms. Swap or extend this guard as the recipe grows.
            && item.getItem().is(XPMagic.MEMORY_CRYSTAL.get())
            && touchesSoulFire(item);
    }

    /**
     * A resting item sits right on the boundary between the soul-fire cell and the block beneath it,
     * so {@code blockPosition()} (its feet) can land on either. Test every block its bounding box
     * overlaps instead — the same overlap vanilla uses to decide the flame touches it.
     */
    private static boolean touchesSoulFire(ItemEntity item) {
        return BlockPos.betweenClosedStream(item.getBoundingBox())
                       .anyMatch(pos -> item.level().getBlockState(pos).is(Blocks.SOUL_FIRE));
    }

    private static void tick(ServerLevel level, ItemEntity item) {
        ItemStack stack = item.getItem();
        int capacity = stack.getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
        if (capacity <= 0) {          // nothing left to burn off — finish it
            finish(level, item, stack.getCount());
            return;
        }

        // soul_fire_time is the total ticks spent in the flame (monotonic), so time / ticksPerCapacity
        // is exactly how much capacity has been burned away — which the tooltip reads back.
        int time = stack.getOrDefault(XPMagic.SOUL_FIRE_TIME.get(), 0) + 1;
        stack.set(XPMagic.SOUL_FIRE_TIME.get(), time);

        if (time % Config.soulFireTicksPerCapacity != 0) { // not a full interval yet
            item.setItem(stack);                           // re-sync the accrued time to clients
            return;
        }

        // Crossed a full interval: strip one point of capacity off the stack.
        int newCapacity = capacity - 1;
        stack.set(XPMagic.XP_CAPACITY.get(), newCapacity);
        item.setItem(stack);

        // How far along the burn is: 0 when full, ~1 as it empties. Referenced against the crystal's
        // baked-in base capacity, so a normal crystal ramps cleanly across its whole drain.
        int base = item.getItem().getItem().getDefaultInstance().getOrDefault(XPMagic.XP_CAPACITY.get(), newCapacity);
        float progress = Mth.clamp(1.0F - (float) newCapacity / base, 0.0F, 1.0F);
        playDrainEffects(level, item, progress);

        if (newCapacity <= 0) {       // fully spent this tick — transform now
            finish(level, item, stack.getCount());
        }
    }

    /** Capacity is gone: the whole stack becomes Time Crystals. */
    private static void finish(ServerLevel level, ItemEntity item, int count) {
        item.setItem(new ItemStack(XPMagic.TIME_CRYSTAL.get(), count));
        playFinishEffects(level, item);
    }

    /**
     * Screaming souls escaping the crystal, growing louder, higher and denser as it empties
     * ({@code progress} 0 -> 1).
     */
    private static void playDrainEffects(ServerLevel level, ItemEntity item, float progress) {
        float volume = 0.4F + progress * 0.9F;   // 0.4 -> 1.3
        float pitch = 0.2F + progress * 0.9F;    // 0.2 -> 1.1 (more frantic near the end)
        level.playSound(null, item.getX(), item.getY(), item.getZ(),
            SoundEvents.GHAST_HURT, SoundSource.BLOCKS, volume, pitch);

        int flames = 4 + (int) (progress * 22);  // 4 -> 26
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            item.getX(), item.getY() + 0.2, item.getZ(), flames, 0.2, 0.25, 0.2, 0.02);
        level.sendParticles(ParticleTypes.SOUL,
            item.getX(), item.getY() + 0.2, item.getZ(), 2 + (int) (progress * 10), 0.2, 0.2, 0.2, 0.03);
    }

    /** A loud final wail, a crystalline chime and a big burst of souls as the transformation lands. */
    private static void playFinishEffects(ServerLevel level, ItemEntity item) {
        level.playSound(null, item.getX(), item.getY(), item.getZ(),
            SoundEvents.GHAST_SCREAM, SoundSource.BLOCKS, 1.6F, 1.3F);
        level.playSound(null, item.getX(), item.getY(), item.getZ(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.SOUL,
            item.getX(), item.getY() + 0.2, item.getZ(), 40, 0.35, 0.4, 0.35, 0.08);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            item.getX(), item.getY() + 0.2, item.getZ(), 30, 0.3, 0.3, 0.3, 0.05);
    }
}
