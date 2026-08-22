package stairwaytoheaven;

import necesse.engine.util.LevelIdentifier;
import stairwaytoheaven.biomes.AuroraShoalsBiome;
import stairwaytoheaven.biomes.DriftlandsBiome;
import stairwaytoheaven.biomes.StormveilBiome;
import stairwaytoheaven.objects.SkySideStairwayObject;
import stairwaytoheaven.objects.SkywardStairwayObject;
import stairwaytoheaven.tiles.CloudturfTile;
import stairwaytoheaven.tiles.MistseaTile;
import stairwaytoheaven.tiles.SkystoneTile;
import stairwaytoheaven.tiles.StormslateTile;

/**
 * Central handle to everything this mod registers.
 *
 * Fields are populated once during {@link StairwayToHeavenMod#init()} while the
 * game registries are open, and read-only afterwards. Keeping every ID in one
 * place mirrors how vanilla registries expose their well-known IDs (e.g.
 * TileRegistry.dirtID) and keeps generation code free of string lookups.
 */
public final class SkyRegistry {

    private SkyRegistry() {
    }

    // ===== Dimension =====

    /** One-world dimension index of the Skyreach: one layer above the surface. */
    public static final int SKY_DIMENSION = 1;

    /** Level identifier of the Skyreach dimension (compare: "surface", "cave", "deepcave"). */
    public static final LevelIdentifier SKYREACH_IDENTIFIER = new LevelIdentifier("skyreach");

    // ===== Biomes =====

    public static DriftlandsBiome driftlands;
    public static StormveilBiome stormveil;
    public static AuroraShoalsBiome auroraShoals;

    // ===== Tiles =====

    public static CloudturfTile cloudturfTile;
    public static SkystoneTile skystoneTile;
    public static StormslateTile stormslateTile;
    public static MistseaTile mistseaTile;

    public static int cloudturfID;
    public static int skystoneTileID;
    public static int stormslateID;
    public static int mistseaID;

    // ===== Objects =====

    public static SkywardStairwayObject stairwayDown;
    public static SkySideStairwayObject stairwayUp;

    public static int stairwayDownID;
    public static int stairwayUpID;

    public static int skystoneRockID;
    public static int aetheriumRockID;
    public static int stormCrystalID;
    public static int stormCrystalRID;
    public static int auroraBloomID;
    public static int auroraBloomRID;
    public static int skyreedsID;
}
