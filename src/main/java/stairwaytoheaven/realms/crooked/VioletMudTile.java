package stairwaytoheaven.realms.crooked;

import java.awt.Color;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;

/**
 * Violet Mud — §13's own name for it, and the realm's slow ground.
 *
 * <p><b>Borrowed sheet:</b> vanilla {@code tiles/ascendedcorruption_splat.png},
 * 224x96 — cracked dark plates with violet veins running between them.
 *
 * <p><b>What it does, and where the number comes from.</b> Vanilla's
 * {@code MudTile.getSlowModifier} is <b>VERIFIED [jar]</b>
 * {@code mob.isFlying() ? super.getSpeedModifier(mob) : new
 * ModifierValue(BuffModifiers.SLOW, 0.25F)}. This is the same line at
 * <b>0.35</b> — deeper than ordinary mud, because in a realm whose whole point
 * is that movement has stopped being reliable, the ground that holds you is one
 * half of the joke and {@link WrongWayTile} is the other.
 *
 * <p>The flying guard is kept exactly as vanilla writes it, so nothing that
 * hovers is affected and no AI has to know this tile exists.
 */
public class VioletMudTile extends CrookedGroundTile {

    public VioletMudTile() {
        super("ascendedcorruption", new Color(72, 40, 96));
        this.isOrganic = true;
    }

    @Override
    public ModifierValue<Float> getSlowModifier(Mob mob) {
        return mob.isFlying()
                ? super.getSlowModifier(mob)
                : new ModifierValue<>(BuffModifiers.SLOW, 0.35F);
    }
}
