package stairwaytoheaven.worldgen;

import java.awt.Point;

import necesse.engine.world.WorldEntity;

/**
 * The canonical Skyreach origin: the fixed, seed-deterministic world position
 * of the Old Warden Spire hub.
 *
 * The new design makes the Spire the CENTER of the sky. Every Stairway to
 * Heaven leads here regardless of where it was placed on the surface — the
 * stairway is a portal, not a coordinate ladder — and world generation
 * radiates outward from this point: distance from the origin drives terrain
 * guarantees, resource density and (later) spawn intensity.
 *
 * Everything here is a pure function of the world generation seed, so the
 * terrain painter (running per region, possibly before the spire is ever
 * stamped), the surface stairway entities and the spire stamping logic all
 * agree on the same position without any shared mutable state.
 */
public final class SkyOrigin {

    /** Salt so the sky origin never mirrors another layer's use of the seed. */
    private static final int SEED_SALT = 0x5EED51CE;

    /**
     * The spire hub island is guaranteed within this radius of the origin
     * (the terrain painter clamps the island mask and the biome mask here).
     */
    public static final int HUB_RADIUS = 56;

    /**
     * The player's arrival tile when ascending: on the spire's south approach
     * path. The 15x15 spire preset is applied centered on the origin, its door
     * sits at origin + (0, +4), the path runs to origin + (0, +7).
     */
    public static final int ARRIVAL_OFFSET_Y = 6;

    /** Radial progression bands (distance in tiles from the origin). */
    public static final float CORE_RADIUS = 700.0F;
    public static final float MID_RADIUS = 1600.0F;

    private SkyOrigin() {
    }

    /**
     * The Skyreach world-generation seed: the same derivation
     * {@code SkyLevel.getWorldGenSeed()} uses, exposed so code running on OTHER
     * levels (the surface stairway entities) can compute sky positions without
     * touching the sky level.
     */
    public static int worldGenSeed(WorldEntity worldEntity) {
        if (worldEntity == null) {
            return 1;
        }
        String worldSeed = worldEntity.worldSeed;
        int derived = (worldSeed != null ? worldSeed.hashCode() : 0) ^ SEED_SALT;
        return derived != 0 ? derived : 1;
    }

    /** Integer hash (fmix32 finalizer) — avalanche for small seed deltas. */
    private static int mix(int x) {
        x ^= x >>> 16;
        x *= 0x85EBCA6B;
        x ^= x >>> 13;
        x *= 0xC2B2AE35;
        x ^= x >>> 16;
        return x;
    }

    /**
     * The canonical spire position: a seed-deterministic offset near the world
     * origin (0, 0). Deterministic, allocation-free, and identical on every
     * call — this is the one sky position everything else is defined relative
     * to.
     *
     * RANGE: Java's % keeps the sign of the dividend, so each axis lands in
     * -576..+192 rather than the symmetric ±192 the arithmetic reads like.
     * That is a wider, negatively-biased box than intended, but it is harmless
     * (the hub clamp in SkyTerrainPainter guarantees land wherever it falls,
     * and the level streams regions in every direction), and it is now part of
     * the world contract: correcting the arithmetic would move the spire in
     * every existing save. Left as-is deliberately — do not "fix" it.
     */
    public static Point compute(int worldGenSeed) {
        int h = mix(worldGenSeed);
        int dx = mix(h ^ 0x9E3779B9) % 385 - 192;
        int dy = mix(h ^ 0x85EBCA77) % 385 - 192;
        return new Point(dx, dy);
    }

    /** Origin for the world this entity belongs to. */
    public static Point compute(WorldEntity worldEntity) {
        return compute(worldGenSeed(worldEntity));
    }

    /** The tile players arrive on when ascending (south approach path). */
    public static Point arrival(int worldGenSeed) {
        Point origin = compute(worldGenSeed);
        return new Point(origin.x, origin.y + ARRIVAL_OFFSET_Y);
    }

    /** Arrival tile for the world this entity belongs to. */
    public static Point arrival(WorldEntity worldEntity) {
        return arrival(worldGenSeed(worldEntity));
    }

    /**
     * Radial progression band of a world position: 0 = core (safe hub region),
     * 1 = mid reaches, 2 = outer reaches. Distance drives resource density in
     * the terrain painter and (planned) spawn intensity via reach-biome
     * variants.
     */
    public static int distanceBand(int worldGenSeed, int tileX, int tileY) {
        Point origin = compute(worldGenSeed);
        float dx = tileX - origin.x;
        float dy = tileY - origin.y;
        return bandFor((float) Math.sqrt(dx * dx + dy * dy));
    }

    /** Band for an already-computed distance (allocation-free hot path). */
    public static int bandFor(float dist) {
        if (dist < CORE_RADIUS) {
            return 0;
        }
        return dist < MID_RADIUS ? 1 : 2;
    }
}
