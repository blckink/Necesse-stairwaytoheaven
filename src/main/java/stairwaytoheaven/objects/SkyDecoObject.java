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
import necesse.inventory.lootTable.LootTable;
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
    /**
     * What a piece the WORLD placed gives when broken; null = its own item.
     * See {@link #setNaturalLoot}.
     */
    private LootTable naturalLoot;

    public SkyDecoObject(String textureName, int variantWidth, Color mapColor, Rectangle collision, String... category) {
        // The box goes through super(): GameObject(Rectangle) derives isSolid
        // and regionType from it (VERIFIED [jar] 1.3.3). Setting this.collision
        // afterwards left the tile OPEN for pathfinding while the box still
        // blocked bodies - settlers walked into standing region keys and
        // hung there, starving, until the piece was mined (2026-09-26).
        super(collision != null ? collision : new Rectangle());
        this.textureName = textureName;
        this.variantWidth = variantWidth;
        this.mapColor = mapColor;
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

    /**
     * Natural pieces break into their MATERIAL, placed ones into themselves.
     *
     * <p>Vanilla's own rule for anything that is both scenery and buildable
     * (CrystalClusterObject, CowSkeletonObject, SurfaceGrassObject,
     * CobwebObject -- jar 1.3.2, each {@code getLootTable}): if
     * {@code level.objectLayer.isPlayerPlaced} the object gives itself back,
     * otherwise it gives what it is made of, or nothing. Without it every
     * scree pile, crystal and dead tree worldgen scattered went into the
     * player's bag as a placeable OBJECT -- the "Grossteil als Objekte" of
     * the 2026-09-24 report (docs/ITEM_CATEGORIES.md). The flag is saved per
     * tile ({@code objectIsPlayerPlaced}, ArrayObjectLayer.java:90), so a piece
     * a player already built in an old save still comes back whole.
     *
     * <p>Since 2026-09-25 none of these pieces has a recipe any more, so the
     * placed-by-player exception is gone too: a leftover from an older save
     * can be set down and broken into its material, instead of sitting in the
     * bag for good (player: "liegen nur im Inventar rum").
     */
    public SkyDecoObject setNaturalLoot(LootTable naturalLoot) {
        this.naturalLoot = naturalLoot;
        return this;
    }

    @Override
    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        if (this.naturalLoot != null) {
            return this.naturalLoot;
        }
        return super.getLootTable(level, layerID, tileX, tileY);
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
