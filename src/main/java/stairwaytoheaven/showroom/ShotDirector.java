package stairwaytoheaven.showroom;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import necesse.engine.gameLoop.GameLoop;
import necesse.engine.gameLoop.GameLoopListener;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;

/**
 * Drives a screenshot tour on the client: for each shot, ask the server to
 * move the player there, wait until the player has arrived and every region
 * under the shot has arrived from the server, let it settle, then capture.
 *
 * <p>It is a {@link GameLoopListener} on the client's {@code GameLoop} — the
 * hook vanilla's own preset preview form uses ({@code PresetPreviewForm.init})
 * — so it ticks on the game thread before the state's own frame tick, which is
 * where vanilla handles its screenshot key, and it survives the player
 * changing level on the first teleport (a level event would not).
 *
 * <p>VERIFIED [jar] (read out of the decompiled 1.3.2 client). Running it is
 * HYPOTHESIS until the player has run it.
 */
final class ShotDirector implements GameLoopListener {

    static final class Shot {
        final String name;
        /** Chat command that moves the player there, or null to shoot where they stand. */
        final String command;
        final int viewX;
        final int viewY;
        final Rectangle rect;

        Shot(String name, String command, int viewX, int viewY, Rectangle rect) {
            this.name = name;
            this.command = command;
            this.viewX = viewX;
            this.viewY = viewY;
            this.rect = rect;
        }
    }

    private static final int NEXT = 0;
    private static final int ARRIVE = 1;
    private static final int LOAD = 2;
    private static final int CAPTURE = 3;
    private static final int PAUSE = 4;

    /** Lights, splatting and wind settle a moment after the last region lands. */
    private static final long SETTLE_MS = 2500;
    private static final long ARRIVE_TIMEOUT_MS = 20000;
    private static final long LOAD_TIMEOUT_MS = 25000;

    private static ShotDirector active;

    private final Client client;
    private final List<Shot> shots;
    private final File outDir;
    private final String preCommand;
    private int index = -1;
    private int state = NEXT;
    private long stateTime;
    private boolean disposed;
    private int saved;
    private final List<String> problems = new ArrayList<>();
    private boolean preSent;

    private ShotDirector(Client client, List<Shot> shots, File outDir, String preCommand) {
        this.client = client;
        this.shots = shots;
        this.outDir = outDir;
        this.preCommand = preCommand;
        this.stateTime = System.currentTimeMillis();
    }

    static boolean isRunning() {
        return active != null && !active.disposed;
    }

    static void stop() {
        if (active != null) {
            active.finish(true);
        }
    }

    /** @return null if started, else why not */
    static String start(Client client, List<Shot> shots, File outDir, String preCommand) {
        if (isRunning()) return Localization.translate("misc", "swhshotsbusy");
        if (!(client.tickManager() instanceof GameLoop)) {
            return "client tick manager is " + client.tickManager().getClass().getSimpleName() + ", not a GameLoop";
        }
        ShotDirector director = new ShotDirector(client, shots, outDir, preCommand);
        active = director;
        ((GameLoop) client.tickManager()).addGameLoopListener(director);
        return null;
    }

    private void send(String command) {
        client.network.sendPacket(new PacketChatMessage(client.getSlot(), command));
    }

    private void chat(String message) {
        if (client.chat != null) client.chat.addMessage(message);
    }

    @Override
    public void frameTick(TickManager tickManager, GameWindow window) {
        if (disposed) return;
        try {
            step();
        } catch (Throwable t) {
            problems.add("tour aborted: " + t);
            t.printStackTrace();
            finish(true);
        }
    }

    private void step() {
        long now = System.currentTimeMillis();
        if (!preSent) {
            preSent = true;
            if (preCommand != null) {
                send(preCommand);
                stateTime = now;
                state = PAUSE;
                return;
            }
        }
        Level level = client.getLevel();
        PlayerMob player = client.getPlayer();
        Shot shot = index >= 0 && index < shots.size() ? shots.get(index) : null;
        switch (state) {
            case NEXT:
                index++;
                if (index >= shots.size()) {
                    finish(false);
                    return;
                }
                shot = shots.get(index);
                chat("[" + (index + 1) + "/" + shots.size() + "] " + shot.name);
                if (shot.command != null) send(shot.command);
                state = ARRIVE;
                stateTime = now;
                return;
            case ARRIVE:
                if (shot == null) {
                    state = NEXT;
                    return;
                }
                if (shot.command == null || (player != null && level != null
                        && Math.abs(player.getTileX() - shot.viewX) <= 4
                        && Math.abs(player.getTileY() - shot.viewY) <= 4)) {
                    state = LOAD;
                    stateTime = now;
                } else if (now - stateTime > ARRIVE_TIMEOUT_MS) {
                    problems.add(shot.name + ": never arrived (is the showroom built? /swhshowroom build)");
                    state = NEXT;
                }
                return;
            case LOAD:
                if (level == null || shot == null) {
                    if (now - stateTime > LOAD_TIMEOUT_MS) state = NEXT;
                    return;
                }
                boolean loaded = regionsLoaded(level, shot.rect);
                if ((loaded && now - stateTime >= SETTLE_MS) || now - stateTime > LOAD_TIMEOUT_MS) {
                    if (!loaded) problems.add(shot.name + ": not every region arrived; shot taken anyway");
                    state = CAPTURE;
                }
                return;
            case CAPTURE: {
                File file = new File(outDir, safe(shot.name) + ".png");
                String error = ShotCapture.capture(client, level, shot.rect, file);
                if (error == null) {
                    saved++;
                } else {
                    String fallback = ShotCapture.captureVanilla(client, level, shot.rect);
                    problems.add(shot.name + ": own capture failed (" + error + ")"
                            + (fallback == null ? "; vanilla mapshot taken into <settings>/screenshots/ instead"
                            : "; vanilla mapshot failed too (" + fallback + ")"));
                }
                state = PAUSE;
                stateTime = now;
                return;
            }
            case PAUSE:
            default:
                // Vanilla's mapshot fallback holds a cooldown until its writer
                // thread finishes; a short pause keeps shots from colliding.
                if (now - stateTime > 600) state = NEXT;
        }
    }

    private static boolean regionsLoaded(Level level, Rectangle r) {
        int rx0 = level.regionManager.getRegionCoordByTile(r.x);
        int ry0 = level.regionManager.getRegionCoordByTile(r.y);
        int rx1 = level.regionManager.getRegionCoordByTile(r.x + r.width - 1);
        int ry1 = level.regionManager.getRegionCoordByTile(r.y + r.height - 1);
        for (int rx = rx0; rx <= rx1; rx++) {
            for (int ry = ry0; ry <= ry1; ry++) {
                if (!level.regionManager.isRegionLoaded(rx, ry)) return false;
            }
        }
        return true;
    }

    static String safe(String name) {
        return name.replaceAll("[^A-Za-z0-9_.@-]", "_");
    }

    private void finish(boolean aborted) {
        if (disposed) return;
        disposed = true;
        if (active == this) active = null;
        // Every PNG is written by its own daemon thread; they finish within a
        // second of the last capture.
        chat(Localization.translate("misc", aborted ? "swhshotsaborted" : "swhshotsdone",
                "count", String.valueOf(saved), "path", outDir.getAbsolutePath()));
        for (String p : problems) {
            chat(" - " + p);
        }
        System.out.println("[swhshots] " + saved + " shot(s) in " + outDir.getAbsolutePath()
                + (problems.isEmpty() ? "" : "; problems: " + problems));
    }

    @Override
    public void drawTick(TickManager tickManager) {
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
