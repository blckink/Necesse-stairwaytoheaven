package stairwaytoheaven.arsenal;

import java.io.FileNotFoundException;
import java.io.IOException;

import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;

/**
 * A vanilla sprite, re-inked in the Skyreach's white and gold at load time.
 *
 * <h2>Why this exists</h2>
 * {@code Server.jar} ships no PNGs, so no vanilla weapon sprite can be
 * recoloured offline and committed — and committing a recoloured copy of the
 * game's art would be redistributing it anyway. The CLIENT has every one of
 * them, though, and a mod's {@code loadTextures} runs there. So an item whose
 * look is "that vanilla weapon, but white and gold" reads the vanilla file at
 * texture-load time and builds its own texture out of it, pixel by pixel.
 *
 * <h2>How the file is read — and why not through {@code GameTexture.fromFile}</h2>
 * {@code GameTexture.fromFile} caches by path in {@code loadedTextures} and
 * hands back the SAME object vanilla draws from (GameTexture.java:150-160,
 * VERIFIED [jar]). Reading pixels off that one means un-finalising vanilla's
 * own texture ({@code ensureNotFinal} → {@code restoreFinal}, a
 * {@code glGetTexImage} round trip, :1070-1091) and leaving it that way. So
 * this goes one step lower, to the call {@code fromFile} itself makes on a
 * cache miss: {@code ResourceEncoder.getResourceBytes(path)} (:298 in the file
 * constructor; ResourceEncoder.java:82-87, which throws
 * {@code FileNotFoundException} for an unknown path). Those bytes are decoded
 * by {@code new GameTexture(String, byte[])} (:274-278) into a brand-new,
 * un-finalised texture that nothing else holds. Vanilla's cryo glaive is never
 * touched and stays blue for every other player of it.
 *
 * <h2>The recolour</h2>
 * A gradient map, not a hue shift. A hue shift ({@code livestock/SkyPelt})
 * keeps saturation and would turn a cyan blade into a saturated YELLOW one;
 * the brief is white and gold, which means the brightest pixels have to lose
 * their colour almost entirely while the darker ones gain it. So every opaque
 * pixel's luminance is placed on the sprite's OWN luminance range (2nd to 98th
 * percentile, so one stray highlight or outline pixel cannot flatten the
 * rest), and that position picks a colour off one ramp:
 *
 * <pre>
 *   0.00  deep bronze   outlines and the darkest shading
 *   0.20  old gold      shadowed metal
 *   0.38  gold          fittings, mid-tones
 *   0.52  pale gold     the transition
 *   0.64  cream         the lit body
 *   0.80  ivory
 *   1.00  white         highlights
 * </pre>
 *
 * Light-to-dark ORDER is what carries a pixel sprite's form, and a monotonic
 * ramp keeps it exactly; alpha is copied unchanged, so the silhouette and every
 * anti-aliased edge are the vanilla artist's. Because the ramp is read off the
 * sprite's own range rather than absolute brightness, the same call works on a
 * light cyan glaive and on a dark red lance without per-sprite tuning.
 *
 * <p><b>Status.</b> The file access and pixel API are VERIFIED [jar] (read out
 * of the decompiled 1.3.2 client classes named above). What the result LOOKS
 * like on the real vanilla art is a HYPOTHESIS: the server install has no
 * sprites, and {@code tools/recolour_preview.py} can only run this same ramp
 * over the mod's own sprites, or over a vanilla dump when one is supplied.
 *
 * <p>Client-only: every caller runs from an {@code Item.load*Texture(s)}
 * override, which a dedicated server never calls. {@code ResourceEncoder}
 * throws {@code IllegalStateException} before its resources are loaded, which
 * is the loud failure wanted if that ever changes.
 */
public final class RecolouredVanillaTexture {

    /** The ramp, as {position, r, g, b}. See the class doc. */
    private static final int[][] WHITE_GOLD = {
            {0, 78, 52, 24},
            {20, 146, 98, 38},
            {38, 212, 160, 62},
            {52, 238, 204, 120},
            {64, 250, 238, 206},
            {80, 255, 251, 240},
            {100, 255, 255, 255},
    };

