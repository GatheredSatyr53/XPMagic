package com.gatheredsatyr53.xpmagic.nbt;

import com.mojang.serialization.Codec;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ConsumableListener;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;

/**
 * Experience stored inside an XP Cocktail. Replaces the 1.12 "StoredExp" NBT tag.
 * Grants the experience back when the cocktail is drunk and shows the amount in the tooltip.
 */
public record StoredExp(int amount) implements ConsumableListener, TooltipProvider {

    public static final Codec<StoredExp> CODEC = ExtraCodecs.NON_NEGATIVE_INT.xmap(StoredExp::new, StoredExp::amount);
    public static final StreamCodec<RegistryFriendlyByteBuf, StoredExp> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, StoredExp::amount, StoredExp::new
    );

    @Override
    public void onConsume(Level level, LivingEntity user, ItemStack stack, Consumable consumable) {
        if (user instanceof Player player)
            player.giveExperiencePoints(this.amount);
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable("item.xpmagic.xp_cocktail.stored_exp", this.amount));
    }
}
