package stairwaytoheaven.realms.hell;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.MobSpawnTable;
import stairwaytoheaven.biomes.GuardedBiome;

/**
 * Shared base of Hell's two bands. Painted per tile into the level's biome
 * layer like every other realm's sub-biomes; neither ever generates as a
 * surface island, so neither carries a generation weight.
 *
 * <h2>Where this layer sits on the ladder</h2>
 * Hell is the rung <b>past</b> the Crooked Beyond, i.e. past the end of
 * vanilla's own incursion arrays: 4450 HP / 285 damage / 65 armour before the
 * §10 uplift, 7342 / 387.6 / 81 after it, drop value x2.75. The whole
 * derivation, and why one step past tier 10 is x4.45 health and only x2.19
 * damage, is written out once in
 * {@link stairwaytoheaven.realms.hell.mobs.HellTier}. The stat lines
 * themselves live in the four mob classes; the tables here only decide how
 * many of each stand on a piece of ground.
 *
 * <h2>What a spawn table can and cannot do here</h2>
 * The engine facts are written out once in {@code stairwaytoheaven.biomes
 * .SkyBiome} and all of them apply unchanged: {@code addLimited}'s searchRange
 * is in PIXELS not tiles, the engine already caps total pressure at four
 * hostiles within eight tiles, and a cap that binds hands its share to the
 * rest of the table rather than leaving the ground empty.
 *
 * <p>Every entry below is capped, and the caps are the mix control: Hell's
 * four hostiles are one of each archetype — standard, elite, ranged, fast —
 * so the caps say what a fight is MADE OF rather than how many mobs exist.
 * None of the four is terrain-restricted, so no entry carries a predicate that
 * cannot fail — the trap {@code SteinfeldBiome}'s header records this mod
 * falling into once.
 *
 * <h2>Where the pressure is</h2>
 * <b>None of this decides WHERE a hostile may appear.</b> A4.1's instruction —
 * <i>"sie sollen mal geballt kommen und ein Gebiet z.b bewachen"</i> — is
 * answered by {@link GuardedBiome}: a pack is PLACED at generation on the
 * ground around the site it guards.
 *
 * <p><b>Hell has no site lattice of its own yet, so these guards are declared
 * and not yet placed.</b> Saying so rather than implying otherwise: Crooked
 * Beyond's three lattices used to reach into this band, and while Hell painted
 * as Crooked that worked, because Crooked's painter stamped a house at each of
 * those sites. It does not stamp anything on Hell ground any more, so
 * {@code SkyLevel.placePacksOf} no longer walks Crooked's lattices in here — a
 * pack on a site with no house is the "guards standing where the loot is not"
 * bug that gate exists to prevent. {@code getGuard} is implemented so that
 * Hell's own lattice, when it lands, has a roster to ask for; until then Hell's
 * pressure is its ambient table above and its four {@code RealmPoiPresets}
 * buildings.
 */
public abstract class HellBiome extends Biome implements GuardedBiome {

    /** Standard and fast hostiles: eight tiles, in pixels, as addLimited counts. */
    public static final int RANGE_STANDARD = 8 * 32;
    /** Ranged hostiles: twelve tiles, nearer the distance they fight at. */
    public static final int RANGE_RANGED = 12 * 32;
    /** Elites: sixteen tiles, so a pair is a pair across a stretch of ground. */
    public static final int RANGE_ELITE = 16 * 32;

    @Override
    public boolean canRain(Level level) {
        // Nothing falls on Hell. Rain here would read as relief.
        return false;
    }

    @Override
    public MobSpawnTable getCritterSpawnTable(Level level) {
        // §22's Ember Pig, Fire Hen and Lava Snail are husbandry and critter
        // content, not ambient scatter, and they are not built. Deliberately
        // empty rather than borrowed: a sky bird out here would undo the band.
        return new MobSpawnTable();
    }

    /**
     * Ambient spawn rate, on the same policy as every other band.
     *
     * <p>See {@code SkyBiome.getSpawnRateMod} for the vanilla precedent
     * ({@code SettlementRuinsBiome} 0.3/0.5, {@code TempleBiome} 0.75/0.75).
     * Hell keeps the mod's shared 0.55/0.75 rather than a harsher pair, for
     * the reason Steinfeld gives: A4.1 is about WHERE the pressure is, not how
     * much of it there is, and Hell's answer to "how much" is its roster.
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
     * What a container out here holds.
     *
     * <p><b>Quantities carry the realm's drop value of x2.75</b>, the step
     * past Crooked Beyond's x2.50. Chances are unchanged — a rung is paid in
     * stack size, so a rare drop stays rare.
     *
     * <p>The materials are Crooked Beyond's own. §20 gives Hell six of its own
     * — Hellsteel Ore, Brimstone, Infernal Brass, Demon Hide, Hellglass,
     * Furnace Heart — and <b>none of them is built</b>; inventing an item
     * family in this pass would mean six icons, six locale pairs and a
     * crafting economy, which is its own piece of work and is recorded as such
     * in {@code docs/OVERVIEW.md}. Until then §17's own framing carries it:
     * the Infernal Fringe is <i>"still Crooked Beyond, with the first hell
     * elements"</i>, so Crooked's currency is what is lying about out here,
     * in Hell's quantities.
     */
    @Override
    public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
        return new LootTable(
                LootItem.between("oddwood", 8, 22),
                ChanceLootItem.between(0.45F, "realityshard", 3, 7),
                ChanceLootItem.between(0.30F, "warpresin", 2, 5),
                ChanceLootItem.between(0.25F, "charwood", 3, 8),
                ChanceLootItem.between(0.14F, "spiritsteelbar", 1, 2));
    }
}
