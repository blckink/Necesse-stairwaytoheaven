package stairwaytoheaven.realms.ghost;

import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.DOWN;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.WALL_LEFT;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.WALL_RIGHT;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * The Mausoleum — the Aftergarden's common tomb, and the smallest of its three
 * POIs.
 *
 * <h2>What it is now (2026-09-24)</h2>
 * A family tomb read from the door: two candle pedestals inside the threshold,
 * four crypt columns carrying the cross vault, the family's sarcophagus in the
 * crossing, a crypt coffin laid in each of the two arms, and at the head of the
 * nave the family altar -- the bone chest between two urns, lit by a wall
 * candle either side. Outside, the four who could not afford a place inside.
 *
 * <p>The coffins were ONE tile before this: {@code cryptcoffin} is a two-tile
 * {@code CoffinObject} (vanilla {@code MultiTile(0, 1, 1, 2, ...)}, the bed's
 * own shape) and a preset writes only what it is told, so the old tomb carried
 * the head of a coffin with no foot. {@link RealmPoiPresets#plan} writes both
 * halves, and throws at load if the plan does not draw the far half where the
 * rotation puts it. The coffins are real containers and hold grave goods.
 *
 * <p>Everything is the game's own crypt set plus the mod's plan interpreter;
 * two lights over ~43 floor tiles, the dim end of the dossier's band.
 */
public class MausoleumPreset extends Preset {

    public static final int WIDTH = 11;
    public static final int HEIGHT = 11;

    /** The tomb, drawn. Every row is exactly {@link #WIDTH} characters. */
    public static final String[] PLAN = {
            "G,,,,,,,,,G",
            ",,#######,,",
            ",,#u=B=u#,,",
            ",,#<===>#,,",
            "###I===I###",
            "#Q===S===Q#",
            "#q=======q#",
            "###I===I###",
            ",,#k===k#,,",
            ",,###D###,,",
            "G,,,,,,,,,G",
    };

    public MausoleumPreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        Legend legend = new Legend(GhostRealm.blackCobbleID)
                .floor('=')
                .floor(',', "spiritstonetile")
                .wall('#', "cryptwall")
                .door('D', "cryptdoor")
                .floor('<').decor('<', "wallcandle", WALL_LEFT)
                .floor('>').decor('>', "wallcandle", WALL_RIGHT)
                // "vase", not "vases": vanilla registers the object as "vase"
                // and only its TEXTURE is "vases" (ObjectRegistry.java:2063).
                .prop('u', "vase")
                .prop('B', "bonechest")
                .prop('I', "cryptcolumn")
                .pair('Q', 'q', "cryptcoffin", DOWN)
                .prop('S', "sarcophagus")
                .prop('k', "stonecandlepedestal")
                .prop('G', "cryptgravestone1").paves('G', "spiritstonetile");
        RealmPoiPresets.plan(this, PLAN, legend);

        // What the tomb was built to hold.
        RealmPoiPresets.stock(this, PLAN, 'B', new LootTable(
                LootItem.between("ectoplasm", 6, 14),
                LootItem.between("bonewood", 5, 12),
                ChanceLootItem.between(0.60F, "soulthread", 3, 8),
                ChanceLootItem.between(0.45F, "spectralore", 3, 7),
                ChanceLootItem.between(0.25F, "spiritsteelbar", 1, 3),
                ChanceLootItem.between(0.20F, "bone", 5, 12)), random);
        // ...and what each of the two family members was buried with.
        RealmPoiPresets.stock(this, PLAN, 'Q', new LootTable(
                LootItem.between("bone", 2, 6),
                ChanceLootItem.between(0.45F, "ectoplasm", 2, 5),
                ChanceLootItem.between(0.35F, "coin", 15, 60)), random);
    }
}
