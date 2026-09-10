package stairwaytoheaven.realms.hell;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.realms.crooked.CrookedRealm;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.SkyNoise;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/**
 * Hell's band painter — the outermost realm on the plane, and until now the
 * only band with no painter of its own.
 *
 * <p>{@code SkyTerrainPainter.describeRealmTile} used to answer a Hell tile
 * with Crooked Beyond's painter, and said why in its own javadoc: <i>"a band
 * that painted nothing would be a hole in the world at depth 0.94-1.00 …
 * delete this case the day it does."</i> This is that day. Everything else
 * about the hosting is unchanged: one level, realms as depth bands, a realm
 * answering only for its own tiles.
 *
 * <h2>The gradient IS the transition</h2>
 * §17 gives Hell its entry as a description rather than a door: <i>"A
 * transition. Still Crooked Beyond, with the first hell elements."</i> So this
 * painter has no gate object and no edge. It cuts two bands out of
 * {@link RealmDepth#localDepth} — how far through Hell's OWN band the tile is,
 * 0 at its inner foot (depth 0.80, where Crooked still has weight) and 1 at
 * the outer end of the world — and the INNER one is deliberately built out of
 * Crooked Beyond's own two grounds mixed with Hell's:
 *
 * <pre>
 * band             ground mix                                      biome
 * Infernal Fringe  violet mud + crooked stripe + black peat + ash   INFERNAL_FRINGE
 * Furnace Reach    black peat + ash sand + dead soil + mist stone   FURNACE_REACH
 * </pre>
 *
 * <p>Read down the mix and Crooked's colour drains out of it exactly the way
 * §17 asks. Within the Fringe the ratio itself moves: at the band's inner foot
 * about four tiles in five are still Crooked's, at its far edge about one in
 * five is. There is no line anywhere at which the ground changes — which is
 * the whole point, and is why the "Übergang" this realm needed is a field and
 * not an object.
 *
 * <h2>The warp, so it is not a bullseye</h2>
 * The band border is displaced by a low-frequency noise field, the same device
 * {@code SteinfeldTerrainPainter} uses on its three bands. Without it Hell
 * would be two concentric rings, which reads as a menu rather than as country.
 * With it black ground reaches inwards along some lines and Crooked's stripe
 * survives far out along others.
 *
 * <h2>Everything here is a pure function</h2>
 * Same contract as every other painter — seed and tile position only, no world
 * state — so region borders are seamless, the offline renderer and the live
 * world agree, and the pressure field and preset placers can ask the same
 * question the painter will answer later.
 *
 * <h2>No new art</h2>
 * Every ground and every prop below is a tile or object THIS MOD ALREADY
 * REGISTERS, reached by its existing {@code SkyRegistry} /
 * {@link CrookedRealm} field. Not one new sheet, not one recolour — which is
 * what makes this pass structure-and-behaviour only and leaves Hell's own
 * palette to the texture pass. {@code docs/VANILLA_ASSET_MAP.md} therefore
 * grows no row for the ground: nothing new is borrowed, it is re-used.
 */
public final class HellTerrainPainter {

    private HellTerrainPainter() {
    }

    // ---- the two bands -----------------------------------------------------

    /** §17's Infernal Fringe: Crooked Beyond with the first hell elements. */
    public static final int BAND_FRINGE = 0;
    /** A3.8's Furnace: black ground, machines, and the elites that outlive them. */
    public static final int BAND_FURNACE = 1;

    /**
     * Where the Fringe ends, as a fraction of Hell's own band.
     *
     * <p>0.45 rather than 0.5 so the Furnace is the larger half: the Fringe is
     * a transition and a transition should be shorter than the place it leads
     * to. Hell's band runs depth 0.80 to 1.00, i.e. 1200 tiles of the 6000-tile
     * plane, so the Fringe is about 540 tiles deep before the warp.
     */
    public static final float FRINGE_END = 0.45F;

    /** Scale and salt of the field that warps the band border. */
    public static final float BAND_SCALE = 260.0F;
    public static final long SALT_BAND = 0x4E11;
    /** How far the border wanders, as a fraction of the band. */
    public static final float BAND_WARP = 0.16F;

    /** Scale and salt of the field that mixes the grounds inside a band. */
    public static final float GROUND_SCALE = 46.0F;
    public static final long SALT_GROUND = 0x4E12;

    /** Salt of the sparse prop scatter. */
    public static final long SALT_PROP = 0x4E13;

    /**
     * How much of the open ground carries a prop.
     *
     * <p>Deliberately below every other realm's: A4.2 is explicit that
     * resources are scarce and A4.1 that the walk between places is empty. Hell
     * is the far end of the world and should feel swept, not decorated. Its
     * density comes from its four POIs and its guard packs, both of which are
     * placed rather than scattered.
     */
    public static final float PROP_CHANCE = 0.030F;

    // ---- the field ---------------------------------------------------------

