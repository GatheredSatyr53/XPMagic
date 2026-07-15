package com.gatheredsatyr53.xpmagic;

import com.gatheredsatyr53.xpmagic.item.ToolStats;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Tool evolution: a Memory Crystal tool earns {@code evolution_potential} by being used for what it is
 * for, and grows in steps as that potential mounts (the arithmetic and the growth itself live in
 * {@link ToolStats}). How far it can grow was fixed at the forge — see
 * {@link com.gatheredsatyr53.xpmagic.item.ChargedToolRecipe}.
 *
 * <p>"For what it is for" is the whole point, so each profile earns from its own kind of work and
 * nothing else: a weapon ({@link XPMagic#EVOLVING_WEAPONS}) earns from kills, never from chopping
 * wood; a digging tool ({@link XPMagic#EVOLVING_DIGGERS}) earns from blocks it is the correct tool
 * for, so a pickaxe cannot be levelled on leaves and crops.
 */
@EventBusSubscriber(modid = XPMagic.MODID)
public final class EvolutionHandler {

    @SubscribeEvent
    static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return; // authoritative item changes are server-side

        // The weapon that struck, not the killer's current mainhand: getWeaponItem resolves through the
        // damage's direct entity, so an arrow or a thrown trident credits nothing rather than crediting
        // whatever the shooter happens to be holding now.
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || !weapon.is(XPMagic.EVOLVING_WEAPONS)) return;

        if (!(event.getSource().getEntity() instanceof Player player)) return; // mobs don't grow tools

        if (ToolStats.addPotential(weapon, 1)) {
            playEvolutionEffects(player);
        }
    }

    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) return;

        // NOT event.getTool(): ServerPlayerGameMode.destroyBlock copies the stack before handing it to
        // playerDestroy (the real one has already taken its durability hit by then), so components
        // written to that copy are dropped on the floor. The live stack is the player's own mainhand.
        ItemStack tool = player.getMainHandItem();
        if (!tool.is(XPMagic.EVOLVING_DIGGERS)) return;

        // Correct-tool-for-drops is the "used as intended" test, and vanilla has already made the call:
        // this event only fires at all when the block was harvested properly. Asking the tool component
        // as well is what separates stone from the dirt a pickaxe merely happened to be holding.
        if (!tool.isCorrectToolForDrops(event.getState())) return;

        if (ToolStats.addPotential(tool, 1)) {
            playEvolutionEffects(player);
        }
    }

    /** A chime and a wash of enchant particles: the tool just crossed into a new step. */
    private static void playEvolutionEffects(Entity at) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.6F);

        if (at.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANT,
                at.getX(), at.getY() + 1.0, at.getZ(), 20, 0.4, 0.5, 0.4, 0.15);
        }
    }
}
