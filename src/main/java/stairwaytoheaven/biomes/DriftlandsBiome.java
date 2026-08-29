package stairwaytoheaven.biomes;

import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobSpawnTable;

/**
 * Driftlands — the common Skyreach biome: silver-green isles, soft wind, home
 * of the Zephyr Ray.
 */
public class DriftlandsBiome extends SkyBiome {

    // addLimited caps local pressure: no more than N of a kind near the
    // spawn point, so a cleared, lit area STAYS calm.
    // WHY THE RAY CAME DOWN AND THE GOLEM CAME IN (v0.9, player report:
    // "zu viele rochen, zu wenig golems"). Read straight off these tables, the
    // Driftlands' DAYTIME LAND roster was the Zephyr Ray and nothing else:
    // the Galehound is darkness-only and the Mistserpent is IN_MISTSEA, so at
    // weight 80 out of 80 the ray was 100% of what a player met in daylight in
    // the mod's COMMON biome. And the Skystone Golem was absent from it
    // entirely — weight 0 here against 25 in the Stormveil and 55 in both the
    // Aurora Shoals and the Skyway. The rarest biomes had the bruiser and the
    // one everybody walks through had the flier, which is exactly the report.
    //
    // Daytime land share, before -> after: ray 100% -> 50%, golem 0% -> 50%.
    // The ray's local cap drops 3 -> 2 as well, because the cap is what a
    // player actually feels: three of one flier in an eight-tile radius is the
    // "zu viele" even when the weight is fair.
    //
    // The golem belongs here on the ground it is made of: `isRockPatch` lays
    // bare skystone scree across 14.7% of Driftlands land (measured over
    // 235,528 tiles, see TECHNICAL_LEARNINGS), so a skystone bruiser wandering
    // the barrens between the meadows is where it should have been all along.
    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(40, "zephyrray", 2, 80)
            .addLimited(40, "skystonegolem", 2, 96)
            // v0.4: night pack hunter of the meadows (darkness-only spawn
            // rules keep torch-lit ground safe as always)
            .addLimited(45, "galehound", 3, 80)
            // The cloud sea between the islands is not empty travelling ground
            .add(28, stairwaytoheaven.mobs.MistserpentHead.IN_MISTSEA, "mistserpent");

    // NOTE: no "cloudlamb" here, deliberately. A sheep cannot be placed by a
    // spawn table at all -- MobChance.spawnMob calls isValidSpawnLocation and
    // nothing in SheepMob -> HusbandryMob -> FriendlyRopableMob -> AttackAnimMob
    // overrides Mob's `return false`. This table asked for one for three
    // releases and silently got nothing. Vanilla has the same constraint and
    // places its sheep, rams, cows and bulls from the island generator instead;
    // ours are placed in SkyLevel.placeCloudLambFlock at region generation.
    public static final MobSpawnTable critters = new MobSpawnTable()
            .addLimited(70, "zephyrfinch", 4, 60)
            // ...and the sky's dairy herd, which CAN be table-spawned because
            // NimbusYakMob implements isValidSpawnLocation itself (see
            // livestock/SkyBreed). The cap is deliberately tight and the range
            // wide: a herd, not a field of yaks, and the animals are permanent
            // (HusbandryMob never sets canDespawn), so the count that
            // addLimited measures is the count that stays.
            .addLimited(35, stairwaytoheaven.livestock.SkyLivestock.NIMBUS_YAK, 4, 90);

    @Override
    public MobSpawnTable getMobSpawnTable(Level level) {
        return mobs;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        return critters;
    }
}
