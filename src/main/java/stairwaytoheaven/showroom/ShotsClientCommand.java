package stairwaytoheaven.showroom;

import java.awt.Rectangle;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import necesse.engine.GlobalData;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.StringParameterHandler;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import stairwaytoheaven.worldgen.SkyOrigin;
import stairwaytoheaven.worldgen.WardenSpirePreset;
import stairwaytoheaven.worldgen.pois.RealmPoiPresets;
import stairwaytoheaven.worldgen.pois.RealmPoiWorldPreset;

/**
 * {@code /swhshots [showroom|<exhibit>|<group>|here|world|stop] [n] [day]} —
 * a client command that photographs the showroom (or the real world) at
 * 32 px per tile, without the HUD, into
 * {@code <Necesse settings>/swh-screenshots/<timestamp>/<name>.png}.
 *
 * <ul>
 * <li>{@code showroom} — every exhibit ({@code /swhshowroom build} first).</li>
 * <li>{@code <exhibit id>} or {@code <group>} (skyreach, eden, steinfeld,
 *     ghostrealm, crookedbeyond, hell, surface) — just those.</li>
 * <li>{@code here [w] [h]} — the area around the player, default 64x36 tiles.</li>
 * <li>{@code world [n]} — the spire and the n (default 8) nearest realm POI
 *     sites around the player. Needs the world to run on this PC (single
 *     player or hosting), because the site list is computed from the world
 *     seed.</li>
 * <li>{@code stop} — abandon a running tour.</li>
 * <li>{@code day} anywhere — first sets the world to noon ({@code /time noon}).</li>
 * </ul>
 *
 * <p>Teleports go through the server as ordinary chat commands
 * ({@code /swhshowroom goto}, {@code /swhshowroom tp}), so they need the same
 * ADMIN permission any other teleport does. See {@code docs/PREVIEW_TOOLS.md}.
 */
public class ShotsClientCommand extends ModularChatCommand {

    public ShotsClientCommand() {
        super("swhshots", "Screenshots of the showroom or the world, one PNG per exhibit/site",
                PermissionLevel.USER, false,
                new CmdParameter("what", new StringParameterHandler("showroom",
                        "showroom", "here", "world", "stop", "skyreach", "eden", "steinfeld",
                        "ghostrealm", "crookedbeyond", "hell", "surface"), true),
                new CmdParameter("arg", new StringParameterHandler(null), true),
                new CmdParameter("arg2", new StringParameterHandler(null), true),
                new CmdParameter("arg3", new StringParameterHandler(null), true));
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient,
                           Object[] args, String[] errors, CommandLog logs) {
        if (client == null) {
            logs.add("swhshots is a client command");
            return;
        }
        List<String> words = new ArrayList<>();
        for (Object a : args) {
            if (a != null) words.add(String.valueOf(a).toLowerCase());
        }
        boolean day = words.remove("day");
        String what = words.isEmpty() ? "showroom" : words.remove(0);
        if (what.equals("stop")) {
            ShotDirector.stop();
            return;
        }
        PlayerMob player = client.getPlayer();
        if (player == null) {
            logs.add("no player");
            return;
        }
        List<ShotDirector.Shot> shots = new ArrayList<>();
        switch (what) {
            case "here": {
                int w = parse(words, 0, 64);
                int h = parse(words, 1, 36);
                shots.add(new ShotDirector.Shot("here_" + player.getTileX() + "_" + player.getTileY(), null,
                        player.getTileX(), player.getTileY(),
                        new Rectangle(player.getTileX() - w / 2, player.getTileY() - h / 2, w, h)));
                break;
            }
            case "world": {
                String problem = worldShots(client, player, parse(words, 0, 8), shots);
                if (problem != null) {
                    logs.add(problem);
                    return;
                }
                break;
            }
            default: {
                for (Showroom.Exhibit e : Showroom.layout().exhibits) {
                    if (what.equals("showroom") || what.equals("all") || what.equals(e.id) || what.equals(e.group)) {
                        shots.add(new ShotDirector.Shot(e.id, "/swhshowroom goto " + e.id,
                                e.viewX(), e.viewY(), e.captureRect()));
                    }
                }
                if (shots.isEmpty()) {
                    logs.add("No exhibit or group \"" + what + "\" (see /swhshowroom list)");
                    return;
                }
            }
        }
        File outDir = new File(GlobalData.appDataPath() + "swh-screenshots/"
                + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
        String problem = ShotDirector.start(client, shots, outDir, day ? "/time noon" : null);
        if (problem != null) {
            logs.add(problem);
            return;
        }
        logs.add(Localization.translate("misc", "swhshotsstart",
                "count", String.valueOf(shots.size()), "path", outDir.getAbsolutePath()));
    }

    private static int parse(List<String> words, int i, int fallback) {
        try {
            return i < words.size() ? Math.max(8, Math.min(400, Integer.parseInt(words.get(i)))) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** The spire plus the nearest accepted realm POI sites; null on success. */
    private static String worldShots(Client client, PlayerMob player, int count, List<ShotDirector.Shot> shots) {
        Server local = client.getLocalServer();
        if (local == null || local.world == null) {
            return Localization.translate("misc", "swhshotsneedlocal");
        }
        int seed = SkyOrigin.worldGenSeed(local.world.worldEntity);
        int ox = SkyOrigin.originX(seed);
        int oy = SkyOrigin.originY(seed);
        int half = WardenSpirePreset.SIZE / 2;
        shots.add(new ShotDirector.Shot("spire", "/swhshowroom tp " + ox + " " + (oy + SkyOrigin.ARRIVAL_OFFSET_Y + 2),
                ox, oy + SkyOrigin.ARRIVAL_OFFSET_Y + 2,
                new Rectangle(ox - half - 4, oy - half - 6, WardenSpirePreset.SIZE + 8, WardenSpirePreset.SIZE + 14)));
        int px = player.getTileX();
        int py = player.getTileY();
        int reach = 1600;
        List<int[]> sites = new ArrayList<>();
        RealmPoiWorldPreset.survey(seed, px - reach, py - reach, px + reach, py + reach,
                (kind, realm, x, y, w, h, stage) -> {
                    if (stage == RealmPoiWorldPreset.STAGE_ACCEPTED) sites.add(new int[]{kind, x, y, w, h});
                });
        sites.sort((a, b) -> Long.compare(dist(a, px, py), dist(b, px, py)));
        for (int i = 0; i < Math.min(count, sites.size()); i++) {
            int[] s = sites.get(i);
            int vx = s[1] + s[3] / 2;
            int vy = s[2] + s[4] + 2;
            shots.add(new ShotDirector.Shot(RealmPoiPresets.key(s[0]) + "@" + s[1] + "_" + s[2],
                    "/swhshowroom tp " + vx + " " + vy, vx, vy,
                    new Rectangle(s[1] - 3, s[2] - 5, s[3] + 6, s[4] + 11)));
        }
        return null;
    }

    private static long dist(int[] s, int px, int py) {
        long dx = s[1] + s[3] / 2 - px;
        long dy = s[2] + s[4] / 2 - py;
        return dx * dx + dy * dy;
    }
}
