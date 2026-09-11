package stairwaytoheaven;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import necesse.engine.GameLog;
import necesse.engine.input.controller.ControllerGlyphCollections;

/**
 * Loads every vanilla controller glyph texture once, on the thread that owns
 * the GL context.
 *
 * Vanilla 1.3.3 loads {@code ControllerGlyphCollections.Glyph} textures lazily.
 * The first caller can be a level draw worker ({@code level-CLIENT-surface-draw-N}):
 * the "Getting Started" objective arrow draws its text there, the text holds a
 * control-key glyph, and {@code GameTexture.makeFinal} then calls
 * {@code glGenTextures} with no context current. LWJGL aborts the JVM on the
 * spot — no crash log, the game window just closes. Seen 2026-09-11 in a fresh
 * SplitRoast world right after the first tree was chopped, which advances the
 * objective to a step whose text shows a gamepad button.
 *
 * Once a glyph holds its texture, the lazy path returns it and never touches GL.
 */
public final class ControllerGlyphPreload {

    private ControllerGlyphPreload() {
    }

    public static void preload() {
        int loaded = 0;
        int failed = 0;
        for (Field field : ControllerGlyphCollections.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            Object value;
            try {
                value = field.get(null);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (value instanceof ControllerGlyphCollections.GlyphCollection) {
                ControllerGlyphCollections.GlyphCollection c = (ControllerGlyphCollections.GlyphCollection) value;
                // Only these three are ever asked for by GLFWControllerBind; the
                // Switch and Steam Deck entries are partly empty paths.
                if (load(c.xbox)) loaded++; else failed++;
                if (load(c.playStation4)) loaded++; else failed++;
                if (load(c.playStation5)) loaded++; else failed++;
            } else if (value instanceof ControllerGlyphCollections.Flair) {
                try {
                    ((ControllerGlyphCollections.Flair) value).getTexture();
                    loaded++;
                } catch (Throwable e) {
                    failed++;
                }
            } else if (value instanceof Map) {
                for (Object glyph : ((Map<?, ?>) value).values()) {
                    if (!(glyph instanceof ControllerGlyphCollections.Glyph)) continue;
                    if (load((ControllerGlyphCollections.Glyph) glyph)) loaded++; else failed++;
                }
            }
        }
        GameLog.out.println("Stairway to Heaven: preloaded " + loaded + " controller glyphs (" + failed + " failed)");
    }

    private static boolean load(ControllerGlyphCollections.Glyph glyph) {
        try {
            return glyph.getTexture() != null;
        } catch (Throwable e) {
            return false;
        }
    }
}