    /**
     * How far through Hell's own band this tile is, warped: 0 at the inner
     * foot, 1 at the outer end.
     */
    public static float localDepth(int seed, int tileX, int tileY, float depth) {
        float local = RealmDepth.localDepth(RealmDepth.REALM_HELL, depth);
        float warp = SkyNoise.fbm(seed + SALT_BAND, tileX, tileY, BAND_SCALE, 2) - 0.5F;
        float warped = local + warp * 2.0F * BAND_WARP;
        return warped < 0.0F ? 0.0F : (warped > 1.0F ? 1.0F : warped);
    }

    /** Which of the two bands a warped local depth is in. */
    public static int bandFor(float localDepth) {
        return localDepth < FRINGE_END ? BAND_FRINGE : BAND_FURNACE;
    }

    /** Band -> the plane's biome class (see {@link SkyTerrainPainter}). */
    public static int biomeClassOf(int band) {
        return band == BAND_FRINGE
                ? SkyTerrainPainter.BIOME_HELL_FRINGE
                : SkyTerrainPainter.BIOME_HELL_FURNACE;
    }

    /**
     * The ground under one tile.
     *
     * <p>In the Fringe the mix is a RATIO that moves with local depth, which is
     * where §17's "still Crooked Beyond, with the first hell elements" actually
     * lives: {@code crookedShare} runs from 0.80 at the band's inner foot to
     * 0.20 at its far edge, and one noise field decides, per tile, which side
     * of that share the tile falls on. In the Furnace there is no Crooked left
     * to mix and the four dark grounds simply share the field.
     */
    public static int groundAt(int seed, int tileX, int tileY, int band, float localDepth) {
        float roll = SkyNoise.fbm(seed + SALT_GROUND, tileX, tileY, GROUND_SCALE, 3);
        if (band == BAND_FRINGE) {
            float crookedShare = 0.80F - 0.60F * (localDepth / FRINGE_END);
            if (roll < crookedShare) {
                // Crooked's own two grounds, in Crooked's own proportion.
                return roll < crookedShare * 0.55F
                        ? CrookedRealm.violetMudID
                        : CrookedRealm.crookedStripeID;
            }
            float hell = (roll - crookedShare) / (1.0F - crookedShare);
            return hell < 0.55F ? SkyRegistry.blackpeatID : SkyRegistry.ashsandID;
        }
        if (roll < 0.34F) {
            return SkyRegistry.blackpeatID;
        }
        if (roll < 0.64F) {
            return SkyRegistry.ashsandID;
        }
        return roll < 0.86F ? SkyRegistry.deadsoilID : SkyRegistry.miststoneID;
    }

    /**
     * The prop on one tile, or 0 for open ground.
     *
     * <p>All five are objects the mod already registers. The Fringe keeps
     * Crooked's teeth rock and the Veil's dead tree — the last things still
     * standing from the realm behind you — and the Furnace keeps only what
     * survives a furnace: bones, rock and the crate that makes a find a find.
     */
    public static int propAt(int seed, int tileX, int tileY, int band) {
        float roll = SkyNoise.hash(seed + (int) SALT_PROP, tileX, tileY);
        if (roll >= PROP_CHANCE) {
            return 0;
        }
        float pick = roll / PROP_CHANCE;
        if (band == BAND_FRINGE) {
            if (pick < 0.34F) {
                return CrookedRealm.teethRockID;
            }
            if (pick < 0.62F) {
                return SkyRegistry.deadtreeID;
            }
            if (pick < 0.86F) {
                return SkyRegistry.ashbonesID;
            }
            return CrookedRealm.crookedCrateID;
        }
        if (pick < 0.40F) {
            return SkyRegistry.ashbonesID;
        }
        if (pick < 0.70F) {
            return SkyRegistry.veilrockID;
        }
        if (pick < 0.88F) {
            return SkyRegistry.deadtreeID;
        }
        return CrookedRealm.crookedCrateID;
    }

    /**
     * One Hell tile: ground, object and sub-biome, as a pure function of the
     * seed and the position.
     *
     * <p>The shape is {@code SteinfeldTerrainPainter.describeBand}'s, unchanged
     * — sea, then walkable coastal rim, then open country — because that shape
     * is what keeps every realm's coastline consistent with the shared island
     * field the plane paints with.
     */
    public static long describeBand(int seed, int tileX, int tileY,
            float island, float waterline, float depth, float distortion) {
        float local = localDepth(seed, tileX, tileY, depth);
        int band = bandFor(local);
        int biomeClass = biomeClassOf(band);

        if (island <= waterline) {
            return SkyTerrainPainter.pack(SkyRegistry.mistseaID, 0, biomeClass, false);
        }

        int tileID = groundAt(seed, tileX, tileY, band, local);

        if (island <= waterline + SkyTerrainPainter.ISLAND_RIM) {
            // Walkable coast: ground, no prop, so a shoreline stays walkable.
            return SkyTerrainPainter.pack(tileID, 0, biomeClass, false);
        }

        return SkyTerrainPainter.pack(tileID, propAt(seed, tileX, tileY, band), biomeClass, false);
    }
}
