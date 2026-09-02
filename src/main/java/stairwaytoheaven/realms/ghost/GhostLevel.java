package stairwaytoheaven.realms.ghost;

import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.level.maps.BiomeGeneratorStackLevel;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.regionSystem.Region;
import stairwaytoheaven.biomes.GuardedBiome;
import stairwaytoheaven.worldgen.SkyNoise;

/**
 * The Ghost Realm / Aftergarden: the mod's fourth rung, dimension +4.
 *
 * <p>Built exactly the way {@code VeilLevel} is built, because that is the
 * closest working relative and re-deriving a level shape nobody asked to change
 * is how working things break. Infinite, generated region by region from the
 * world seed, and {@code isCave = true} so the realm has no day: light comes
 * only from what the player brings and from what glows here — lantern trees,
 * spirit mushrooms and the ectoplasm itself. "Life is gone" is not a mood note;
 * it is why there is no sun.
 *
 * <h2>Two things this does that VeilLevel does not</h2>
 * <ol>
 * <li>{@link #placeGuardPacks} — the half of {@code WORLD_DESIGN} A4.1 that
 *     {@link GhostPressure} cannot buy. The pressure field makes open ground
 *     silent; this makes arriving at a POI loud, by standing a persistent pack
 *     over the loot at generation. The mechanism is
 *     {@code SkyLevel.placeGuardPacks}, ported to this realm's own three
 *     lattices.</li>
 * <li>Nothing else. There is no resident placement and no herd placement here:
 *     the three Ghost NPCs ({@code WORLD_DESIGN} §11) and the five Ghost
 *     animals (§12) are a separate job and are deferred — see
 *     {@code docs/realms/ghost.md}.</li>
 * </ol>
 */
public class GhostLevel extends BiomeGeneratorStackLevel {

