package stairwaytoheaven.mobs;

import java.awt.Point;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.CrystalGolemMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * Crooked Golem — the Beetle Outlands' slow heavy bruiser.
 *
 * <p><b>Vanilla parent: {@link CrystalGolemMob}. Only the texture changed.</b>
 * Every number, ability and behaviour is inherited untouched — 500 HP, armour
 * 40, speed 20, the 130-damage charged beam with its 2000 ms wind-up and 200 ms
 * stick, the warning beam, the charge particles, the AI tree and its 544/320/384
 * ranges. Nothing here is a rebalance; the ladder those numbers sit on is
 * {@code docs/BALANCE.md}.
 *
 * <h2>Why a class exists at all</h2>
 * Until now the Outlands spawned vanilla's crystal golem by string ID, so it
 * wore vanilla's crystal-cave art in a biome that is neither. The art is now
 * ours ({@code mobs/crookedgolem.png}), and the ONLY way to put it on the mob is
 * this subclass: vanilla resolves the sheet from the static field
 * {@code MobRegistry.Textures.crystalGolem}, read inline inside
 * {@code addDrawables}, and {@code Mob} exposes no per-instance texture hook
 * (VERIFIED [jar]). Assigning into that static field would repaint vanilla's own
 * crystal golems in vanilla's own crystal caves, so {@link #addDrawables} is
 * overridden instead and samples {@link #texture}.
 *
 * <p><b>Why the override does not call {@code super}.</b> It cannot:
 * {@code super.addDrawables} IS the vanilla body draw, and calling it would put
 * vanilla's sprite back on top of ours. Vanilla's own {@code AscendedGolemMob}
 * has exactly this problem and solves it the same way, by not calling super.
 * Nothing is lost by that: the {@code super.addDrawables(...)} line at the top of
 * {@code CrystalGolemMob.addDrawables} reaches {@code Mob.addDrawables}, whose
 * body is EMPTY (VERIFIED [jar]) — health bars, status bars and rider drawables
 * are added by {@code Mob.addDrawablesLoop} around it, not by it.
 *
 * <p>{@link #spawnDeathParticles} is overridden for the same one reason: the
 * gibs are cut out of the mob's own sheet, so on vanilla's texture a Crooked
 * Golem would shatter into crystal shards. The artist drew the gib strip in the
 * same place vanilla keeps it (32 px row 8, columns 0-3), so the sprite indices
 * are unchanged — including vanilla's {@code nextInt(5)}, which picks the empty
 * fifth column one time in five on vanilla's sheet too.
 */
public class CrookedGolemMob extends CrystalGolemMob {

    /**
     * Our sheet, filled by {@code SkyMobs.loadTextures} on the client only —
     * same pattern as {@link GloomShadeMob#texture}. It stays null on a
     * dedicated server, which never draws, hence the guards below.
     */
    public static GameTexture texture;

    /**
     * Vanilla's {@code CrystalGolemMob.spawnDeathParticles}, verbatim, with our
     * sheet in place of {@code MobRegistry.Textures.crystalGolem}.
     */
    @Override
    public void spawnDeathParticles(float knockbackX, float knockbackY) {
        if (texture == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            this.getLevel().entityManager.addParticle(
                    new FleshParticle(this.getLevel(), texture, GameRandom.globalRandom.nextInt(5), 8, 32,
                            this.x, this.y, 20.0F, knockbackX, knockbackY),
                    Particle.GType.IMPORTANT_COSMETIC);
        }
    }

    /**
     * Vanilla's {@code CrystalGolemMob.addDrawables}, ported line for line: the
     * same draw offsets (-32 / -57), the same animation frame, the same
     * attacking-frame snap to column 0, the same swim mask, the same
     * {@code minLevelCopy(100)} floor that keeps the golem readable in the dark,
     * the same enemy tracker and the same shadow pass. Only the
     * {@link GameTexture} being sampled is ours.
     */
    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 22 - 10;
        int drawY = camera.getDrawY(y) - 44 - 7 - 6;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += this.getBobbing(x, y);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        if (this.isAttacking) {
            sprite.x = 0;
        }

        final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
        final DrawOptions drawOptions = texture
                .initDraw()
                .sprite(sprite.x, sprite.y, 64)
                .addMaskShader(swimMask)
                .startGlowOptions(level, (long) this.getID())
                .light(light.minLevelCopy(100.0F))
                .applyEnemyTracker(this, perspective)
                .pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                swimMask.use();
                drawOptions.draw();
                swimMask.stop();
            }
        });

        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }
}
