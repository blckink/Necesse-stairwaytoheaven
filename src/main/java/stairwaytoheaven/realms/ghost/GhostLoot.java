package stairwaytoheaven.realms.ghost;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;

/**
 * What the Aftergarden's dead leave behind.
 *
 * <h2>How the numbers are set</h2>
 * {@code docs/BALANCE.md} §5 gives the Ghost Realm a <b>drop value of x1.9</b>
 * against the Skyreach floor. The Skyreach's own baseline is 1-2 of a material
 * per drop, so 1.9 of that, rounded to whole items, is <b>2-4</b> — and that is
 * where the multiplier is spent. The CHANCES are left alone on purpose: paying
 * the rung in stack size means a kill still sometimes drops nothing, so the
 * drop stays something you earned rather than something you were issued. That
 * is the same choice {@code FenWraithMob} made when it was tuned to this exact
 * rung, and keeping it consistent is the point.
 *
 * <p><b>Ectoplasm is vanilla's own item</b>, not one of ours. It already exists
 * ({@code ItemRegistry}: {@code ectoplasm}, brokerValue 12.0F, and it is the
 * mid-value line of the first incursion's own biome loot), the game already
 * ships its icon and both of its names, and it is already what vanilla's
 * deep-cave spirits drop. Registering a second, differently-named ectoplasm
 * would have been a worse item in every respect. It is therefore the realm's
 * universal ghost resource exactly as {@code WORLD_DESIGN} §10 asks, and the
 * Soul Basin's cost is denominated in it.
 */
public final class GhostLoot {

    private GhostLoot() {
    }

    /**
     * The standard drop: ectoplasm every time, and one of the realm's worked
     * materials sometimes.
     */
    public static LootTable standard() {
        return new LootTable(
                LootItem.between("ectoplasm", 2, 4),
                new ChanceLootItemList(0.45F, LootItem.between("soulthread", 2, 4)),
                new ChanceLootItemList(0.25F, LootItem.between("bonewood", 2, 4)));
    }

    /** The skeletal roster: bone as well, since they are visibly made of it. */
    public static LootTable bony() {
        return new LootTable(
                LootItem.between("ectoplasm", 2, 4),
                LootItem.between("bone", 2, 6),
                new ChanceLootItemList(0.40F, LootItem.between("bonewood", 2, 4)));
    }

    /**
     * The elite: more of everything, the only mob-source of spectral ore, and
     * the only mob-source of the realm's two weapons.
     *
     * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} A3.3: <b>"The same ghost weapons
     * also drop randomly in the Ghost region, so a player who never trades
     * still finds them."</b> This is that drop, and it is on the ELITE table
     * rather than on {@link #standard()} for the reason the class comment above
     * already gives for stack sizes: a weapon that every drifter hands out is
     * something you were issued, not something you earned.
     *
     * <p><b>4%.</b> Vanilla's own reference for a weapon on a mob table is the
     * incursion boss chest, not the trash mob, so there is no equal-tier chance
     * to copy; the number is instead read off the two rungs this table already
     * uses. Spectral ore — the rarest MATERIAL here — is 40%, and the finished
     * bar is 20%; a finished weapon is worth about eight bars (see
     * {@code GhostGuideMob}'s price list), so it sits an order of magnitude
     * below the bar. At 4% each, an elite is a 1-in-13 chance of some weapon,
     * which is a run of the Aftergarden rather than a grind.
     */
    public static LootTable elite() {
        return new LootTable(
                LootItem.between("ectoplasm", 4, 7),
                new ChanceLootItemList(0.65F, LootItem.between("soulthread", 3, 6)),
                new ChanceLootItemList(0.40F, LootItem.between("spectralore", 2, 4)),
                new ChanceLootItemList(0.20F, LootItem.between("spiritsteelbar", 1, 1)),
                new ChanceLootItemList(0.04F, LootItem.between("spiritsteelreaver", 1, 1)),
                new ChanceLootItemList(0.04F, LootItem.between("gravewindbow", 1, 1)));
    }

    /** The ambushers: they were sitting on something, so they drop it. */
    public static LootTable ambusher() {
        return new LootTable(
                LootItem.between("ectoplasm", 2, 4),
                new ChanceLootItemList(0.50F, LootItem.between("bonewood", 3, 6)),
                new ChanceLootItemList(0.35F, LootItem.between("spectralore", 1, 3)),
                new ChanceLootItemList(0.20F, LootItem.between("coin", 30, 90)));
    }
}
