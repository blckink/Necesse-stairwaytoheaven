package stairwaytoheaven.realms.ghost;

import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.LEFT;
import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.RIGHT;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * The Sunken Graveyard -- the drowned parish's burial ground, whose raised
 * centre keeps its cache above the marsh.
 *
 * <p>Until 2026-09-24 a fence ring round nine identical stones and a chest.
 * Now it reads as a graveyard somebody still visits: a cryptpath walk from the
 * gate to the raised spirit-stone centre, where the parish chest stands
 * between two grave candles and a coffin lies in front of it, dug up and never
 * put back (it is a real container: grave goods). The old north row carries
 * the surface-style headstones of the first dead and the mourner at its head;
 * two rows of crypt stones flank the walk; a bench for the mourners faces the
 * centre; withered shrubs grow on the untended graves; a lantern either side
 * of the gate, outside the fence.
 *
 * <p>Built through {@link RealmPoiPresets#plan}: the ring is one straight run
 * per side, so no post stands alone, and the gate is a real
 * {@code cryptfencegate} in the ring. Open air, so the light is the
 * threshold and the monument only (dossier §0.5): two lanterns, two candles.
 */
public class SunkenGraveyardPreset extends Preset {
    public static final int WIDTH = 15;
    public static final int HEIGHT = 15;

    /** The graveyard, drawn one character per tile; '.' writes nothing. */
    public static final String[] PLAN = {
            "...............",
            ".fffffffffffff.",
            ".f;3;3;M;4;4;f.",
            ".f;;;;;;;;;;;f.",
            ".f;1;_____;1;f.",
            ".f;;;_kBk_;;;f.",
            ".f;;;_____;;wf.",
            ".f;2;_Qq__;2;f.",
            ".f;;;_____;;;f.",
            ".f;;;;;,;;;;;f.",
            ".f;1nN;,;;;1;f.",
            ".f;;;;;,;;;;;f.",
            ".f;;w;;,;;w;;f.",
            ".ffffffgffffff.",
            "......L.L......",
    };

    public SunkenGraveyardPreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        Legend legend = new Legend(GhostRealm.graveyardSoilID)
                .floor(';')
                .floor(',', "cryptpath")
                .floor('_', "spiritstonetile")
                .fence('f', "cryptfence")
                .fence('g', "cryptfencegate")
                .prop('B', "bonechest").paves('B', "spiritstonetile")
                .prop('k', "candle").paves('k', "spiritstonetile")
                .pair('Q', 'q', "cryptcoffin", RIGHT)
                .paves('Q', "spiritstonetile").paves('q', "spiritstonetile")
                .prop('1', "cryptgravestone1")
                .prop('2', "cryptgravestone2")
                .prop('3', "gravestone1")
                .prop('4', "gravestone2")
                .prop('M', "mournerstatue")
                // Faces the centre: a bench turned to 3 faces north, the
                // Sky Town's pond benches are the reference.
                .pair('N', 'n', "deadwoodbench", LEFT)
                .loose('L', "lantern")
                .prop('w', "withershrub");
        RealmPoiPresets.plan(this, PLAN, legend);
        RealmPoiPresets.stock(this, PLAN, 'B', new LootTable(
                LootItem.between("ectoplasm", 8, 16),
                ChanceLootItem.between(0.75F, "bonewood", 5, 12),
                ChanceLootItem.between(0.55F, "soulthread", 4, 8),
                ChanceLootItem.between(0.25F, "spiritsteelbar", 1, 2)), random);
        // What was buried with whoever lies in the open coffin: the realm's
        // cheapest materials, and a few coins.
        RealmPoiPresets.stock(this, PLAN, 'Q', new LootTable(
                LootItem.between("bone", 3, 8),
                ChanceLootItem.between(0.50F, "soulthread", 1, 3),
                ChanceLootItem.between(0.40F, "coin", 10, 40)), random);
    }
}
