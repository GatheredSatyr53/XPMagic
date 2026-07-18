package com.gatheredsatyr53.xpmagic;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Time Crystal armour insulates its wearer from heat. Each equipped piece cuts incoming fire-type
 * damage (fire, lava, hot floor, fireballs — everything tagged {@link DamageTypeTags#IS_FIRE}) by a
 * quarter, so a full four-piece set is fully immune. Ordinary armour is useless here: fire damage
 * bypasses the armour/toughness attributes entirely, so the reduction is applied by hand before the
 * vanilla damage pipeline runs (LivingIncomingDamageEvent fires after invulnerability checks but
 * before any mitigation).
 *
 * <p>This is deliberately independent of {@code Properties.fireResistant()}, which only stops the
 * dropped item from burning up and never protects the wearer.
 */
@EventBusSubscriber(modid = XPMagic.MODID)
public final class FireProtectionHandler {

    /** Reduction contributed by each worn piece; 4 pieces * 0.25 = full immunity. */
    private static final float REDUCTION_PER_PIECE = 0.25F;

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_FIRE)) return;

        int pieces = wornPieces(event.getEntity());
        if (pieces == 0) return;

        float factor = 1.0F - pieces * REDUCTION_PER_PIECE;
        if (factor <= 0.0F) {
            // Full set: total immunity. Cancel the whole damage sequence — exactly what vanilla does
            // for the Fire Resistance effect (LivingEntity.hurtServer returns early), so there is no
            // hurt sound, red flash, knockback or hit wobble either. Merely setting the amount to 0
            // still lets all of those side effects play.
            event.setCanceled(true);
            return;
        }
        // Partial set: keep the reduced hit, hurt reaction and all. You still get singed, just less.
        event.setAmount(event.getAmount() * factor);
    }

    /** How many of the four Time Crystal armour pieces this entity currently wears. */
    private static int wornPieces(LivingEntity entity) {
        int count = 0;
        if (isPiece(entity.getItemBySlot(EquipmentSlot.HEAD),  XPMagic.TIME_CRYSTAL_HELMET.get()))     count++;
        if (isPiece(entity.getItemBySlot(EquipmentSlot.CHEST), XPMagic.TIME_CRYSTAL_CHESTPLATE.get())) count++;
        if (isPiece(entity.getItemBySlot(EquipmentSlot.LEGS),  XPMagic.TIME_CRYSTAL_LEGGINGS.get()))   count++;
        if (isPiece(entity.getItemBySlot(EquipmentSlot.FEET),  XPMagic.TIME_CRYSTAL_BOOTS.get()))      count++;
        return count;
    }

    private static boolean isPiece(ItemStack stack, Item piece) {
        return stack.is(piece);
    }
}
