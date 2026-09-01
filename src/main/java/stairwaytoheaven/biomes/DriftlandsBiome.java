package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * Driftlands — the common Skyreach biome: silver-green isles, soft wind, home
 * of the Zephyr Ray.
 */
public class DriftlandsBiome extends SkyBiome {

    // WHY THE RAY CAME DOWN AND THE GOLEM CAME IN (v0.9, player report:
    // "zu viele rochen, zu wenig golems"). Read straight off these tables, the
    // Driftlands' DAYTIME LAND roster was the Zephyr Ray and nothing else:
    // the Galehound is darkness-only and the Mistserpent is IN_MISTSEA, so at
    // weight 80 out of 80 the ray was 100% of what a player met in daylight in
    // the mod's COMMON biome. And the Skystone Golem was absent from it
    // entirely — weight 0 here against 25 in the Stormveil and 55 in both the
    // Aurora Shoals and the Skyway. The rarest biomes had the bruiser and the
    // one everybody walks through had the flier, which is exactly the report.
    // Daytime land share, before -> after: ray 100% -> 50%, golem 0% -> 50%.
    // That split is still what these weights say, and it stays.
    //
    // The golem belongs here on the ground it is made of: `isRockPatch` lays
    // bare skystone scree across 14.7% of Driftlands land (measured over
    // 235,528 tiles, see TECHNICAL_LEARNINGS), so a skystone bruiser wandering
    // the barrens between the meadows is where it should have been all along.
    //
    // WHAT THE ENDGAME PASS CHANGED: the caps, not the shares. Every radius
    // here used to be 80 or 96 — 2.5 and 3 tiles, not the eight tiles the old
    // comment claimed (see SkyBiome: addLimited counts in PIXELS). A cap that
    // only looks 2.5 tiles around the spawn tile cannot hold a species down
    // over the ground a player walks, and it did not need to when a ray had
    // 220 HP. With the Skyreach's floor moved to a tier-1 incursion — and the
    // ray one of the weakest things on it — the same three rays are a wall, so
    // the caps are now written over a radius that means something.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            // Standard. Three within eight tiles: the engine allows four
            // hostiles there, so the ray can be three of them and never the
            // fourth. (Four would be dead weight — see SkyBiome.)
            .addLimited(40, "zephyrray", 3, RANGE_STANDARD)
            // Elite. Two within sixteen tiles: two golems at the new floor is
            // the fight this ground is for and a third is a blockade. The old
            // "2 within 3 tiles" let a walk across one rock patch meet far
            // more than two.
            .addLimited(40, "skystonegolem", 2, RANGE_ELITE)
            // Fast. v0.4 night pack hunter of the meadows (darkness-only spawn
            // rules keep torch-lit ground safe as always). A pack is three and
            // stays three — now measured over eight tiles, so it is ONE pack
            // rather than a pack every 2.5 tiles.
            .addLimited(45, "galehound", 3, RANGE_STANDARD)
            // The cloud sea between the islands is not empty travelling ground
            // — but it was the one piece of it with no cap at all, on the mod's
            // heaviest mob. VERIFIED [jar] each of the serpent's fourteen
            // segments is a hostile of its own (HostileWormMobBody sets
            // isHostile = true), so one serpent already spends the engine's
            // four-hostiles-per-eight-tiles budget wherever it swims, and two
            // surfacing together is not a harder fight, it is an impassable
            // sea. One at a time now, the way vanilla caps its own giant worms
            // (see SkyBiome.mistseaSerpent). The weight is untouched: the cap
            // is the right lever, tickets never were.
            .add(28, mistseaSerpent(1), "mistserpent");

    // NOTE: no "cloudlamb" here, deliberately. A sheep cannot be placed by a
    // spawn table at all -- MobChance.spawnMob calls isValidSpawnLocation and
    // nothing in SheepMob -> HusbandryMob -> FriendlyRopableMob -> AttackAnimMob
    // overrides Mob's `return false`. This table asked for one for three
    // releases and silently got nothing. Vanilla has the same constraint and
    // places its sheep, rams, cows and bulls from the island generator instead;
    // ours are placed in SkyLevel.placeCloudLambFlock at region generation.
    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(70, "zephyrfinch", 4, RANGE_STANDARD);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }

    /**
     * The common biome pays in common goods -- the floor of the loot curve.
     *
     * The base table in {@link SkyBiome} is the common cargo; this adds what
     * only this biome gives, so a crate tells the player where they are. The
     * amounts carry the same tier-1 incursion rate as the base table, by the
     * rule stated there.
     */
    @Override
    public LootTable getCrateLootTable(necesse.level.maps.Level level, int tileX, int tileY) {
        return new LootTable(
                // 2-6 -> 2-7 (expected 4.0 -> 4.5, +13%).
                ChanceLootItem.between(0.50F, "windwheat", 2, 7),
                // 3-8 -> 4-9: the max alone was only +9%, so the floor moved
                // with it (expected 5.5 -> 6.5, +18%).
                ChanceLootItem.between(0.30F, "nimbuswood", 4, 9),
                super.getCrateLootTable(level, tileX, tileY)
        );
    }

    /**
     * The Driftlands' guard: a Skystone Golem with a pack of hounds.
     *
     * The golem is the reason a wreck out here is a fight rather than a
     * pickup, and the hounds are the reason you cannot walk around it.
     */
    @Override
    public Guard getGuard() {
        return new Guard(new String[]{"skystonegolem"},
                new String[]{"galehound", "galehound", "zephyrray"}, 4, 6);
    }
}
