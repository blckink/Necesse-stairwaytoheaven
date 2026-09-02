package stairwaytoheaven.realms.steinfeld;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.biomes.GuardedBiome;

/**
 * Shared base of Steinfeld's three bands. Painted per tile into the level's
 * biome layer like the Skyreach's and the Veil's sub-biomes; none of them ever
 * generates as a surface island, so none carries a generation weight.
 *
 * <h2>Where this layer sits on the ladder</h2>
 * Steinfeld is <b>incursion tier 5</b>: 2100 HP / 200 damage / 50 armour, drop
 * value x1.6 ({@code docs/BALANCE.md} §5). That number is not a taste call —
 * <b>VERIFIED [jar]</b> {@code BiomeMissionIncursionData} sums two per-tier
 * arrays, {@code healthScalingPerTier} and {@code damageScalingPerTier}, and
 * summed to tier 5 they are +1.12 health and +0.54 damage, i.e. x2.12 and
 * x1.54 on the Skyreach floor of 1000 HP / 130 damage
 * ({@code AscendedGolemMob.MAX_HEALTH} on Classic and
 * {@code CrystalGolemMob.damage}). 1000 x 2.12 = 2120 and 130 x 1.54 = 200.2,
 * which the ladder reads as 2100 / 200. The stat lines themselves live in the
 * four mob classes; the tables here only decide how many of each stand on a
 * piece of ground.
 *
 * <h2>What a spawn table can and cannot do here</h2>
 * The engine facts are written out once in {@code stairwaytoheaven.biomes
 * .SkyBiome} and all of them apply unchanged: {@code addLimited}'s searchRange
 * is in PIXELS not tiles, the engine already caps total pressure at four
 * hostiles within eight tiles, and a cap that binds hands its share to the rest
 * of the table rather than leaving the ground empty. Two additions specific to
 * this realm:
 *
 * <ul>
 * <li><b>Every entry is capped, and the caps are the mix control.</b> The
 *     realm's four hostiles are one of each archetype — standard, elite,
 *     ranged, fast — so the caps say what a fight is made of rather than how
 *     many mobs exist.</li>
 * <li><b>{@code MobSpawnTable.getRandomMob} filters by the entry predicate
 *     FIRST and draws by weight afterwards</b> (MobSpawnTable.java:131-138).
 *     An entry whose predicate cannot fail therefore stays in every draw and
 *     can only be rejected later, by {@code isValidSpawnLocation} — which is
 *     how this mod lost its Mistserpent for three releases. Steinfeld has no
 *     liquid and no terrain-restricted resident, so every entry here is
 *     legitimately terrain-free; the caps are the only predicates and they are
 *     real ones.</li>
 * </ul>
 *
 * <h2>Where the pressure is</h2>
 * None of this decides WHERE a hostile may appear. That is
 * {@link SteinfeldPressure}, which returns 0 tickets across most of the open
 * Reach — the walk between places is silent — and 600 on the ground a POI
 * stands on. A4.1's instruction is explicit that a spawn table cannot answer
 * it, and this file does not try to.
 */
public abstract class SteinfeldBiome extends Biome implements GuardedBiome {

    /** Standard and fast hostiles: eight tiles, in pixels, as addLimited counts. */
    public static final int RANGE_STANDARD = 8 * 32;
    /** Ranged hostiles: twelve tiles, nearer the distance they fight at. */
    public static final int RANGE_RANGED = 12 * 32;
    /** Elites: sixteen tiles, so a pair is a pair across a stretch of ground. */
    public static final int RANGE_ELITE = 16 * 32;

    @Override
    public boolean canRain(Level level) {
        // The Reach has fog, not weather. Rain over a dead heath would read as
        // life; the fog is a light/ambient matter and belongs to the level.
        return false;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        // Nothing lives here that is not already dead. Deliberately empty
        // rather than borrowed: an ambient bird would undo the whole realm.
        return new MobSpawnTable();
    }

    /**
     * Ambient spawn rate, on the same policy as the sky and the Veil.
     *
     * See {@code SkyBiome.getSpawnRateMod} for the vanilla precedent
     * ({@code SettlementRuinsBiome} 0.3/0.5, {@code TempleBiome} 0.75/0.75).
     * Steinfeld keeps the mod's shared 0.55/0.75 rather than a harsher pair:
     * A4.1 is about WHERE the pressure is, not how much of it there is, and
     * Steinfeld's answer to "how much" is its roster.
     */
    @Override
    public float getSpawnRateMod(Level level) {
        return super.getSpawnRateMod(level) * 0.55F;
    }

    @Override
    public float getSpawnCapMod(Level level) {
        return super.getSpawnCapMod(level) * 0.75F;
    }

    /**
     * What a container in the Reach holds.
     *
     * <p>This table is doing more work than it looks. Steinfeld's POIs place
     * the mod's own salvage crate, and — <b>VERIFIED [jar]</b> —
     * {@code GravestoneObject.getLootTable} (GravestoneObject.java:47) returns
     * {@code level.getCrateLootTable(tileX, tileY)} for any stone the player
     * did not place. So every gravestone in a grave field is a container that
     * answers here, and breaking one open is the reason to walk into a walled
     * plot full of guards.
     *
     * <p><b>Quantities carry the realm's drop value of x1.6</b>
     * ({@code docs/BALANCE.md} §5). The Skyreach crate is the baseline this
     * multiplies against: skystone 4-10 there, pale stone 6-16 here
     * (expected 7.0 -> 11.0, x1.57). Chances are unchanged — the rung is paid
     * in stack size, so a rare drop stays rare.
     *
     * <p>Each band adds what only IT gives on top of this, so opening a
     * container tells the player how far out they are.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("palestone", 6, 16),
                ChanceLootItem.between(0.45F, "gravesalt", 2, 5),
                ChanceLootItem.between(0.30F, "spiritmoss", 1, 4),
                ChanceLootItem.between(0.14F, "echoshard", 1, 2),
                // Bone belongs to a graveyard and the mod already has a use
                // for it: the Veil's Cinder Cantor drops it and vanilla's
                // whole bone furniture family is built from it.
                ChanceLootItem.between(0.25F, "bone", 2, 6));
    }
}
