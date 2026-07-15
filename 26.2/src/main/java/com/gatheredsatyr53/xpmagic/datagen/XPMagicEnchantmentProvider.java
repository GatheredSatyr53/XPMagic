package com.gatheredsatyr53.xpmagic.datagen;

import com.gatheredsatyr53.xpmagic.XPMagic;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public final class XPMagicEnchantmentProvider {

    public static final ResourceKey<Enchantment> SATURATION =
        ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(XPMagic.MODID, "saturation"));

    private XPMagicEnchantmentProvider() {}

    public static RegistrySetBuilder provideEnchantments() {
        return new RegistrySetBuilder().add(Registries.ENCHANTMENT, bootstrap -> {
            HolderGetter<Item> items = bootstrap.lookup(Registries.ITEM);
            HolderGetter<Enchantment> enchantments = bootstrap.lookup(Registries.ENCHANTMENT);

            // Saturation carries no enchantment effect components on purpose. None of them can touch
            // mob loot, so the drop swap lives in a global loot modifier (loot/MemoryDropModifier)
            // that reads this enchantment off the killer. Vanilla's Silk Touch is built the same way:
            // its only component zeroes block XP, while the actual drop swap sits in loot tables.
            bootstrap.register(
                SATURATION,
                Enchantment.enchantment(
                        Enchantment.definition(
                            items.getOrThrow(XPMagic.EVOLVING_WEAPONS),
                            30,
                            1,
                            Enchantment.dynamicCost(3, 1),
                            Enchantment.dynamicCost(4, 2),
                            2,
                            EquipmentSlotGroup.MAINHAND
                        )
                    )
                    // Looting multiplies a mob's drops, Saturation replaces them — together they are
                    // an argument about the same loot, so keep them off the same weapon. Built as a
                    // direct set because vanilla has no exclusive_set tag covering Looting.
                    .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.LOOTING)))
                    .build(SATURATION.identifier())
            );
        });
    }
}
