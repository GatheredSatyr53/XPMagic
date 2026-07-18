package com.gatheredsatyr53.xpmagic.item;

import com.gatheredsatyr53.xpmagic.client.GuidebookScreenOpener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The XPMagic guidebook. Right-clicking opens an in-game book explaining the mod's progression, so
 * players discover the machines and recipes that the mod otherwise never advertises.
 *
 * <p>The book screen is client-only, so {@link GuidebookScreenOpener} (which touches client classes)
 * is only referenced on the logical client. On a dedicated server {@code use} runs but the client
 * branch is never taken, so that class is never loaded.
 */
public class GuidebookItem extends Item {
    public GuidebookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            GuidebookScreenOpener.open();
        }
        return InteractionResult.SUCCESS;
    }
}
