package stairwaytoheaven.mobs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.critters.CritterMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * The Skyreach's ambient wildlife: harmless critters with the vanilla wander
 * AI (CritterMob default), one species per sub-biome. They can despawn and be
 * hunted for a small forage drop, exactly like vanilla critters.
 *
 * Subclasses exist because MobRegistry needs a distinct no-arg-constructible
 * class per stringID (same pattern as SpireCatMob.Black/Tabby).
 */
public abstract class SkyCritterMob extends CritterMob {

    public static GameTexture mothTexture;
    public static GameTexture beetleTexture;

    public static final LootTable mothLoot = new LootTable(
            new ChanceLootItemList(0.4F, LootItem.between("aurorapetal", 1, 1)));
    public static final LootTable beetleLoot = new LootTable(
            new ChanceLootItemList(0.4F, LootItem.between("stormshard", 1, 1)));

    private final int kind; // 0 = lamb, 1 = moth, 2 = beetle

    protected SkyCritterMob(int kind, int health, float speed) {
        super(health);
        this.kind = kind;
        this.setSpeed(speed);
        this.setFriction(3.0F);
        this.collision = new Rectangle(-6, -4, 12, 8);
        this.hitBox = new Rectangle(-8, -8, 16, 14);
        this.selectBox = new Rectangle(-9, -16, 18, 20);
    }

    private GameTexture texture() {
        return this.kind == 1 ? mothTexture : beetleTexture;
    }

    @Override
    public LootTable getLootTable() {
        return this.kind == 1 ? mothLoot : beetleLoot;
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        GameTexture texture = this.texture();
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 16;
        int drawY = camera.getDrawY(y) - 26;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        final TextureDrawOptionsEnd drawOptions = texture
                .initDraw()
                .sprite(sprite.x, sprite.y, 32)
                .light(light)
                .pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                drawOptions.draw();
            }
        });
        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }

    /** Glowmoth — the pale moths Peanut chases over the Aurora Shoals. */
    public static class GlowMoth extends SkyCritterMob {
        public GlowMoth() {
            super(1, 15, 30.0F);
        }
    }

    /** Sparkbeetle — a slate beetle with a faint charge shimmer (Stormveil). */
    public static class SparkBeetle extends SkyCritterMob {
        public SparkBeetle() {
            super(2, 20, 22.0F);
        }
    }
}
