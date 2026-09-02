package stairwaytoheaven.realms.crooked;

import java.awt.Color;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.level.gameTile.TerrainSplatterTile;

/**
 * Wrong-Way Tile — §13's own name, and the realm's loudest answer to the
 * player's one-line theme: <i>"reality no longer works properly."</i>
 *
 * <p><b>Borrowed sheet:</b> vanilla {@code tiles/ascendedvoid_splat.png},
 * 224x192 — flat black with a scatter of holes. It is the ground where the path
 * stops being a path.
 *
 * <h2>What it does</h2>
 * It gives you SPEED and takes away FRICTION, so a wrong-way run carries you
 * past where you aimed and you cannot stop when you get there.
 * {@code WORLD_DESIGN.md} A3.6 asks for <i>"paths that run visibly wrong"</i>,
 * and this is the version of that the engine will actually pay for.
 *
 * <p><b>The numbers are calibrated, not chosen.</b> Vanilla's {@code IceTile}
 * is <b>VERIFIED [jar]</b> {@code SPEED +0.25} and {@code FRICTION -0.75}, both
 * behind a {@code mob.isFlying()} guard. This is one step past it in each
 * direction — far enough that a corridor of it is a place you have to plan for,
 * near enough that it is recognisably the same mechanic a player already knows
 * from a frozen lake.
 *
 * <p><b>It never damages anything.</b> The realm's danger is its guards
 * ({@link CrookedPressure}); a floor that hurt you for standing on it would be
 * a trap rather than a joke, and A4.1's whole complaint is about being
 * interrupted rather than about being weak.
 *
 * <h2>Why it draws under everything</h2>
 * {@code getTerrainPriority} is {@code PRIORITY_TERRAIN_BOT} (0), the bottom of
 * the terrain band, so the striped and violet grounds bleed INTO it rather than
 * the other way round. That is what makes a wrong-way run read as a hole torn
 * in the ground instead of as a black stripe painted on top of it.
 */
public class WrongWayTile extends CrookedGroundTile {

    public WrongWayTile() {
        super("ascendedvoid", new Color(16, 14, 22));
    }

    @Override
    public ModifierValue<Float> getSpeedModifier(Mob mob) {
        return mob.isFlying()
                ? super.getSpeedModifier(mob)
                : new ModifierValue<>(BuffModifiers.SPEED, 0.30F);
    }

    @Override
    public ModifierValue<Float> getFrictionModifier(Mob mob) {
        return mob.isFlying()
                ? super.getFrictionModifier(mob)
                : new ModifierValue<>(BuffModifiers.FRICTION, -0.85F);
    }

    @Override
    public int getTerrainPriority() {
        return TerrainSplatterTile.PRIORITY_TERRAIN_BOT;
    }
}
