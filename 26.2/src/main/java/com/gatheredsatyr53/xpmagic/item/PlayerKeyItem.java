package com.gatheredsatyr53.xpmagic.item;

import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.nbt.PlayerOwner;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A name tag bound to a single player. Only that player may place their key into
 * an XP Keeping Machine, and the machine drains experience from that player.
 * A blank key is bound to whoever right-clicks it first.
 */
public class PlayerKeyItem extends Item {

    public PlayerKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.has(XPMagic.PLAYER_OWNER.get()))
            return InteractionResult.PASS;
        if (!level.isClientSide())
            stack.set(XPMagic.PLAYER_OWNER.get(), new PlayerOwner(player.getUUID(), player.getGameProfile().name()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        if (stack.has(XPMagic.PLAYER_OWNER.get()))
            stack.addToTooltip(XPMagic.PLAYER_OWNER.get(), context, display, builder, flag);
        else
            builder.accept(Component.translatable("item.xpmagic.player_key.unbound"));
    }
}
