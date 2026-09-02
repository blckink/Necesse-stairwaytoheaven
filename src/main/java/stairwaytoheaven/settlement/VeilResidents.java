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
 * Where the Veil's three named residents actually stand.
 *
 * <h2>Why they are here and not in the Ghost Realm</h2>
 *
 * {@code docs/WORLD_DESIGN.md} §11 files Mortimer, Caspern and Eleanor under
 * the Ghost Realm / Aftergarden (§10), which is not built. The Veil is the
 * layer that realm grows out of — §41.5 says so in as many words — it exists
 * today, it is reachable today through a Seance Circle and the Warden's silver
 * bell, and its ground is already bone piles and ash. So it is the home the mod
 * can honestly give them now. When the Ghost Realm ships, moving them is one
 * changed level identifier.
 *
 * <h2>Why placement lives in a class of its own</h2>
 *
 * {@code VeilLevel} is a shared file: the Ghost Realm is being built in
 * parallel. Everything here is in one place so the hook in that level is a
 * single call, and so this can be re-pointed at another level without touching
 * the level class at all.
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
 * They stand beside a bone pile ({@code ashbones}) for the same reason the
 * Skyreach three stand beside a workshop: a figure alone in the dark is not a
 * discovery, and the landmark is what makes them findable.
 */
public final class VeilResidents {

    private VeilResidents() {
    }

    /** Rare. A hollow with somebody still in it is the exception. */
    private static final float REGION_CHANCE = 0.14F;

    /** How far a bone pile may be and still count as "beside". */
    private static final int LANDMARK_RANGE = 3;

    /**
     * One roll per generated region. Server-side only — a client generating a
     * region for rendering must never add mobs.
     */
    public static void place(Level level, Region region, int worldGenSeed) {
        if (level == null || level.isClient() || region == null) {
            return;
        }
        Server server = level.getServer();
        if (server == null) {
            return;
        }
        long seed = (long) worldGenSeed * 0x9E3779B97F4A7C15L
                ^ ((long) region.regionX * 0x2545F491L)
                ^ ((long) region.regionY * 0x14057B7EL);
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
            if (!hasBonePileNear(level, tileX, tileY)) {
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

    private static boolean hasBonePileNear(Level level, int tileX, int tileY) {
        for (int dx = -LANDMARK_RANGE; dx <= LANDMARK_RANGE; dx++) {
            for (int dy = -LANDMARK_RANGE; dy <= LANDMARK_RANGE; dy++) {
                if (level.getObjectID(tileX + dx, tileY + dy) == SkyRegistry.ashbonesID) {
                    return true;
                }
            }
        }
        return false;
    }
}
