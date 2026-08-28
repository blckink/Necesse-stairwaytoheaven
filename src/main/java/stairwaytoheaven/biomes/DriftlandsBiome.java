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
    public static final MobSpawnTable mobs = new MobSpawnTable()
            .addLimited(80, "zephyrray", 3, 80)
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
