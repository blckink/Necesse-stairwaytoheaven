package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Ashen Reach — the Veil's grey dune waste, home of the future Ashwyrm and
 * present ground of the Cinder Cantor.
 */
public class AshenReachBiome extends VeilBiome {

    // The dunes are the layer's RANGED ground: open sightlines, nothing to
    // break line of fire, and a caster that teleports out of melee the moment
    // its own bolts land on you. The old weights did not say that — the stray
    // shade led the table 100 to 70 in the one biome that is supposed to
    // belong to something else.
    //
    // Caps use the package's named radii now for the reason given in
    // SkyBiome: addLimited's searchRange is pixels, so the old 80/96 were 2.5
    // and 3 tiles and never bound on ground a player crosses.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard, and still only a stray: "only stray shades wander in
            // from the fen" was the biome's line and the numbers now agree
            // with it. Weight 100 -> 55, which is 34% of the table instead of
            // half of it; cap 2 over eight tiles.
            .addLimited(55, "gloomshade", 2, SkyBiome.RANGE_STANDARD)
            // --- content/arsenal ---
            // Ranged, and now the plurality of the table at 44%: the Ashen
            // Reach's first real inhabitant, and the reason to come here. Its
            // own weight is untouched — the dunes became its ground by the
            // stray shade stepping back, not by the cantor being inflated. It
            // teleports when its own bolts land on you, so two at a time is
            // already a fight; the pair is now measured over twelve tiles
            // instead of three, which is the distance it actually fights at.
            .addLimited(70, "cindercantor", 2, SkyBiome.RANGE_RANGED)
            // Elite. Wraiths wander up out of the fen onto the dunes. Weight
            // 30 -> 35: with the shade down to 55 the wraith is what stops the
            // dunes being a pure ranged duel, and open ground is exactly where
            // a slow armoured thing that leaves fire behind it is survivable.
            .addLimited(35, "fenwraith", 2, SkyBiome.RANGE_ELITE);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }
}
