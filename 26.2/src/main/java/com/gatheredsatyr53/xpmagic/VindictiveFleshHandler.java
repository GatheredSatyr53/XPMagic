package com.gatheredsatyr53.xpmagic;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * Vindictive Flesh feeds a Memory Pearl. Put a pearl in an anvil's left slot and flesh in the right,
 * and the pearl comes out packed denser — its {@code xp_capacity} raised above the base 40, up to
 * {@link Config#vindictivePearlMaxCapacity}, at a cost in experience levels.
 *
 * <p>This is the pearl's answer to what lightning is for a crystal (see
 * {@link com.gatheredsatyr53.xpmagic.LightningChargingHandler}): genuine external energy raising a
 * vessel's size, not a dupe. {@code xp_capacity} is how much experience the pearl <em>can</em> hold,
 * never experience it holds unbidden — a bigger pearl still starts empty. The gain is tracked on its
 * own {@link XPMagic#VINDICTIVE_CAPACITY} component so the tooltip can name it rather than mistake it
 * for an explosion's compaction, which never touches a pearl.
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
     * Pure so a game test can exercise it without standing up an anvil menu. Consumes as much flesh as
     * the pearl has room for and no more, so a stack of flesh tops the pearl off in one take rather than
     * being wasted past the cap.
     */
    public static Result feed(ItemStack pearl, int fleshAvailable) {
        if (pearl.getCount() != 1 || fleshAvailable <= 0) return null; // capacity is per-stack; keep it unambiguous

        int perFlesh = Config.vindictiveCapacityPerFlesh;
        if (perFlesh <= 0) return null;

        int current = pearl.getOrDefault(XPMagic.XP_CAPACITY.get(), 0);
        int room = Config.vindictivePearlMaxCapacity - current;
        if (room <= 0) return null; // already at or past the ceiling

        int roomInFlesh = (room + perFlesh - 1) / perFlesh;       // flesh needed to fill the room, rounded up
        int used = Math.min(fleshAvailable, roomInFlesh);
        int gained = Math.min(room, used * perFlesh);              // never overshoot the ceiling
        if (gained <= 0) return null;

        ItemStack output = pearl.copy();
        output.set(XPMagic.XP_CAPACITY.get(), current + gained);
        output.set(XPMagic.VINDICTIVE_CAPACITY.get(),
                   pearl.getOrDefault(XPMagic.VINDICTIVE_CAPACITY.get(), 0) + gained);

        return new Result(output, used, used * Config.vindictiveXpCostPerFlesh);
    }
}
