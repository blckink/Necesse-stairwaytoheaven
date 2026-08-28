package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Ashen Reach — the Veil's grey dune waste, home of the future Ashwyrm. For
 * now only stray shades wander in from the fen.
 */
public class AshenReachBiome extends VeilBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(100, "gloomshade", 2, 80)
            // --- content/arsenal ---
            // The Ashen Reach's first real inhabitant - until now only stray
            // shades wandered in from the fen. Teleports when its own bolts
            // land on you, so two at a time is already a fight.
            .addLimited(70, "cindercantor", 2, 96)
            // Wraiths wander up out of the fen onto the dunes.
            .addLimited(30, "fenwraith", 2, 80);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }
}
