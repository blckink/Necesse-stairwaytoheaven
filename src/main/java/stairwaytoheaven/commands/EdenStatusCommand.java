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
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.realms.eden.EdenLevel;
import stairwaytoheaven.realms.eden.EdenPressure;

/** Generates Eden and reports the parts that a successful compile cannot prove. */
public class EdenStatusCommand extends ModularChatCommand {
    private static final int RADIUS = 80;

    public EdenStatusCommand() {
        super("edenstatus", "Generates and inspects Eden around the origin (debug)",
                PermissionLevel.ADMIN, false);
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient,
            Object[] args, String[] errors, CommandLog logs) {
        Level level = server.world.getLevel(SkyRegistry.EDEN_IDENTIFIER);
        if (!(level instanceof EdenLevel)) {
            logs.add("FAIL: level for identifier \"" + SkyRegistry.EDEN_IDENTIFIER + "\" is "
                    + level.getClass().getSimpleName() + " (expected EdenLevel)");
            return;
        }
        Map<String, Integer> tiles = new HashMap<>();
        Map<String, Integer> biomes = new HashMap<>();
        Map<String, Integer> objects = new HashMap<>();
        int calm = 0;
        int active = 0;
        synchronized (level) {
            level.regionManager.ensureTilesAreLoaded(-RADIUS, -RADIUS, RADIUS, RADIUS);
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int y = -RADIUS; y <= RADIUS; y++) {
                    tiles.merge(TileRegistry.getTileStringID(level.getTileID(x, y)), 1, Integer::sum);
                    biomes.merge(BiomeRegistry.getBiome(level.getBiomeID(x, y)).getStringID(), 1, Integer::sum);
                    if (level.getObjectID(x, y) != 0) {
                        objects.merge(level.getObject(x, y).getStringID(), 1, Integer::sum);
                    }
                    if (EdenPressure.spawnTickets(level, x, y) == 0) {
                        calm++;
                    } else {
                        active++;
                    }
                }
            }
        }
        logs.add("Eden OK: class=" + level.getClass().getSimpleName()
                + " identifier=" + level.getIdentifier()
                + " dimension=" + level.getIdentifier().getOneWorldDimension()
                + " isCave=" + level.isCave);
        tiles.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> logs.add("  eden tile " + e.getKey() + " x" + e.getValue()));
        biomes.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> logs.add("  eden biome " + e.getKey() + " x" + e.getValue()));
        objects.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(12)
                .forEach(e -> logs.add("  eden object " + e.getKey() + " x" + e.getValue()));
        logs.add("eden pressure: calm=" + calm + " active=" + active);
        logs.add("eden registry: bronze=" + ItemRegistry.getItemID("edenbronzebar")
                + " serpent=" + MobRegistry.getMobID("edenserpent")
                + " forbidden=" + MobRegistry.getMobID("forbiddenserpent"));
        logs.add("EDEN_STATUS_DONE");
    }
}
