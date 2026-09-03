package stairwaytoheaven.bosses;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.level.maps.Level;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.util.TileText;

/**
 * What one boss portal remembers: whether its guardian is already out.
 *
 * <h2>Why the spawn is written here rather than reused</h2>
 *
 * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B5 is explicit, and it is right.
 * The reusable half is {@code RoyalEggObject.spawnBoss}
 * (RoyalEggObject.java:107-120): {@code MobRegistry.getMob(id, level)}, then
 * {@code level.entityManager.addMob} at a random angle {@code 960F} away, then
 * one {@code misc.bossawoke} chat line. That works on any level at all.
 *
 * <p>{@code BossSpawnPortalMob} is NOT reusable and would be the obvious wrong
 * choice: its {@code interact} calls {@code this.remove(...)} unless the level
 * is an {@code IncursionLevel} (BossSpawnPortalMob.java:160-169). On the sky
 * plane a player's first click would delete the portal and summon nothing.
 * <b>VERIFIED [jar]</b>.
 *
 * <h2>One boss at a time</h2>
 *
 * <p>A portal that summons on every click is a boss-loot faucet, so this
 * remembers the {@code uniqueID} of the mob it last woke and refuses while that
 * mob is alive. The record is saved with the object entity, so it survives a
 * restart — {@code ObjectEntitySave} writes {@link #addSaveData} into the
 * region file and rebuilds the entity through the object's own
 * {@code getNewObjectEntity} on the way back in.
 *
 * <p>A second, cruder guard backs it up: any live mob of the same kind within
 * {@link #BUSY_RADIUS} of the portal counts as "already awake" even if the
 * uniqueID record was lost (an old save, a mob that changed hands). The radius
 * is twice the spawn distance, so it covers exactly the ground a summon of this
 * portal can reach and no more — two portals a realm apart never block each
 * other.
 */
public class BossPortalObjectEntity extends ObjectEntity {

    /**
     * How far from the portal a boss appears, in pixels.
     * {@code RoyalEggObject.spawnBoss}'s own {@code distance = 960.0F}
     * (RoyalEggObject.java:115) — thirty tiles, far enough that the fight does
     * not begin on top of the player.
     */
    public static final float SPAWN_DISTANCE = 960.0F;

    /**
     * How far out "already awake" looks. Twice {@link #SPAWN_DISTANCE}, which
     * is the whole disc a summon from this portal can place a boss in, plus the
     * same again for the boss having walked.
     */
    public static final float BUSY_RADIUS = SPAWN_DISTANCE * 2.0F;

    /**
     * Tries at finding a spawn tile that is neither solid nor Mistsea.
     *
     * <p>Vanilla takes one angle and uses it. It can, because an incursion
     * level is a closed arena; the sky is about 61% open water, so one angle
     * would drop a boss in the sea two times in three. Twelve tries at the same
     * distance is the same idea {@code SkyLevel.placePackAt} uses to keep guard
     * packs out of the Mistsea without moving their site.
     */
    private static final int SPAWN_ATTEMPTS = 12;

    /** Which realm's boss this portal answers to. */
    public final int realm;

    /**
     * {@code uniqueID} of the last boss this portal woke, or 0 for none.
     * Saved, because "already summoned" has to survive a restart.
     */
    private int bossUniqueID;

    public BossPortalObjectEntity(Level level, String type, int realm, int tileX, int tileY) {
        super(level, type, tileX, tileY);
        this.realm = realm;
    }

    @Override
    public void addSaveData(SaveData save) {
        save.addInt("bossUniqueID", this.bossUniqueID);
    }

    @Override
    public void applyLoadData(LoadData save) {
        this.bossUniqueID = save.getInt("bossUniqueID", 0, false);
    }

