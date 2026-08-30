import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.worldgen.SkyLandscape;
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

    /**
     * Every mod ID field the painter can return, in registration order.
     *
     * The painter reaches into more than one holder — SkyRegistry for the
     * Skywatch's materials, SkyCloudmarbleSet for the Skyway Passages' — so
     * every holder it can name has to be filled here. A holder that is missed
     * leaves its fields at 0, and the dump then prints "-" (nothing) for a
     * tile the painter is in fact placing something on, which is precisely the
     * kind of silently-wrong render this whole harness exists to prevent.
     */
    private static final Map<Integer, String> NAMES = new LinkedHashMap<>();

    /** The ID holders, in the order their fields are numbered. */
    private static final Class<?>[] ID_HOLDERS = {
            SkyRegistry.class,
            stairwaytoheaven.SkyCloudmarbleSet.class,
            // The settlement stations live in their own holder, and leaving it
            // out did exactly what the comment above warns about: the painter
            // was placing looms, forges and kilns and the dump reported zero of
            // them, because their IDs were still 0 and 0 means "no object".
            stairwaytoheaven.settlement.SkyProfessions.class,
    };

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
        String mode = args.length > 6 ? args[6] : "world";
        boolean relativeToOrigin = mode.equals("origin") || mode.equals("station")
                || mode.equals("skyway");

        assignSyntheticIDs();

        java.awt.Point origin = SkyOrigin.compute(seed);
        if (relativeToOrigin) {
            x0 += origin.x;
            y0 += origin.y;
        }
        if (mode.equals("skyway")) {
            // Frame the screen on the Skyway Passages. A biome that covers
            // ~14% of the land is not reliably under any fixed offset, so a
            // fixed-offset render of it is a render of whatever happened to be
            // there — which is exactly how a calibration instrument starts
            // lying about a biome.
            int[] spot = bestSkywayWindow(seed, origin, width, height);
            if (spot == null) {
                System.out.println("no Skyway passage found near the origin for seed " + seed);
                System.exit(1);
            }
            System.out.println("framing Skyway passage at " + spot[0] + "," + spot[1]
                    + " (" + spot[2] + " Skyway tiles, " + spot[3] + " of them built)");
            x0 = spot[0] - width / 2;
            y0 = spot[1] - height / 2;
        }
        if (mode.equals("station")) {
            // Frame the screen on a real designed place rather than a random
            // patch: "is this composition legible" is the whole question.
            int[] place = SkyLandscape.designedPlaceNear(seed, x0, y0, origin.x, origin.y, 4);
            if (place == null) {
                System.out.println("no designed place near " + x0 + "," + y0);
                System.exit(1);
            }
            System.out.println("framing designed place kind=" + place[2] + " radius=" + place[3]
                    + " at " + place[0] + "," + place[1]);
            x0 = place[0] - width / 2;
            y0 = place[1] - height / 2;
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

    /**
     * The window near the origin holding the most Skyway ground, preferring
     * one a passage runs through: the balustrades, gates and Seraphs are the
     * whole point of the biome, and open ground alone does not show them.
     *
     * @return {@code {centreX, centreY, skywayTiles, builtSkywayTiles}}
     */
    private static int[] bestSkywayWindow(int seed, java.awt.Point origin, int width, int height) {
        int[] best = null;
        int bestScore = 0;
        int reach = 500;
        for (int cx = origin.x - reach; cx <= origin.x + reach; cx += 10) {
            for (int cy = origin.y - reach; cy <= origin.y + reach; cy += 10) {
                int skyway = 0;
                int built = 0;
                // Sampled every other tile: this runs over ~10k candidate
                // windows and only needs to rank them.
                for (int x = cx - width / 2; x < cx + width / 2; x += 2) {
                    for (int y = cy - height / 2; y < cy + height / 2; y += 2) {
                        long d = SkyTerrainPainter.describeTile(seed, x, y, origin.x, origin.y);
                        if (SkyTerrainPainter.descBiome(d) != SkyTerrainPainter.BIOME_SKYWAY) {
                            continue;
                        }
                        skyway++;
                        if (SkyTerrainPainter.descBuilt(d)) {
                            built++;
                        }
                    }
                }
                int score = skyway + built * 6;
                if (score > bestScore) {
                    bestScore = score;
                    best = new int[]{cx, cy, skyway * 4, built * 4};
                }
            }
        }
        return best;
    }

    private static String name(int id) {
        String n = NAMES.get(id);
        return n == null ? (id == 0 ? "-" : "id" + id) : n;
    }

    /**
     * Fills every {@code static int ...ID} field of each {@link #ID_HOLDERS}
     * with a distinct synthetic value and records the field name, so the dump
     * reads as names rather than numbers. Two vanilla materials the built
     * landscape uses are named explicitly, since they have no mod registration
     * to borrow from.
     */
    private static void assignSyntheticIDs() throws Exception {
        int next = 100;
        for (Class<?> holder : ID_HOLDERS) {
            for (Field f : holder.getDeclaredFields()) {
                if (f.getType() != int.class || java.lang.reflect.Modifier.isFinal(f.getModifiers())) {
                    continue;
                }
                f.setAccessible(true);
                f.setInt(null, next);
                String n = f.getName();
                if (n.endsWith("ID")) {
                    n = n.substring(0, n.length() - 2);
                }
                NAMES.put(next, n);
                next++;
            }
        }
        // Readable aliases for what those two actually resolve to in game.
        NAMES.put(stairwaytoheaven.settlement.SkyProfessions.windsilkLoomID, "windsilkLoom");
        NAMES.put(stairwaytoheaven.settlement.SkyProfessions.aetherForgeID, "aetherForge");
        NAMES.put(stairwaytoheaven.settlement.SkyProfessions.stormglassKilnID, "stormglassKiln");
        NAMES.put(SkyRegistry.skyroadTileID, "skyroad");
        NAMES.put(SkyRegistry.skyplinthTileID, "skyplinth");
    }
}
