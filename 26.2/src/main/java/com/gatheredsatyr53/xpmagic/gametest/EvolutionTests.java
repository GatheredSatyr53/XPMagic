package com.gatheredsatyr53.xpmagic.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.gatheredsatyr53.xpmagic.Config;
import com.gatheredsatyr53.xpmagic.XPMagic;
import com.gatheredsatyr53.xpmagic.item.ToolStats;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * The arithmetic this mechanic rests on, checked against the real recipe manager rather than by hand.
 * Registered in {@link XPMagicGameTests}; run with {@code ./gradlew runGameTestServer}.
 *
 * <p>These deliberately go through the loaded recipe rather than constructing {@code ChargedToolRecipe}
 * directly — the thing worth guarding is that a crystal in a crafting grid comes out as a tool with the
 * right ceiling, and a hand-built recipe object would not prove the datagen'd JSON still points at our
 * serializer.
 */
public final class EvolutionTests {

    private EvolutionTests() {}

    /**
     * A crystal's lightning charge lives inside its xp_capacity, so the ceiling must be built from the
     * capacity the crystal owes to its own density alone. This is the one piece of arithmetic where
     * being wrong is invisible in play — the tool merely grows further than it should — so it gets a
     * charged crystal and an uncharged one and checks the charge was subtracted exactly once.
     */
    public static final Consumer<GameTestHelper> CAP_EXCLUDES_LIGHTNING = helper -> {
        ItemStack plain = crystal(24, 0);   // 20 base + 4 compaction
        ItemStack charged = crystal(30, 10); // 20 base + 0 compaction + 10 from the sky

        ItemStack sword = craftSword(helper, plain, charged);

        // Only density buys the ceiling: (24 - 0) + (30 - 10) = 44 capacity, worth 880 points — which
        // is then trimmed to whole steps, and 880 is deliberately not a round number of them.
        int expectedCap = ToolStats.ceilingFrom(44);
        int actualCap = sword.getOrDefault(XPMagic.MAX_EVOLUTION_POTENTIAL.get(), 0);
        helper.assertTrue(actualCap == expectedCap,
            "ceiling should exclude the lightning share: expected " + expectedCap + ", got " + actualCap);
        helper.assertTrue(actualCap < 44 * Config.evolutionPerCapacity,
            "this case is meant to have a remainder to trim; if it no longer does, it stops testing the trim");

        // The charge itself still crosses over whole, and still pays out at once.
        int charge = sword.getOrDefault(XPMagic.LIGHTNING_CHARGE.get(), 0);
        helper.assertTrue(charge == 10, "charge should pool onto the tool: expected 10, got " + charge);

        double damage = bonusDamage(sword);
        double expectedDamage = 10 * Config.lightningDamagePerCharge;
        helper.assertTrue(Math.abs(damage - expectedDamage) < 1.0E-4,
            "charge should buy damage at once: expected " + expectedDamage + ", got " + damage);

        helper.succeed();
    };

    /**
     * A fresh tool must not already be ahead, and the first step must actually move the number — the
     * whole mechanic is invisible if the attribute never changes.
     */
    public static final Consumer<GameTestHelper> STEPS_RAISE_DAMAGE = helper -> {
        ItemStack sword = craftSword(helper, crystal(20, 0), crystal(20, 0));

        helper.assertTrue(ToolStats.steps(sword) == 0, "a fresh tool should be at step 0");
        helper.assertTrue(bonusDamage(sword) == 0.0,
            "an uncharged fresh tool should carry no bonus, got " + bonusDamage(sword));

        int maxSteps = ToolStats.maxSteps(sword);
        helper.assertTrue(maxSteps == 800 / Config.evolutionStepCost,
            "two 20-capacity crystals should buy 800 potential, got " + maxSteps + " steps");

        // One point short of the first step: still nothing.
        ToolStats.addPotential(sword, Config.evolutionStepCost - 1);
        helper.assertTrue(ToolStats.steps(sword) == 0, "should still be at step 0 one point short");
        helper.assertTrue(bonusDamage(sword) == 0.0, "no bonus before the first whole step");

        // The point that crosses over.
        boolean stepped = ToolStats.addPotential(sword, 1);
        helper.assertTrue(stepped, "crossing a step boundary should report a step");
        helper.assertTrue(ToolStats.steps(sword) == 1, "should be at step 1");

        double expected = sword.getOrDefault(XPMagic.EVOLUTION_GAIN.get(), 0.0F);
        double actual = bonusDamage(sword);
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4,
            "step 1 should pay one evolution_gain: expected " + expected + ", got " + actual);

        // Growth stops where the crystals said, however long the grinding goes on.
        ToolStats.addPotential(sword, 10_000);
        helper.assertTrue(ToolStats.steps(sword) == maxSteps,
            "potential should clamp to the ceiling, got " + ToolStats.steps(sword) + " of " + maxSteps);

