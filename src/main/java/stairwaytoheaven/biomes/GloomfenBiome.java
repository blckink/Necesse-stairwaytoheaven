package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Gloomfen — the Veil's moonlit marsh: black crooked trees, whisper reeds and
 * pale shroom-light. Home of the Gloom Shade.
 */
public class GloomfenBiome extends VeilBiome {

    // The fen is the Veil's common ground and carries the layer's full mix:
    // standard, elite and ranged. There is no FAST archetype in it because the
    // Veil has none to give — the layer's whole hostile roster is the Gloom
    // Shade, the Fen Wraith and the Cinder Cantor, and none of them is built
    // to run you down. That is a roster gap, not a table gap; it cannot be
    // fixed by weighting.
    //
    // The caps below moved with the endgame pass. They used to be written in
    // what looked like tiles and is not: addLimited counts in PIXELS (see
    // SkyBiome), so "4, 80" was four shades within two and a half tiles, which
    // is a cap that essentially never bound. At the Veil's rung a shade is a
    // tier-7 incursion mob, and the ground has to be readable at that weight.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard. Weight 100 -> 90 and cap 4 -> 3 within eight tiles.
            // Four could never have been the thing that blocked a spawn: the
            // engine refuses one outright once four hostiles are already
            // inside those same eight tiles (SkyBiome), so a same-kind count
            // of four is never reached. Three leaves room for the fen's other
            // two to show up at all.
            .addLimited(90, "gloomshade", 3, SkyBiome.RANGE_STANDARD)
            // --- content/arsenal ---
            // Elite. The fen's own dead: slow, armoured, and they leave
            // burning pools behind them, so the marsh stops being safe to
            // stand in. 3 -> 2, measured over the elite radius of sixteen
            // tiles: two sets of pools is ground you have to leave, three is
            // ground you cannot cross.
            .addLimited(55, "fenwraith", 2, SkyBiome.RANGE_ELITE)
            // Ranged. A cantor strays out of the ash to sing over the water.
            // Weight 20 -> 30: 20 of the old table's 175 tickets is one spawn
            // in nine, so the fen answered almost nothing at range, and a
            // realm whose only ranged threat is a stray is a realm you fight
            // one way. 30 of 175 is one in six — still a visitor, now a
            // visitor you plan for.
            .addLimited(30, "cindercantor", 2, SkyBiome.RANGE_RANGED);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * The fen's guard: shades around a wraith.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"fenwraith"},
                new String[]{"gloomshade", "gloomshade", "cindercantor"}, 4, 6);
    }
}
