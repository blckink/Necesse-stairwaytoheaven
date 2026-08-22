package stairwaytoheaven.worldgen;

/**
 * Deterministic, allocation-free value noise used by Skyreach region generation.
 *
 * Every function is a pure function of (worldSeed, tileX, tileY), so neighboring
 * regions generate seamlessly and regeneration is fully reproducible — the same
 * guarantee vanilla's BiomeGeneratorStack gives cave layers.
 */
public final class SkyNoise {
    private SkyNoise() {
    }

    /** 64-bit mix hash of a lattice point, mapped to [0, 1). */
    public static float hash(long seed, int x, int y) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 23) * 0xC2B2AE3D27D4EB4FL;
        h ^= y * 0x165667B19E3779F9L;
        h = Long.rotateLeft(h, 29) * 0x27D4EB2F165667C5L;
        h ^= h >>> 32;
        h *= 0x2545F4914F6CDD1DL;
        h ^= h >>> 29;
        // 24 mantissa bits -> uniform float in [0, 1)
        return (h >>> 40) / (float) (1 << 24);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    /** Bilinear value noise at an arbitrary point, lattice cell size = scale tiles. */
    public static float value(long seed, float x, float y, float scale) {
        float fx = x / scale;
        float fy = y / scale;
        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        float tx = smooth(fx - x0);
        float ty = smooth(fy - y0);
        float v00 = hash(seed, x0, y0);
        float v10 = hash(seed, x0 + 1, y0);
        float v01 = hash(seed, x0, y0 + 1);
        float v11 = hash(seed, x0 + 1, y0 + 1);
        float top = v00 + (v10 - v00) * tx;
        float bottom = v01 + (v11 - v01) * tx;
        return top + (bottom - top) * ty;
    }

    /** Fractal (fBm) value noise, normalized back to [0, 1]. */
    public static float fbm(long seed, float x, float y, float scale, int octaves) {
        float sum = 0.0F;
        float amplitude = 1.0F;
        float totalAmplitude = 0.0F;
        float frequency = 1.0F;
        for (int i = 0; i < octaves; i++) {
            // Different seed per octave so octaves are uncorrelated
            sum += value(seed + i * 0x51ED2701L, x * frequency, y * frequency, scale) * amplitude;
            totalAmplitude += amplitude;
            amplitude *= 0.5F;
            frequency *= 2.0F;
        }
        return sum / totalAmplitude;
    }

    /** Per-tile chance roll in [0, 1), independent of the noise fields above. */
    public static float tileRoll(long seed, int tileX, int tileY, int salt) {
        return hash(seed ^ (0xA24BAED4963EE407L + salt * 0x9FB21C651E98DF25L), tileX, tileY);
    }
}
