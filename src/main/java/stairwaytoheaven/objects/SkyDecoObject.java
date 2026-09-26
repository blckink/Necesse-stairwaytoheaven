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
    private int cellHeight;
    private int[] cells;
    private int[] scalePercents;
    private boolean randomMirror;

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
    /**
     * For sheets that stack several whole pictures in rows, like vanilla's
     * {@code deadwood} and {@code willowtree} (128 px cells, one tree per
     * row): draw only a {@code cellHeight}-tall cell, picked per tile from
     * the given (column, row) pairs. Without this every variant is a
     * full-height column strip of the whole sheet.
     */
    public SkyDecoObject setCells(int cellHeight, int... colRowPairs) {
        this.cellHeight = cellHeight;
        this.cells = colRowPairs;
        return this;
    }

    /**
     * Draw each placed piece at one of these sizes (percent of the sheet),
     * picked per tile, and mirror half of them. For big borrowed sheets with
     * few variants, like vanilla's 128 px {@code deadwood}: at full size and
     * unmirrored a grove was four trees repeated, each four tiles wide
     * (player, 2026-09-27: "viel zu fett und identisch alle"). The foot stays
     * on the tile; only the picture shrinks.
     */
    public SkyDecoObject setVariety(boolean randomMirror, int... scalePercents) {
        this.randomMirror = randomMirror;
        this.scalePercents = scalePercents.length > 0 ? scalePercents : null;
        return this;
    }

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
        int height = this.cellHeight > 0 ? this.cellHeight : this.texture.getHeight();
        int variants = this.cells != null
                ? this.cells.length / 2
                : Math.max(1, this.texture.getWidth() / this.variantWidth);
        int variant;
        boolean mirror;
        int percent;
        synchronized (this.drawRandom) {
            GameRandom random = this.drawRandom.seeded(getTileSeed(tileX, tileY));
            variant = random.nextInt(variants);
            mirror = this.randomMirror && random.nextBoolean();
            percent = this.scalePercents != null
                    ? this.scalePercents[random.nextInt(this.scalePercents.length)]
                    : 100;
        }
        int col = this.cells != null ? this.cells[variant * 2] : variant;
        int row = this.cells != null ? this.cells[variant * 2 + 1] : 0;
        int width = this.variantWidth * percent / 100;
        int drawHeight = height * percent / 100;
        int drawX = camera.getTileDrawX(tileX) - width / 2 + 16;
        int drawY = camera.getTileDrawY(tileY) - drawHeight + 32;
        final TextureDrawOptionsEnd options = this.texture
                .initDraw()
                .section(col * this.variantWidth, (col + 1) * this.variantWidth, row * height, (row + 1) * height)
                .light(light)
                .mirror(mirror, false)
                .size(width, drawHeight)
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
        int height = this.cellHeight > 0 ? this.cellHeight : this.texture.getHeight();
        int col = this.cells != null ? this.cells[0] : 0;
        int row = this.cells != null ? this.cells[1] : 0;
        int drawY = camera.getTileDrawY(tileY) - height + 32;
        this.texture.initDraw()
                .section(col * this.variantWidth, (col + 1) * this.variantWidth, row * height, (row + 1) * height)
                .alpha(alpha)
                .draw(drawX, drawY);
    }
}