    /**
     * A player used the portal.
     *
     * <p>Three answers, and each one says so: the realm's key piece has not
     * been built (§B2), the guardian is already out, or the boss wakes.
     */
    public void use(Server server, ServerClient client) {
        Level level = this.getLevel();
        if (level == null || server == null || client == null) {
            return;
        }
        SkyBossLadder.Boss boss = SkyBossLadder.forRealm(this.realm);
        if (boss == null) {
            // Reserved realm (Hell). No portal is registered for one, so this
            // is defensive only -- but a silent click is a bug report, and a
            // sentence is not.
            TileText.at(client, this.tileX, this.tileY, new LocalMessage("misc", "bossportalsilent"));
            return;
        }
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world == null || !world.bossPortalsUnlocked(this.realm)) {
            TileText.at(client, this.tileX, this.tileY, new LocalMessage("misc", "bossportallocked"));
            return;
        }
        if (this.guardianAwake(level, boss)) {
            TileText.at(client, this.tileX, this.tileY, new LocalMessage("misc", "bossportalbusy"));
            return;
        }
        Mob mob = this.summon(level, boss);
        if (mob == null) {
            TileText.at(client, this.tileX, this.tileY, new LocalMessage("misc", "bossportalsilent"));
            return;
        }
        this.bossUniqueID = mob.getUniqueID();
        // Everyone who can see the portal is told, over the portal, rather
        // than in a chat log nobody reads.
        TileText.atAll(server, level, this.tileX, this.tileY,
                new LocalMessage("misc", "bossportalawoke", "name", mob.getLocalization()));
    }

    /** Is the boss this portal woke still out there? */
    private boolean guardianAwake(Level level, SkyBossLadder.Boss boss) {
        if (this.bossUniqueID != 0) {
            Mob known = level.entityManager.mobs.get(this.bossUniqueID, false);
            if (known != null && !known.removed() && known.getHealth() > 0) {
                return true;
            }
        }
        float centreX = this.tileX * 32 + 16;
        float centreY = this.tileY * 32 + 16;
        for (Mob mob : level.entityManager.mobs) {
            if (mob == null || mob.removed() || mob.getHealth() <= 0) {
                continue;
            }
            if (!boss.mobStringID.equals(mob.getStringID())) {
                continue;
            }
            float dx = mob.x - centreX;
            float dy = mob.y - centreY;
            if (dx * dx + dy * dy <= BUSY_RADIUS * BUSY_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wakes the realm's boss, scaled, at a random angle around the portal.
     *
     * <p>Everything about the placement is {@code RoyalEggObject.spawnBoss}'s
     * except the ground check and the {@code ensureTileIsLoaded}, which the sky
     * needs because it is generated region by region: a boss placed thirty tiles
     * away can land in a region nothing has asked for yet.
     */
    private Mob summon(Level level, SkyBossLadder.Boss boss) {
        Mob mob = MobRegistry.getMob(boss.mobStringID, level);
        if (mob == null) {
            return null;
        }
        int spawnX = this.tileX * 32 + 16;
        int spawnY = this.tileY * 32 + 16;
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            float angle = GameRandom.globalRandom.nextInt(360);
            float nx = (float) Math.cos(Math.toRadians(angle));
            float ny = (float) Math.sin(Math.toRadians(angle));
            int x = this.tileX * 32 + 16 + (int) (nx * SPAWN_DISTANCE);
            int y = this.tileY * 32 + 16 + (int) (ny * SPAWN_DISTANCE);
            // floorDiv, not /: the sky plane has negative tile coordinates, and
            // integer division truncates towards zero, so x = -5 would name
            // tile 0 instead of tile -1 and the check would read the wrong
            // ground for every site west or north of the origin.
            int candidateX = Math.floorDiv(x, 32);
            int candidateY = Math.floorDiv(y, 32);
            level.regionManager.ensureTileIsLoaded(candidateX, candidateY);
            if (!level.isTileWithinBounds(candidateX, candidateY) || level.isSolidTile(candidateX, candidateY)
                    || level.getTile(candidateX, candidateY).isLiquid) {
                // Remember the last try anyway: an island so small that twelve
                // angles all miss should still get its fight, and a boss in the
                // Mistsea is better than a click that does nothing.
                spawnX = x;
                spawnY = y;
                continue;
            }
            spawnX = x;
            spawnY = y;
            break;
        }
        level.entityManager.addMob(mob, spawnX, spawnY);
        // AFTER addMob, because that is where the engine runs Mob.init() and
        // sets the boss's health -- see BossScaling.apply.
        BossScaling.apply(mob, boss.tier);
        return mob;
    }
}
