package stairwaytoheaven.showroom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;

import necesse.engine.GlobalData;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.StringParameterHandler;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.gfx.res.ResourceEncoder;
import necesse.gfx.res.ResourceFile;

/**
 * {@code /swhdumpsprites [all]} — writes the game's own sprites to
 * {@code <Necesse settings>/swh-sprites/}, paths preserved
 * ({@code objects/stonewall.png}, {@code tiles/grass.png}, ...), which is the
 * layout every offline tool in this repo reads through {@code NECESSE_SPRITES}
 * / {@code --vanilla} (size_audit, wall_render_preview, sky_map_render,
 * scene_preview, preset_render, locale_audit). The mod's own PNGs go to
 * {@code swh-sprites/_mod/} beside them, exactly as the game loaded them.
 *
 * <p>Default: every PNG under {@code objects/ tiles/ items/ mobs/ player/
 * buffs/ particles/} — every prefix {@code docs/VANILLA_ASSET_MAP.md} borrows
 * from. {@code all}: every PNG the game has.
 *
 * <p>Reads through {@code ResourceEncoder.getAllFiles()} /
 * {@code ResourceFile.loadBytes}, the same table {@code GameTexture.fromFile}
 * decodes from (VERIFIED [jar]). A dedicated server has no resource table,
 * which is why this is a client command. Running it is HYPOTHESIS until the
 * player has run it.
 */
public class DumpSpritesClientCommand extends ModularChatCommand {

    static final String[] PREFIXES = {"objects/", "tiles/", "items/", "mobs/", "player/", "buffs/", "particles/"};
    static final String MOD_ID = "stairwaytoheaven";

    private static volatile boolean running;

    public DumpSpritesClientCommand() {
        super("swhdumpsprites", "Writes the game's sprites to <settings>/swh-sprites for the offline renderers",
                PermissionLevel.USER, false,
                new CmdParameter("scope", new StringParameterHandler("borrows", "borrows", "all"), true));
    }

    @Override
    public void runModular(Client client, Server server, ServerClient serverClient,
                           Object[] args, String[] errors, CommandLog logs) {
        if (!ResourceEncoder.isLoaded()) {
            logs.add("No game resources are loaded here (a dedicated server has none).");
            return;
        }
        if (running) {
            logs.add(Localization.translate("misc", "swhshotsbusy"));
            return;
        }
        boolean all = args.length > 0 && "all".equalsIgnoreCase(String.valueOf(args[0]));
        File out = new File(GlobalData.appDataPath() + "swh-sprites");
        running = true;
        logs.add(Localization.translate("misc", "swhdumpstart", "path", out.getAbsolutePath()));
        Thread t = new Thread(() -> {
            String result;
            try {
                result = dump(out, all);
            } catch (Throwable e) {
                result = "dump failed: " + e;
                e.printStackTrace();
            } finally {
                running = false;
            }
            System.out.println("[swhdumpsprites] " + result);
            if (client.chat != null) client.chat.addMessage(result);
        }, "swhdumpsprites");
        t.setDaemon(true);
        t.start();
    }

    private static boolean wanted(String path, boolean all) {
        if (!path.endsWith(".png")) return false;
        if (all) return true;
        for (String p : PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    static String dump(File out, boolean all) throws IOException {
        // The mod's own files first, straight out of its jar: that is also the
        // list of resource paths the mod overrides in the game's table.
        Set<String> modPaths = new HashSet<>();
        int modCount = 0;
        LoadedMod self = null;
        for (LoadedMod m : ModLoader.getEnabledMods()) {
            if (MOD_ID.equals(m.id)) self = m;
        }
        if (self != null && self.jarFile != null) {
            Enumeration<JarEntry> entries = self.jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || !name.startsWith("resources/") || !name.endsWith(".png")) continue;
                String path = name.substring("resources/".length());
                modPaths.add(path);
                try (InputStream in = self.jarFile.getInputStream(e)) {
                    writeFile(new File(out, "_mod/" + path), readAll(in));
                    modCount++;
                }
            }
        }
        int vanilla = 0;
        List<String> overridden = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        StringBuilder manifest = new StringBuilder();
        for (Map.Entry<String, ResourceFile> e : ResourceEncoder.getAllFiles()) {
            String path = e.getKey();
            if (!wanted(path, all)) continue;
            if (modPaths.contains(path)) {
                // The table holds the mod's copy under this path; vanilla's
                // original is no longer reachable from inside the game.
                overridden.add(path);
                continue;
            }
            try {
                writeFile(new File(out, path), e.getValue().loadBytes(true));
                manifest.append(path).append('\n');
                vanilla++;
            } catch (IOException ex) {
                failed.add(path + " (" + ex.getMessage() + ")");
            }
        }
        StringBuilder notes = new StringBuilder();
        notes.append("# swhdumpsprites: ").append(vanilla).append(" vanilla PNG(s), ")
                .append(modCount).append(" mod PNG(s) in _mod/\n");
        notes.append("# scope: ").append(all ? "all" : String.join(" ", PREFIXES)).append('\n');
        for (String o : overridden) notes.append("# overridden by the mod (vanilla copy not dumped): ").append(o).append('\n');
        for (String f : failed) notes.append("# failed: ").append(f).append('\n');
        writeFile(new File(out, "MANIFEST.txt"), (notes + manifest.toString()).getBytes(StandardCharsets.UTF_8));
        return Localization.translate("misc", "swhdumpdone", "count", String.valueOf(vanilla),
                "mod", String.valueOf(modCount), "path", out.getAbsolutePath())
                + (failed.isEmpty() ? "" : " (" + failed.size() + " failed, see MANIFEST.txt)");
    }

    /** InputStream.readAllBytes is Java 9; the mod compiles to Java 8. */
    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[16384];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }

    private static void writeFile(File f, byte[] bytes) throws IOException {
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("cannot create " + parent);
        }
        try (OutputStream o = new FileOutputStream(f)) {
            o.write(bytes);
        }
    }
}
