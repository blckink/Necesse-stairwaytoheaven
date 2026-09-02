package stairwaytoheaven.realms.eden;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Eden Garden — the common zone: deep green ground, absurd fruit, and snakes in
 * the tall grass.
 *
 * <p>A3.3, in one line: <i>"beauty can be dangerous"</i>. The garden's roster
 * is the two things that hide in vegetation — the Eden Serpent and the Bloom
 * Maw — plus the hornet that does not hide at all.
 */
public class EdenGardenBiome extends EdenBiome {

    /**
     * <b>Weights, and what they say a walk looks like.</b> The serpent is the
     * realm's standard enemy and takes half the table; the Bloom Maw is
     * stationary, so it reads as terrain you walked into rather than as a
     * chaser and can afford a real share; the hornet is the fast one and is
     * the smallest slice because it is the one that interrupts.
     *
     * <p>Every entry carries {@link #ON_LAND}. That is not belt and braces: the
     * garden borders the lagoons everywhere, and an entry with no terrain
     * predicate stays in every draw and fails at placement
     * (MobSpawnTable.java:131-138) — the bug that cost this mod its
     * Mistserpent.
     */
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard. Three within eight tiles: the engine allows four
            // hostiles there, so the serpent can be three of them and never
            // the fourth (see SkyBiome for why four would be dead weight).
            .add(50, onLandLimited("edenserpent", 3, RANGE_STANDARD), "edenserpent")
            // Standard, stationary. Two, because a third simply walls a path.
            .add(30, onLandLimited("bloommaw", 2, RANGE_STANDARD), "bloommaw")
            // Fast. Two within eight tiles; a swarm of golden hornets is
            // exactly the "angeflogen kommen" the A4.1 pass removed.
            .add(20, onLandLimited("goldenhornet", 2, RANGE_STANDARD), "goldenhornet");

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    /** The garden pays in what grows in it. */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                ChanceLootItem.between(0.50F, "paradiseapple", 2, 6),
                ChanceLootItem.between(0.35F, "moonmelon", 2, 5),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }

    /**
     * The garden's guard: a Bloom Maw rooted over the loot with serpents around
     * it.
     *
     * <p>The Maw cannot follow you, which is the point — it is the reason the
     * cache is a fight rather than a pickup, and the serpents are the reason
     * you cannot simply walk around the Maw.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"bloommaw"},
                new String[]{"edenserpent", "edenserpent", "goldenhornet"}, 4, 6);
    }
}
