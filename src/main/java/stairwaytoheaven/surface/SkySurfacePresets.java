package stairwaytoheaven.surface;

import java.util.concurrent.atomic.AtomicInteger;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.worldPresets.GenerationPresetsWorldPreset;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.SimpleGenerationPreset;
import necesse.engine.world.worldPresets.WorldApplyAreaPredicate;
import necesse.engine.world.worldPresets.WorldPreset;
import necesse.engine.world.worldPresets.WorldPresetTester;
import necesse.level.maps.presets.Preset;

/**
 * The mod's three rare Surface points of interest, registered the way vanilla
 * scatters structures across the streamed Surface.
 *
 * <h2>The mechanism (read out of the game, not invented)</h2>
 * {@code SurfaceLevel.generateRegion} (necesse/level/maps/SurfaceLevel.java:23)
 * brackets every region it generates with
 * {@code worldEntity.startPresetGenerationInRegion} /
 * {@code runPresetGenerationInRegion}. Those resolve to
 * {@code WorldPresetsRegion} → {@code LevelPresetsRegion}, and the queue they
 * run is filled once per 1024x1024 <i>preset region</i> by
 * {@code WorldPresetRegistry.initRegion}, which walks every registered
 * {@link WorldPreset} and calls {@code addToRegion} on the ones whose
 * {@code shouldAddToRegion} accepts the level. Vanilla's own surface structures
 * are one such entry: {@code registerPreset("surfacepresets", new
 * SurfacePresetsWorldPreset())}, a {@link GenerationPresetsWorldPreset} holding
 * a ticket-weighted list of {@link SimpleGenerationPreset}s at
 * {@code presetsPerRegion = 0.05F}.
 *
 * <p>This class is the same thing, scoped to
 * {@link LevelIdentifier#SURFACE_IDENTIFIER} and one order of magnitude rarer.
 * Nothing here hooks the surface level, replaces vanilla surface generation or
 * changes vanilla's own preset list — the Surface stays the main world and
 * gains three optional things to find (docs/DESIGN_DECISIONS.md).
 *
 * <h2>Why this cannot overwrite a player's build or a vanilla structure</h2>
 * Two independent guards, both vanilla's:
 * <ul>
 *   <li><b>Already-generated regions are skipped entirely.</b>
 *       {@code LevelPresetsRegion.startGenerateRegion} sets
 *       {@code hasAlreadyGeneratedRegion} on any queued preset one of whose
 *       occupied regions is already generated, and
 *       {@code runGenerateRegion} then never places it. A player can only build
 *       in a region that has generated, so a POI can never land on a build.</li>
 *   <li><b>Occupied-space boards.</b> {@link SimpleGenerationPreset} checks and
 *       claims the {@code "villages"}, {@code "minibiomes"} and {@code "loot"}
 *       boards. Vanilla's presets are all registered before any mod's
 *       {@code init()}, and {@code WorldPresetRegistry} keeps registration
 *       order for equal priority, so every vanilla structure is already on the
 *       boards when ours picks a tile.</li>
 * </ul>
 * On top of that, {@code LevelPresetsRegion.PlaceableWorldPreset} gives every
 * surface preset {@code removeIfWithinSpawnRegionRange =
 * SpawnTileFinder.CLEAR_SPAWN_REGION_RANGE}, so none of these can land on the
 * world spawn.
 *
 * <h2>No map marker, because vanilla gives this class of structure none</h2>
 * Vanilla's world-preset marker path is a consumable {@code WorldPresetMapItem}
 * that resolves through {@code WorldEntity.findClosestWorldPreset}, and
 * {@code ItemRegistry} points those six items at exactly six presets:
 * {@code surfacevillages}, {@code dungeonentrance}, {@code piratevillages} and
 * three boss arenas. Nothing in {@code SurfacePresetsWorldPreset} — no hunter
 * cabin, no abandoned camp, no crashed meteor — is on that path, and it cannot
 * be: {@code GenerationPresetsWorldPreset}'s constructor sets
 * {@code shouldSaveGenerated = false}, so those placements are never written to
 * the generated-presets file that {@code findClosestWorldPreset} searches.
 * The mod's own {@code MapIconRegistry} use ({@code quest/SkyMapMarkers}) is for
 * quest destinations, which these are not. So: found by exploring.
 */
public class SkySurfacePresets extends GenerationPresetsWorldPreset {

    /** Registry string ID. */
    public static final String STRING_ID = "swhsurfacepois";

    /**
     * Structures per 16x16 region, i.e. the density knob
     * ({@code GenerationPresetsWorldPreset.addToRegion} turns it into
     * {@code (tileWidth * tileHeight / 256) * presetsPerRegion} draws per
     * 1024x1024 preset region). Vanilla's whole surface list runs at 0.05 for
     * about ninety structure types; ours runs at 0.0035 for three, which lands
     * near one of each per 390x390 tiles — the frequency of a single vanilla
     * surface structure type, not of vanilla's whole catalogue.
     */
    public static final float PRESETS_PER_REGION = 0.0035F;

