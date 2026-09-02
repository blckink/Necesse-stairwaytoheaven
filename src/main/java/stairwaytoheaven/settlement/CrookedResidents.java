package stairwaytoheaven.settlement;

import necesse.engine.network.server.Server;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.realms.crooked.CrookedSites;

/**
 * Where Mr. Knott, the Doorman, actually stands — {@code docs/WORLD_DESIGN.md}
 * §15: "Mr. Knott, standing at a free-standing red door."
 *
 * <h2>Why the Door Yard</h2>
 * Crooked Beyond already has the exact structure his flavour text describes:
 * {@code DoorYardPreset} — free-standing Beetlefreak doors, none attached to
 * anything, standing in rows on a chequered forecourt. Rather than scan for the
 * door object the way {@code VeilResidents}/{@code SkyLevel} scan for a bone
 * pile or a workstation, this asks {@link CrookedSites#nearestDoorYard} the
 * same question {@code CrookedLevel.placeGuardPacks} already asks it: is this
 * tile within reach of a stamped Door Yard site? That is more precise than an
 * object scan (a stray {@code beetleDoorClosedID} placed by some other preset
 * would never fool it) and costs nothing new — the lattice already exists.
 *
 * <h2>The rules</h2>
 * Deterministic from the level seed and the region coordinates; persistent
 * ({@code canDespawn = false}); and — via
 * {@link SkywatchWorldData#residentClaimed} — at most one per world, the same
 * contract every other named resident in the mod keeps.
 */
public final class CrookedResidents {

    private CrookedResidents() {
    }

    /** Rare. A yard with somebody actually keeping it is the exception. */
    private static final float REGION_CHANCE = 0.20F;

    /**
     * How close a candidate tile must be to a Door Yard's site centre.
     * {@code DoorYardPreset} is 17x13 tiles; a radius comfortably inside that
     * footprint keeps him standing among the doors rather than merely near
     * them.
     */
    private static final float DOORYARD_RADIUS = 9.0F;

    public static final String KNOTT = "knottsettler";

    /** One roll per generated region. Server-side only. */
    public static void place(Level level, Region region, int worldGenSeed) {
        if (level == null || level.isClient() || region == null) {
            return;
        }
        Server server = level.getServer();
        if (server == null) {
            return;
        }
        long seed = (long) worldGenSeed * 0x9E3779B97F4A7C15L
                ^ ((long) region.regionX * 0xB55A4F09L)
                ^ ((long) region.regionY * 0x27220A95L);
        GameRandom random = new GameRandom(seed);
        if (!random.getChance(REGION_CHANCE)) {
            return;
        }
        if (SkywatchWorldData.residentClaimed(server, KNOTT)) {
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
            CrookedSites.Site yard = CrookedSites.nearestDoorYard(worldGenSeed, tileX, tileY);
            if (!yard.exists || yard.distance > DOORYARD_RADIUS) {
                continue;
            }
            Mob mob = MobRegistry.getMob(KNOTT, level);
            if (mob == null) {
                return;
            }
            mob.canDespawn = false;
            level.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
            SkywatchWorldData.claimResident(server, KNOTT);
            return;
        }
    }
}
