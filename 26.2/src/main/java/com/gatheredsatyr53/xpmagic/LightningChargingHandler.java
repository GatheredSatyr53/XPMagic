package com.gatheredsatyr53.xpmagic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Lightning charging: a Memory Crystal item entity struck by lightning absorbs the bolt and gains
 * xp_capacity (up to {@link Config#lightningMaxCapacity}). The lightning share is tracked in the
 * {@code lightning_charge} component, kept apart from the explosion's compaction so both read in the
 * tooltip, and it drives the crystal's glint. The strike is cancelled so the crystal isn't burned.
 *
 * <p>A single {@link LightningBolt} strikes every tick it lives (plus its {@code flashes} re-fire
 * {@code tick()} several more times), so the event fires many times per bolt. We dedupe per bolt —
 * one bolt charges a given item entity exactly once — via a weak map keyed on the bolt.
 */
@Mod.EventBusSubscriber(modid = XPMagic.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LightningChargingHandler {

    /** Item-entity ids each live bolt has already charged. Weakly keyed so bolts clear on discard. */
    private static final WeakHashMap<LightningBolt, Set<Integer>> CHARGED_BY_BOLT = new WeakHashMap<>();

    @SubscribeEvent
    static boolean onStruck(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return false; // don't touch mobs/players
        if (!item.isAlive() || !item.getItem().is(XPMagic.MEMORY_CRYSTAL.get())) return false;

        Level level = item.level();
        if (level.isClientSide()) return false; // fires server-side only, but stay defensive

        // One bolt = one charge per item entity. Skip if this bolt already charged this entity,
        // but still cancel so the repeated strikes never set the crystal on fire.
        Set<Integer> already = CHARGED_BY_BOLT.computeIfAbsent(event.getLightning(), b -> new HashSet<>());
        if (!already.add(item.getId())) return true;

        ItemStack struck = item.getItem();
        int capacity = struck.getOrDefault(XPMagic.XP_CAPACITY.get(), 0);

        // Already saturated: swallow the bolt without changing anything.
        if (capacity >= Config.lightningMaxCapacity || Config.lightningChargePerStrike == 0) {
            playChargeEffects(level, item);
            return true;
        }

        int newCapacity = Math.min(capacity + Config.lightningChargePerStrike, Config.lightningMaxCapacity);
        int added = newCapacity - capacity; // may be less than a full strike right at the cap
        int newCharge = struck.getOrDefault(XPMagic.LIGHTNING_CHARGE.get(), 0) + added;

        // One bolt = one crystal. Peel a single crystal off the stack, charge it, keep the rest raw.
        ItemStack result = new ItemStack(XPMagic.MEMORY_CRYSTAL.get());
        result.set(XPMagic.XP_CAPACITY.get(), newCapacity);
        result.set(XPMagic.LIGHTNING_CHARGE.get(), newCharge);
        result.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); // visible mark of a charged crystal

        struck.shrink(1);
        if (struck.isEmpty()) {
            item.setItem(result);
        } else {
            item.setItem(struck); // re-sync the leftover to clients
            ItemEntity out = new ItemEntity(level, item.getX(), item.getY(), item.getZ(), result);
            out.setDefaultPickUpDelay();
            level.addFreshEntity(out);
            already.add(out.getId()); // this bolt must not re-charge the crystal it just spawned
        }

        playChargeEffects(level, item);
        return true; // cancel the strike: the crystal absorbed it
    }

    /** Crystalline chime plus a burst of electric sparks marking a successful charge. */
    private static void playChargeEffects(Level level, ItemEntity item) {
        level.playSound(null, item.getX(), item.getY(), item.getZ(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);

        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                item.getX(), item.getY() + 0.2, item.getZ(), 30, 0.3, 0.3, 0.3, 0.1);
            server.sendParticles(ParticleTypes.END_ROD,
                item.getX(), item.getY() + 0.2, item.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        }
    }
}
