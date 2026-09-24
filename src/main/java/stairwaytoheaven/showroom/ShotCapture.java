package stairwaytoheaven.showroom;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import necesse.engine.Settings;
import necesse.engine.network.client.Client;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTexture.GameFrameBuffer;
import necesse.gfx.shader.GameShader;
import necesse.level.maps.Level;
import org.lwjgl.opengl.GL20;

/**
 * Renders one rectangle of the client's level into an off-screen frame buffer
 * at 32 px per tile and writes it as a PNG. CLIENT ONLY: never loaded on a
 * dedicated server (only {@link ShotDirector} references it, and only while a
 * tour is running on a client).
 *
 * <h2>Where this comes from</h2>
 * It is vanilla's own map screenshot, {@code Renderer.takeMapshot(Level,
 * Client, GameCamera)} (decompiled 1.3.2, {@code Renderer.java:661-718}), step
 * for step, with two changes: the file gets the exhibit's name instead of a
 * timestamp, and the camera is the exhibit's rectangle instead of the map
 * tool's. That method is what the full-screen map's "screenshot tool" button
 * calls ({@code MainGameFormManager}), so the draw path is the game's own:
 * {@code level.drawUtils.prepareDraw/draw} with {@code Settings.hideUI} set —
 * tiles, splatting, objects, walls, mobs and lighting exactly as on screen,
 * and no HUD.
 *
 * <p>VERIFIED [jar] by reading the decompiled source. Running it is HYPOTHESIS
 * until it has been run on a real client.
 */
final class ShotCapture {

    private ShotCapture() {
    }

    /**
     * Must be called on the game thread outside of a draw pass — a
     * {@code GameLoopListener.frameTick} is exactly where vanilla handles its
     * own screenshot key.
     *
     * @return null on success, else why it failed
     */
    static String capture(Client client, Level level, Rectangle tiles, File file) {
        GameWindow window = WindowManager.getWindow();
        int w = tiles.width * 32;
        int h = tiles.height * 32;
        GameCamera camera = new GameCamera(tiles.x * 32, tiles.y * 32, w, h);
        PlayerMob player = client == null ? null : client.getPlayer();
        ByteBuffer buffer = null;
        String error = null;
        GL20.glUseProgram(0);
        GameFrameBuffer frameBuffer = window.getNewFrameBuffer(w, h);
        try {
            frameBuffer.bindFrameBuffer();
            if (frameBuffer.isComplete()) {
                boolean lastHideUI = Settings.hideUI;
                Settings.hideUI = true;
                try {
                    level.runGLContextRunnables();
                    level.drawUtils.prepareDraw(camera, player, null, true);
                    level.drawUtils.draw(camera, player, null, true);
                    window.update();
                } finally {
                    Settings.hideUI = lastHideUI;
                }
                buffer = Renderer.readColorBufferFromFrameBuffer(frameBuffer, false);
            } else {
                error = "frame buffer " + w + "x" + h + " is not complete";
            }
        } catch (Throwable t) {
            error = t.toString();
        } finally {
            frameBuffer.unbindFrameBuffer();
            frameBuffer.dispose();
            GameShader shader = Renderer.getCurrentShader();
            if (shader != null) {
                shader._ScreenUse();
            } else {
                GL20.glUseProgram(0);
            }
            window.makeCurrent();
            window.getInput().clearInput();
            window.tickWindowResize(true);
        }
        if (buffer == null) {
            return error == null ? "no pixels" : error;
        }
        // Copy off the GL buffer now; encode and write on a worker thread, as
        // vanilla's ShotSave does, so a big exhibit does not stall a frame.
        byte[] rgb = new byte[w * h * 3];
        buffer.position(0);
        buffer.get(rgb, 0, Math.min(rgb.length, buffer.remaining()));
        Thread writer = new Thread(() -> writePng(rgb, w, h, file), "swhshot-" + file.getName());
        writer.setDaemon(true);
        writer.start();
        return null;
    }

    /** RGB rows bottom-up (GL order) to a top-down PNG. */
    private static void writePng(byte[] rgb, int w, int h, File file) {
        try {
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                int row = (h - 1 - y) * w * 3;
                for (int x = 0; x < w; x++) {
                    int i = row + x * 3;
                    image.setRGB(x, y, ((rgb[i] & 0xFF) << 16) | ((rgb[i + 1] & 0xFF) << 8) | (rgb[i + 2] & 0xFF));
                }
            }
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            ImageIO.write(image, "png", file);
        } catch (Throwable t) {
            System.err.println("[swhshots] could not write " + file + ": " + t);
        }
    }

    /**
     * The fallback: vanilla's own map screenshot of the same rectangle, saved
     * under vanilla's timestamped name in {@code <settings>/screenshots/}.
     */
    static String captureVanilla(Client client, Level level, Rectangle tiles) {
        try {
            Renderer.takeMapshot(level, client,
                    new GameCamera(tiles.x * 32, tiles.y * 32, tiles.width * 32, tiles.height * 32));
            return null;
        } catch (Throwable t) {
            return t.toString();
        }
    }
}
