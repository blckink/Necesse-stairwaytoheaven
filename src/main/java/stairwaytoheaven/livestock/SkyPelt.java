package stairwaytoheaven.livestock;

import java.awt.Color;

import necesse.gfx.gameTexture.GameTexture;

/**
 * Runtime recolouring of VANILLA sprite sheets into sky palettes.
 *
 * <p>WHY THIS EXISTS. A mod texture and a vanilla texture are the same thing to
 * the engine: {@code GameTexture.fromFile} formats the path, looks it up in
 * {@code GameTexture.loadedTextures} and, on a miss, reads it through
 * {@code ResourceEncoder.getResourceBytes(path)} — ONE flat
 * {@code resources.files} map keyed by path (jar 1.3.2,
 * GameTexture.java:167-199 and ResourceEncoder.java:75-86). Mod resources are
 * merged into that map, which is how {@code mobs/cloudlamb} resolves; the same
 * call therefore resolves {@code mobs/cow} from mod code. So the Skyreach's
 * livestock can be drawn on vanilla's own farm-animal sheets, with the frame
 * layout, the matching shadow sheets and the flesh-particle cells the
 * renderers already expect, instead of on new art that would have to reproduce
 * all three.
 *
 * <p>Straight reuse would be confusing, though — a Nimbus Yak that is pixel for
 * pixel a cow IS a cow. So each sheet is recoloured: hue and saturation are
 * replaced and VALUE is kept, which preserves every bit of the original
 * shading and silhouette. That is the same technique vanilla itself uses at
 * load time to build a food's buff icon out of its item icon
 * (FoodConsumableItem.loadTextures, jar 1.3.2).
 *
 * <p>All of this is client-only: it runs from {@code initResources()}, which a
 * dedicated server never calls.
 */
public final class SkyPelt {

    private SkyPelt() {
    }

    /**
     * Recolour a vanilla sheet.
     *
     * <p>The result is deliberately NOT finalized. {@code makeFinal()} uploads
     * the pixels to the GPU and drops the buffer, so a finalized texture can
     * only be read back through a {@code glGetTexImage} round trip
     * ({@code GameTexture.restoreFinal}). Callers that only ever DRAW the sheet
     * call {@link GameTexture#makeFinal()} themselves; callers that hand it to
     * something which reads it again — a {@code FoodConsumableItem} builds its
     * buff icon out of its item icon and finalizes it afterwards — leave it
     * alone.
     *
     * @param vanillaPath resource path WITHOUT the extension, e.g. "mobs/cow"
     * @param debugName   name for the new texture (engine debug output only)
     * @param hue         target hue, 0..1 (the range {@link Color#RGBtoHSB} uses)
     * @param satFloor    saturation given even to originally grey/white pixels
     * @param satScale    how much of the original saturation is kept on top
     * @param briScale    multiplier on the original brightness
     * @param briAdd      constant added to brightness afterwards
     */
    public static GameTexture tint(String vanillaPath, String debugName, float hue,
                                   float satFloor, float satScale, float briScale, float briAdd) {
        // forceNotFinalize: a finalized texture has released its pixel buffer.
        // getPixel() would restore it (GameTexture.ensureNotFinal), but asking
        // for a readable one up front is what vanilla does when it needs to
        // read a sheet it is about to composite.
        GameTexture source = GameTexture.fromFile(vanillaPath, true);
        GameTexture out = new GameTexture(debugName, source.getWidth(), source.getHeight());
        float[] hsb = new float[3];
        for (int x = 0; x < source.getWidth(); x++) {
            for (int y = 0; y < source.getHeight(); y++) {
                int alpha = source.getAlpha(x, y);
                if (alpha == 0) {
                    continue;
                }
                Color.RGBtoHSB(source.getRed(x, y), source.getGreen(x, y), source.getBlue(x, y), hsb);
                float saturation = clamp(satFloor + hsb[1] * satScale);
                float brightness = clamp(hsb[2] * briScale + briAdd);
                Color tinted = new Color(Color.HSBtoRGB(hue, saturation, brightness));
                out.setPixel(x, y, tinted.getRed(), tinted.getGreen(), tinted.getBlue(), alpha);
            }
        }
        source.makeFinal();
        return out;
    }

    /** {@link #tint} for a sheet that is only ever drawn, never read again. */
    public static GameTexture tintFinal(String vanillaPath, String debugName, float hue,
                                        float satFloor, float satScale, float briScale, float briAdd) {
        return tint(vanillaPath, debugName, hue, satFloor, satScale, briScale, briAdd).makeFinal();
    }

    /**
     * A washed-out copy of an already recoloured sheet, for the "the player has
     * taken the crop off it" state of an animal whose vanilla archetype ships
     * no such frame. Vanilla has {@code sheep_sheared} and {@code ram_sheared};
     * it has nothing at all for a plucked bird.
     */
    public static GameTexture bleach(GameTexture source, String debugName,
                                     float satScale, float briAdd) {
        GameTexture out = new GameTexture(debugName, source.getWidth(), source.getHeight());
        float[] hsb = new float[3];
        for (int x = 0; x < source.getWidth(); x++) {
            for (int y = 0; y < source.getHeight(); y++) {
                int alpha = source.getAlpha(x, y);
                if (alpha == 0) {
                    continue;
                }
                Color.RGBtoHSB(source.getRed(x, y), source.getGreen(x, y), source.getBlue(x, y), hsb);
                Color washed = new Color(Color.HSBtoRGB(hsb[0], clamp(hsb[1] * satScale),
                        clamp(hsb[2] + briAdd)));
                out.setPixel(x, y, washed.getRed(), washed.getGreen(), washed.getBlue(), alpha);
            }
        }
        source.makeFinal();
        return out.makeFinal();
    }

    private static float clamp(float value) {
        return value < 0.0F ? 0.0F : (value > 1.0F ? 1.0F : value);
    }
}
