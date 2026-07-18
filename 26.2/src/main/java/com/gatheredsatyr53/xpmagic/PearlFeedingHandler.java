package com.gatheredsatyr53.xpmagic;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * Packing a Memory Pearl denser on an anvil. Put a pearl in the left slot and one of the feed items in
 * the right, and the pearl comes out with a higher {@code xp_capacity} — above the base 40 — at a cost
 * in experience levels.
 *
 * <p>This is the pearl's answer to what lightning is for a crystal (see
 * {@link com.gatheredsatyr53.xpmagic.LightningChargingHandler}): genuine external energy raising a
 * vessel's size, not a dupe. {@code xp_capacity} is how much experience the pearl <em>can</em> hold,
 * never experience it holds unbidden — a bigger pearl still starts empty.
 *
 * <p>There are three feed items, the mod's three closing acts, each an independent {@link PearlFuel}:
 * Vindictive Flesh, Nostalgic Coal, and the Fruit of Knowledge. The key word is <em>independent</em>:
 * each caps how much <em>it</em> has added — read off its own component — not the pearl's absolute
 * capacity. That is what lets the three slices of 20 compose to a round 100 whatever order they are
 * applied in; a cap on absolute capacity would let whichever source ran last reach the ceiling alone and
 * starve the others. Each source's tally is a real datum (the cap depends on it), not merely a tooltip
 * label — though it earns its tooltip line too, so the gain is not mistaken for an explosion's
 * compaction, which never touches a pearl.
 *
 * <p>The anvil rather than a crafting recipe because a vanilla recipe would drop the pearl's existing
 * capacity on the floor (the reason {@link com.gatheredsatyr53.xpmagic.item.ChargedToolRecipe} exists),
 * while {@link AnvilUpdateEvent} hands us the input stacks to build the output from — carrying every
 * component forward — and gives the experience cost for free.
 */
@EventBusSubscriber(modid = XPMagic.MODID)
public final class PearlFeedingHandler {

    private PearlFeedingHandler() {}

    /**
     * One independent way to pack a pearl denser. Everything that varies between the three acts lives
     * here, so the handler, the {@code feed} arithmetic and the tooltip are all written once. Suppliers
     * throughout because both the registry holders and the config values resolve after class-load.
     *
     * @param ingredient    the item consumed from the anvil's right slot
     * @param tally         the component recording how much capacity this source has added to a pearl
     * @param sliceCap      the most capacity this source may ever add to one pearl
     * @param perItem       capacity gained per ingredient consumed
     * @param xpCostPerItem experience levels charged per ingredient consumed
     * @param tooltipKey    translation key for this source's tooltip line
     * @param color         colour of that line
     */
    public record PearlFuel(Supplier<Item> ingredient,
                            Supplier<DataComponentType<Integer>> tally,
                            IntSupplier sliceCap, IntSupplier perItem, IntSupplier xpCostPerItem,
                            String tooltipKey, ChatFormatting color) {}

    /** The three acts, in the order they were built. Read by both the anvil handler and the tooltip. */
    public static final List<PearlFuel> FUELS = List.of(
        new PearlFuel(XPMagic.VINDICTIVE_FLESH, XPMagic.VINDICTIVE_CAPACITY,
            () -> Config.vindictiveCapacityCap, () -> Config.vindictiveCapacityPerFlesh,
            () -> Config.vindictiveXpCostPerFlesh, "tooltip.xpmagic.vindictive_capacity", ChatFormatting.RED),
        new PearlFuel(XPMagic.NOSTALGIC_COAL, XPMagic.NOSTALGIC_CAPACITY,
            () -> Config.nostalgicCapacityCap, () -> Config.nostalgicCapacityPerCoal,
            () -> Config.nostalgicXpCostPerCoal, "tooltip.xpmagic.nostalgic_capacity", ChatFormatting.YELLOW),
        new PearlFuel(XPMagic.FRUIT_OF_KNOWLEDGE, XPMagic.KNOWLEDGE_CAPACITY,
            () -> Config.knowledgeCapacityCap, () -> Config.knowledgeCapacityPerFruit,
            () -> Config.knowledgeXpCostPerFruit, "tooltip.xpmagic.knowledge_capacity", ChatFormatting.GREEN));

    /** What feeding a stack of one fuel to a pearl would produce, or null if nothing would. */
    public record Result(ItemStack output, int materialCost, int xpCost) {}

    @SubscribeEvent
    static void onAnvil(AnvilUpdateEvent event) {
        if (!event.getLeft().is(XPMagic.MEMORY_PEARL.get())) return;

        for (PearlFuel fuel : FUELS) {
            if (!event.getRight().is(fuel.ingredient().get())) continue;

            Result result = feed(event.getLeft(), fuel, event.getRight().getCount());
            if (result == null) return; // this fuel's slice is already full — nothing to do

            event.setOutput(result.output());
            event.setMaterialCost(result.materialCost()); // how many items the take consumes
            event.setXpCost(result.xpCost());
            return; // one fuel matched; the right slot cannot be two items at once
        }
    }

    /**
     * Pure so a game test can exercise it without standing up an anvil menu. Consumes only as much of
     * {@code available} as the fuel's remaining slice has room for, so a whole stack tops that slice off
     * in one take rather than being wasted — and the slice is measured against what THIS fuel has already
     * added, not against absolute capacity, so another source having grown the pearl does not eat into it.
     */
    public static Result feed(ItemStack pearl, PearlFuel fuel, int available) {
        if (pearl.getCount() != 1 || available <= 0) return null; // capacity is per-stack; keep it unambiguous

        int perItem = fuel.perItem().getAsInt();
        if (perItem <= 0) return null;

        int already = pearl.getOrDefault(fuel.tally().get(), 0);
        int room = fuel.sliceCap().getAsInt() - already; // this fuel's own remaining slice, not absolute
        if (room <= 0) return null; // this source has already given all it can

        int roomInItems = (room + perItem - 1) / perItem;    // items needed to fill the room, rounded up
        int used = Math.min(available, roomInItems);
        int gained = Math.min(room, used * perItem);          // never overshoot the slice
        if (gained <= 0) return null;

        ItemStack output = pearl.copy();
        output.set(XPMagic.XP_CAPACITY.get(), pearl.getOrDefault(XPMagic.XP_CAPACITY.get(), 0) + gained);
        output.set(fuel.tally().get(), already + gained);

        return new Result(output, used, used * fuel.xpCostPerItem().getAsInt());
    }
}
