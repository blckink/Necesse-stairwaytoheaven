package stairwaytoheaven.realms.crooked;

import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.level.maps.BiomeGeneratorStackLevel;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.biomes.GuardedBiome;
import stairwaytoheaven.worldgen.SkyNoise;

/**
 * Crooked Beyond — Tier 4, the realm where the rules of the world have decayed
 * along with the landscape.
 *
 * <p>Built exactly the way {@link stairwaytoheaven.level.SkyLevel} and
 * {@link stairwaytoheaven.level.VeilLevel} are built: an infinite
 * {@code BiomeGeneratorStackLevel} generated region by region from the world
 * seed, with a per-region painter bracketed by the world entity's preset
 * generation so structures stamp inside the same pass.
 *
 * <p><b>Not a cave.</b> {@code isCave} stays false, so the realm follows the
 * world's day/night cycle like the Skyreach does. That is a deliberate choice
 * and not a copy-paste: black-and-white stripes, a chequerboard and a neon-green
 * sea only read as themselves in light, and what this realm LOOKS like is most
 * of what it is. It also keeps the three inherited Crooked bodies behaving the
 * way they already do on the Outlands rim — dark-spawners on a lit level, so the
 * place is uneasy by day and genuinely dangerous after dark.
 *
 * <h2>How a player gets here</h2>
 * Through a Crooked Door, which is a Seance Circle opened inside the Skyreach's
 * Beetle Outlands. See {@link stairwaytoheaven.objects.SeanceCircleObject} for
 * the rule and {@link CrookedDoorObject} for the pair. The Outlands are the rim
 * of this place and now say so: their portal lattice was already standing rings
 * nobody could use.
 */
public class CrookedLevel extends BiomeGeneratorStackLevel {

