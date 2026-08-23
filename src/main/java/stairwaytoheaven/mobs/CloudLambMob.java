package stairwaytoheaven.mobs;

import java.awt.Point;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.SheepMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * The Cloudlamb — a REAL sky sheep, not a critter: extends the vanilla
 * SheepMob so ropes, feeding troughs, breeding, growing up and shearing all
 * work exactly like surface livestock (shearing yields vanilla wool). Only
 * the fleece and the kill loot are sky-flavored.
 *
 * Vanilla SheepMob draws through a private getTexture(), so the draw and
 * death-particle methods are overridden 1:1 with our textures (same sheet
 * layout as vanilla sheep.png: 6x4 walk grid + a 5th row of fleece chunks).
 * The vanilla sheep/lamb shadows are reused.
 */
public class CloudLambMob extends SheepMob {

    public static GameTexture texture;
    public static GameTexture shearedTexture;

    public static final LootTable cloudLambLoot = new LootTable(
            LootItem.between("rawmutton", 1, 2), LootItem.between("windsilk", 1, 2));

    private GameTexture bodyTexture() {
        GameTexture sheared = shearedTexture;
        return (this.hasWool() || sheared == null) ? texture : sheared;
    }

    @Override
    public LootTable getLootTable() {
        return !this.isGrown() ? new LootTable() : cloudLambLoot;
    }

    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        GameTexture t = bodyTexture();
        if (t == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            this.getLevel().entityManager.addParticle(
                    new FleshParticle(this.getLevel(), t, GameRandom.globalRandom.nextInt(5), 8, 32,
                            this.x, this.y, 10.0F, knockbackX, knockbackY),
                    Particle.GType.IMPORTANT_COSMETIC);
        }
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        GameTexture t = bodyTexture();
        if (t == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 22 - 10;
        int drawY = camera.getDrawY(y) - 44 - 7;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += this.getBobbing(x, y);
        GameTexture shadowTexture = this.isGrown() ? MobRegistry.Textures.sheep_shadow : MobRegistry.Textures.lamb_shadow;
        TextureDrawOptions shadow = shadowTexture.initDraw().sprite(0, dir, 64).light(light).pos(drawX, drawY);
        tileList.add(tm -> shadow.draw());
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
        final DrawOptions options = t
                .initDraw()
                .sprite(sprite.x, sprite.y, 64)
                .startGlowOptions(level, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .addMaskShader(swimMask)
                .pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                swimMask.use();
                options.draw();
                swimMask.stop();
            }
        });
    }
}