    /**
     * Required by LevelRegistry: the game reconstructs registered levels through
     * this exact constructor signature when loading a saved world (the seed is
     * restored afterwards via applyLoadData, same as vanilla cave levels).
     */
    public GhostLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
        super(identifier, width, height, worldEntity);
        this.setup();
    }

    /** Used on first generation, when the world generator supplies the seed. */
    public GhostLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity, int seed) {
        super(identifier, width, height, worldEntity, seed);
        this.setup();
    }

    private void setup() {
        this.isCave = true;
        this.baseBiome = GhostRealm.aftergarden;
    }

    public int getWorldGenSeed() {
        if (this.seed != 0) {
            return this.seed;
        }
        return worldGenSeed(this.getWorldEntity() != null ? this.getWorldEntity().worldSeed : null);
    }

    /**
     * The Aftergarden's terrain seed, derived from the world seed alone.
     *
     * <p>Static and level-free for the same reason {@code VeilLevel}'s is: the
     * POI placement test runs inside the world-preset system, which is handed a
     * {@code WorldEntity} and never a {@code Level}. It has to be able to ask
     * {@link GhostTerrainPainter} the same questions the painter will answer
     * later and get the same answers, with no level in hand.
     *
     * <p>The salt is what stops the realm mirroring another layer's coastline
     * in the same world.
     */
    public static int worldGenSeed(String worldSeed) {
        int derived = (worldSeed != null ? worldSeed.hashCode() : 0) ^ 0x64057A11;
        return derived != 0 ? derived : 1;
    }

    @Override
    public void generateRegion(Region region) {
        super.generateRegion(region);
        this.startDirtyRegionTracking();
        int presetGenerationUniqueID = this.getWorldEntity().startPresetGenerationInRegion(region, this.seed);
        try {
            GhostTerrainPainter.paintRegion(region, this.getWorldGenSeed());
        } finally {
            this.getWorldEntity().runPresetGenerationInRegion(presetGenerationUniqueID, region, this.seed);
            this.removeDirtyRegion(region.regionX, region.regionY);
        }
    }

    @Override
    public void onRegionGenerated(Region region, boolean skipGenerateForced) {
        super.onRegionGenerated(region, skipGenerateForced);
        region.checkGenerationValid();
        placeGuardPacks(region);
    }

    @Override
    public boolean canRain() {
        return false;
    }

    // ---- Guarded places ---------------------------------------------------

    /**
     * The packs that stand over the Aftergarden's loot.
     *
     * <p>{@link GhostPressure} is the half that makes open ground quiet; this
     * is the half that makes arriving somewhere loud. Both halves read the same
     * three lattices, so a pack cannot end up standing where the tomb is not.
     *
     * <p><b>VERIFIED [jar]</b>, and the reason a placed pack is the right answer
     * rather than an expensive one: {@code EntityManager.tickMobSpawning} counts
     * only {@code m.isHostile && m.canDespawn} against the player's spawn cap,
     * so a persistent guard costs the ambient budget nothing; and because it
     * never despawns, a site stays guarded between visits instead of being
     * repopulated by whatever the weather rolled.
     */
    private void placeGuardPacks(Region region) {
        if (this.isClient()) {
            return;
        }
        placePacksOf(region, GhostTerrainPainter.MAUSOLEUM_CELL,
                GhostTerrainPainter.SALT_MAUSOLEUM, GhostTerrainPainter.MAUSOLEUM_CHANCE, 0x9E3779B1L);
        placePacksOf(region, GhostTerrainPainter.MANOR_CELL,
                GhostTerrainPainter.SALT_MANOR, GhostTerrainPainter.MANOR_CHANCE, 0x85EBCA77L);
        placePacksOf(region, GhostTerrainPainter.GRAVEYARD_CELL,
                GhostTerrainPainter.SALT_GRAVEYARD, GhostTerrainPainter.GRAVEYARD_CHANCE, 0xC2B2AE3DL);
    }

    /**
     * Every site of one lattice whose pack reaches into this region.
     *
     * <p><b>Why the loop is over cells and not over tiles.</b> A region is
     * 16x16 tiles and a pack spreads over a disc of radius
     * {@link GhostPressure#GUARD_RADIUS} = 7, so a pack routinely straddles four
     * regions. Scanning this region for site centres would place a pack only
     * when the centre happened to land inside it and lose the rest; scanning the
     * cells that could REACH this region, deriving every member's position from
     * the site seed, and placing only the members whose tile falls inside it,
     * puts each guard down exactly once whatever order the regions generate in.
     */
    private void placePacksOf(Region region, int cell, int salt, float chance, long saltMix) {
        int seed = this.getWorldGenSeed();
        int reach = (int) Math.ceil(GhostPressure.GUARD_RADIUS) + 1;
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
                placePackAt(region, siteX, siteY, saltMix);
            }
        }
    }

    /**
     * One pack, around one site.
     *
     * <p>Every member's tile is a pure function of the site position and the
     * member's index, so the pack is the same in every save and on every client,
     * and a member is placed by whichever region happens to contain its tile.
     * The eight-attempt search per member keeps a pack out of the ectoplasm
     * without moving the site.
     */
    private void placePackAt(Region region, int siteX, int siteY, long saltMix) {
        Biome biome = this.getBiome(siteX, siteY);
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
        float radius = GhostPressure.GUARD_RADIUS;
        for (int i = 0; i < size; i++) {
            String who = guard.memberAt(i, random.nextFloat());
            // Anchors stand close in; rabble spreads to the edge of the ground
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
                        && this.getTileID(tileX, tileY) != GhostRealm.ectoplasmID
                        && this.getObjectID(tileX, tileY) == 0;
            }
            if (!found) {
                continue;
            }
            // Only this region's share: the other members belong to the regions
            // their own tiles fall in, and are placed when those generate.
            if (tileX < region.tileXOffset || tileX >= region.tileXOffset + region.tileWidth
                    || tileY < region.tileYOffset || tileY >= region.tileYOffset + region.tileHeight) {
                continue;
            }
            Mob mob = MobRegistry.getMob(who, this);
            if (mob == null) {
                continue;
            }
            mob.canDespawn = false;
            this.entityManager.addMob(mob, tileX * 32 + 16, tileY * 32 + 16);
        }
    }
}
