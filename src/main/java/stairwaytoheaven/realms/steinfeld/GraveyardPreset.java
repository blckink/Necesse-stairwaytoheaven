package stairwaytoheaven.realms.steinfeld;

import static stairwaytoheaven.worldgen.pois.RealmPoiPresets.LEFT;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets.Legend;

/**
 * A walled plot, hand-laid rather than grown from the noise field —
 * {@code docs/WORLD_DESIGN.md} §7's "gravestones" made into a PLACE rather
 * than a scatter, on {@link SteinfeldSites}' own rare lattice. See that
 * class's header for why this is a second lattice and not a variation on
 * {@link SteinfeldTerrainPainter}'s organic grave field.
 *
 * <h2>The pilgrims' plot (2026-09-24)</h2>
 * It used to be a fence, a mourner, eight stones and a salvage crate on bare
 * soil. Now it is a plot somebody tends: a weathered-stone walk from the gate
 * to the mourner, who stands on a mist-stone plinth between two candle
 * pedestals; two rows of graves either side of the walk, each with a widow
 * flower or a dead heaven bloom planted at its foot; a bench for visitors
 * facing the mourner; and in the corner the gravedigger's things -- his chest
 * and the pile of pale stone the next headstone will be cut from. A lantern
 * stands either side of the walk inside the gate.
 *
 * <p>The loot was never in the game before this: it was added to a
 * {@code skycrate}, which is a {@code RandomCrateObject} -- a breakable with no
 * inventory, so {@code Preset.addInventory} had nothing to fill. It is in the
 * gravedigger's {@code birchchest} now, a real container.
 *
 * <p>Every flower stands on grave soil, which is organic: a
 * {@code GrassObject} on anything else is deleted by its own {@code isValid}.
 */
public class GraveyardPreset extends Preset {

    public static final int WIDTH = 13;
    public static final int HEIGHT = 13;

    /** The plot, drawn one character per tile; '.' writes nothing. */
    public static final String[] PLAN = {
            "#############",
            "#;;;;;;;;;;;#",
            "#;;;;kMk;;;;#",
            "#;;;;_,_;;;;#",
            "#;g;g;,;g;g;#",
            "#;w;b;,;b;w;#",
            "#;;;;;,;;;;;#",
            "#;g;g;,;g;g;#",
            "#;b;w;,;w;b;#",
            "#;;;;;,;;;r;#",
            "#;;nN;,;;X;;#",
            "#;;;;L,L;;;;#",
            "######G######",
    };

    public GraveyardPreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        Legend legend = new Legend(SkyRegistry.gravesoilID)
                .floor(';')
                .floor(',', "weatheredstonetile")
                .floor('_', "miststonetile")
                .fence('#', "cryptfence")
                .fence('G', "cryptfencegate")
                .prop('M', "mournerstatue").paves('M', "miststonetile")
                .prop('k', "stonecandlepedestal").paves('k', "miststonetile")
                .prop('g', "cryptgravestone1")
                .prop('w', "widowflower")
                .prop('b', "deadheavenbloom")
                .prop('r', "palestonerock")
                // Faces the mourner: a bench turned to 3 faces north.
                .pair('N', 'n', "birchbench", LEFT)
                .prop('X', "birchchest")
                .prop('L', "lantern");
        RealmPoiPresets.plan(this, PLAN, legend);
        RealmPoiPresets.stock(this, PLAN, 'X', LOOT, random);
    }

    /**
     * What a plot this well-kept has been given. Weighted toward Pale Stone
     * and Grave Salt — the two materials {@code docs/WORLD_DESIGN.md} §7
     * names for this ground — with a real chance at both rarer resources, at
     * the realm's x1.6 drop value.
     */
    public static final LootTable LOOT = new LootTable(
            LootItem.between("palestone", 8, 18),
            LootItem.between("gravesalt", 4, 9),
            ChanceLootItem.between(0.45F, "spiritmoss", 2, 5),
            ChanceLootItem.between(0.30F, "bone", 3, 7),
            ChanceLootItem.between(0.18F, "echoshard", 1, 2));
}
