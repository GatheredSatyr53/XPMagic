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
 * ({@code powderPerCrystal} powder each). The blast compacts the powder — packing it
 * denser than the loose grains it came from — so each crystal holds more than the sum of
 * its inputs: base + a random 0..{@code crystalBonusMax} bonus xp_capacity from compaction.
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
                && item.isAlive()
                && item.getItem().is(XPMagic.MEMORY_POWDER.get())) {
                powders.add(item);
                totalPowder += item.getItem().getCount();
            }
        }

        int crystals = totalPowder / powderPerCrystal;
        if (crystals == 0) return; // not enough powder for a single crystal

        // Shield the powders from the blast itself so the leftover (odd) powder survives,
        // then consume the reacted ones by hand.
        event.getAffectedEntities().removeAll(powders);

        int toConsume = crystals * powderPerCrystal;
        for (ItemEntity item : powders) {
            if (toConsume == 0) break;
            ItemStack stack = item.getItem();
            int take = Math.min(toConsume, stack.getCount());
            toConsume -= take;
            stack.shrink(take);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack); // re-sync the surviving remainder to clients
            }
        }

        // Spawn the crystals at the blast centre. Each is rolled individually, since its bonus
        // xp_capacity is stored per-stack — so crystals with different rolls won't merge.
        Vec3 center = event.getExplosion().center();
        int baseCapacity = XPMagic.MEMORY_CRYSTAL.get().getDefaultInstance()
                                                        .getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
        RandomSource random = level.getRandom();
        for (int i = 0; i < crystals; i++) {
            ItemStack crystal = new ItemStack(XPMagic.MEMORY_CRYSTAL.get());
            int bonus = Config.crystalBonusMax > 0 ? random.nextInt(Config.crystalBonusMax + 1) : 0;
            crystal.set(XPMagic.XP_CAPACITY.get(), baseCapacity + bonus);
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
