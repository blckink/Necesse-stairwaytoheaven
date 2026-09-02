package stairwaytoheaven.realms.steinfeld;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;

/**
 * A walled plot, hand-laid rather than grown from the noise field —
 * {@code docs/WORLD_DESIGN.md} §7's "gravestones" made into a PLACE rather
 * than a scatter, on {@link SteinfeldSites}' own rare lattice. See that
 * class's header for why this is a second lattice and not a variation on
 * {@link SteinfeldTerrainPainter}'s organic grave field.
 *
 * <h2>Legend</h2>
 * <pre>
 *   #   the wall (vanilla cryptfence, same sheet Ghost's own graveyard uses)
 *   G   the gate — the only break in the wall
 *   M   the mourner at the head of the plot (mossymonkstatue)
 *   g   a gravestone, alternating vanilla cryptgravestone1 / cryptgravestone2
 *   C   the salvage crate
 *   .   grave soil, bare
 * </pre>
 *
 * <p>Every object here is vanilla's own, read by literal path — nothing new
 * was drawn (see {@code docs/realms/steinfeld.md}'s borrowed-art table).
 */
public class GraveyardPreset extends Preset {

    public static final int WIDTH = 13;
    public static final int HEIGHT = 13;

    private static final String[] PLAN = {
            "#############",
            "#...........#",
            "#.....M.....#",
            "#...........#",
            "#.g...g...g.#",
            "#...........#",
            "#.g...C...g.#",
            "#...........#",
            "#.g...g...g.#",
            "#...........#",
            "#...........#",
            "#...........#",
            "######G######",
    };

    public GraveyardPreset(GameRandom random) {
        super(WIDTH, HEIGHT);

        int gravestone2 = ObjectRegistry.getObjectID("cryptgravestone2");
        int gate = ObjectRegistry.getObjectID("cryptfencegate");

        for (int y = 0; y < PLAN.length; y++) {
            String row = PLAN[y];
            for (int x = 0; x < row.length(); x++) {
                this.setTile(x, y, SkyRegistry.gravesoilID);
                switch (row.charAt(x)) {
                    case '#':
                        this.setObject(x, y, SkyRegistry.gravefenceID);
                        break;
                    case 'G':
                        this.setObject(x, y, gate);
                        break;
                    case 'M':
                        this.setObject(x, y, SkyRegistry.mournerstatueID);
                        break;
                    case 'g':
                        // Alternating stones, the same variety SunkenGraveyardPreset
                        // uses one dimension over: a field of identical stones reads
                        // as tiled floor rather than as graves.
                        this.setObject(x, y, ((x + y) & 1) == 0
                                ? SkyRegistry.steinfeldgravestoneID : gravestone2);
                        break;
                    case 'C':
                        this.setObject(x, y, SkyRegistry.skyCrateID);
                        break;
                    default:
                        break; // '.' is bare floor
                }
            }
        }

        this.addInventory(LOOT, random, 6, 6, new Object[0]);
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
