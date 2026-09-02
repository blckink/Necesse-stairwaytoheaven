package stairwaytoheaven.settlement;

import necesse.engine.network.server.Server;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.quest.SkywatchWorldData;

/**
 * Where the mod's Mortimer / Caspern / Eleanor trio actually stands.
 *
 * <h2>History — why this class is named for the Veil</h2>
 *
 * {@code docs/WORLD_DESIGN.md} §11 files Mortimer, Caspern and Eleanor under
 * the Ghost Realm / Aftergarden (§10). When this class was first written that
 * realm was not built yet, so it stood the three beside the Veil's bone piles
 * instead — the layer §41.5 calls "a Ghost Realm in everything but name" — and
 * said so in its own doc comment: "When the Ghost Realm ships, moving them is
 * one changed level identifier."
 *
 * <h2>Now that it has shipped</h2>
 *
 * The Ghost Realm is built (see {@code stairwaytoheaven.realms.ghost.GhostLevel}),
 * so it is the home {@link #GHOST_LANDMARK_ID} points {@link #placeInGhost} at,
 * and {@code GhostLevel.onRegionGenerated} is the only caller left. The Veil
 * stopped rolling for them the same day: {@code VeilLevel} no longer calls this
 * class at all, because a resident who might be in EITHER of two dimensions is
 * a resident the player cannot reliably go looking for, and giving the new
 * realm "a reason to go there" is the whole point of this pass. {@code veil2}
 * itself is untouched and still loads (§41.2) — only the Skyreach three still
 * stand there is no longer true of anyone.
 *
 * <h2>Why placement lives in a class of its own</h2>
 *
 * Kept generic rather than folded into {@code GhostLevel} directly: the three
 * residents, their rarity and their claim bookkeeping are all one policy, and a
 * bare {@code Level}/{@code Region}/landmark-ID call is the smallest surface a
 * level class needs to host it.
 *
 * <h2>The rules, mirroring {@code SkyLevel.placeResident}</h2>
 *
 * Deterministic from the level seed and the region coordinates, so the same
 * world always stands the same person in the same place; persistent
 * ({@code canDespawn = false}); and at most ONE of each person per world, which
 * is enforced through {@link SkywatchWorldData#residentsClaimed} rather than by
 * scanning entities, because the other route that can produce them — travelling
 * to a settlement, {@link SkyArrivals} — happens on a different level entirely
 * and would never see an entity scan.
 *
 * They stand beside a landmark for the same reason the Skyreach three stand
 * beside a workshop: a figure alone in the dark is not a discovery, and the
 * landmark is what makes them findable. The Ghost Realm's is its own
 * gravestone prop ({@link #GHOST_LANDMARK_ID}) rather than the Veil's bone
 * pile — both read as "somebody is buried here", which is the point.
 */
public final class VeilResidents {

    private VeilResidents() {
    }

    /** Rare. A graveyard with somebody still in it is the exception. */
    private static final float REGION_CHANCE = 0.14F;

    /** How far the landmark may be and still count as "beside". */
    private static final int LANDMARK_RANGE = 3;

    /** The Ghost Realm's own gravestone prop, resolved lazily (registry order). */
    private static int GHOST_LANDMARK_ID = -1;

    private static int ghostLandmarkID() {
        if (GHOST_LANDMARK_ID < 0) {
            GHOST_LANDMARK_ID = stairwaytoheaven.realms.ghost.GhostRealm.gravestoneID;
        }
        return GHOST_LANDMARK_ID;
    }

    /**
     * One roll per generated Ghost Realm region, beside a gravestone. Server
     * side only — a client generating a region for rendering must never add
     * mobs. This is the only caller left; see the class doc for why the Veil no
     * longer rolls for the same three.
     */
    public static void placeInGhost(Level level, Region region, int worldGenSeed) {
        place(level, region, worldGenSeed, ghostLandmarkID(), 0x2545F491L, 0x14057B7EL);
    }

    /**
     * @param landmarkObjectID the object a resident must stand within
     *                         {@link #LANDMARK_RANGE} tiles of
     * @param saltX            per-caller salt so two callers on two different
     *                         levels never roll the same region in lock-step
     */
    private static void place(Level level, Region region, int worldGenSeed,
            int landmarkObjectID, long saltX, long saltY) {
        if (level == null || level.isClient() || region == null) {
            return;
        }
        Server server = level.getServer();
        if (server == null) {
            return;
        }
        long seed = (long) worldGenSeed * 0x9E3779B97F4A7C15L
                ^ ((long) region.regionX * saltX)
                ^ ((long) region.regionY * saltY);
        GameRandom random = new GameRandom(seed);
        if (!random.getChance(REGION_CHANCE)) {
            return;
        }
        String who = SkySettlers.VEIL_RESIDENTS[random.nextInt(SkySettlers.VEIL_RESIDENTS.length)];

        // One per world, whichever route got there first.
        if (SkywatchWorldData.residentClaimed(server, who)) {
            return;
        }
        // ...and Eleanor's ending is final: a world where she was let go never
        // grows another one. WORLD_DESIGN §11 is the whole point of that.
        if (SkySettlers.ELEANOR.equals(who)
                && SkywatchWorldData.eleanorGone(server)) {
            return;
        }

        for (int attempt = 0; attempt < 40; attempt++) {
            int tileX = region.tileXOffset + random.getIntBetween(2, region.tileWidth - 3);
            int tileY = region.tileYOffset + random.getIntBetween(2, region.tileHeight - 3);
            if (!level.isTileWithinBounds(tileX, tileY) || level.isSolidTile(tileX, tileY)) {
                continue;
            }
            if (level.getObjectID(tileX, tileY) != 0) {
                continue;
            }
            if (!hasLandmarkNear(level, tileX, tileY, landmarkObjectID)) {
                continue;
            }
            Mob mob = MobRegistry.getMob(who, level);
            if (mob == null) {
                return;
            }
            mob.canDespawn = false;
            level.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
            SkywatchWorldData.claimResident(server, who);
            return;
        }
    }

    private static boolean hasLandmarkNear(Level level, int tileX, int tileY, int landmarkObjectID) {
        if (landmarkObjectID < 0) {
            return false;
        }
        for (int dx = -LANDMARK_RANGE; dx <= LANDMARK_RANGE; dx++) {
            for (int dy = -LANDMARK_RANGE; dy <= LANDMARK_RANGE; dy++) {
                if (level.getObjectID(tileX + dx, tileY + dy) == landmarkObjectID) {
                    return true;
                }
            }
        }
        return false;
    }
}
