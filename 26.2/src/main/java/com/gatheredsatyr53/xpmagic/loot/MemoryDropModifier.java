package com.gatheredsatyr53.xpmagic.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Rewards a memory-infused drop when the harvester carries {@code enchantment}. Two modes, chosen by
 * the JSON's {@code add} flag:
 *
 * <ul>
 *   <li><b>replace</b> (default): each {@code from} item has a {@code chance} of becoming a {@code to}
 *       item — the mob-drop swap, e.g. an ender pearl into a memory pearl.
 *   <li><b>add</b>: the original loot is left whole and, with {@code chance}, one {@code to} item is
 *       appended as a bonus. Here {@code from} is unused (and omitted) — the roll keys off the loot
 *       table firing, not off a particular drop, so a gravel table that rolled flint instead of gravel
 *       still gets its grain.
 * </ul>
 *
 * <p>The Saturation enchantment itself is an empty flag — it holds no enchantment effect components,
 * because none of them can touch mob loot. Vanilla's Silk Touch works the same way: the drop swap
 * lives in loot, and the enchantment is only what loot asks about. This modifier is the asking side.
 *
 * <p>Which table and which item are left to the JSON ({@code from}/{@code to} plus a
 * {@code LootTableIdCondition}), so a new pairing costs a data file and no Java.
 */
public class MemoryDropModifier extends LootModifier {

    public static final MapCodec<MemoryDropModifier> CODEC = RecordCodecBuilder.mapCodec(
        instance -> codecStart(instance)
            .and(
                instance.group(
                    Enchantment.CODEC.fieldOf("enchantment").forGetter(m -> m.enchantment),
                    BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("from").forGetter(m -> m.from),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("to").forGetter(m -> m.to),
                    net.minecraft.util.ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(m -> m.chance),
                    Codec.BOOL.optionalFieldOf("add", false).forGetter(m -> m.add)
                )
            )
            .apply(instance, MemoryDropModifier::new)
    );

    private final Holder<Enchantment> enchantment;
    private final Optional<Item> from;
    private final Item to;
    private final float chance;
    private final boolean add;

    public MemoryDropModifier(LootItemCondition[] conditions, int priority, Holder<Enchantment> enchantment,
                              Optional<Item> from, Item to, float chance, boolean add) {
        super(conditions, priority);
        this.enchantment = enchantment;
        this.from = from;
        this.to = to;
        this.chance = chance;
        this.add = add;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // Two loot contexts carry the enchantment differently. Mob drops (entities/...) name the
        // killer as ATTACKING_ENTITY, mirroring EnchantedCountIncreaseFunction; the level goes through
        // EventHooks so other mods' adjustments to it apply to us too (NeoForge asks any mod with an
        // enchantment-driven loot rule to fire EnchantedEntityLootEvent). Block drops (blocks/...) have
        // no attacker at all — the enchantment lives on the mining TOOL, so we read the level off it.
        int level = 0;
        Entity killer = context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);
        if (killer instanceof LivingEntity living) {
            level = EnchantmentHelper.getEnchantmentLevel(this.enchantment, living);
            level = EventHooks.getEntityLootEnchantmentLevel(this.enchantment, level, context);
        }
        if (level == 0) {
            ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
            if (tool != null) {
                level = EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, tool);
            }
        }
        if (level == 0) return generatedLoot;

        RandomSource random = context.getRandom();

        if (this.add) {
            // Bonus on top: the original loot is untouched, and one roll decides a single extra item.
            if (random.nextFloat() >= this.chance) return generatedLoot;
            ObjectArrayList<ItemStack> result = new ObjectArrayList<>(generatedLoot.size() + 1);
            result.addAll(generatedLoot);
            result.add(new ItemStack(this.to));
            return result;
        }

        Item fromItem = this.from.orElseThrow(
            () -> new IllegalStateException("a replace-mode memory_drop modifier needs a 'from' item"));
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>(generatedLoot.size());
        for (ItemStack stack : generatedLoot) {
            if (!stack.is(fromItem)) {
                result.add(stack);
                continue;
            }
            // Rolled per item rather than per stack, so a lucky drop of three pearls can come back
            // part memory and part ordinary instead of flipping all-or-nothing.
            int converted = 0;
            for (int i = 0; i < stack.getCount(); i++) {
                if (random.nextFloat() < this.chance) converted++;
            }
            if (converted < stack.getCount()) {
                result.add(stack.copyWithCount(stack.getCount() - converted));
            }
            if (converted > 0) {
                result.add(new ItemStack(this.to, converted));
            }
        }
        return result;
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
