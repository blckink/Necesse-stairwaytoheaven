package stairwaytoheaven.settlement;

import necesse.engine.network.server.Server;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchWorldData;

/**
 * Where Ives, the Verger of the Quiet Reach, actually stands.
 *
 * <h2>Why a class rather than three lines in {@code SkyLevel}</h2>
 * The same call {@link VeilResidents} makes, for the same stated reason: a
 * resident's rarity, their landmark rule and their one-per-world claim are one
 * policy, and a bare {@code Level}/{@code Region} call is the smallest surface
 * a level class needs to host it. Steinfeld gains a second person by adding a
 * name to {@code SkySettlers.STEINFELD_RESIDENTS}, not by touching
 * {@code SkyLevel}.
 *
 * <h2>The landmark: a broken angel</h2>
 * Every found resident in this mod stands beside something that says "somebody
 * was here" — a workshop for the Skyreach three, a Knowledge Tree for Eveleen,
 * a gravestone for the Ghost trio, a red door for Mr. Knott. Steinfeld's is the
 * <b>broken angel</b> ({@code SkyRegistry.brokenangelID}), which
 * {@code SteinfeldTerrainPainter} scatters through the realm as {@code
 * P_BROKEN_ANGEL} and {@code RuinedChapelPreset} stands inside its chapel. It
 * is the right landmark twice over: it is common enough that the conjunction of
 * roll and landmark actually resolves, and {@code docs/WORLD_DESIGN.md} §7
 * names it as the realm's own image of the death of paradise — which is the
 * thing Ives is out here tidying up after.
 *
 * <p>The mourner statue would have worked as well and is deliberately not used:
 * {@code RealmPoiPresets} stands one at the centre of the Steinfeld Memorial,
 * so keying on it would have put the realm's only person inside its only
 * building disproportionately often.
 *
 * <h2>The rules, mirroring {@link VeilResidents} exactly</h2>
 * Deterministic from the world seed and the region coordinates, so the same
 * world always stands him in the same place; persistent
 * ({@code canDespawn = false}); and one per world through
 * {@link SkywatchWorldData#residentsClaimed} rather than an entity scan,
 * because he can also be produced by a {@code /swhreset world} retrofit walking
 * a region a second time and an entity scan of one region would not see him
 * standing in the next one.
 */
public final class SteinfeldResidents {

    private SteinfeldResidents() {
    }

    /**
     * Rare, and the same 0.14F {@link VeilResidents} rolls.
     *
     * <p>Not lower despite him being the only person in the realm, and not
     * higher either. The roll is half the gate — a broken angel must also stand
     * within {@link #LANDMARK_RANGE} tiles — and it is the CONJUNCTION that
     * decides how findable he is, exactly the point {@code SkyLevel.placeEveleen}
     * makes about her own generous 0.35F. Steinfeld's band is 2280 tiles deep,
     * so a per-region chance this size still resolves many times over the realm;
     * the one-per-world claim is what keeps that to a single Ives.
     */
    private static final float REGION_CHANCE = 0.14F;

    /** How far the landmark may be and still count as "beside". */
    private static final int LANDMARK_RANGE = 3;

    /** One roll per generated Steinfeld region, beside a broken angel. */
    public static void place(Level level, Region region, int worldGenSeed) {
        if (level == null || level.isClient() || region == null) {
            return;
        }
        Server server = level.getServer();
        if (server == null) {
            return;
        }
        // Salts of this class's own, so Steinfeld and the Ghost band never roll
        // the same region number in lock-step -- the reason VeilResidents takes
        // its two as parameters.
        long seed = (long) worldGenSeed * 0x9E3779B97F4A7C15L
                ^ ((long) region.regionX * 0x6A09E667L)
                ^ ((long) region.regionY * 0xBB67AE85L);
        GameRandom random = new GameRandom(seed);
        if (!random.getChance(REGION_CHANCE)) {
            return;
        }
        String who = SkySettlers.STEINFELD_RESIDENTS[
                random.nextInt(SkySettlers.STEINFELD_RESIDENTS.length)];
        if (SkywatchWorldData.residentClaimed(server, who)) {
            return;
        }

        int landmarkID = SkyRegistry.brokenangelID;
        if (landmarkID <= 0) {
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
            if (!hasLandmarkNear(level, tileX, tileY, landmarkID)) {
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