    /**
     * Required by {@code LevelRegistry}: the game reconstructs registered levels
     * through this exact constructor signature when loading a saved world (the
     * seed is restored afterwards via {@code applyLoadData}, same as vanilla
     * cave levels).
     */
    public CrookedLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
        this.setup();
    }

    /** Used on first generation, when the world generator supplies the seed. */
    public CrookedLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity, int seed) {
        super(identifier, width, height, worldEntity, seed);
        this.setup();
    }

    private void setup() {
        this.isCave = false;
        this.baseBiome = CrookedRealm.stripedWaste;
    }

    public int getWorldGenSeed() {
        if (this.seed != 0) {
            return this.seed;
        }
        return worldGenSeed(this.getWorldEntity() != null ? this.getWorldEntity().worldSeed : null);
    }

    /**
     * The realm's terrain seed, derived from the world seed alone.
     *
     * <p>Static and level-free on purpose, for the reason
     * {@link stairwaytoheaven.level.VeilLevel#worldGenSeed} records: the preset
     * placer runs inside the world-preset system, which is handed a
     * {@code WorldEntity} and never a {@code Level}. It has to be able to ask
     * {@link CrookedTerrainPainter} the same questions the painter will answer
     * later, and get the same answers, without a level in hand.
     *
     * <p>The salt differs from the Skyreach's and the Veil's so the three layers
     * never mirror one another's coastlines.
     */
    public static int worldGenSeed(String worldSeed) {
        int derived = (worldSeed != null ? worldSeed.hashCode() : 0) ^ 0x0C0FFEE5;
        return derived != 0 ? derived : 1;
    }

    @Override
    public void generateRegion(Region region) {
        super.generateRegion(region);
        this.startDirtyRegionTracking();
        int presetGenerationUniqueID = this.getWorldEntity().startPresetGenerationInRegion(region, this.seed);
        try {
            CrookedTerrainPainter.paintRegion(region, this.getWorldGenSeed());
        } finally {
            this.getWorldEntity().runPresetGenerationInRegion(presetGenerationUniqueID, region, this.seed);
            this.removeDirtyRegion(region.regionX, region.regionY);
        }
    }

    @Override
    public void onRegionGenerated(Region region, boolean skipGenerateForced) {
        super.onRegionGenerated(region, skipGenerateForced);
        region.checkGenerationValid();
        this.placeGuardPacks(region);
        stairwaytoheaven.settlement.CrookedResidents.place(this, region, this.getWorldGenSeed());
    }

    @Override
    public boolean canRain() {
        return false;
    }

    // ---- Guarded places ------------------------------------------------------

    /**
     * The packs that stand over this realm's loot.
     *
     * <p>{@code WORLD_DESIGN.md} A4.1: <i>"sie sollen mal geballt kommen und ein
     * Gebiet z.b bewachen wo es loot gibt, in anderen Ecken aber nicht dauernd
     * angeflogen kommen"</i>. {@link CrookedPressure} is the half that makes the
     * open realm quiet; this is the half that makes arriving somewhere loud.
     *
     * <p>The guards are found through the same three lattices the painter paved
     * the ground with and the preset placer stamped the buildings on
     * ({@link CrookedSites}), so a pack cannot end up standing where the
     * building is not.
     */
    private void placeGuardPacks(Region region) {
        if (this.isClient()) {
            return;
        }
        this.placePacksOf(region, CrookedSites.DOORYARD_CELL, CrookedSites.SALT_DOORYARD,
                CrookedSites.DOORYARD_CHANCE, 0x9E3779B1L);
        this.placePacksOf(region, CrookedSites.INVERTED_CELL, CrookedSites.SALT_INVERTED,
                CrookedSites.INVERTED_CHANCE, 0x85EBCA77L);
        this.placePacksOf(region, CrookedSites.LONGTABLE_CELL, CrookedSites.SALT_LONGTABLE,
                CrookedSites.LONGTABLE_CHANCE, 0xC2B2AE3DL);
    }

    /**
     * Every site of one lattice whose pack reaches into this region.
     *
     * <p><b>Why the loop is over cells and not over tiles</b>, copied verbatim
     * in reasoning from {@code SkyLevel.placePacksOf}: a region is 16x16 tiles
     * and a pack is spread over a disc of radius
     * {@link CrookedPressure#GUARD_RADIUS} = 8, so a pack routinely straddles
     * four regions. Scanning this region for site centres would place a pack
     * only when the centre happened to fall inside it and lose the rest.
     * Scanning the cells that could REACH this region, deriving every member's
     * position from the site seed, and placing only the members whose tile falls
     * inside it, puts each guard down exactly once no matter which order the
     * regions generate in.
     */
    private void placePacksOf(Region region, int cell, int salt, float chance, long saltMix) {
        int seed = this.getWorldGenSeed();
        int reach = (int) Math.ceil(CrookedPressure.GUARD_RADIUS) + 1;
        int minX = region.tileXOffset - reach;
        int minY = region.tileYOffset - reach;
        int maxX = region.tileXOffset + region.tileWidth + reach;
        int maxY = region.tileYOffset + region.tileHeight + reach;
        for (int cx = Math.floorDiv(minX, cell); cx <= Math.floorDiv(maxX, cell); cx++) {
            for (int cy = Math.floorDiv(minY, cell); cy <= Math.floorDiv(maxY, cell); cy++) {
                if (SkyNoise.hash(seed + salt, cx, cy) >= chance) {
                    continue;
                }
                int siteX = Math.round(cx * cell + SkyNoise.hash(seed + salt + 1, cx, cy) * cell);
                int siteY = Math.round(cy * cell + SkyNoise.hash(seed + salt + 2, cx, cy) * cell);
                if (siteX < minX || siteX > maxX || siteY < minY || siteY > maxY) {
                    continue;
                }
                this.placePackAt(region, siteX, siteY, saltMix);
            }
        }
    }

    /**
     * One pack, around one site.
     *
     * <p>Every member's tile is a pure function of the site position and the
     * member's index, so the pack is the same in every save and on every client,
     * and a member is placed by whichever region happens to contain its tile.
     * The eight-attempt search per member keeps a pack out of the Spill without
     * moving the site.
     */
    private void placePackAt(Region region, int siteX, int siteY, long saltMix) {
        necesse.level.maps.biomes.Biome biome = this.getBiome(siteX, siteY);
        if (!(biome instanceof GuardedBiome)) {
            return;
        }
        GuardedBiome.Guard guard = ((GuardedBiome) biome).getGuard();
        if (guard == null) {
            return;
        }
        long packSeed = (this.getWorldGenSeed() * 0x9E3779B97F4A7C15L)
                ^ ((long) siteX * saltMix)
                ^ ((long) siteY * 0xC2B2AE3DL);
        GameRandom random = new GameRandom(packSeed);
        int size = random.getIntBetween(guard.minSize, guard.maxSize);
        float radius = CrookedPressure.GUARD_RADIUS;
        for (int i = 0; i < size; i++) {
            String who = guard.memberAt(i, random.nextFloat());
            // Anchors stand close in, rabble spreads to the edge of the ground
            // the pressure field marks as the site's own.
            float near = i < guard.anchors.length ? 3.0F : radius;
            int tileX = 0;
            int tileY = 0;
            boolean found = false;
            for (int attempt = 0; attempt < 8 && !found; attempt++) {
                tileX = siteX + random.getIntBetween(-(int) near, (int) near);
                tileY = siteY + random.getIntBetween(-(int) near, (int) near);
                found = this.isTileWithinBounds(tileX, tileY)
                        && !this.isSolidTile(tileX, tileY)
                        && this.getTileID(tileX, tileY) != CrookedRealm.spillID
                        && this.getObjectID(tileX, tileY) == 0;
            }
            if (!found) {
                continue;
            }
            // Only this region's share: the other members belong to the regions
            // their own tiles fall in, and will be placed when those generate.
            if (tileX < region.tileXOffset || tileX >= region.tileXOffset + region.tileWidth
                    || tileY < region.tileYOffset || tileY >= region.tileYOffset + region.tileHeight) {
                continue;
            }
            Mob mob = MobRegistry.getMob(who, this);
            if (mob == null) {
                continue;
            }
            // Persistent, exactly like the Skyreach's packs. VERIFIED [jar]:
            // EntityManager.tickMobSpawning counts only (isHostile &&
            // canDespawn) against the spawn cap, so a placed guard does not eat
            // the ambient budget -- and the site is still guarded when the
            // player comes back for the crate they left.
            mob.canDespawn = false;
            this.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
        }
    }

    /**
     * Where a player arriving through a Crooked Door lands.
     *
     * <p>A door is opened at an arbitrary tile of the Skyreach's Outlands, and
     * the Crooked side of it is placed at the SAME coordinates — which in an
     * infinite realm made mostly of Spill is very often open water. Rather than
     * drown the arrival, {@link CrookedDoorObjectEntity} reclaims the landing
     * tile the way the Veil rift does; this is the tile it reclaims TO.
     */
    public static int landingTileID() {
        return CrookedRealm.crookedStripeID;
    }

    /** The Skyreach identifier a Crooked door leads back to. */
    public static LevelIdentifier returnIdentifier() {
        return SkyRegistry.SKYREACH_IDENTIFIER;
    }
}
