package stairwaytoheaven.realms.crooked;

import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.DOWN;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * The Door Yard — {@code WORLD_DESIGN.md} §13's <i>"doors without a house"</i>,
 * built as a place rather than as a prop.
 *
 * <p>Eleven Beetlefreak doors standing in rows on a chequered forecourt, each
 * one shut, none of them attached to anything. A player who opens one walks
 * through and is exactly where they were. That is the entire joke and it is the
 * cheapest true statement this realm can make about itself.
 *
 * <h2>Whose yard it is (2026-09-24)</h2>
 * It used to be doors and lanterns and nothing else. Now it is somebody's
 * workplace -- the Doorman's (§15), who keeps doors the way other people keep
 * sheep: a red doormat laid in front of every door, as if each were somebody's
 * front door; a desk under the lantern at the east side with his papers and a
 * table clock, his chair turned to it, a bench beside it and a crate of spare
 * hinges; three long chairs lined up in front of the centre door like a
 * waiting room, which is where the realm's joke meets Hell's
 * (§18's "then you are in the wrong queue"); and the barrel, which is still the
 * reason to come.
 *
 * <h2>Two engine facts this plan depends on</h2>
 * <ul>
 * <li><b>A free-standing door is legal.</b> Neither {@code DoorObject} nor
 *     {@code WallDoorObject} overrides {@code isValid} (VERIFIED [jar]), so a
 *     door with no wall beside it is not swept away on validation. Its sibling
 *     <b>window</b> is not: {@code WallWindowObject.isValid} rejects itself
 *     unless its walls are exactly one opposite pair of its own family, which
 *     is why the "windows in the ground" image is the {@code groundwindow}
 *     prop instead.</li>
 * <li><b>Multi-tile pieces are written whole</b> by
 *     {@link RealmPoiPresets#plan}: the bench's far half is drawn as {@code n}
 *     and the interpreter throws at load if it is not where the rotation puts
 *     it.</li>
 * </ul>
 */
public class DoorYardPreset extends Preset {

    public static final int WIDTH = 17;
    public static final int HEIGHT = 13;

    /** The yard, drawn one character per tile; ' ' writes nothing. */
    public static final String[] PLAN = {
            "  ___________    ",
            " _____________   ",
            " __D___D___D__   ",
            "__-___-___-___   ",
            "__L_______C_L__  ",
            "______________W  ",
            "__D_HHHB____D__  ",
            "__-___________   ",
            "__L___W____L_cN  ",
            "__________yhx_n  ",
            " __D___D___D__   ",
            " __-___-___-_    ",
            "  _________      ",
    };

    public DoorYardPreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        // Everything that is written at all stands on paving: the chequerboard
        // IS the yard, and a door with no floor under it reads as a door that
        // fell over rather than one left standing.
        Legend legend = new Legend(SkyRegistry.marbleCheckerID)
                .floor('_')
                .rug('-', "redyarncarpet")
                .door('D', "beetledoor")
                .prop('L', "bentlantern")
                .prop('C', "crookedclock")
                .prop('W', "groundwindow")
                .prop('H', "longchair")
                .prop('B', "barrel")
                .prop('c', "crookedcrate")
                .pair('N', 'n', "bonebench", DOWN)
                .table('y', "bonemodulartable", "stackofpaper")
                .chair('h', "bonechair")
                .table('x', "bonedesk");
        RealmPoiPresets.plan(this, PLAN, legend);
        RealmPoiPresets.stock(this, PLAN, 'B', LOOT, random);
    }

    /**
     * What is behind eleven doors that go nowhere.
     *
     * <p>Weighted to Strange Fabric and Reality Shard, i.e. the BUILT half of
     * the realm's economy, because this is a made place. Amounts already carry
     * the realm's drop value ({@code SkyMobTiers.CROOKED_DROP_VALUE} = 2.5): a
     * preset barrel sits in an ordinary level with no {@code LevelModifiers.LOOT}
     * on it, so the multiplier has to be in the table.
     */
    public static final LootTable LOOT = new LootTable(
            LootItem.between("strangefabric", 6, 14),
            LootItem.between("realityshard", 2, 5),
            ChanceLootItem.between(0.50F, "warpresin", 4, 10),
            ChanceLootItem.between(0.35F, "eyeseed", 2, 5),
            ChanceLootItem.between(0.30F, "oddwood", 5, 12));
}
