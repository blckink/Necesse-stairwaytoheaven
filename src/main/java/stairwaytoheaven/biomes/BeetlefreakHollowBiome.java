package stairwaytoheaven.biomes;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.SkyRegistry;

/**
 * Beetlefreak Hollows — the Veil's rare wrong place.
 *
 * The other two Veil sub-biomes are landscape; this one is a symptom. It is
 * cut out of them by its own noise band rather than tiling with them, so it
 * always reads as something that happened TO the fen rather than a region of
 * it: striped ground, crooked masonry, and the shades that gather around it.
 *
 * Deliberately the hardest ground in the layer. The Hollows is where the
 * Crooked House stands, and a house nobody guards is not worth finding.
 */
public class BeetlefreakHollowBiome extends VeilBiome {

    // WHAT "DENSEST" CAN AND CANNOT MEAN HERE. This table used to be one line
    // — 100 tickets of gloomshade, cap 6, range 60 — and both numbers in it
    // were fiction. addLimited's searchRange is PIXELS (see SkyBiome), so 60
    // is under two tiles; and no cap above three can bind at eight tiles at
    // all, because the engine refuses the spawn outright once four hostiles
    // are already inside the same eight tiles, and it measures that square
    // around the circle addLimited measures. A cap of 6 was never reachable,
    // and neither is 4.
    //
    // Which leaves the only honest way to make this the hardest ground in the
    // Veil: not more bodies, but a worse three. The fen's elite and the ash's
    // ranged caster both stand here now, so the House is guarded by the whole
    // roster rather than by the layer's weakest member in bulk.
    //
    // This is not free. VERIFIED [jar] EntityManager.spawnRandomMob retries
    // within the same tick — on a failed placement it does
    // `spawnTable = spawnTable.withoutRandomMob(randomMob)` and rolls again —
    // so a three-entry table converts ticks into spawns more often than a
    // one-entry table did. The ceiling above it does not move: the same method
    // only spawns while `countMobs(...) < client.getMobSpawnCap(level)`. So
    // the Hollows fills its share of that cap faster and with a worse mix,
    // which is what "the hardest ground in the layer" can honestly buy.
    //
    // Both additions are safe to table-spawn: FenWraithMob and CinderCantorMob
    // implement isValidSpawnLocation via SkySpawnRules.daylightSpawn, which is
    // the check a spawn entry silently fails when a mob inherits Mob's
    // `return false` (the Cloud Lamb bug, see DriftlandsBiome).
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard, and still the ground's own inhabitant: three within
            // eight tiles is every hostile the engine will place there bar
            // one, and that one is what the other two entries are for.
            .addLimited(100, "gloomshade", 3, SkyBiome.RANGE_STANDARD)
            // Elite. The burning pools are worst exactly here, where the
            // striped ground funnels you between masonry instead of letting
            // you walk around anything.
            .addLimited(40, "fenwraith", 2, SkyBiome.RANGE_ELITE)
            // Ranged. A cantor at the House rather than a cantor straying to
            // the water: this is the one place in the Veil where all three
            // archetypes stand on the same ground.
            .addLimited(35, "cindercantor", 2, SkyBiome.RANGE_RANGED);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * Bridging inside the Hollows reclaims the striped ground, not the fen's
     * moss — the wrongness is the point, and a player who bridges a channel
     * here should not be handing themselves a patch of normal marsh.
     */
    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return SkyRegistry.beetlefreakTile;
    }

    /**
     * The Hollows' guard: the densest shade population in the Veil, massed.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"fenwraith"},
                new String[]{"gloomshade", "gloomshade", "gloomshade", "cindercantor"}, 5, 7);
    }
}