    /** Relative draw weights, in the same units vanilla's list uses (70-300). */
    public static final int CRATER_TICKETS = 120;
    public static final int CAMP_TICKETS = 100;
    public static final int SHRINE_TICKETS = 70;

    /**
     * The three structures, held so a probe can read their placement counters.
     * Constructing them here is safe: {@link SimpleGenerationPreset#init()} —
     * the part that builds a real {@link Preset} and therefore needs every
     * object and tile ID to be resolved — runs later, from {@code addPreset}
     * inside {@link #addCorePresets()} at registry close.
     */
    public final CraterGeneration crater = new CraterGeneration();
    public final CampGeneration camp = new CampGeneration();
    public final ShrineGeneration shrine = new ShrineGeneration();

    public SkySurfacePresets() {
        super(PRESETS_PER_REGION);
    }

    @Override
    public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
        return presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER);
    }

    @Override
    public void addCorePresets() {
        this.addPreset(CRATER_TICKETS, this.crater);
        this.addPreset(CAMP_TICKETS, this.camp);
        this.addPreset(SHRINE_TICKETS, this.shrine);
    }

    /** The three in a fixed order, for reporting. */
    public SkySurfaceGeneration[] all() {
        return new SkySurfaceGeneration[]{this.crater, this.camp, this.shrine};
    }

    // -----------------------------------------------------------------------
    // The three structures
    // -----------------------------------------------------------------------

    /**
     * Shared shape for all three: mirror on both axes, no rotation (the same
     * flags {@code SmallForgottenShrineGenerationPreset} and
     * {@code CrashedMeteorGenerationPreset} use), 20 placement attempts, and
     * every one of the five vanilla surface biomes. Sky debris does not care
     * which biome it lands in — but the ground does have to be land, which is
     * what {@link #setupTester} enforces.
     */
    public abstract static class SkySurfaceGeneration extends SimpleGenerationPreset {

        /**
         * How many times this POI has actually been stamped into a level this
         * session. Incremented in {@link #modifyPreset}, which
         * {@code SimpleGenerationPreset.addToRegion}'s place function calls at
         * the moment the preset is written — so this counts real placements,
         * not queued intentions. Read by {@code SkySurfaceStatusCommand}.
         */
        public final AtomicInteger placed = new AtomicInteger();

        protected SkySurfaceGeneration() {
            super(20, true, true, false, false,
                    BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SNOW,
                    BiomeRegistry.SWAMP, BiomeRegistry.DESERT);
        }

        /** The object ID a probe counts to prove this POI landed. */
        public abstract String signatureObject();

        @Override
        public void setupTester(WorldPresetTester tester) {
            // Every fourth tile of the footprint, plus its four corners, must be
            // dry land. runGridCheck starts with runCornerCheck, so a POI can
            // neither hang off a coast nor sit in a river.
            tester.addApplyPredicate(new WorldApplyAreaPredicate(0, 0, tester.width - 1, tester.height - 1, 0,
                    new WorldApplyAreaPredicate.WorldApplyGridTest(4) {
                        @Override
                        public boolean isValidTile(WorldPreset preset, LevelPresetsRegion presetsRegion,
                                BiomeGeneratorStack generatorStack, int tileX, int tileY) {
                            return !SkySurfaceGeneration.this.isWaterOrLavaOrBeach(presetsRegion, generatorStack, tileX, tileY);
                        }
                    }));
        }

        @Override
        public Preset modifyPreset(Preset preset, LevelPresetsRegion presetsRegion,
                BiomeGeneratorStack generatorStack, int tileX, int tileY) {
            this.placed.incrementAndGet();
            return preset;
        }
    }

    /** The Fallen Sky Fragment. */
    public static class CraterGeneration extends SkySurfaceGeneration {
        @Override
        public String signatureObject() {
            return SkyFragmentCraterPreset.SIGNATURE_OBJECT;
        }

        @Override
        public Preset getPreset(GameRandom random) {
            return new SkyFragmentCraterPreset(random);
        }
    }

    /** The Stranded Aeronaut Camp. */
    public static class CampGeneration extends SkySurfaceGeneration {
        @Override
        public String signatureObject() {
            return AeronautCampPreset.SIGNATURE_OBJECT;
        }

        @Override
        public Preset getPreset(GameRandom random) {
            return new AeronautCampPreset(random);
        }
    }

    /** The Derelict Skyward Shrine. */
    public static class ShrineGeneration extends SkySurfaceGeneration {
        @Override
        public String signatureObject() {
            return SkywardShrinePreset.SIGNATURE_OBJECT;
        }

        @Override
        public Preset getPreset(GameRandom random) {
            return new SkywardShrinePreset(random);
        }
    }
}
