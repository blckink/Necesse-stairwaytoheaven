package stairwaytoheaven.realms.steinfeld;

import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;

/**
 * A roofless nave, hand-laid rather than grown from the noise field —
 * {@code docs/WORLD_DESIGN.md} §7's "broken angel statues" made into a PLACE
 * rather than a scatter, on {@link SteinfeldSites}' own rare lattice. See
 * that class's header for why this is a second lattice and not a variation
 * on {@link SteinfeldTerrainPainter}'s organic ruined chapel.
 *
 * <h2>Legend</h2>
 * <pre>
 *   A   the broken angel, where the altar would be (mod's own seraph statue)
 *   M   a mourner flanking the angel (mossymonkstatue)
 *   L   a colonnade pillar (vanilla cryptcolumn) — two lines down the nave
 *   S   a fallen roof slab on the open floor (skywatchrubble)
 *   X   the salvage crate, at the door end
 *   .   cracked heaven marble, bare
 * </pre>
 *
 * <p>No walls: a roof and its walls are what a RUIN has lost, and open
 * colonnades read as "roofless" the moment the sky shows between them —
 * exactly what {@link SteinfeldTerrainPainter}'s own procedural chapel does
 * with the same two-line pillar pattern, here fixed in place instead of
 * scattered by noise.
 */
public class RuinedChapelPreset extends Preset {

    public static final int WIDTH = 11;
    public static final int HEIGHT = 15;

    private static final String[] PLAN = {
            ".....A.....",
            "..M.....M..",
            "...........",
            ".L.......L.",
            "...........",
            "....S......",
            ".L.......L.",
            "...........",
            "......S....",
            ".L.......L.",
            "...........",
            "...........",
            ".L.......L.",
            ".....X.....",
            "...........",
    };

    public RuinedChapelPreset(GameRandom random) {
        super(WIDTH, HEIGHT);

        for (int y = 0; y < PLAN.length; y++) {
            String row = PLAN[y];
            for (int x = 0; x < row.length(); x++) {
                this.setTile(x, y, SkyRegistry.crackedmarbleID);
                switch (row.charAt(x)) {
                    case 'A':
                        this.setObject(x, y, SkyRegistry.brokenangelID);
                        break;
                    case 'M':
                        this.setObject(x, y, SkyRegistry.mournerstatueID);
                        break;
                    case 'L':
                        this.setObject(x, y, SkyRegistry.chapelcolumnID);
                        break;
                    case 'S':
                        this.setObject(x, y, SkyRegistry.heavenslabID);
                        break;
                    case 'X':
                        this.setObject(x, y, SkyRegistry.skyCrateID);
                        break;
                    default:
                        break; // '.' is bare floor
                }
            }
        }

        this.addInventory(LOOT, random, 5, 13, new Object[0]);
    }

    /**
     * What a chapel this far from the door still keeps. Cracked Heaven
     * Marble is the nave's own floor, sold back to the player who breaks in;
     * Echo Shard is more common here than anywhere else in the realm — a
     * chapel is where the apparitions §7 mentions were seen last. Quantities
     * at the realm's x1.6 drop value.
     */
    public static final LootTable LOOT = new LootTable(
            LootItem.between("palestone", 6, 14),
            ChanceLootItem.between(0.50F, "echoshard", 1, 3),
            ChanceLootItem.between(0.35F, "spiritmoss", 2, 4),
            ChanceLootItem.between(0.22F, "gravesalt", 2, 5));
}
