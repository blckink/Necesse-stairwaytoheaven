package stairwaytoheaven.realms.crooked;

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
 * Stripe Beetle — {@code WORLD_DESIGN.md} §14's catchable Crooked animal, and
 * the only thing in the realm that is not trying to kill you.
 *
 * <h2>Netable, and why that is the whole design</h2>
 * {@code NetableMob} is the entire vanilla catchability mechanism: the net
 * checks for exactly this marker interface and then removes the mob through the
 * normal death path, so the loot table still applies. Vanilla's butterflies and
 * bees work this way and so does the mod's Dew Snail.
 *
 * <p>The loot table therefore drops the Striped Shell <b>unconditionally</b>. A
 * chance-based table on a netted critter reads to the player as the animal
 * vanishing for nothing, which is a real playtest report this mod has already
 * received once (see {@code SkyCritterMob.snailLoot}). The shell is the reward
 * for the catch; the Warp Resin on top is the bonus roll.
 *
 * <h2>Art</h2>
 * <b>Borrowed sheet:</b> vanilla {@code mobs/scorpion.png}, 224x128 — seven
 * animation columns across four facing rows of 32px cells, which is exactly the
 * layout {@code getAnimSprite}/{@code sprite(x, y, 32)} expects. A hard-shelled,
 * many-legged, plated crawler: the closest thing the game already owns to a
 * beetle the size of a dinner plate. Recorded as a stand-in in
 * {@code docs/realms/crooked.md}.
 *
 * <p>The mod's own {@code mobs/sparkbeetle.png} was the other candidate and is
 * deliberately not used: it is the Stormveil's own animal, and two species
 * wearing one sheet in one mod is a bug report waiting to happen.
 */
public class StripeBeetleMob extends CritterMob implements NetableMob {

    /** Filled by {@link CrookedRealm#loadTextures()} on the client only. */
    public static GameTexture texture;

    /**
     * The catch is the reward, so the shell is certain.
     *
     * <p>Two entries and no more: A4.2 asks that a run leave the player still
     * wanting something specific, and a critter that hands out the realm's rare
     * material would undercut every crate and every guarded site in it.
     */
    public static final LootTable lootTable = new LootTable(
            LootItem.between("stripedshell", 1, 1),
            new ChanceLootItemList(0.35F, LootItem.between("warpresin", 1, 1)));

    public StripeBeetleMob() {
        // 30 HP: between the mod's Dew Snail (25) and its Spark Beetle (20)
        // scaled for a realm where everything is bigger. It is a critter, not a
        // rung of the ladder -- SkyMobTiers governs hostiles, and nothing that
        // cannot fight back belongs on it.
        super(30);
        this.setSpeed(14.0F);
        this.setFriction(3.0F);
        this.collision = new Rectangle(-7, -5, 14, 10);
        this.hitBox = new Rectangle(-9, -9, 18, 16);
        this.selectBox = new Rectangle(-10, -18, 20, 22);
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    /**
     * Same shape as {@code SkyCritterMob.addDrawables}: {@code super} first
     * (which is where the critter base does its own bookkeeping), then our sheet
     * on top, then the shared shadow pass.
     */
    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 16;
        int drawY = camera.getDrawY(y) - 26;
        Point sprite = this.getAnimSprite(x, y, this.getDir());
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
}
