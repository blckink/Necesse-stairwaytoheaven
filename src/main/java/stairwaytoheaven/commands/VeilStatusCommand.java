package stairwaytoheaven.commands;

import java.util.HashMap;
import java.util.Map;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.VeilLevel;

/**
 * Admin/debug: forces the Veil to generate around the origin and reports
 * terrain/biome/object statistics (Level-first lock order, see
 * SkyreachStatusCommand for the deadlock story).
 */
public class VeilStatusCommand extends ModularChatCommand {

    private static final int SCAN_RADIUS_TILES = 48;

    public VeilStatusCommand() {
        super("veilstatus", "Generates and inspects the Veil around the origin (debug)", PermissionLevel.ADMIN, false);
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
        Level level = server.world.getLevel(SkyRegistry.VEIL_IDENTIFIER);
        if (!(level instanceof VeilLevel)) {
            logs.add("FAIL: level for identifier \"" + SkyRegistry.VEIL_IDENTIFIER + "\" is "
                    + level.getClass().getSimpleName() + " (expected VeilLevel)");
            return;
        }
        synchronized (level) {
            int r = SCAN_RADIUS_TILES;
            level.regionManager.ensureTilesAreLoaded(-r, -r, r, r);
            Map<String, Integer> tiles = new HashMap<>();
            Map<String, Integer> biomes = new HashMap<>();
            Map<String, Integer> objects = new HashMap<>();
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    tiles.merge(TileRegistry.getTileStringID(level.getTileID(x, y)), 1, Integer::sum);
                    biomes.merge(BiomeRegistry.getBiome(level.getBiomeID(x, y)).getStringID(), 1, Integer::sum);
                    if (level.getObjectID(x, y) != 0) {
                        objects.merge(level.getObject(x, y).getStringID(), 1, Integer::sum);
                    }
                }
            }
            logs.add("Veil OK: class=" + level.getClass().getSimpleName()
                    + " identifier=" + level.getIdentifier()
                    + " dimension=" + level.getIdentifier().getOneWorldDimension()
                    + " isCave=" + level.isCave);
            tiles.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> logs.add("  tile " + e.getKey() + " x" + e.getValue()));
            biomes.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> logs.add("  biome " + e.getKey() + " x" + e.getValue()));
            objects.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> logs.add("  object " + e.getKey() + " x" + e.getValue()));
        }
        logs.add("VEIL_STATUS_DONE");
    }
}
