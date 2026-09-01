package stairwaytoheaven.tiles;

import java.awt.Color;

/**
 * Cold dawn turf of the Aurora Shoals.
 *
 * WHY THIS EXISTS. The Shoals are 13% of the Skyreach's land and the rarest of
 * its four biomes — cold dawn light, Aurora Blooms, the richest Aetherium — and
 * until now they had no ground of their own. {@code SkyTerrainPainter} named
 * Stormveil and the Skyway explicitly and let everything else fall through to
 * {@code cloudturfID}, so the biome the player travels furthest to reach walked
 * on the Driftlands' common meadow. {@code isAurora} was even computed two
 * lines above that branch and then never used for the ground.
 *
 * The palette is deliberately restrained: a cool lilac-grey turf, barely rose.
 * The Shoals' rose and teal belong to the Aurora Bloom growing on it — those
 * accents are what a player remembers about the biome, and a ground that wore
 * them would spend them.
 *
 * Terrain priority sits between Cloudturf (210) and Stormslate (220): the dawn
 * turf blends over the common meadow where the two biomes meet, and the bare
 * skystone plate (230) and the Skyway's paving (260) still surface through it,
 * which is the same relationship every other sky ground already has.
 */
public class AuroraShoalTile extends SkyGroundTile {

    public AuroraShoalTile() {
        super(false, "aurorashoal");
        this.mapColor = new Color(161, 152, 176);
        this.canBeMined = true;
    }

    @Override
    public int getTerrainPriority() {
        return 215;
    }
}
