package stairwaytoheaven;

import necesse.engine.util.LevelIdentifier;
import stairwaytoheaven.biomes.AshenReachBiome;
import stairwaytoheaven.biomes.AuroraShoalsBiome;
import stairwaytoheaven.biomes.DriftlandsBiome;
import stairwaytoheaven.biomes.GloomfenBiome;
import stairwaytoheaven.biomes.StormveilBiome;
import stairwaytoheaven.objects.SkySideStairwayObject;
import stairwaytoheaven.objects.SkywardStairwayObject;
import stairwaytoheaven.objects.VeilRiftObject;
import stairwaytoheaven.objects.VeilSideRiftObject;
import stairwaytoheaven.tiles.CloudturfTile;
import stairwaytoheaven.tiles.MistseaTile;
import stairwaytoheaven.tiles.MurkmossTile;
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
    public static int windwheatID;
    public static int cloudberryBushID;

    // ===== v0.2: building set & quest structure =====

    public static int skystoneBrickWallID;
    public static int nightfellWallID;
    public static int marbleCheckerID;
    public static int gloomwoodFloorID;
    public static int skyironFenceID;
    /** The CLOSED fence gate. FenceGateObject.registerGatePair also registers
     *  "skyironfencegateopen"; worldgen must only ever place the closed one. */
    public static int skyironFenceGateID;
    public static int wardenCandelabraID;
    public static int flickerGarlandID;
    public static int catBasketID;
    public static int skywatchBannerID;
    public static int wardenBeaconOffID;
    public static int wardenBeaconOnID;
    public static int skyAnchorID;

    // ===== v0.3: The Veil =====

    /** One-world dimension index of the Veil: below the deep caves. */
    public static final int VEIL_DIMENSION = -3;
    public static final LevelIdentifier VEIL_IDENTIFIER = new LevelIdentifier("veil");

    public static GloomfenBiome gloomfen;
    public static AshenReachBiome ashenReach;

    public static MurkmossTile murkmossTile;
    public static int murkmossID;
    public static int blackpeatID;
    public static int ashsandID;
    public static int murkwaterID;

    public static VeilRiftObject veilRiftDown;
    public static VeilSideRiftObject veilRiftUp;
    public static int veilRiftDownID;
    public static int veilRiftUpID;
    public static int seanceCircleID;

    public static int whisperreedsID;
    public static int gloomshroomID;
    public static int veilrockID;
    public static int ashbonesID;
    public static int deadtreeID;
    public static int ghostLanternID;

    // ===== v0.4 "The Living Sky": per-biome fill =====

    // Trees + saplings
    public static int nimbuswillowID, fulgurpineID, prismabirchID;
    public static int nimbusSaplingID, fulgurSaplingID, prismaSaplingID;
    /** v0.8 Sky Seraph tree: single-column sheet with a frost half. */
    public static int skySeraphTreeID, skySeraphSaplingID;

    // Plants
    public static int cloudbellID, skytulipID, staticmossID;
    public static int thunderbloomID, glowfernID, auroralilyID;

    // Dense meadow tall grasses (walk-through carpets)
    public static int tallcloudgrassID, stormsedgeID, prismgrassID;
    /** v0.7 stone barrens: what grows on the grey skystone ground. */
    public static int skylichenID, cragbloomID, skyscreeID;

    // Ores
    public static int fulguriteRockID, prismshardRockID;

    // Buildable wood floors
    public static int nimbusFloorID, charFloorID, prismFloorID;

    // ===== v0.7 "The Skywatch Roads": materials of the built landscape =====

    /**
     * Ground of the Skywatch roads, aprons and gate footings.
     *
     * This is vanilla's {@code snowstonepathtile}, deliberately: a road wants
     * the native {@code PathTiledTile} archetype (it blends its own edges into
     * whatever terrain it crosses, bridges a shore tile below it, and gives
     * +10% movement speed, which is what makes following a road worth it), and
     * the mod cannot ship a new tile texture without a generator change. Of
     * the vanilla paths it is the only one that stays legible against all
     * three sky grounds at once — pale blue cut stone over silver-green
     * cloudturf, dark violet stormslate and blue-grey skystone.
     *
     * If a {@code tiles/skystonepath.png} is ever generated, registering
     * {@code new PathTiledTile("skystonepath", ...)} and pointing this field at
     * it is the entire migration for NEW regions.
     */
    public static int skyroadTileID;
    /**
     * The mod's Marble Checker, used ONLY as an accent: monument plinths and
     * the decorative inlay bands inside a court.
     *
     * The first calibration render paved whole courts with it and a 26-tile
     * chequerboard swallowed the screen — exactly the "carpet the world"
     * failure this pass exists to avoid. A chequered floor is a highlight.
     */
    public static int skyplinthTileID;

    // Props the built landscape composes with (registered in SkyBuildingSet).
    public static int gloomRavenStatueID;
    public static int skywatchRubbleID;
    public static int chargeCrystalID;
    public static int auroraShardsID;
    public static int starfallID;
    public static int skywatchTelescopeID;
    public static int skywatchAstrolabeID;
}
