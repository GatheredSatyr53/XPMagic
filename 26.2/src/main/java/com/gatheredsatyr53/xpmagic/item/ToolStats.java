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

import java.util.List;

/**
 * The single place that turns a Memory Crystal tool's components into the numbers it fights or digs
 * with. Two sources feed one stat, and which stat that is depends on the tool's profile:
 *
 * <ul>
 * <li>{@code lightning_charge} — carried over from the crystals at the forge and paid out at once
 *     (see {@link ChargedToolRecipe}). It never grows afterward.</li>
 * <li>{@code evolution_potential} — earned point by point through use, and spent in whole steps of
 *     {@code evolutionStepCost}. Its ceiling, {@code max_evolution_potential}, was decided at the
 *     forge from the crystals' {@code xp_capacity}: a tool can only grow as far as the density of
 *     the crystals it was made from allows.</li>
 * </ul>
 *
 * <p>A tool grows along three attributes, not one, and they come in over the course of its life:
 *
 * <ul>
 * <li>The <em>primary</em> stat rises from the first step — attack damage for a weapon
 *     ({@link XPMagic#EVOLVING_WEAPONS}), mining efficiency for a digger
 *     ({@link XPMagic#EVOLVING_DIGGERS}). Its rate rides on the tool as {@code evolution_gain},
 *     because it has to be set against how many steps that tool can reach — which follows from how
 *     many crystals its recipe takes, and so differs between a two-crystal sword and a three-crystal
 *     axe. A datapack can grant primary growth to a tool this mod never registered by tagging it and
 *     giving it the component; this class needs to know nothing about it.</li>
 * <li>Two <em>milestones</em> then unlock a second and third attribute at fixed fractions of full
 *     growth ({@link #SECONDARY_AT} and {@link #TERTIARY_AT}) — a weapon gains knockback then luck, a
 *     digger gains reach then luck. Each ramps from nothing at its threshold to a target bonus at full
 *     growth, and that target is a plain number <em>in the milestone attribute's own units</em>
 *     (blocks of reach, points of knockback), decoupled from {@code evolution_gain}. This is the whole
 *     reason milestones are priced separately: +2 reach is a lot and +12 damage is a lot, and a single
 *     rate cannot mean both — pricing reach off the primary's gain once put reach near +8 blocks.</li>
 * </ul>
 *
 * <p>The primary digger stat is mining efficiency, an attribute rather than a rewrite of the
 * {@code minecraft:tool} component on purpose: vanilla only adds it when the tool's own speed for that
 * block already beats 1.0 (see {@code Player#getDestroySpeed}), so a grown pickaxe tears through stone
 * but digs dirt no faster than a fresh one. The tool gets better at its job, not at everything — which
 * is also why the reach milestone uses {@code block_interaction_range} (a flat, ungated reach) and not
 * {@code block_break_speed} (an ungated multiplier that would speed up dirt too).
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

    /**
     * One id per milestone slot, distinct from each other and from the primary bonus, so two milestones
     * never collide even if some datapack later points both at the same attribute (a shared id on a
     * shared attribute would silently overwrite one — see {@code ItemAttributeModifiers.Entry.matches},
     * which keys on attribute + id).
     */
    private static final Identifier[] MILESTONE_IDS = {
        Identifier.fromNamespaceAndPath(XPMagic.MODID, "evolution_milestone_1"),
        Identifier.fromNamespaceAndPath(XPMagic.MODID, "evolution_milestone_2"),
    };

    /** Fractions of full growth at which the second and third attributes begin to come in. */
    public static final double SECONDARY_AT = 0.3;
    public static final double TERTIARY_AT = 0.7;

    /**
     * A secondary attribute that ramps in over the tail of a tool's growth: nothing until {@code start}
     * of full growth, then linearly up to {@code target} — expressed in the attribute's own units — at
     * full growth. Priced entirely apart from the primary {@code evolution_gain} for the reason spelled
     * out on the class: reach and damage do not share a scale.
     */
    private record Milestone(double start, Holder<Attribute> attribute, double target) {
        double bonusAt(double fraction) {
            if (fraction <= this.start) return 0.0;
            return this.target * (fraction - this.start) / (1.0 - this.start);
        }
    }

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
     * Rewrites the tool's attribute modifiers from its components: the primary stat from
     * {@code lightning_charge} and {@code evolution_potential}, and the two milestone attributes from
     * how far along its growth the tool is. Always rebuilds from the item's prototype rather than from
     * whatever the stack currently carries, so repeated calls neither stack a bonus on top of itself
     * nor leave a stale one behind; the stack's stats are a pure function of its components.
     *
     * <p>Call after anything that changes {@code lightning_charge}, {@code evolution_potential} or
     * {@code max_evolution_potential}. Harmless on a stack that is neither weapon nor digger.
     */
    public static void recompute(ItemStack stack) {
        Holder<Attribute> primary;
        double fromCharge;
        List<Milestone> milestones;

        int charge = stack.getOrDefault(XPMagic.LIGHTNING_CHARGE.get(), 0);
        double gain = stack.getOrDefault(XPMagic.EVOLUTION_GAIN.get(), 0.0F);
        int steps = steps(stack);
        int maxSteps = maxSteps(stack);

        if (stack.is(XPMagic.EVOLVING_WEAPONS)) {
            primary = Attributes.ATTACK_DAMAGE;
            fromCharge = Math.min(charge * Config.lightningDamagePerCharge, Config.lightningMaxDamageBonus);
            milestones = weaponMilestones();
        } else if (stack.is(XPMagic.EVOLVING_DIGGERS)) {
            primary = Attributes.MINING_EFFICIENCY;
            fromCharge = Math.min(charge * Config.lightningMiningEfficiencyPerCharge,
                                  Config.lightningMaxMiningEfficiencyBonus);
            milestones = diggerMilestones();
        } else {
            return; // not an evolving tool — leave it exactly as it is
        }

        // The prototype holds the base damage and attack speed that Properties.sword(...) and friends
        // put there. Building on it keeps those; starting from EMPTY would strip the tool to bare fists.
        // It is also what makes recompute pure: a bonus whose source is gone is simply not re-added.
        ItemAttributeModifiers modifiers =
            stack.getPrototype().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        if (fromCharge > 0) {
            modifiers = withMainhand(modifiers, primary, LIGHTNING_BONUS_ID, fromCharge);
        }

        double fromEvolution = steps * gain;
        if (fromEvolution > 0) {
            modifiers = withMainhand(modifiers, primary, EVOLUTION_BONUS_ID, fromEvolution);
        }

        // Milestones ramp their own attribute over the tail of growth, each in its own units. Guard the
        // fraction against a zero ceiling; a tool that can't grow contributes no milestones anyway.
        double fraction = maxSteps > 0 ? (double) steps / maxSteps : 0.0;
        for (int i = 0; i < milestones.size(); i++) {
            double bonus = milestones.get(i).bonusAt(fraction);
            if (bonus > 0) {
                modifiers = withMainhand(modifiers, milestones.get(i).attribute(), MILESTONE_IDS[i], bonus);
            }
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);

        if (charge > 0) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); // same mark a charged crystal carries
        }
    }

    /** A weapon gains knockback, then luck. Targets are read live so config edits take effect. */
    private static List<Milestone> weaponMilestones() {
        return List.of(
            new Milestone(SECONDARY_AT, Attributes.ATTACK_KNOCKBACK, Config.evolutionKnockbackBonus),
            new Milestone(TERTIARY_AT, Attributes.LUCK, Config.evolutionLuckBonus));
    }

    /** A digger gains reach, then luck. Reach is flat and ungated — see the class doc on why not speed. */
    private static List<Milestone> diggerMilestones() {
        return List.of(
            new Milestone(SECONDARY_AT, Attributes.BLOCK_INTERACTION_RANGE, Config.evolutionReachBonus),
            new Milestone(TERTIARY_AT, Attributes.LUCK, Config.evolutionLuckBonus));
    }

    private static ItemAttributeModifiers withMainhand(ItemAttributeModifiers modifiers,
                                                       Holder<Attribute> attribute, Identifier id, double amount) {
        return modifiers.withModifierAdded(attribute,
            new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND);
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
