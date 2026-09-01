package stairwaytoheaven.mobs;

import java.awt.Point;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.AscendedGolemMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * Rare Crooked Golem — the Beetle Outlands' wall: the rarest thing in the mix
 * and the hardest single body in the sky.
 *
 * <p><b>Vanilla parent: {@link AscendedGolemMob}. Only the texture changed.</b>
 * Everything else is inherited untouched — the 400/750/1000/1300/1800 health
 * getter, the magenta charge particles and warning beam, the ascended beam
 * projectile, the fade death sound and the ten-shard death burst, and all of
 * {@code CrystalGolemMob} underneath it. Nothing here is a rebalance; the ladder
 * those numbers sit on is {@code docs/BALANCE.md}.
 *
 * <p><b>Inherited quirk, kept on purpose.</b> {@code AscendedGolemMob.serverTick}
 * counts a {@code lifeTime} up in 50 ms steps and removes the mob once it passes
 * {@code deathTime} = 20000, so this golem walks off after twenty seconds
 * (VERIFIED [jar]) — vanilla built it as the Ascended Wizard's temporary summon.
 * That is exactly how the Outlands' {@code ascendedgolem} entry has behaved
 * since it shipped; changing it would be a rebalance, which this pass is not.
 *
 * <h2>Why a class exists at all</h2>
 * The art is now ours ({@code mobs/rarecrookedgolem.png}), and the only way to
 * put it on the mob is this subclass: vanilla resolves the sheet from the static
 * field {@code MobRegistry.Textures.ascendedGolem}, read inline inside
 * {@code addDrawables}, and {@code Mob} exposes no per-instance texture hook
 * (VERIFIED [jar]). Assigning into that static field would repaint the Ascended
 * Wizard's own summons, so {@link #addDrawables} is overridden instead and
 * samples {@link #texture}.
 *
 * <p><b>No {@code super} call, and vanilla makes none either.</b>
 * {@code AscendedGolemMob.addDrawables} does not call {@code super} — it has the
 * same problem this class does, because its own parent's version would draw the
 * crystal golem sheet. Nothing is lost: {@code Mob.addDrawables} has an EMPTY
 * body (VERIFIED [jar]).
 *
 * <p>{@code spawnDeathParticles} is NOT overridden here, unlike its two
 * siblings: {@code AscendedGolemMob} replaces the parent's sheet-cut gibs with
 * ten {@code ascendedParticle} shards, which sample a vanilla particle atlas
 * rather than the mob's own body, so there is no mob texture in it to swap.
 */
public class RareCrookedGolemMob extends AscendedGolemMob {

    /**
     * Our sheet, filled by {@code SkyMobs.loadTextures} on the client only —
     * same pattern as {@link GloomShadeMob#texture}. It stays null on a
     * dedicated server, which never draws, hence the guard below.
     */
    public static GameTexture texture;

    /**
     * Vanilla's {@code AscendedGolemMob.addDrawables}, ported line for line: the
     * same draw offsets (-32 / -57), the same animation frame, the same
     * attacking-frame snap to column 0, the same swim mask, the same
     * {@code minLevelCopy(150)} floor — one step brighter than the ordinary
     * golem's 100, which is what makes the rare one glow — the same enemy
     * tracker and the same shadow pass. Only the {@link GameTexture} being
     * sampled is ours.
     *
     * <p><b>One line deviates, on purpose.</b> Vanilla looks the sinking tile up
     * as {@code getLevel().getTile(x / 32, y / 32)}; this uses
     * {@code getTileCoordinate}, which is {@code x >> 5} (VERIFIED [jar]:
     * {@code GameMath.getTileCoordinate}). The two agree for every non-negative
     * coordinate and differ by one tile for negative ones, because {@code /}
     * truncates toward zero and {@code >>} floors. Vanilla never notices: its
     * summoned golem only ever stands in an island level, whose tiles start at
     * 0. The Skyreach is region-generated around a computed origin and is full
     * of negative coordinates — the integration test's own cats sit at
     * {@code -353,33} — so keeping vanilla's expression would import a latent
     * off-by-one into the one world where it actually fires, and it would read
     * as the golem sinking by the wrong neighbour's tile. Both siblings use the
     * {@code getTileCoordinate} form, which is also what
     * {@code CrystalGolemMob} itself uses.
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
                .light(light.minLevelCopy(150.0F))
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
