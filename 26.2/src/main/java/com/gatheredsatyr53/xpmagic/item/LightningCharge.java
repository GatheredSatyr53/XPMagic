package com.gatheredsatyr53.xpmagic.item;

import com.gatheredsatyr53.xpmagic.Config;
import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The lightning charge a weapon carries over from the Memory Crystals it was forged from, and the
 * attack damage that charge buys. The charge itself is absorbed by the crystals in
 * {@link com.gatheredsatyr53.xpmagic.LightningChargingHandler}; this class only spends it.
 *
 * <p>Note what is deliberately <em>not</em> carried over: {@code xp_capacity}. The XP Keeping Machine
 * accepts any stack holding that component as a matrix (see {@code XPKeepingMachineMenu.isMatrix}), so
 * a tool carrying it could be fed into the machine and drained for cocktails. Only
 * {@code lightning_charge} crosses from crystal to tool.
 */
public final class LightningCharge {

    /**
     * Our own attack-damage modifier, kept apart from the item's base damage entry
     * ({@code Item.BASE_ATTACK_DAMAGE_ID}) so the bonus reads as its own tooltip line instead of
     * silently folding into the weapon's base number.
     */
    private static final Identifier DAMAGE_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(XPMagic.MODID, "lightning_charge_bonus");

    private LightningCharge() {}

    /**
     * Records {@code charge} on the stack and (re)computes the attack damage it grants.
     *
     * <p>Safe to call repeatedly on the same stack: {@code withModifierAdded} drops any existing entry
     * with our attribute and id before adding the new one, so the bonus is recomputed rather than
     * stacked on top of itself.
     */
    public static void applyTo(ItemStack stack, int charge) {
        if (charge <= 0) return;

        stack.set(XPMagic.LIGHTNING_CHARGE.get(), charge);

        double bonus = Math.min(charge * Config.lightningDamagePerCharge, Config.lightningMaxDamageBonus);

        // getOrDefault falls back to the item's default component — for a weapon that already holds the
        // base damage and attack speed put there by Properties.sword(...). Building on it keeps those;
        // starting from EMPTY would strip the weapon down to bare fists.
        ItemAttributeModifiers modifiers =
            stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers.withModifierAdded(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(DAMAGE_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND));

        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); // same mark a charged crystal carries
    }
}
