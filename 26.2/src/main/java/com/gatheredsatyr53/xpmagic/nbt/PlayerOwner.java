package com.gatheredsatyr53.xpmagic.nbt;

import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * Owner recorded on a Player Key. The XP Keeping Machine drains experience from
 * this player, identified by UUID (stable across name changes); the name is
 * cached only for the tooltip.
 */
public record PlayerOwner(UUID id, String name) implements TooltipProvider {

    public static final Codec<PlayerOwner> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        UUIDUtil.CODEC.fieldOf("id").forGetter(PlayerOwner::id),
        Codec.STRING.fieldOf("name").forGetter(PlayerOwner::name)
    ).apply(inst, PlayerOwner::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerOwner> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, PlayerOwner::id,
        ByteBufCodecs.STRING_UTF8, PlayerOwner::name,
        PlayerOwner::new
    );

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable("item.xpmagic.player_key.owner", this.name).withStyle(ChatFormatting.GRAY));
        if (flag.hasShiftDown()) {
            consumer.accept(Component.literal("UUID: " + id.toString()).withStyle(ChatFormatting.BLUE));
        }
    }
}
