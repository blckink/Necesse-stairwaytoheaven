package stairwaytoheaven.objects;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * Generic bottom-anchored static deco object (Gloomwillow, Warden beacon,
 * sky anchor, ...). One-tile footprint, tall sprite, seeded per-tile variant
 * pick — the SingleRockObject drawing idiom with configurable variant width.
 */
public class SkyDecoObject extends GameObject {

    private final String textureName;
    private final int variantWidth;
    public GameTexture texture;
    private final GameRandom drawRandom = new GameRandom();

    public SkyDecoObject(String textureName, int variantWidth, Color mapColor, Rectangle collision, String... category) {
        this.textureName = textureName;
        this.variantWidth = variantWidth;
        this.mapColor = mapColor;
        if (collision != null) {
            this.collision = collision;
        }
        this.isLightTransparent = true;
        if (category.length > 0) {
            this.setItemCategory(category);
            this.setCraftingCategory(category);
        } else {
            this.setItemCategory("objects", "decorations");
            this.setCraftingCategory("objects", "decorations");
        }
    }

    public SkyDecoObject setLight(int level, float hue, float sat) {
        this.lightLevel = level;
        this.lightHue = hue;
        this.lightSat = sat;
        this.roomProperties.add("lights");
        return this;
    }

    public SkyDecoObject setTool(ToolType toolType) {
        this.toolType = toolType;
        return this;
    }

    public SkyDecoObject setObjectHealth(int objectHealth) {
        this.objectHealth = objectHealth;
        return this;
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/" + this.textureName);
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, Level level,
            int tileX, int tileY, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (this.texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(tileX, tileY);
        int variants = Math.max(1, this.texture.getWidth() / this.variantWidth);
        int variant;
        synchronized (this.drawRandom) {
            variant = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(variants);
        }
        int drawX = camera.getTileDrawX(tileX) - this.variantWidth / 2 + 16;
        int drawY = camera.getTileDrawY(tileY) - this.texture.getHeight() + 32;
        final TextureDrawOptionsEnd options = this.texture
                .initDraw()
                .section(variant * this.variantWidth, (variant + 1) * this.variantWidth, 0, this.texture.getHeight())
                .light(light)
                .pos(drawX, drawY);
        list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
                return 16;
            }

            @Override
            public void draw(TickManager tickManager) {
                options.draw();
            }
        });
    }

    @Override
    public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
        if (this.texture == null) {
            return;
        }
        int drawX = camera.getTileDrawX(tileX) - this.variantWidth / 2 + 16;
        int drawY = camera.getTileDrawY(tileY) - this.texture.getHeight() + 32;
        this.texture.initDraw()
                .section(0, this.variantWidth, 0, this.texture.getHeight())
                .alpha(alpha)
                .draw(drawX, drawY);
    }
}
