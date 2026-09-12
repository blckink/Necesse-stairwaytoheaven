package stairwaytoheaven.mobs;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.critters.CritterMob;
import necesse.entity.mobs.misc.NetableMob;
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
 *
 * <h2>All of them are netable</h2>
 * {@code NetableMob} sits on this base class rather than on one subclass,
 * because the whole class is "harmless ambient wildlife" and that is exactly
 * what a net is for. It is the entire vanilla catchability mechanism: the net
 * checks this marker and nothing else ({@code NetToolItem.canHitMob} is
 * {@code mob instanceof NetableMob}, jar NetToolItem.java:47), then removes the
 * mob through the normal death path so the loot table still applies.
 *
 * <p>Vanilla's own net does this. The mod adds no net and no net recipe of its
 * own -- see the note in {@code livestock/SkyLivestock.registerItems}.
 */
public abstract class SkyCritterMob extends CritterMob implements NetableMob {

    public static GameTexture mothTexture;
    public static GameTexture beetleTexture;
    public static GameTexture finchTexture;
    public static GameTexture snailTexture;

    /**
     * Every critter here is netable, so every table below drops something with
     * certainty. A net catch is just {@code target.remove(0, 0, attacker,
     * true)} -- the mob's loot table is the entire reward -- so a purely
     * chance-based table reads as the animal vanishing for nothing. That is a
     * playtest report this mod has already received once, about the snail.
     *
     * <p>These three used to be 0.4F rolls, from when they could only be hunted
     * and not caught. The roll is now gone rather than kept as a bonus on top:
     * unlike the snail and the Stripe Beetle, these three have no item OF
     * THEMSELVES to hand over, so the material has to be the catch reward
     * itself. One unit, which is what the roll paid out when it paid at all.
     */
    public static final LootTable mothLoot = new LootTable(
            LootItem.between("aurorapetal", 1, 1));
    public static final LootTable beetleLoot = new LootTable(
            LootItem.between("stormshard", 1, 1));
    public static final LootTable finchLoot = new LootTable(
            LootItem.between("windsilk", 1, 1));
    /**
     * The snail has an item of itself, so it drops that with certainty and
     * keeps the shard as a bonus roll on top -- the shape vanilla's netted
     * critters use (FireflyMob's table is one unconditional LootItem).
     */
    public static final LootTable snailLoot = new LootTable(
            LootItem.between("dewsnail", 1, 1),
            new ChanceLootItemList(0.35F, LootItem.between("prismshard", 1, 1)));

    private final int kind; // 0 = lamb, 1 = moth, 2 = beetle, 3 = finch, 4 = snail

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
        switch (this.kind) {
            case 1: return mothTexture;
            case 3: return finchTexture;
            case 4: return snailTexture;
            default: return beetleTexture;
        }
    }

    @Override
    public LootTable getLootTable() {
        switch (this.kind) {
            case 1: return mothLoot;
            case 3: return finchLoot;
            case 4: return snailLoot;
            default: return beetleLoot;
        }
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

    /** Zephyr Finch — a tiny darting meadow bird (Driftlands, v0.4). */
    public static class ZephyrFinch extends SkyCritterMob {
        public ZephyrFinch() {
            super(3, 15, 34.0F);
        }
    }

    /**
     * Dew Snail — a slow glowing snail of the shoals (Aurora, v0.4). Netable
     * like every other critter here; the marker is on the base class.
     */
    public static class DewSnail extends SkyCritterMob {
        public DewSnail() {
            super(4, 25, 8.0F);
        }
    }
}
