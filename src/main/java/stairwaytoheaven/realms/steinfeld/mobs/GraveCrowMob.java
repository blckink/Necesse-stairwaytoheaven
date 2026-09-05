package stairwaytoheaven.realms.steinfeld.mobs;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.manager.EntityManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.CrazedRavenMob;
import necesse.entity.projectile.CrazedRavenFeatherProjectile;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.mobs.SkyMobTiers;
import stairwaytoheaven.mobs.SkySpawnRules;

/**
 * Grave Crow — the flying resident, {@code docs/WORLD_DESIGN.md} §7's fourth
 * named enemy and the Reach's ranged threat.
 *
 * <h2>Vanilla base</h2>
 * {@link CrazedRavenMob}, art {@code mobs/crazedraven} — a black corvid that
 * fires a spread of feather projectiles rather than closing to melee, which
 * is exactly the RANGED role this mob fills on the realm's ladder. It is the
 * closest thing to a crow the game draws.
 *
 * <h2>Tier: Steinfeld row, RANGED role</h2>
 * {@link SteinfeldTier}: realm row 2100 / 200 / 50, RANGED role
 * {@code x0.70} HP / {@code x0.85} damage = <b>1470 HP / 170 damage / 50
 * armour</b> — the exact line {@code SteinfeldTier}'s own class comment
 * prints for this mob. Vanilla's raven is 400 HP / 100 damage / 30 armour and
 * is left untouched; only this subclass is retuned.
 *
 * <h2>Why {@code init()} rebuilds the whole attack instead of one method</h2>
 * {@code CrazedRavenMob.fireCrazedRavenProjectiles} is a {@code private
 * static} method that reads the vanilla class's own shared
 * {@code public static GameDamage damage} field directly — there is no
 * override seam, and writing to that field would re-tune vanilla's own raven
 * everywhere it spawns (the same reasoning {@code RimeSentryMob} records for
 * {@code FrostSentryMob.damage}). So {@link #init} replaces the AI outright,
 * on vanilla's own {@code ConfusedPlayerChaserWandererAI} shape (480 search,
 * 320 shoot range, 20s wander), and {@link #fireFeathers} is this class's own
 * copy of the FIRST of vanilla's three feather volleys — two feathers at
 * {@code +/-30} degrees — rather than the full three-wave, six-feather combo.
 * One clean volley against OUR damage is the realm's ranged threat; the
 * shortened combo is a deliberate simplification, not a missing feature.
 */
public class GraveCrowMob extends CrazedRavenMob {

    /** Steinfeld row 2100 x 0.70 (ranged role) = 1470 on Classic. */
    public static final MaxHealthGetter MAX_HEALTH =
            SteinfeldTier.health(SkyMobTiers.ROLE_RANGED_HP);
    /** Steinfeld row 200 x 0.85 (ranged role) = 170. */
    public static final GameDamage DAMAGE =
            SteinfeldTier.damage(SkyMobTiers.ROLE_RANGED_DAMAGE);
    /** The realm's armour, unreduced. */
    public static final int ARMOR = SteinfeldTier.ARMOR;

    /**
     * A grave crow works the treeline for what the wind carries loose: mostly
     * nothing, sometimes a scrap of Spirit Moss caught in a wing. Vanilla's
     * own raven feather and egg drops are replaced outright — a corvid here
     * has nothing to do with the surface's chicken coop.
     */
    public static LootTable lootTable = new LootTable(
            ChanceLootItem.between(0.25F, "spiritmoss", 1, 2),
            ChanceLootItem.between(0.10F, "echoshard", 1, 1));

    public GraveCrowMob() {
        super();
        this.difficultyChanges.setMaxHealth(MAX_HEALTH);
        this.setArmor(ARMOR);
    }

    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this,
                new ConfusedPlayerChaserWandererAI<GraveCrowMob>(() -> false, 480, 320, 20000, false, false) {
                    @Override
                    public boolean attackTarget(GraveCrowMob mob, Mob target) {
                        if (!mob.canAttack()) {
                            return false;
                        }
                        fireFeathers(mob, target);
                        this.wanderAfterAttack = GameRandom.globalRandom.getChance(0.75F);
                        return true;
                    }
                });
    }

    /**
     * Vanilla's OWN first volley ({@code CrazedRavenMob.java:75-79}), copied
     * rather than reached into: {@code mob.attack(...)} plays the attack
     * animation and lets the client see the wind-up, then two feathers fan
     * {@code +/-30} degrees apart against {@link #DAMAGE}.
     */
    private static void fireFeathers(GraveCrowMob mob, Mob target) {
        EntityManager entityManager = mob.getLevel().entityManager;
        mob.attack(target.getX(), target.getY(), false);
        for (int i = 0; i < 2; i++) {
            CrazedRavenFeatherProjectile projectile = new CrazedRavenFeatherProjectile(
                    mob.getLevel(), mob.x, mob.y, target.x, target.y, 80.0F, 576, DAMAGE, mob, 50);
            projectile.setAngle(projectile.getAngle() - 30.0F + (float) (i * 60));
            entityManager.projectiles.add(projectile);
        }
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return SkySpawnRules.daylightSpawn(this, server, client, targetX, targetY);
    }

    /**
     * Bestiary face: it subclasses CrazedRavenMob, so it wears that creature's
     * face in the journal too. {@code Mob.getMobIcon()} is overridable and
     * {@code FormJournalEntryComponent} asks the MOB rather than the registry,
     * so this needs no PNG of its own -- see {@link stairwaytoheaven.mobs.BorrowedMobIcon}
     * for why borrowing the face is the right answer and not a shortcut.
     */
    @Override
    public necesse.gfx.gameTexture.GameTexture getMobIcon() {
        return stairwaytoheaven.mobs.BorrowedMobIcon.from("crazedraven", super.getMobIcon());
    }

}
