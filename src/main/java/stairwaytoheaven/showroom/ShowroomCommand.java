package stairwaytoheaven.showroom;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;

import necesse.engine.GlobalData;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.StringParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.level.SkyLevel;

/**
 * {@code /swhshowroom [status|build|check|goto|tp|list|clear|export] [arg] [arg]}
 * — the exhibition of every building, material and realm ground. See
 * {@link Showroom} and {@code docs/PREVIEW_TOOLS.md}.
 *
 * <ul>
 * <li>{@code status} (default) — where it is, how many exhibits, built or not.
 *     Changes nothing.</li>
 * <li>{@code build} — (re)builds the whole showroom, then runs {@code check}.
 *     Idempotent: a second build writes the same thing again.</li>
 * <li>{@code check} — compares every exhibit with the preset it came from.</li>
 * <li>{@code goto <id>} — puts you in front of an exhibit ({@code list} names
 *     them).</li>
 * <li>{@code tp <x> <y>} — puts you on a sky-level tile (used by
 *     {@code /swhshots world}).</li>
 * <li>{@code clear} — flattens the showroom rectangle to bare floor.</li>
 * <li>{@code export [dir]} — writes what stands in each exhibit as JSON for
 *     {@code tools/preset_render.py}.</li>
 * </ul>
 *
 * <p>ADMIN, like every other command this mod adds. Nothing here writes outside
 * {@code Showroom.layout().bounds}.
 */
public class ShowroomCommand extends ModularChatCommand {

    public static final String DONE = "SWH_SHOWROOM_DONE";

    public ShowroomCommand() {
        super("swhshowroom",
                "Builds/visits the Stairway to Heaven showroom of every building and material",
                PermissionLevel.ADMIN, false,
                new CmdParameter("mode", new StringParameterHandler("status",
                        "status", "build", "check", "goto", "tp", "list", "clear", "export"), true),
                new CmdParameter("arg", new StringParameterHandler(null), true),
                new CmdParameter("arg2", new StringParameterHandler(null), true));
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient,
                           Object[] args, String[] errors, CommandLog logs) {
        String mode = args.length > 0 && args[0] != null ? String.valueOf(args[0]).toLowerCase() : "status";
        String arg = args.length > 1 && args[1] != null ? String.valueOf(args[1]) : null;
        String arg2 = args.length > 2 && args[2] != null ? String.valueOf(args[2]) : null;
        try {
            run(server, serverClient, mode, arg, arg2, logs);
        } catch (Exception e) {
            logs.add("SHOWROOM FAIL: " + e);
            e.printStackTrace();
        }
        logs.add(DONE);
    }

    private void run(Server server, ServerClient caller, String mode, String arg, String arg2, CommandLog logs)
            throws Exception {
        Level level = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        if (!(level instanceof SkyLevel)) {
            logs.add("SHOWROOM FAIL: sky level is " + (level == null ? "null" : level.getClass().getSimpleName()));
            return;
        }
        SkyLevel sky = (SkyLevel) level;
        Showroom.Layout layout = Showroom.layout();
        Rectangle b = layout.bounds;
        switch (mode) {
            case "build":
                synchronized (level) {
                    for (String line : Showroom.build(server, sky).lines) logs.add(line);
                    Showroom.Report check = Showroom.check(sky);
                    logs.add(check.lines.get(check.lines.size() - 1));
                    for (String line : check.lines) {
                        if (line.contains("missing=") && !line.contains("missing=0 ")
                                && !line.startsWith("SHOWROOM_CHECK")) {
                            logs.add(line);
                        }
                    }
                }
                logs.add("Walk it: /swhshowroom goto " + layout.exhibits.get(0).id
                        + "   (all IDs: /swhshowroom list)");
                break;
            case "check":
                synchronized (level) {
                    for (String line : Showroom.check(sky).lines) logs.add(line);
                }
                break;
            case "clear":
                synchronized (level) {
                    for (String line : Showroom.clear(server, sky).lines) logs.add(line);
                }
                break;
            case "export": {
                File dir = new File(arg != null ? arg : GlobalData.appDataPath() + "swh-export");
                synchronized (level) {
                    for (String line : Showroom.export(sky, dir).lines) logs.add(line);
                }
                break;
            }
            case "list":
                for (Showroom.Exhibit e : layout.exhibits) {
                    logs.add(e.id + "  (" + e.kind + ", " + e.group + ", " + e.width + "x" + e.height
                            + " at " + e.x + "," + e.y + ")");
                }
                break;
            case "goto": {
                if (caller == null) {
                    logs.add("goto needs a player (run it from the chat, not the console)");
                    break;
                }
                Showroom.Exhibit e = Showroom.find(arg);
                if (e == null) {
                    logs.add("No exhibit \"" + arg + "\". /swhshowroom list names them.");
                    break;
                }
                teleport(caller, e.viewX(), e.viewY());
                logs.add("SHOWROOM goto " + e.id + " -> " + e.viewX() + "," + e.viewY());
                break;
            }
            case "tp": {
                if (caller == null) {
                    logs.add("tp needs a player (run it from the chat, not the console)");
                    break;
                }
                int x;
                int y;
                try {
                    x = Integer.parseInt(arg);
                    y = Integer.parseInt(arg2);
                } catch (Exception ex) {
                    logs.add("usage: /swhshowroom tp <tileX> <tileY>");
                    break;
                }
                teleport(caller, x, y);
                logs.add("SHOWROOM tp -> " + x + "," + y);
                break;
            }
            default: {
                boolean built;
                synchronized (level) {
                    built = Showroom.isBuilt(level);
                }
                logs.add("SHOWROOM status: exhibits=" + layout.exhibits.size() + " built=" + built
                        + " bounds=" + b.x + "," + b.y + " " + b.width + "x" + b.height
                        + " level=" + SkyRegistry.SKYREACH_IDENTIFIER);
                logs.add(built ? "Walk it: /swhshowroom goto " + layout.exhibits.get(0).id
                        : "Not built yet: /swhshowroom build");
                break;
            }
        }
    }

    private static void teleport(ServerClient caller, int tileX, int tileY) {
        caller.changeLevel(SkyRegistry.SKYREACH_IDENTIFIER,
                l -> new Point(tileX * 32 + 16, tileY * 32 + 16), true);
    }
}
