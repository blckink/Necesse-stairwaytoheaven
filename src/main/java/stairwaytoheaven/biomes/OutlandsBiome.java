package stairwaytoheaven.biomes;

import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import stairwaytoheaven.SkyRegistry;

/**
 * The Beetle Outlands — the sky's first wrong place, and the only sky biome you
 * cannot reach by walking a short way.
 *
 * Where the other four sub-biomes are weather and geology, this one is a
 * symptom. It is cut out of them by {@link stairwaytoheaven.worldgen.SkyOutlands},
 * whose patch threshold falls with distance from the spire, so it is impossible
 * near home and ordinary in the far reaches.
 *
 * It is deliberately built from what the Veil already owned: the striped
 * beetlefreak ground, blackpeat, dead trees, ash bones, gloom shrooms and the
 * Gloom Shade. That layer was behind a Seance Circle almost nobody opened; the
 * art was drawn, registered and unseen. Nothing new had to be made for the sky
 * to stop being uniformly bright.
 *
 * The densest hostile population in the sky, on purpose. Wrong ground the
 * player can stroll across is just a different colour of grass.
 */
public class OutlandsBiome extends SkyBiome {

    public static final MobSpawnTable mobs = new MobSpawnTable()
            // The Veil's own resident, finally somewhere a player will meet it.
            .addLimited(100, "gloomshade", 6, 60)
            // The fen's dead came with the ground they haunt.
            .addLimited(45, "fenwraith", 3, 80)
            .addLimited(25, "cindercantor", 2, 96);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /**
     * Bridging a channel out here reclaims the striped ground, not cloudturf.
     *
     * Same reasoning the Beetlefreak Hollows shipped with: the wrongness is the
     * point, and a player who bridges a gap inside an Outland should not be
     * handing themselves a patch of the nice world back.
     */
    @Override
    public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
        return SkyRegistry.beetlefreakTile;
    }

    /**
     * What an Outland crate holds: the Veil's materials, which have never had a
     * container to come out of because the Veil had no crates at all.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("veilessence", 2, 5),
                ChanceLootItem.between(0.40F, "gloomshroom", 1, 3),
                ChanceLootItem.between(0.25F, "charwood", 2, 6),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }
}