        helper.succeed();
    };

    /**
     * A tool's stats must be a pure function of its components — recompute rebuilds from the item's
     * prototype precisely so that a bonus whose source is gone leaves no trace behind.
     *
     * <p>Repeated calls are the easy half and would pass either way, since {@code withModifierAdded}
     * evicts any entry with the same attribute and id. The half that actually pins the prototype is
     * below: when a bonus falls to zero its {@code if (bonus > 0)} branch stops running altogether, so
     * a recompute built on the stack's own modifiers would silently keep paying out the stale entry
     * forever. Rebuilding from the prototype is what drops it.
     */
    public static final Consumer<GameTestHelper> RECOMPUTE_IS_PURE = helper -> {
        ItemStack sword = craftSword(helper, crystal(20, 0), crystal(30, 10));

        ToolStats.addPotential(sword, Config.evolutionStepCost * 3);
        double once = bonusDamage(sword);
        helper.assertTrue(once > 0, "three steps and a charge should be worth something, got " + once);

        for (int i = 0; i < 5; i++) {
            ToolStats.recompute(sword);
        }
        helper.assertTrue(Math.abs(once - bonusDamage(sword)) < 1.0E-4,
            "recompute must not compound: " + once + " became " + bonusDamage(sword) + " after five calls");

        // Take both sources away. Every bonus must go with them, base damage must stay.
        sword.set(XPMagic.EVOLUTION_POTENTIAL.get(), 0);
        sword.set(XPMagic.LIGHTNING_CHARGE.get(), 0);
        ToolStats.recompute(sword);

        helper.assertTrue(bonusDamage(sword) == 0.0,
            "a bonus whose source is gone must not survive recompute, got " + bonusDamage(sword));
        helper.assertTrue(hasBaseDamage(sword),
            "recompute must keep the weapon's own base damage from Properties.sword(...)");

        helper.succeed();
    };

    /**
     * Potential and growth must run out together. A ceiling that is not a whole number of steps would
     * leave a tail of points that buy nothing: the tooltip would keep counting up while the tool had
     * quietly stopped growing, which reads as the mechanic being broken rather than finished.
     *
     * <p>Uses crystals compacted to 67 between them — 1340 points, the case from the bug report where
     * the last 40 were dead — and checks the tool is spending its very last point on its last step.
     */
    public static final Consumer<GameTestHelper> CEILING_IS_WHOLE_STEPS = helper -> {
        ItemStack pickaxe = craft(helper, "Memory Crystal Pickaxe",
            crystal(23, 0),  crystal(22, 0), crystal(22, 0), // 67 capacity -> 1340 raw
            ItemStack.EMPTY, rod(),          ItemStack.EMPTY,
            ItemStack.EMPTY, rod(),          ItemStack.EMPTY);

        int ceiling = pickaxe.getOrDefault(XPMagic.MAX_EVOLUTION_POTENTIAL.get(), 0);
        helper.assertTrue(ceiling % Config.evolutionStepCost == 0,
            "a ceiling of " + ceiling + " is not a whole number of "
            + Config.evolutionStepCost + "-point steps");

        // Grind it out completely: the last point of potential must land the last step.
        ToolStats.addPotential(pickaxe, 100_000);
        int potential = pickaxe.getOrDefault(XPMagic.EVOLUTION_POTENTIAL.get(), 0);

        helper.assertTrue(potential == ceiling,
            "a fully ground tool should sit exactly on its ceiling: " + potential + " of " + ceiling);
        helper.assertTrue(ToolStats.steps(pickaxe) == ToolStats.maxSteps(pickaxe),
            "the last point of potential should buy the last step");
        helper.assertTrue(potential == ToolStats.maxSteps(pickaxe) * Config.evolutionStepCost,
            "no potential may be left over that buys nothing: " + potential + " points against "
            + ToolStats.maxSteps(pickaxe) + " steps");

        helper.succeed();
    };

    /**
     * The weapon half of {@link com.gatheredsatyr53.xpmagic.EvolutionHandler}, driven through a real
     * kill rather than by calling the handler: spawn a mob, hit it with a player holding the sword, and
     * see whether the sword in the player's hand grew.
     */
    public static final Consumer<GameTestHelper> KILL_GROWS_WEAPON = helper -> {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper);

        ItemStack sword = craftSword(helper, crystal(20, 0), crystal(20, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);

        LivingEntity victim = helper.spawn(EntityTypes.CHICKEN, BlockPos.ZERO);
        victim.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
        helper.assertTrue(victim.isDeadOrDying(), "the victim should have died");

        int potential = player.getMainHandItem().getOrDefault(XPMagic.EVOLUTION_POTENTIAL.get(), 0);
        helper.assertTrue(potential == 1, "a kill should grant the sword 1 potential, got " + potential);

        helper.succeed();
    };

    /**
     * The digging half, and the one worth the most: it goes through {@code ServerPlayerGameMode
     * .destroyBlock}, which hands {@code BlockDropsEvent} a <em>copy</em> of the tool. A handler writing
     * to that copy would lose every point silently, with nothing in the log to show for it — so this
     * test asserts on the stack still in the player's hand.
     *
     * <p>It also pins the "as intended" rule from the other side: the same pickaxe against dirt, which
     * it is not the correct tool for, must earn nothing.
     */
    public static final Consumer<GameTestHelper> MINING_GROWS_DIGGER = helper -> {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = survivalPlayer(helper);
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);

        ItemStack pickaxe = craftPickaxe(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        helper.assertTrue(player.gameMode.destroyBlock(pos), "the stone should have been destroyed");

        int afterStone = player.getMainHandItem().getOrDefault(XPMagic.EVOLUTION_POTENTIAL.get(), 0);
        helper.assertTrue(afterStone == 1,
            "mining stone with a crystal pickaxe should grant 1 potential, got " + afterStone
            + " (a copied tool stack would leave this at 0)");

        // Dirt: the pickaxe breaks it, but it is not the tool for the job and must earn nothing.
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        helper.assertTrue(player.gameMode.destroyBlock(pos), "the dirt should have been destroyed");

        int afterDirt = player.getMainHandItem().getOrDefault(XPMagic.EVOLUTION_POTENTIAL.get(), 0);
        helper.assertTrue(afterDirt == 1,
            "digging dirt with a pickaxe is not using it as intended and should grant nothing, got "
            + afterDirt);

        helper.succeed();
    };

    /**
     * A survival player: the mock the framework hands out otherwise runs in creative, and a creative
     * player's {@code preventsBlockDrops} short-circuits {@code destroyBlock} before any drops event
     * ever fires — the mining test would then pass by never testing anything.
     */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        return (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
    }

    /** A Memory Crystal with the given total capacity, of which {@code charge} came from lightning. */
    private static ItemStack crystal(int capacity, int charge) {
        ItemStack stack = new ItemStack(XPMagic.MEMORY_CRYSTAL.get());
        stack.set(XPMagic.XP_CAPACITY.get(), capacity);
        if (charge > 0) {
            stack.set(XPMagic.LIGHTNING_CHARGE.get(), charge);
        }
        return stack;
    }

    /**
     * Runs the sword's real pattern through the loaded recipes, so this exercises the datagen'd JSON,
     * the serializer and {@code assemble} together rather than any of them in isolation.
     */
    private static ItemStack craftSword(GameTestHelper helper, ItemStack top, ItemStack middle) {
        return craft(helper, "Memory Crystal Sword",
            ItemStack.EMPTY, top,    ItemStack.EMPTY,
            ItemStack.EMPTY, middle, ItemStack.EMPTY,
            ItemStack.EMPTY, rod(),  ItemStack.EMPTY);
    }

    /** Three plain crystals, so the pickaxe comes out with the ceiling its recipe implies. */
    private static ItemStack craftPickaxe(GameTestHelper helper) {
        return craft(helper, "Memory Crystal Pickaxe",
            crystal(20, 0),  crystal(20, 0), crystal(20, 0),
            ItemStack.EMPTY, rod(),          ItemStack.EMPTY,
            ItemStack.EMPTY, rod(),          ItemStack.EMPTY);
    }

    private static ItemStack rod() {
        return new ItemStack(XPMagic.TIME_CRYSTAL_ROD.get());
    }

    private static ItemStack craft(GameTestHelper helper, String what, ItemStack... grid) {
        ServerLevel level = helper.getLevel(); // ServerLevel, not Level: only its recipeAccess() is the full RecipeManager
        CraftingInput input = CraftingInput.of(3, 3, List.of(grid));

        return level.recipeAccess()
            .getRecipeFor(RecipeType.CRAFTING, input, level)
            .map(found -> found.value().assemble(input))
            .orElseThrow(() -> new AssertionError("no crafting recipe matched the " + what + " pattern"));
    }

    /** The attack damage our modifiers add, ignoring the weapon's own base entry. */
    private static double bonusDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers =
            stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        double total = 0.0;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)
                && entry.modifier().id().getNamespace().equals(XPMagic.MODID)) {
                total += entry.modifier().amount();
            }
        }
        return total;
    }

    /**
     * Whether the weapon still carries the base attack damage the item's own Properties put there —
     * the thing that starting a rebuild from {@code ItemAttributeModifiers.EMPTY} would quietly strip,
     * leaving a sword that hits like a fist.
     */
    private static boolean hasBaseDamage(ItemStack stack) {
        ItemAttributeModifiers modifiers =
            stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)
                && entry.modifier().id().equals(Item.BASE_ATTACK_DAMAGE_ID)) {
                return true;
            }
        }
        return false;
    }

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(XPMagic.MODID, path);
    }
}
