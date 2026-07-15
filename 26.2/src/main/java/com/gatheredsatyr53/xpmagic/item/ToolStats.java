package com.gatheredsatyr53.xpmagic.item;

import com.gatheredsatyr53.xpmagic.Config;
import com.gatheredsatyr53.xpmagic.XPMagic;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The single place that turns a Memory Crystal tool's components into the numbers it fights or digs
 * with. Two sources feed one stat, and which stat that is depends on the tool's profile:
 *
 * <ul>
 * <li>{@code lightning_charge} — carried over from the crystals at the forge and paid out at once
 *     (see {@link ChargedToolRecipe}). It never grows afterwards.</li>
 * <li>{@code evolution_potential} — earned point by point through use, and spent in whole steps of
 *     {@code evolutionStepCost}. Its ceiling, {@code max_evolution_potential}, was decided at the
 *     forge from the crystals' {@code xp_capacity}: a tool can only grow as far as the density of
 *     the crystals it was made from allows.</li>
 * </ul>
 *
 * <p>Weapons ({@link XPMagic#EVOLVING_WEAPONS}) spend both on attack damage; digging tools
 * ({@link XPMagic#EVOLVING_DIGGERS}) spend both on mining efficiency. The tag decides only which stat;
 * <em>how much</em> a step is worth rides on the tool as {@code evolution_gain}, because it has to be
 * set against how many steps that tool can reach — which follows from how many crystals its recipe
 * takes. Between the tag and the component, a datapack can grant evolution to a tool this mod never
 * registered, and this class needs to know nothing about it.
 *
 * <p>Mining efficiency is an
 * attribute rather than a rewrite of the {@code minecraft:tool} component on purpose: vanilla only
 * adds it when the tool's own speed for that block already beats 1.0 (see {@code
 * Player#getDestroySpeed}), so a grown pickaxe tears through stone but digs dirt no faster than a
 * fresh one. The tool gets better at its job, not at everything.
 *
 * <p>Note what is deliberately <em>not</em> carried onto a tool: {@code xp_capacity} itself. The XP
 * Keeping Machine treats any stack holding that component as a matrix (see
 * {@code XPKeepingMachineMenu.isMatrix}), so a tool carrying it could be fed in and drained for
 * cocktails. The crystals' capacity is read at the forge and written out as
 * {@code max_evolution_potential}, which no machine accepts.
 */
public final class ToolStats {

    /**
     * Our own modifier ids, kept apart from the item's base entry ({@code Item.BASE_ATTACK_DAMAGE_ID})
     * so each bonus reads as its own tooltip line instead of folding into the tool's base number. They
     * are also what keeps the two sources from treading on each other: {@code withModifierAdded} only
     * evicts an entry matching both the attribute and the id, so charge and evolution stay separate
     * entries and either can be recomputed without disturbing the other.
     */
    private static final Identifier LIGHTNING_BONUS_ID =
        Identifier.fromNamespaceAndPath(XPMagic.MODID, "lightning_charge_bonus");

    private static final Identifier EVOLUTION_BONUS_ID =
        Identifier.fromNamespaceAndPath(XPMagic.MODID, "evolution_bonus");

    private ToolStats() {}

    /**
     * How many whole steps of growth the tool has earned. Potential is clamped to the tool's ceiling
     * first, so a tool that somehow banked more than it can hold still stops where its crystals said.
     */
    public static int steps(ItemStack stack) {
        int potential = stack.getOrDefault(XPMagic.EVOLUTION_POTENTIAL.get(), 0);
        int ceiling = stack.getOrDefault(XPMagic.MAX_EVOLUTION_POTENTIAL.get(), 0);
        return Math.min(potential, ceiling) / Config.evolutionStepCost;
    }

    /** The most steps this tool will ever reach — what its crystals bought it at the forge. */
    public static int maxSteps(ItemStack stack) {
        return stack.getOrDefault(XPMagic.MAX_EVOLUTION_POTENTIAL.get(), 0) / Config.evolutionStepCost;
    }

    /**
     * The ceiling a summed crystal capacity buys, trimmed down to whole steps.
     *
     * <p>The trim is what keeps the tooltip honest. Capacity rarely divides evenly — three crystals
     * that an explosion compacted to 67 between them are worth 1340 points, but the thirteenth and last
     * step lands at 1300, and a player mining out that remaining 40 would be earning nothing while the
     * numbers still climbed. Rounding down means potential and growth run out together.
     */
    public static int ceilingFrom(int capacity) {
        int raw = capacity * Config.evolutionPerCapacity;
        return raw - (raw % Config.evolutionStepCost);
    }

    /**
     * Rewrites the tool's attribute modifiers from its two components. Always rebuilds from the item's
     * prototype rather than from whatever the stack currently carries, so repeated calls neither stack
     * a bonus on top of itself nor drift; the stack's stats are a pure function of its components.
     *
     * <p>Call after anything that changes {@code lightning_charge}, {@code evolution_potential} or
     * {@code max_evolution_potential}. Harmless on a stack that is neither weapon nor digger.
     */
    public static void recompute(ItemStack stack) {
        Holder<Attribute> attribute;
        double fromCharge;

        int charge = stack.getOrDefault(XPMagic.LIGHTNING_CHARGE.get(), 0);

        // What a step pays out rides on the tool itself, so the two profiles below only decide which
        // stat it lands in. A tool in a profile tag but carrying no gain simply never grows.
        double gain = stack.getOrDefault(XPMagic.EVOLUTION_GAIN.get(), 0.0F);
        double fromEvolution = steps(stack) * gain;

        if (stack.is(XPMagic.EVOLVING_WEAPONS)) {
            attribute = Attributes.ATTACK_DAMAGE;
            fromCharge = Math.min(charge * Config.lightningDamagePerCharge, Config.lightningMaxDamageBonus);
        } else if (stack.is(XPMagic.EVOLVING_DIGGERS)) {
            attribute = Attributes.MINING_EFFICIENCY;
            fromCharge = Math.min(charge * Config.lightningMiningEfficiencyPerCharge,
                                  Config.lightningMaxMiningEfficiencyBonus);
        } else {
            return; // not an evolving tool — leave it exactly as it is
        }

        // The prototype holds the base damage and attack speed that Properties.sword(...) and friends
        // put there. Building on it keeps those; starting from EMPTY would strip the tool to bare fists.
        ItemAttributeModifiers modifiers =
            stack.getPrototype().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        if (fromCharge > 0) {
            modifiers = modifiers.withModifierAdded(attribute,
                new AttributeModifier(LIGHTNING_BONUS_ID, fromCharge, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        }
        if (fromEvolution > 0) {
            modifiers = modifiers.withModifierAdded(attribute,
                new AttributeModifier(EVOLUTION_BONUS_ID, fromEvolution, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);

        if (charge > 0) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); // same mark a charged crystal carries
        }
    }

    /**
     * Adds {@code points} of evolution potential and recomputes the tool's stats if that crossed into a
     * new step. Returns true on a step, so callers can celebrate it.
     *
     * <p>Potential stops accruing at the ceiling: banking points a tool can never spend would let a
     * player grind a worn-out tool "nearly there" forever, and the tooltip would lie about progress.
     */
    public static boolean addPotential(ItemStack stack, int points) {
        int ceiling = stack.getOrDefault(XPMagic.MAX_EVOLUTION_POTENTIAL.get(), 0);
        if (ceiling <= 0) return false; // forged without crystals behind it — nothing to grow into

        int before = steps(stack);
        int potential = stack.getOrDefault(XPMagic.EVOLUTION_POTENTIAL.get(), 0);
        if (potential >= ceiling) return false;

        stack.set(XPMagic.EVOLUTION_POTENTIAL.get(), Math.min(potential + points, ceiling));

        if (steps(stack) == before) return false;
        recompute(stack);
        return true;
    }
}
