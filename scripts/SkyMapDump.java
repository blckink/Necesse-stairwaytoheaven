import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.SkyTerrainPainter;

/**
 * Dumps what {@link SkyTerrainPainter#describeTile} decides for a rectangle of
 * Skyreach tiles, as plain text, without booting the game.
 *
 * This is the calibration instrument for worldgen. The rule this repo learned
 * the hard way (docs/TECHNICAL_LEARNINGS.md, "Placement is not a sprite
 * problem") is that a formation field must be judged at SCREEN scale, because
 * a whole-world overview makes a perfectly good structure look like a speck and
 * a perfectly awful one look like texture. So: dump the real decision function,
 * composite the real sprites at 1x in scripts/sky_map_render.py, and look at
 * exactly what a player sees on one screen.
 *
 * describeTile() is the same function paintRegion() writes into the world, so
 * this is not a model of generation — it IS generation. The only thing faked
 * here is the ID space: SkyRegistry's public static int fields are normally
 * filled by the game registries, so this harness fills them with synthetic IDs
 * and keeps a reverse map, which is why no game bootstrapping is needed.
 *
 * Run: scripts/sky_map_render.sh
 */
public class SkyMapDump {

    /** Every SkyRegistry ID field the painter can return, in registration order. */
    private static final Map<Integer, String> NAMES = new LinkedHashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println("usage: SkyMapDump <seed> <x0> <y0> <width> <height> <outfile> [originMode]");
            System.exit(2);
        }
        int seed = Integer.parseInt(args[0]);
        int x0 = Integer.parseInt(args[1]);
        int y0 = Integer.parseInt(args[2]);
        int width = Integer.parseInt(args[3]);
        int height = Integer.parseInt(args[4]);
        String out = args[5];
        boolean relativeToOrigin = args.length > 6 && args[6].equals("origin");

        assignSyntheticIDs();

        java.awt.Point origin = SkyOrigin.compute(seed);
        if (relativeToOrigin) {
            x0 += origin.x;
            y0 += origin.y;
        }

        try (BufferedWriter w = new BufferedWriter(new FileWriter(out))) {
            w.write("# seed=" + seed + " origin=" + origin.x + "," + origin.y
                    + " x0=" + x0 + " y0=" + y0 + " w=" + width + " h=" + height + "\n");
            for (int y = y0; y < y0 + height; y++) {
                StringBuilder line = new StringBuilder();
                for (int x = x0; x < x0 + width; x++) {
                    long desc = SkyTerrainPainter.describeTile(seed, x, y, origin.x, origin.y);
                    if (x > x0) {
                        line.append(' ');
                    }
                    line.append(name(SkyTerrainPainter.descTile(desc)))
                            .append('/')
                            .append(name(SkyTerrainPainter.descObject(desc)))
                            .append('/')
                            .append(SkyTerrainPainter.descBiome(desc))
                            .append(SkyTerrainPainter.descBuilt(desc) ? "/B" : "/-");
                }
                w.write(line.toString());
                w.write('\n');
            }
        }
        System.out.println("wrote " + out + " (" + width + "x" + height + " tiles, origin " + origin.x + "," + origin.y + ")");
    }

    private static String name(int id) {
        String n = NAMES.get(id);
        return n == null ? (id == 0 ? "-" : "id" + id) : n;
    }

    /**
     * Fills every {@code public static int ...ID} field of SkyRegistry with a
     * distinct synthetic value and records the field name, so the dump reads as
     * names rather than numbers. Two vanilla materials the built landscape uses
     * are named explicitly, since they have no mod registration to borrow from.
     */
    private static void assignSyntheticIDs() throws Exception {
        int next = 100;
        for (Field f : SkyRegistry.class.getDeclaredFields()) {
            if (f.getType() != int.class || java.lang.reflect.Modifier.isFinal(f.getModifiers())) {
                continue;
            }
            f.setInt(null, next);
            String n = f.getName();
            if (n.endsWith("ID")) {
                n = n.substring(0, n.length() - 2);
            }
            NAMES.put(next, n);
            next++;
        }
        // Readable aliases for what those two actually resolve to in game.
        NAMES.put(SkyRegistry.skyroadTileID, "skyroad");
        NAMES.put(SkyRegistry.skycourtTileID, "skycourt");
        NAMES.put(SkyRegistry.skyplinthTileID, "skyplinth");
    }
}