    private RecolouredVanillaTexture() {
    }

    /**
     * The vanilla texture at {@code vanillaPath}, re-inked white and gold.
     * Finalised: it is only ever drawn.
     *
     * @param vanillaPath resource path without extension, e.g.
     *                    {@code "player/weapons/cryoglaive"}
     * @param debugName   the new texture's name in engine debug output
     * @throws FileNotFoundException if the game has no such file — callers that
     *         mirror an optional vanilla texture ({@code Item.loadAttackTexture})
     *         treat that exactly as vanilla does and fall back to null
     */
    public static GameTexture whiteGold(String vanillaPath, String debugName) throws FileNotFoundException {
        byte[] bytes;
        try {
            bytes = ResourceEncoder.getResourceBytes(vanillaPath + ".png");
        } catch (FileNotFoundException missing) {
            throw missing;
        } catch (IOException broken) {
            FileNotFoundException wrapped = new FileNotFoundException(
                    "Could not read " + vanillaPath + ": " + broken.getMessage());
            wrapped.initCause(broken);
            throw wrapped;
        }
        GameTexture out = new GameTexture(debugName, bytes);
        gradientMap(out, WHITE_GOLD);
        return out.makeFinal();
    }

    /**
     * {@link #whiteGold}, or {@link GameTexture#fromFile}'s ERR tile when the
     * vanilla file is missing — for an item ICON, where the engine itself
     * would draw ERR on a miss rather than null.
     */
    public static GameTexture whiteGoldOrError(String vanillaPath, String debugName) {
        try {
            return whiteGold(vanillaPath, debugName);
        } catch (FileNotFoundException missing) {
            return GameTexture.fromFile(vanillaPath);
        }
    }

    /** Remaps every opaque pixel of {@code texture} in place. */
    static void gradientMap(GameTexture texture, int[][] ramp) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        int[] histogram = new int[256];
        int opaque = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (texture.getAlpha(x, y) == 0) {
                    continue;
                }
                histogram[luma(texture, x, y)]++;
                opaque++;
            }
        }
        if (opaque == 0) {
            return;
        }
        int lo = percentile(histogram, opaque, 0.02F);
        int hi = percentile(histogram, opaque, 0.98F);
        float span = Math.max(1, hi - lo);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int alpha = texture.getAlpha(x, y);
                if (alpha == 0) {
                    continue;
                }
                float t = (luma(texture, x, y) - lo) / span;
                int[] rgb = sample(ramp, t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t));
                texture.setPixel(x, y, rgb[0], rgb[1], rgb[2], alpha);
            }
        }
    }

    /** Rec. 601 luma, 0..255. */
    private static int luma(GameTexture texture, int x, int y) {
        int value = Math.round(0.299F * texture.getRed(x, y)
                + 0.587F * texture.getGreen(x, y)
                + 0.114F * texture.getBlue(x, y));
        return value < 0 ? 0 : (value > 255 ? 255 : value);
    }

    private static int percentile(int[] histogram, int total, float fraction) {
        int target = Math.round(total * fraction);
        int seen = 0;
        for (int value = 0; value < histogram.length; value++) {
            seen += histogram[value];
            if (seen > target) {
                return value;
            }
        }
        return histogram.length - 1;
    }

    /** Linear interpolation along the ramp; {@code t} in 0..1. */
    static int[] sample(int[][] ramp, float t) {
        float position = t * 100.0F;
        for (int i = 1; i < ramp.length; i++) {
            if (position <= ramp[i][0]) {
                int[] a = ramp[i - 1];
                int[] b = ramp[i];
                float f = (position - a[0]) / Math.max(1.0F, b[0] - a[0]);
                return new int[]{
                        Math.round(a[1] + (b[1] - a[1]) * f),
                        Math.round(a[2] + (b[2] - a[2]) * f),
                        Math.round(a[3] + (b[3] - a[3]) * f),
                };
            }
        }
        int[] last = ramp[ramp.length - 1];
        return new int[]{last[1], last[2], last[3]};
    }
}
