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
import java.awt.Point;

import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.realms.eden.EdenPressure;
import stairwaytoheaven.worldgen.RealmDepth;
import stairwaytoheaven.worldgen.RealmLanding;

/**
 * Generates Eden and reports the parts a successful compile cannot prove.
 *
 * <p>{@code docs/PLAN_ONE_PLANE.md}: Eden is a BAND of the sky plane, not a
 * dimension, so this no longer looks up an {@code eden2} level and no longer
 * scans the origin -- the origin is the spire, which is Tier 0. It samples
 * around a point {@link RealmLanding} puts in the middle of Eden's band, which
 * is the same point Eden's own door lands a player on.
 */
public class EdenStatusCommand extends ModularChatCommand {
    private static final int RADIUS = 80;

    public EdenStatusCommand() {
        super("edenstatus", "Generates and inspects Eden around the origin (debug)",
                PermissionLevel.ADMIN, false);
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient,
            Object[] args, String[] errors, CommandLog logs) {
        Level level = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        if (!(level instanceof SkyLevel)) {
            logs.add("FAIL: level for identifier \"" + SkyRegistry.SKYREACH_IDENTIFIER + "\" is "
                    + (level == null ? "null" : level.getClass().getSimpleName()) + " (expected SkyLevel)");
            return;
        }
        int seed = ((SkyLevel) level).getWorldGenSeed();
        Point at = RealmLanding.find(seed, RealmDepth.REALM_EDEN, 0, 0);
        Map<String, Integer> tiles = new HashMap<>();
        Map<String, Integer> biomes = new HashMap<>();
        Map<String, Integer> objects = new HashMap<>();
        int calm = 0;
        int active = 0;
        synchronized (level) {
            level.regionManager.ensureTilesAreLoaded(at.x - RADIUS, at.y - RADIUS,
                    at.x + RADIUS, at.y + RADIUS);
            for (int x = at.x - RADIUS; x <= at.x + RADIUS; x++) {
                for (int y = at.y - RADIUS; y <= at.y + RADIUS; y++) {
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
                + " isCave=" + level.isCave
                + " sampledAt=" + at.x + "," + at.y
                + " depth=" + RealmDepth.depthAt(at.x, at.y,
                        stairwaytoheaven.worldgen.SkyOrigin.originX(seed),
                        stairwaytoheaven.worldgen.SkyOrigin.originY(seed)));
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
