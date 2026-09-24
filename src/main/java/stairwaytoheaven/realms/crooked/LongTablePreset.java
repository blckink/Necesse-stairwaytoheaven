package stairwaytoheaven.realms.crooked;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * The Long Table — {@code WORLD_DESIGN.md} §13's <i>"absurdly long chairs"</i>
 * given somewhere to be absurd, and the richest place in the realm.
 *
 * <p>A hall with no roof and no walls: twenty-one paces of chequered floor
 * running between a free-standing door at the west end and one to the south,
 * and down the middle of it <b>a real table</b> -- seventeen bone tables in
 * one line, laid for a feast that went off a very long time ago: plates and
 * chalices, a teapot, spilled goblets, a skull, a rotten pig on a dish, a
 * table clock that does not agree with the two crooked clocks either side of
 * it. Down both sides stand the long chairs, which cannot be sat on and face
 * whichever way they please. At the head of the table, the one ordinary chair
 * in the realm -- turned to the table, as a chair should be -- and behind it
 * the barrel the host keeps the good things in.
 *
 * <p>Until 2026-09-24 there was no table at all: thirty-four long chairs
 * either side of a plank runner "that is only a table because the chairs say
 * so". The joke survives; it is funnier with the feast on it.
 *
 * <p>Its shape is deliberately a CORRIDOR rather than a yard. The other two POIs
 * are places you walk around; this is a place that makes you walk down it, past
 * every guard in the pack, to reach the thing at the end. That is the guarded
 * shape A4.1 asks for, expressed in geometry rather than in numbers.
 */
public class LongTablePreset extends Preset {

    public static final int WIDTH = 23;
    public static final int HEIGHT = 9;

    /** The hall, drawn one character per tile; ' ' writes nothing. */
    public static final String[] PLAN = {
            "   _______________     ",
            "  __L_____C_____L__    ",
            " _HHHHHHHHHHHHHHHHH_   ",
            "D,TTTTTTTTTTTTTTTTTh,B ",
            " _HHHHHHHHHHHHHHHHH_   ",
            "  __L__W__C__W__L__    ",
            "   _______________     ",
            "    _____________      ",
            "     ____D____         ",
    };

    public LongTablePreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        Legend legend = new Legend(SkyRegistry.marbleCheckerID)
                .floor('_')
                // The table stands on the plank runner the hall always had.
                .floor(',', "gloomwoodfloortile")
                .door('D', "beetledoor")
                .prop('H', "longchair")
                .table('T', "bonemodulartable", "plate", "oldchalices", "rottenpigdish", "mug",
                        "brokenplate", "oldsoup", "bloodgoblet", "plate", "skull", "rottenfishstew",
                        "mug", "tableclock", "oldplate", "teapot", "dirtyplate", "bloodgobletspilled",
                        "goldchalice")
                .paves('T', "gloomwoodfloortile")
                .chair('h', "bonechair").paves('h', "gloomwoodfloortile")
                .prop('C', "crookedclock")
                .prop('L', "bentlantern")
                .prop('W', "groundwindow")
                .prop('B', "barrel").paves('B', "gloomwoodfloortile");
        RealmPoiPresets.plan(this, PLAN, legend);
        RealmPoiPresets.stock(this, PLAN, 'B', LOOT, random);
    }

    /**
     * What is at the head of the table.
     *
     * <p>The best table in the realm, and the only one that hands out Reality
     * Shards in a quantity worth planning around — which is what makes the walk
     * past the pack a decision rather than a chore. Amounts carry the realm's
     * drop value ({@code CROOKED_DROP_VALUE} = 2.5); see
     * {@link DoorYardPreset#LOOT} for why the multiplier lives in the table.
     */
    public static final LootTable LOOT = new LootTable(
            LootItem.between("realityshard", 4, 9),
            LootItem.between("strangefabric", 8, 16),
            LootItem.between("warpresin", 5, 12),
            ChanceLootItem.between(0.60F, "eyeseed", 4, 8),
            ChanceLootItem.between(0.50F, "oddwood", 8, 16),
            ChanceLootItem.between(0.30F, "stripedshell", 2, 4));
}
