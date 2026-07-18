package com.gatheredsatyr53.xpmagic.handler;
import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.Config;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

/**
 * Explosion crafting: Memory Powders caught in a blast fuse into Memory Crystals
 * ({@code powderPerCrystal} powder each). A crystal's base xp_capacity is the capacity of the
 * powder actually consumed for it — so it can never be worth more than its inputs — and the blast
 * then compacts that denser, adding a random 0..{@code crystalBonusMax} bonus on top. With
 * default-capacity powder (10 each) the base lands at the crystal's nominal 20.
 */
@EventBusSubscriber(modid = XPMagic.MODID)
public final class ExplosionCraftingHandler {

    @SubscribeEvent
    static void onDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return; // authoritative item changes happen server-side only

        if (event.getExplosion().radius() < Config.explosionMinRadius) return; // too weak to fuse

        int powderPerCrystal = Config.powderPerCrystal;

        // Gather every Memory Powder item entity the blast touched, and tally the total powder count.
        List<ItemEntity> powders = new ArrayList<>();
        int totalPowder = 0;
        for (Entity entity : event.getAffectedEntities()) {
            if (entity instanceof ItemEntity item
                && item.isAlive()) {
                ItemStack stack = item.getItem();
                if (stack.is(XPMagic.MEMORY_POWDER.get())) {
                    powders.add(item);
                    totalPowder += stack.getCount();
                }
            }
        }

        int crystals = totalPowder / powderPerCrystal;
        if (crystals == 0) return; // not enough powder for a single crystal

        // Shield the powders from the blast itself so the leftover (odd) powder survives,
        // then consume the reacted ones by hand.
        event.getAffectedEntities().removeAll(powders);

        // Consume the reacted powders, summing the xp_capacity they actually carry — the crystals'
        // base capacity is drawn from this, never from a fixed constant, so under-filled powder
        // (e.g. capacity 3 instead of the default 10) can't be laundered into a full 20 crystal.
        int toConsume = crystals * powderPerCrystal;
        int consumedCapacity = 0;
        for (ItemEntity item : powders) {
            if (toConsume == 0) break;
            ItemStack stack = item.getItem();
            int perUnit = stack.getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
            int take = Math.min(toConsume, stack.getCount());
            toConsume -= take;
            consumedCapacity += take * perUnit;
            stack.shrink(take);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack); // re-sync the surviving remainder to clients
            }
        }

        // Spawn the crystals at the blast centre. Each carries an even share of the consumed
        // capacity as its base (remainder spread over the first few so the total is conserved
        // exactly), plus a random compaction bonus. Distinct rolls mean crystals won't merge.
        Vec3 center = event.getExplosion().center();
        int baseEach = consumedCapacity / crystals;
        int remainder = consumedCapacity % crystals;
        RandomSource random = level.getRandom();
        for (int i = 0; i < crystals; i++) {
            int base = baseEach + (i < remainder ? 1 : 0);
            int bonus = Config.crystalBonusMax > 0 ? random.nextInt(Config.crystalBonusMax + 1) : 0;
            ItemStack crystal = new ItemStack(XPMagic.MEMORY_CRYSTAL.get());
            crystal.set(XPMagic.XP_CAPACITY.get(), base + bonus);
            ItemEntity result = new ItemEntity(level, center.x, center.y, center.z, crystal);
            result.setDefaultPickUpDelay();
            level.addFreshEntity(result);
        }

        playSynthesisEffects(level, center);
    }

    /** Crystalline chime plus a burst of enchant/spark particles marking a successful fusion. */
    private static void playSynthesisEffects(Level level, Vec3 center) {
        level.playSound(null, center.x, center.y, center.z,
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.7F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                center.x, center.y + 0.5, center.z, 40, 0.6, 0.6, 0.6, 0.2);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                center.x, center.y + 0.5, center.z, 12, 0.3, 0.3, 0.3, 0.05);
        }
    }
}
