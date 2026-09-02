package stairwaytoheaven.realms.steinfeld.mobs;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.hostile.CrystalGolemMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.laserProjectile.CrystalGolemBeamProjectile;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkyMobTiers;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Hollow Angel — a broken heaven guardian, {@code docs/WORLD_DESIGN.md} §7's
 * third named resident and the realm's elite.
 *
 * <h2>Vanilla base</h2>
 * {@link CrystalGolemMob}, art {@code mobs/crystalgolem} — a tall crystalline
 * figure in pale pink and sky-blue with a gem crest and arms held open, which
 * charges a beam and fires it down a warning line. It is also, VERIFIED
 * [jar], the mod's OWN measured floor: {@code SteinfeldTier}'s class comment
 * derives the whole ladder from {@code CrystalGolemMob.damage} = 130 and
 * armour 40. Casting the guardian that FOUNDS the mod's difficulty curve as
 * the thing that broke and now guards a dead heaven's ruins is not an
 * accident of convenience — it is the one vanilla body in the game that
 * already reads as "sky-coloured construct," which is what a heaven guardian
 * has to be. Subclassing keeps the charge-and-beam attack, the warning
 * particle line and the crystal death particles unchanged.
 *
 * <h2>Tier: Steinfeld row, ELITE role</h2>
 * {@link SteinfeldTier}: realm row 2100 / 200 / 50, ELITE role
 * {@code x1.40} HP, damage and armour unchanged = <b>2940 HP / 200 damage /
 * 50 armour</b> — the exact line {@code SteinfeldTier}'s own class comment
 * prints for this mob. Vanilla's crystal golem is 500 HP / 130 damage / 40
 * armour and is left untouched; only this subclass is retuned.
 *
 * <h2>Why {@code init()} is NOT overridden</h2>
 * Unlike the mod's other retuned mobs, {@code CrystalGolemMob}'s beam damage
 * does not live inside its AI tree closure — it lives in
 * {@link #getProjectile}, an ordinary overridable instance method that
 * {@code shootAbilityProjectile} calls through {@code this}. Overriding just
 * that one method is enough: {@code init()}'s own
 * {@code CrystalGolemMob.CrystalGolemAI} is inherited unchanged (timing only,
 * 544 search / 320 shoot / 384 stick — none of it is damage), and the call
 * still resolves to THIS class's {@code getProjectile} by ordinary virtual
 * dispatch. That is also why {@code CrystalGolemMob.damage}, the vanilla
 * static every OTHER crystal golem in the world shares, is never written to.
 */
public class HollowAngelMob extends CrystalGolemMob {

    /** Steinfeld row 2100 x 1.40 (elite role) = 2940 on Classic. */
    public static final MaxHealthGetter MAX_HEALTH =
            SteinfeldTier.health(SkyMobTiers.ROLE_ELITE_HP);
    /** Steinfeld row, unmodified by the elite role: 200 damage. */
    public static final GameDamage DAMAGE = SteinfeldTier.damage(100);
    /** The realm's armour, unreduced. */
    public static final int ARMOR = SteinfeldTier.ARMOR;

    /**
     * What is left of a guardian: Echo Shard is its own shattered light, and
     * Heaven Slab is quite literally a piece of it. Quantities at the realm's
     * x1.6 drop value.
     */
    public static LootTable lootTable = new LootTable(
            LootItem.between("echoshard", SteinfeldTier.drop(1), SteinfeldTier.drop(3)),
            ChanceLootItem.between(0.45F, "palestone", 3, 8),
            ChanceLootItem.between(0.20F, "gravesalt", 1, 2));

    public HollowAngelMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    /**
     * Vanilla's own call site is {@code new CrystalGolemBeamProjectile(this
     * .getLevel(), this, this.x, this.y, (float) targetX, (float) targetY,
     * distance, damage, 20)} ({@code CrystalGolemMob.java:207}), reading the
     * shared static. This is that line with {@link #DAMAGE} in its place.
     */
    @Override
    public Projectile getProjectile(int targetX, int targetY, int distance) {
        return new CrystalGolemBeamProjectile(this.getLevel(), this, this.x, this.y,
                (float) targetX, (float) targetY, distance, DAMAGE, 20);
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }
}
