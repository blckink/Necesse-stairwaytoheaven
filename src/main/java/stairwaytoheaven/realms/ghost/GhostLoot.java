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

    /** The elite: more of everything, and the only mob-source of spectral ore. */
    public static LootTable elite() {
        return new LootTable(
                LootItem.between("ectoplasm", 4, 7),
                new ChanceLootItemList(0.65F, LootItem.between("soulthread", 3, 6)),
                new ChanceLootItemList(0.40F, LootItem.between("spectralore", 2, 4)),
                new ChanceLootItemList(0.20F, LootItem.between("spiritsteelbar", 1, 1)));
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
