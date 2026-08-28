package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Gloomfen — the Veil's moonlit marsh: black crooked trees, whisper reeds and
 * pale shroom-light. Home of the Gloom Shade.
 */
public class GloomfenBiome extends VeilBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(100, "gloomshade", 4, 80)
            // --- content/arsenal ---
            // The fen's own dead: slow, armoured, and they leave burning
            // pools behind them, so the marsh stops being safe to stand in.
            .addLimited(55, "fenwraith", 3, 80)
            // A cantor strays out of the ash to sing over the water.
            .addLimited(20, "cindercantor", 2, 96);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }
}
