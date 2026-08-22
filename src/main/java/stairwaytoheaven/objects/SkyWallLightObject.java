package stairwaytoheaven.objects;

import necesse.gfx.gameTexture.GameTexture;
import necesse.level.gameObject.WallTorchObject;

/**
 * Wall-mounted light with its own art — the vanilla WallCandleObject pattern
 * (walllantern): reuse WallTorchObject's attach/orientation/wire logic, only
 * swap the texture (64x128: 2 columns lit/unlit x 4 attach orientations).
 * Used for the Mistglass Lantern and the Flickerlight Garland.
 */
public class SkyWallLightObject extends WallTorchObject {

    private final String textureName;

    public SkyWallLightObject(String textureName, int lightLevel, float lightHue, float lightSat) {
        this.textureName = textureName;
        this.lightLevel = lightLevel;
        this.lightHue = lightHue;
        this.lightSat = lightSat;
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/" + this.textureName);
    }
}
