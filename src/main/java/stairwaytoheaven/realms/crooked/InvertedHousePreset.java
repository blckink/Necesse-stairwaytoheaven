package stairwaytoheaven.realms.crooked;

import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.DOWN;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.RIGHT;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * The Inside-Out House — a house whose rooms are on the outside.
 *
 * <p>{@code WORLD_DESIGN.md} A3.6 asks for a realm where <i>"the rules of the
 * world decay"</i> and lists <i>"windows lying on the floor"</i> as the kind of
 * image that sells it. This is that idea at building scale: a sealed block of
 * Beetlefreak masonry with a shut door and no way in stands in the middle, and
 * everything that ought to be inside it is laid out around it in the open, in
 * the shape of the rooms it should have had.
 *
 * <h2>The rooms, since 2026-09-24</h2>
 * Until then "the rooms" were four long chairs, a clock and a barrel on bare
 * planks. Now each is a room you can name from a distance, marked out on the
 * planks by its own rug the way a floor plan marks it: the <b>bedroom</b>
 * west (a bone bed and dresser on a violet rug), the <b>kitchen</b> east (the
 * pot, a counter with a cutting board, the pantry barrel and a sack, on a
 * patch of chequer tile), the <b>parlour</b> south (a dinner table with its
 * chairs round it and the two long chairs as the sofas, on a green rug, the
 * window lying in the corner of it), and -- in full view of all three -- the
 * <b>bathroom</b>: a bathtub and a toilet standing in the open. The door in the
 * masonry still opens onto more masonry; that is the joke, and it is the one
 * door in the mod that is meant to lead nowhere.
 *
 * <h2>Sealing</h2>
 * The masonry block is SOLID: every cell of it is a wall, so there is no
 * interior cell to break into and find empty. The rooms outside it are floor,
 * not interior, and are meant to be walked on.
 */
public class InvertedHousePreset extends Preset {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 13;

    /** The house, drawn one character per tile; ' ' writes nothing. */
    public static final String[] PLAN = {
            "  ,,,,,,,,,,,  ",
            " ,E:::L,,,okk, ",
            " ,e:::,,C,,,=, ",
            " ,R:::#####==l ",
            " ,::::#####==s ",
            ",,,:::#####=== ",
            ",,,,,,##D##,,, ",
            ",,Uu,,,,,,,,,, ",
            " ,Z,;;;;hh;;W, ",
            " ,,,;H;hTth;H; ",
            " ,,,;;;;hh;;;, ",
            "  ,,,,L,,,,,,  ",
            "   ,,,,,,,,,   ",
    };

    public InvertedHousePreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        // Planks under everything, including under the masonry: a house built
        // ON its own floor is what makes "the rooms are outside" read as a
        // mistake rather than as a courtyard.
        Legend legend = new Legend(SkyRegistry.gloomwoodFloorID)
                .floor(',')
                .floor('=', "checkerstonetile")
                .rug(':', "purplecarpet")
                .rug(';', "greencarpet")
                .wall('#', "beetlewall")
                .door('D', "beetledoor")
                .pair('E', 'e', "bonebed", DOWN)
                .carpet('E', "purplecarpet").carpet('e', "purplecarpet")
                .prop('R', "bonedresser", RIGHT).carpet('R', "purplecarpet")
                .prop('L', "bentlantern")
                .prop('C', "crookedclock")
                .prop('o', "cookingpot").paves('o', "checkerstonetile")
                .table('k', "bonemodulartable", "cuttingboard", "stewpot").paves('k', "checkerstonetile")
                .prop('l', "barrel").paves('l', "checkerstonetile")
                .prop('s', "sack").paves('s', "checkerstonetile")
                .pair('U', 'u', "bonebathtub", RIGHT)
                .prop('Z', "bonetoilet")
                .prop('W', "groundwindow").carpet('W', "greencarpet")
                .prop('H', "longchair").carpet('H', "greencarpet")
                .pair('T', 't', "bonedinnertable", RIGHT)
                .carpet('T', "greencarpet").carpet('t', "greencarpet")
                .serves('T', "teapot").serves('t', "plate")
                .chair('h', "bonechair").carpet('h', "greencarpet");
        RealmPoiPresets.plan(this, PLAN, legend);
        RealmPoiPresets.stock(this, PLAN, 'l', LOOT, random);
    }

    /**
     * The pantry of a house with no inside.
     *
     * <p>Weighted toward what GREW rather than what was built — Oddwood, Warp
     * Resin, Eye Seed — because this is the one POI that reads as somewhere
     * people lived rather than somewhere something was made. Amounts carry the
     * realm's drop value ({@code CROOKED_DROP_VALUE} = 2.5) for the reason
     * {@link DoorYardPreset#LOOT} records.
     */
    public static final LootTable LOOT = new LootTable(
            LootItem.between("oddwood", 8, 18),
            LootItem.between("warpresin", 4, 10),
            ChanceLootItem.between(0.55F, "eyeseed", 3, 7),
            ChanceLootItem.between(0.40F, "strangefabric", 3, 8),
            ChanceLootItem.between(0.22F, "realityshard", 1, 3));
}
