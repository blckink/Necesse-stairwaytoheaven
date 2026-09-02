package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * Aurora Shoals — the rare Skyreach biome: shallow mist banks under cold dawn
 * light, rich in Aetherium. Guarded by Skystone Golems.
 */
public class AuroraShoalsBiome extends SkyBiome {

    // Rare but hard, and it stays the hardest ground in the Skyreach — but the
    // sentence "few golems, capped tight" was not true of the numbers under it:
    // three golems within 96 PIXELS (three tiles, see SkyBiome) is not a tight
    // cap, it is no cap at all over any ground a player crosses. The four
    // archetypes are all here — elite, standard, fast, ranged — and after the
    // endgame pass they are within reach of each other in weight, because they
    // are now within reach of each other in threat.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Elite. 3 -> 2, and over sixteen tiles rather than three. Two
            // golems at a tier-1 incursion's health and armour is the fight
            // this biome is for; three was the worst encounter in the mod.
            // Weight 55 -> 45 for the same reason: with the rest of the table
            // no longer trash, the golem does not need half the tickets to be
            // the thing the Shoals are remembered for.
            .addLimited(45, "skystonegolem", 2, RANGE_ELITE)
            // Standard.
            .addLimited(40, "zephyrray", 2, RANGE_STANDARD)
            // Fast. v0.4 glass-cannon dive bird — the counterpart to the
            // golem, and the pass raises it off the mod's lowest health, so
            // the pair is measured over eight tiles instead of 2.5.
            .addLimited(40, "dawnpiercer", 2, RANGE_STANDARD)
            // Ranged. Flying artillery over the mist banks: the shoals had
            // nothing that shoots back at range. Weight 40 -> 35 and the pair
            // now spans the ranged radius — two shooters over open mist is a
            // fight you have to close; a third one you cannot see is a fight
            // you lose without meeting it.
            .addLimited(35, "auroraflake", 2, RANGE_RANGED)
            // The cloud sea between the islands is not empty travelling ground.
            //
            // Weight 28 -> 300, and this is not a difficulty change: it is what
            // makes the serpent appear AT ALL. MobSpawnTable.getRandomMob
            // filters by the entry predicate FIRST and only then draws by
            // weight (VERIFIED [jar], MobSpawnTable.java:131-138). The serpent
            // carries IN_MISTSEA, so it can never be drawn on land whatever its
            // weight; the land entries carry no terrain predicate, so on a sea
            // tile they all stay in the draw and then fail the liquid check
            // afterwards. At 28 against a table of ~223-328 that meant roughly
            // nine draws in ten over the sea were spent on a mob that could not
            // stand there, which is why the sky's one roaming threat was never
            // seen. At 300 a sea draw lands on the serpent about half the time.
            // The cap of one per spawn ring is what keeps it occasional rather
            // than everywhere -- see DriftlandsBiome for why it is capped at all.
            .add(300, mistseaSerpent(1), "mistserpent");

    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(80, "glowmoth", 5, RANGE_STANDARD)
            .addLimited(60, "dewsnail", 3, RANGE_STANDARD);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }

    /**
     * The Shoals are the richest ground in the sky, and their crates say so.
     *
     * The base table in {@link SkyBiome} is the common cargo; this adds what
     * only this biome gives, so a crate tells the player where they are. The
     * amounts carry the same tier-1 incursion rate as the base table, by the
     * rule stated there.
     */
    @Override
    public LootTable getCrateLootTable(necesse.level.maps.Level level, int tileX, int tileY) {
        return new LootTable(
                // 2-5 -> 2-6 (expected 3.5 -> 4.0, +14%).
                LootItem.between("aurorapetal", 2, 6),
                // 1-4 -> 1-5 (expected 2.5 -> 3.0, +20%).
                ChanceLootItem.between(0.45F, "prismshard", 1, 5),
                // Unchanged: a third bar would be +33%, past the band, and the
                // bar is what makes this the richest crate in the sky exactly
                // because it does not come in handfuls.
                ChanceLootItem.between(0.15F, "aetheriumbar", 1, 2),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }

    /**
     * The Shoals' guard: the golem, a Dawnpiercer and Aurora Flakes at range.
     *
     * The rarest ground in the sky carries the widest pack, because a Shoal
     * wreck is the one a player travels TO rather than trips over.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"skystonegolem", "dawnpiercer"},
                new String[]{"auroraflake", "auroraflake", "zephyrray"}, 5, 7);
    }
}
