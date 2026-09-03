package stairwaytoheaven;

import necesse.engine.util.LevelIdentifier;
import stairwaytoheaven.biomes.AshenReachBiome;
import stairwaytoheaven.biomes.AuroraShoalsBiome;
import stairwaytoheaven.biomes.DriftlandsBiome;
import stairwaytoheaven.biomes.GloomfenBiome;
import stairwaytoheaven.biomes.SkywayBiome;
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

    /**
     * One-world dimension index of the Skyreach: one layer above the surface,
     * and <b>the only dimension this mod adds</b>.
     *
     * <p>{@code docs/PLAN_ONE_PLANE.md}: every realm is a BAND of this level,
     * picked by {@code worldgen.RealmDepth} from distance to the Old Warden
     * Spire, because {@code docs/WORLD_DESIGN.md} §3 asks for overlapping
     * biome WEIGHTS and a dimension is the one thing that cannot overlap.
     */
    public static final int SKY_DIMENSION = 1;

    /** Level identifier of the Skyreach dimension (compare: "surface", "cave", "deepcave"). */
    /**
     * WORLD GENERATION 2.
     *
     * <p>Both dimension identifiers carry a generation number, and bumping it
     * is how a worldgen change reaches a save that has already explored the
     * old terrain. Regions are written per level identifier, so a new
     * identifier is simply a level nobody has generated yet: the player's
     * stairway leads into a freshly built Skyreach, and the old one stays on
     * disk untouched rather than being deleted. Roll the number back and the
     * previous world is exactly where it was.
     *
     * <p>What a bump costs, and it is not nothing: everything built or
     * recorded up there is left behind with the old level — the Warden and his
     * settlement, the beacon, the cats, player-built structures, and each
     * player's bound return stairway. Inventories and the surface are
     * untouched. Bump it only when the generated world has changed enough that
     * seeing the old one would be worse.
     *
     * <p>The identifiers stay string LITERALS on purpose. tools/locale_audit.py
     * finds level names by matching the LevelIdentifier constructor call with a
     * quoted argument, so building them by concatenation would leave that gate
     * quietly checking nothing. (Spelling that pattern out in a comment makes
     * the audit match the comment, which is how this sentence got reworded.)
     * Each generation therefore also needs its own {@code [level]} entry in
     * both locales — the audit fails until it is there.
     */
    public static final int WORLD_GENERATION = 2;

    public static final LevelIdentifier SKYREACH_IDENTIFIER = new LevelIdentifier("skyreach2");

    /**
     * <b>RETIRED as a dimension.</b> {@code docs/PLAN_ONE_PLANE.md} collapsed
     * the six modded levels into one plane: the Garden of Eden is a BAND of
     * {@code skyreach2} chosen by {@code worldgen.RealmDepth}, not a level of
     * its own, and {@code StairwayToHeavenMod.registerDimension} no longer
     * registers it. The constant stays for two reasons and no others: the
     * dimension index must never be reused by a future realm, and
     * {@code tools/locale_audit.py} finds level names by matching a
     * {@code LevelIdentifier} constructor call with a quoted argument, so
     * deleting the identifier would orphan its {@code [level]} row in both
     * locales. Nothing generates against either any more.
     */
    public static final int EDEN_DIMENSION = 2;

    /** @see #EDEN_DIMENSION — retired as a dimension, kept as a name. */
    public static final LevelIdentifier EDEN_IDENTIFIER = new LevelIdentifier("eden2");

    // ===== Biomes =====

    public static DriftlandsBiome driftlands;
    public static StormveilBiome stormveil;
    public static AuroraShoalsBiome auroraShoals;
    /** v0.8 Skyway Passages: the built biome, paved in Skyway and Cloudmarble. */
    public static SkywayBiome skyway;
    /**
     * The Beetle Outlands: the sky's wrong ground, gated by distance from the
     * spire rather than by the biome noise. See
     * {@link stairwaytoheaven.worldgen.SkyOutlands}.
     */
    public static stairwaytoheaven.biomes.OutlandsBiome outlands;

    // ===== Tiles =====

    public static CloudturfTile cloudturfTile;
    public static stairwaytoheaven.tiles.AuroraShoalTile auroraShoalTile;
    public static SkystoneTile skystoneTile;
    public static StormslateTile stormslateTile;
    public static MistseaTile mistseaTile;

    public static int skyCrateID;
    public static int skyCacheID;
    public static int skyBalloonID;
    public static int aeronautWreckID;
    public static int skyParcelID;
    public static int cloudturfID;
    public static int auroraShoalID;
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
    /** The sapling a broken Cloudberry Bush leaves behind — its only drop. */
    public static int cloudberrySaplingID;

    // ===== v0.2: building set & quest structure =====

    public static int skystoneBrickWallID;
    public static int nightfellWallID;
    public static int nightfellDoorID;
    /** Veil: the Beetlefreak wall set, from supplied art. */
    public static int beetleWallID;
    /** Closed and open halves of the Beetlefreak door, and its window. */
    public static int beetleDoorClosedID;
    public static int beetleDoorOpenID;
    public static int beetleWindowID;
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

    /**
     * <b>RETIRED as a dimension.</b> {@code docs/PLAN_ONE_PLANE.md} collapsed
     * the six modded levels into one plane: the Veil's ground (WORLD_DESIGN §41.5: Gloomfen and Ashen Reach
     * into the Ghost band, the Beetlefreak Hollows into the Crooked band) is a BAND of
     * {@code skyreach2} chosen by {@code worldgen.RealmDepth}, not a level of
     * its own, and {@code StairwayToHeavenMod.registerDimension} no longer
     * registers it. The constant stays for two reasons and no others: the
     * dimension index must never be reused by a future realm, and
     * {@code tools/locale_audit.py} finds level names by matching a
     * {@code LevelIdentifier} constructor call with a quoted argument, so
     * deleting the identifier would orphan its {@code [level]} row in both
     * locales. Nothing generates against either any more.
     */
    public static final int VEIL_DIMENSION = -3;

    /** @see #VEIL_DIMENSION — retired as a dimension, kept as a name. */
    public static final LevelIdentifier VEIL_IDENTIFIER = new LevelIdentifier("veil2");

    public static GloomfenBiome gloomfen;
    public static AshenReachBiome ashenReach;
    /** The Veil's rare wrong place, cut out of the other two. */
    public static stairwaytoheaven.biomes.BeetlefreakHollowBiome beetlefreakHollow;

    public static MurkmossTile murkmossTile;
    public static int murkmossID;
    public static int blackpeatID;
    public static int ashsandID;
    public static int murkwaterID;
    /** Eden grass, from the supplied art: the Garden of Eden's first ground. */
    public static int overgrownEdenID;
    /** Veil: the Beetlefreak ground, on vanilla's spidernest tile setup. */
    public static int beetlefreakID;
    /** The same tile as an instance, for biome under-liquid reclamation. */
    public static stairwaytoheaven.tiles.BeetlefreakTile beetlefreakTile;

    public static VeilRiftObject veilRiftDown;
    public static VeilSideRiftObject veilRiftUp;
    public static int veilRiftDownID;
    public static int veilRiftUpID;
    public static int seanceCircleID;
    /** The Outlands' crystal massifs. Vanilla RockObject, supplied art. */
    public static int evilwallID;

    public static int whisperreedsID;
    public static int gloomshroomID;
    public static int veilrockID;
    public static int ashbonesID;
    public static int deadtreeID;
    public static int ghostLanternID;

    // ===== Tier 4: Crooked Beyond =====

    /**
     * <b>RETIRED as a dimension.</b> {@code docs/PLAN_ONE_PLANE.md} collapsed
     * the six modded levels into one plane: Crooked Beyond is a BAND of
     * {@code skyreach2} chosen by {@code worldgen.RealmDepth}, not a level of
     * its own, and {@code StairwayToHeavenMod.registerDimension} no longer
     * registers it. The constant stays for two reasons and no others: the
     * dimension index must never be reused by a future realm, and
     * {@code tools/locale_audit.py} finds level names by matching a
     * {@code LevelIdentifier} constructor call with a quoted argument, so
     * deleting the identifier would orphan its {@code [level]} row in both
     * locales. Nothing generates against either any more.
     */
    public static final int CROOKED_DIMENSION = 5;

    /** @see #CROOKED_DIMENSION — retired as a dimension, kept as a name. */
    public static final LevelIdentifier CROOKED_IDENTIFIER = new LevelIdentifier("crooked2");

    /**
     * The doorway pair between the Skyreach's Outlands rim and Crooked Beyond.
     *
     * <p>Held here rather than in {@code realms.crooked.CrookedRealm} because
     * {@code SeanceCircleObject} — a Skyreach object — is what turns a ring
     * into the down-side door, and the Skyreach package must not have to know
     * the realm package's internals to do it.
     */
    public static int crookedDoorDownID;
    public static int crookedDoorUpID;

    // ===== v0.4 "The Living Sky": per-biome fill =====

    // Trees + saplings
    public static int nimbuswillowID, fulgurpineID, prismabirchID;
    public static int nimbusSaplingID, fulgurSaplingID, prismaSaplingID;
    /** v0.8 Sky Seraph tree: single-column sheet with a frost half. */
    public static int skySeraphTreeID, skySeraphSaplingID;
    /** v0.9 Cloud Tree: the Driftlands' tree, supplied art on the birch sheet. */
    public static int cloudTreeID, cloudSaplingID;

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

    // ===== v0.10: the Ghost Realm / Aftergarden =====

    /**
     * <b>RETIRED as a dimension.</b> {@code docs/PLAN_ONE_PLANE.md} collapsed
     * the six modded levels into one plane: the Ghost Realm is a BAND of
     * {@code skyreach2} chosen by {@code worldgen.RealmDepth}, not a level of
     * its own, and {@code StairwayToHeavenMod.registerDimension} no longer
     * registers it. The constant stays for two reasons and no others: the
     * dimension index must never be reused by a future realm, and
     * {@code tools/locale_audit.py} finds level names by matching a
     * {@code LevelIdentifier} constructor call with a quoted argument, so
     * deleting the identifier would orphan its {@code [level]} row in both
     * locales. Nothing generates against either any more.
     */
    public static final int GHOST_DIMENSION = 4;

    /** @see #GHOST_DIMENSION — retired as a dimension, kept as a name. */
    public static final LevelIdentifier GHOST_IDENTIFIER = new LevelIdentifier("ghost2");

    // ===== v1.0: Steinfeld / The Quiet Reach =====

    /**
     * <b>RETIRED as a dimension.</b> {@code docs/PLAN_ONE_PLANE.md} collapsed
     * the six modded levels into one plane: Steinfeld / The Quiet Reach is a BAND of
     * {@code skyreach2} chosen by {@code worldgen.RealmDepth}, not a level of
     * its own, and {@code StairwayToHeavenMod.registerDimension} no longer
     * registers it. The constant stays for two reasons and no others: the
     * dimension index must never be reused by a future realm, and
     * {@code tools/locale_audit.py} finds level names by matching a
     * {@code LevelIdentifier} constructor call with a quoted argument, so
     * deleting the identifier would orphan its {@code [level]} row in both
     * locales. Nothing generates against either any more.
     */
    public static final int STEINFELD_DIMENSION = 3;

    /** @see #STEINFELD_DIMENSION — retired as a dimension, kept as a name. */
    public static final LevelIdentifier STEINFELD_IDENTIFIER = new LevelIdentifier("steinfeld2");

    public static stairwaytoheaven.realms.steinfeld.QuietMeadowBiome quietMeadow;
    public static stairwaytoheaven.realms.steinfeld.SlabFieldsBiome slabFields;
    public static stairwaytoheaven.realms.steinfeld.GraveHeathBiome graveHeath;

    /** Steinfeld's seven grounds, inner band first. See SteinfeldTerrainPainter. */
    public static int palegrassID;
    public static int weatheredstoneID;
    public static int crackedmarbleID;
    public static int deadsoilID;
    public static int ashgrassID;
    public static int miststoneID;
    public static int gravesoilID;
    /** The instance the biomes hand back for under-liquid reclamation. */
    public static stairwaytoheaven.realms.steinfeld.tiles.PaleGrassTile palegrassTile;

    /** Steinfeld flora and props. */
    public static int witheredtuftID;
    public static int palereedID;
    public static int widowflowerID;
    public static int deadheavenbloomID;
    public static int ghostmushroomID;
    public static int spiritmosspatchID;
    public static int palestonerockID;
    public static int gravesaltrockID;
    public static int steinfeldgravestoneID;
    public static int mournerstatueID;
    public static int brokenangelID;
    public static int chapelcolumnID;
    public static int heavenslabID;
    public static int gravefenceID;

    // No Fallen Gate yet: the Garden of Eden -- the dimension directly below
    // Steinfeld on this same ladder -- is registered exactly this far too
    // (a dimension index and a level class) and has no player-facing entry
    // object either. Building one gate while the realm underneath still has
    // none would not make Steinfeld more reachable than its neighbour; both
    // are deferred together. See docs/realms/steinfeld.md.
}
