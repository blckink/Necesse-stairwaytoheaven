package stairwaytoheaven.mobs;

import java.awt.Point;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.CrystalArmadillo;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * Crooked Armadillo — the Beetle Outlands' charger: armour 60 while it walks,
 * then it rolls up and comes at you at speed 200.
 *
 * <p><b>Vanilla parent: {@link CrystalArmadillo}. Only the texture changed.</b>
 * Every number and behaviour is inherited untouched — 400 HP, armour 60 walking
 * and 40 balled, the 90-damage collision hit that only applies while balled, the
 * knockback swing from 0.0 to 1.5, the dust puffs it kicks up rolling, the
 * zero stopping distance, and the AI node that balls it up the moment it has a
 * chaser target. Nothing here is a rebalance; the ladder those numbers sit on is
 * {@code docs/BALANCE.md}.
 *
 * <h2>Why a class exists at all</h2>
 * The art is now ours ({@code mobs/crookedarmadillo.png}), and the only way to
 * put it on the mob is this subclass: vanilla resolves the sheet from the static
 * field {@code MobRegistry.Textures.crystalArmadillo}, read inline inside
 * {@code addDrawables}, and {@code Mob} exposes no per-instance texture hook
 * (VERIFIED [jar]). Assigning into that static field would repaint vanilla's own
 * armadillos in vanilla's own crystal caves, so {@link #addDrawables} is
 * overridden instead and samples {@link #texture}.
 *
 * <p><b>Why the override does not call {@code super}.</b> It cannot:
 * {@code super.addDrawables} IS the vanilla body draw. Nothing is lost by
 * skipping it — the {@code super.addDrawables(...)} line at the top of
 * {@code CrystalArmadillo.addDrawables} reaches {@code Mob.addDrawables}, whose
 * body is EMPTY (VERIFIED [jar]).
 *
 * <p><b>The one thing not ported: the glow pass.</b> Vanilla draws this mob
 * TWICE — the body sheet at raw ambient light, then
 * {@code crystalArmadillo_light} over it at a {@code minLevelCopy(100)} floor,
 * which is a scattered half-body dither mask that makes its crystal shell shine
 * in the dark (VERIFIED [jar]: 22,996 of the body's 50,720 opaque pixels). We
 * have one sheet, not two, and ours is a bone-white shell rather than crystal,
 * so the second pass is dropped instead of being faked by drawing our own body
 * over itself.
 *
 * <p><b>The light floor is NOT dropped with it.</b> That overlay is where
 * vanilla's armadillo keeps its entire minimum-light guarantee: the body pass
 * alone is {@code .light(light)}, so without the overlay the mob would be
 * invisible at ambient 0 — and these are dark-spawners in a level that follows
 * the day/night cycle, so a 200-speed charger doing 90 on contact would arrive
 * unseen while the two golems beside it stayed lit. The single pass here
 * therefore carries {@code minLevelCopy(100.0F)}, the same floor the overlay
 * had and the same one {@code CrystalGolemMob} puts on its own single pass. It
 * is not pixel-identical to vanilla — ours lights the whole animal where vanilla
 * lights 45% of it in a dither — but it is much closer to vanilla's outcome
 * than darkness is. Drawing {@code mobs/crookedarmadillo_light.png} and adding
 * the second {@code DrawOptions} restores the exact behaviour.
 *
 * <p>{@link #spawnDeathParticles} is overridden for the same reason
 * {@code addDrawables} is: the gibs are cut out of the mob's own sheet, so on
 * vanilla's texture ours would shatter into crystal shards. The artist drew the
 * gib strip in the same place vanilla keeps it (32 px row 8, columns 0-3), so
 * the sprite indices are unchanged — including vanilla's {@code nextInt(5)},
 * which picks the empty fifth column one time in five on vanilla's sheet too.
 */
public class CrookedArmadilloMob extends CrystalArmadillo {

    /**
     * Our sheet, filled by {@code SkyMobs.loadTextures} on the client only —
     * same pattern as {@link GloomShadeMob#texture}. It stays null on a
     * dedicated server, which never draws, hence the guards below.
     */
    public static GameTexture texture;

    /**
     * Vanilla's {@code CrystalArmadillo.spawnDeathParticles}, verbatim, with our
     * sheet in place of {@code MobRegistry.Textures.crystalArmadillo}.
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
     * Vanilla's {@code CrystalArmadillo.addDrawables}, ported line for line: the
     * same draw offsets (-32 / -51), the same animation frame, the same rolling
     * frames (columns 6 and 7, alternating every 100 ms of local time) while
     * balled, the same enemy tracker and the same shadow pass. Two things are
     * ours: the {@link GameTexture} being sampled, and the {@code minLevelCopy}
     * floor that moved onto this pass when the glow pass it used to live on was
     * dropped — both explained in the class comment.
     *
     * <p>This mob has no swim mask in vanilla and gets none here.
     */
    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList,
            Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 22 - 10;
        int drawY = camera.getDrawY(y) - 44 - 7;
        int dir = this.getDir();
        Point sprite = this.getAnimSprite(x, y, dir);
        drawY += this.getBobbing(x, y);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);

        if (this.isBall) {
            sprite.x = 6 + (int) (this.getLevel().getWorldEntity().getLocalTime() / 100L % 2L);
        }

        final DrawOptions drawOptions = texture
                .initDraw()
                .sprite(sprite.x, sprite.y, 64)
                .startGlowOptions(level, (long) this.getID())
                .light(light.minLevelCopy(100.0F))
                .applyEnemyTracker(this, perspective)
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
