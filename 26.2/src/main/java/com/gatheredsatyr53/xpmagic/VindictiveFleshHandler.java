package com.gatheredsatyr53.xpmagic;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * Vindictive Flesh feeds a Memory Pearl. Put a pearl in an anvil's left slot and flesh in the right,
 * and the pearl comes out packed denser — its {@code xp_capacity} raised above the base 40, at a cost
 * in experience levels.
 *
 * <p>This is the pearl's answer to what lightning is for a crystal (see
 * {@link com.gatheredsatyr53.xpmagic.LightningChargingHandler}): genuine external energy raising a
 * vessel's size, not a dupe. {@code xp_capacity} is how much experience the pearl <em>can</em> hold,
 * never experience it holds unbidden — a bigger pearl still starts empty.
 *
 * <p>Flesh is one <em>independent</em> source among the pearl's several, so its ceiling is on how much
 * flesh itself may add — {@link Config#vindictiveCapacityCap}, tracked on the pearl's
 * {@link XPMagic#VINDICTIVE_CAPACITY} component — not on the pearl's absolute capacity. That is what
 * lets three such sources of 20 compose to a round 100 whatever order they are applied in, and it is
 * why the component is read here and not merely shown in the tooltip: it is this source's own tally.
 * (The tooltip names it for the same reason it exists — so the gain is not mistaken for an explosion's
 * compaction, which never touches a pearl.)
 *
 * <p>The anvil rather than a crafting recipe because a vanilla recipe would drop the pearl's existing
 * capacity on the floor (the reason {@link com.gatheredsatyr53.xpmagic.item.ChargedToolRecipe} exists),
 * while {@link AnvilUpdateEvent} hands us the input stacks to build the output from — carrying every
 * component forward — and gives the experience cost for free.
 */
@EventBusSubscriber(modid = XPMagic.MODID)
public final class VindictiveFleshHandler {

    private VindictiveFleshHandler() {}

    /** What feeding {@code fleshAvailable} flesh to {@code pearl} would produce, or null if nothing would. */
    public record Result(ItemStack output, int fleshUsed, int xpCost) {}

    @SubscribeEvent
    static void onAnvil(AnvilUpdateEvent event) {
        if (!event.getLeft().is(XPMagic.MEMORY_PEARL.get())
            || !event.getRight().is(XPMagic.VINDICTIVE_FLESH.get())) {
            return; // not our combination — leave the anvil to vanilla and other mods
        }

        Result result = feed(event.getLeft(), event.getRight().getCount());
        if (result == null) return; // pearl already packed to the ceiling, or nothing to add

        event.setOutput(result.output());
        event.setMaterialCost(result.fleshUsed()); // how many flesh the take consumes
        event.setXpCost(result.xpCost());
    }

    /**
     * Pure so a game test can exercise it without standing up an anvil menu. Consumes only as much flesh
     * as the pearl's remaining flesh-slice has room for, so a whole stack tops that slice off in one take
     * rather than being wasted — and the slice is measured against what flesh has already added, not
     * against absolute capacity, so another source having grown the pearl does not eat into it.
     */
    public static Result feed(ItemStack pearl, int fleshAvailable) {
        if (pearl.getCount() != 1 || fleshAvailable <= 0) return null; // capacity is per-stack; keep it unambiguous

        int perFlesh = Config.vindictiveCapacityPerFlesh;
        if (perFlesh <= 0) return null;

        int fromFlesh = pearl.getOrDefault(XPMagic.VINDICTIVE_CAPACITY.get(), 0);
        int room = Config.vindictiveCapacityCap - fromFlesh; // flesh's own remaining slice, not absolute
        if (room <= 0) return null; // this source has already given all it can

        int roomInFlesh = (room + perFlesh - 1) / perFlesh;       // flesh needed to fill the room, rounded up
        int used = Math.min(fleshAvailable, roomInFlesh);
        int gained = Math.min(room, used * perFlesh);              // never overshoot the slice
        if (gained <= 0) return null;

        ItemStack output = pearl.copy();
        output.set(XPMagic.XP_CAPACITY.get(), pearl.getOrDefault(XPMagic.XP_CAPACITY.get(), 0) + gained);
        output.set(XPMagic.VINDICTIVE_CAPACITY.get(), fromFlesh + gained);

        return new Result(output, used, used * Config.vindictiveXpCostPerFlesh);
    }
}
