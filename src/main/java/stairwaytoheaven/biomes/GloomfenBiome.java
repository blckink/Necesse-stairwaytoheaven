package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Gloomfen — the Veil's moonlit marsh: black crooked trees, whisper reeds and
 * pale shroom-light. Home of the Gloom Shade.
 */
public class GloomfenBiome extends VeilBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(100, "gloomshade", 4, 80);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }
}
