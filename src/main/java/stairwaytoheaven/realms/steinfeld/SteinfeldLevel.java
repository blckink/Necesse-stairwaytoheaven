package stairwaytoheaven.realms.steinfeld;

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
 * Steinfeld — The Quiet Reach. The mod's third dimension, two layers above the
 * Skyreach ({@code SkyRegistry.STEINFELD_DIMENSION} = 3).
 *
 * <p>Infinite and generated region by region from the world seed, exactly like
 * {@code SkyLevel} and {@code VeilLevel}. Not a cave: {@code isCave} stays
 * false, so the Reach follows the world's day/night light the way a surface
 * does, and the fog is a matter of the ground's own colour rather than of
 * darkness. A dead realm that is also pitch black would read as a cave.
 *
 * <p>Everything about the terrain is in {@link SteinfeldTerrainPainter}; this
 * class does the one thing a painter cannot, which
 * {@code docs/WORLD_DESIGN.md} A4.1 requires: {@link #placeGuardPacks} puts a
 * persistent pack on the ground around every POI, at generation, so a
 * guarded place is guarded before the player arrives and is still guarded
 * when they come back for the crate they left.
 *
 * <p><b>Deferred:</b> §7's "World events" line — transparent, unattackable
 * ghosts that walk to a grave, a door, or the map edge — is not built here.
 * See docs/realms/steinfeld.md.
 */
public class SteinfeldLevel extends BiomeGeneratorStackLevel {

    /**
     * Required by LevelRegistry: the game reconstructs a registered level
     * through this exact constructor when loading a saved world (the seed comes
     * back afterwards through applyLoadData, same as vanilla cave levels).
     */
    public SteinfeldLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
        this.setup();
    }

    /** Used on first generation, when the world generator supplies the seed. */
    public SteinfeldLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity, int seed) {
        super(identifier, width, height, worldEntity, seed);
        this.setup();
    }

    private void setup() {
        this.isCave = false;
        this.baseBiome = SkyRegistry.slabFields;
    }

    /**
     * Steinfeld's terrain seed, derived from the world seed alone and salted so
     * the Reach never mirrors another layer's layout. Static and level-free, so
     * anything outside a level — a world preset, an offline harness — can ask
     * the painter the same question and get the same answer.
     */
    public static int worldGenSeed(String worldSeed) {
        int derived = (worldSeed != null ? worldSeed.hashCode() : 0) ^ 0x51E1FE1D;
        return derived != 0 ? derived : 1;
    }

    public int getWorldGenSeed() {
        if (this.seed != 0) {
            return this.seed;
        }
        return worldGenSeed(this.getWorldEntity() != null ? this.getWorldEntity().worldSeed : null);
    }

    @Override
    public void generateRegion(Region region) {
        super.generateRegion(region);
        this.startDirtyRegionTracking();
        int presetGenerationUniqueID = this.getWorldEntity().startPresetGenerationInRegion(region, this.seed);
        try {
            SteinfeldTerrainPainter.paintRegion(region, this.getWorldGenSeed());
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
    }

    // ---- Guarded places -----------------------------------------------------

    /**
     * The packs that stand over Steinfeld's loot.
     *
     * <p>Modelled one-for-one on {@code SkyLevel.placeGuardPacks}, including
     * the reason the loop is over LATTICE CELLS rather than over the region's
     * tiles: a region is 16x16 tiles and a pack is spread over a disc of radius
     * {@link SteinfeldPressure#GUARD_RADIUS} = 7, so a pack routinely straddles
     * four regions. Scanning this region for site centres would place a pack
     * only when the centre happened to fall inside it and silently lose the
     * rest. Scanning the cells that can REACH this region, deriving every
     * member's tile from the site seed, and placing only the members whose tile
     * lands inside, puts each guard down exactly once no matter what order the
     * regions generate in.
     *
     * <p>Steinfeld has ONE lattice, so unlike the sky there is no way for the
     * guards and the loot to come from different fields.
     */
    private void placeGuardPacks(Region region) {
        if (this.isClient()) {
            return;
        }
        int seed = this.getWorldGenSeed();
        int reach = (int) Math.ceil(SteinfeldPressure.GUARD_RADIUS) + 1;
        int minX = region.tileXOffset - reach;
        int minY = region.tileYOffset - reach;
        int maxX = region.tileXOffset + region.tileWidth + reach;
        int maxY = region.tileYOffset + region.tileHeight + reach;

        // The organic POI lattice -- grave field, ruined chapel or statue
        // field, painted tile by tile by SteinfeldTerrainPainter.
        this.scanLattice(region, seed, minX, minY, maxX, maxY,
                SteinfeldTerrainPainter.POI_CELL, SteinfeldTerrainPainter.SALT_POI,
                SteinfeldTerrainPainter.POI_CHANCE);
        // SteinfeldSites' two hand-authored landmarks -- see that class's
        // header for why they are a second lattice. Same guard placement,
        // same radius: A4.1 does not stop applying because the building was
        // drawn by hand rather than grown from noise.
        this.scanLattice(region, seed, minX, minY, maxX, maxY,
                SteinfeldSites.GRAVEYARD_CELL, SteinfeldSites.SALT_GRAVEYARD,
                SteinfeldSites.GRAVEYARD_CHANCE);
        this.scanLattice(region, seed, minX, minY, maxX, maxY,
                SteinfeldSites.CHAPEL_CELL, SteinfeldSites.SALT_CHAPEL,
                SteinfeldSites.CHAPEL_CHANCE);
    }

    /** Every site of one lattice that can reach this region, packed once each. */
    private void scanLattice(Region region, int seed, int minX, int minY, int maxX, int maxY,
            int cell, int salt, float chance) {
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
                this.placePackAt(region, siteX, siteY);
            }
        }
    }

    /**
     * One pack, around one site.
     *
     * <p>Every member's tile is a pure function of the site position and the
     * member's index, so the pack is the same in every save and on every
     * client, and a member is placed by whichever region happens to contain its
     * tile.
     */
    private void placePackAt(Region region, int siteX, int siteY) {
        necesse.level.maps.biomes.Biome biome = this.getBiome(siteX, siteY);
        if (!(biome instanceof GuardedBiome)) {
            return;
        }
        GuardedBiome.Guard guard = ((GuardedBiome) biome).getGuard();
        if (guard == null) {
            return;
        }
        long packSeed = (this.getWorldGenSeed() * 0x9E3779B97F4A7C15L)
                ^ ((long) siteX * 0x85EBCA77L)
                ^ ((long) siteY * 0xC2B2AE3DL);
        GameRandom random = new GameRandom(packSeed);
        int size = random.getIntBetween(guard.minSize, guard.maxSize);
        float radius = SteinfeldPressure.GUARD_RADIUS;
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
            // Persistent, exactly like the sky's packs. VERIFIED [jar]:
            // EntityManager.tickMobSpawning counts only
            // (isHostile && canDespawn) against the spawn cap, so a placed
            // guard costs the ambient budget nothing -- and the site is still
            // guarded when the player comes back for the crate they left.
            mob.canDespawn = false;
            this.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
        }
    }

    // ---- The ghosts (deferred) -----------------------------------------------
    //
    // docs/WORLD_DESIGN.md §7's "World events" line -- transparent, unattackable
    // ghosts that walk to a grave, a door, or the map edge -- is NOT built in
    // this pass. It needs its own invulnerable-and-untargetable mob archetype
    // with destination-seeking pathing, which is a real feature in its own
    // right and not one of this pass's four residents. Left out cleanly rather
    // than half-wired: there was a call here to a SteinfeldProcessionEvent that
    // was never written, which would not have compiled. See
    // docs/realms/steinfeld.md's Deferred section.

    @Override
    public boolean canRain() {
        // Fog, not weather. See SteinfeldBiome.canRain.
        return false;
    }
}
