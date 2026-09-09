package stairwaytoheaven.realms.eden;

import java.awt.Point;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.HostileMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/**
 * Shared base of the Garden of Eden's five dangers.
 *
 * <p><b>Why the roster is written rather than subclassed.</b> Every one of
 * Eden's enemies originally wore VANILLA sprite sheets. Each is still its own
 * {@code HostileMob}; this class retains the proven draw geometry (offsets,
 * cell size and direction rows), while {@link EdenRealm#loadTextures()} now
 * loads the five purpose-built Eden sheets.
 *
 * <p>The bespoke sheets preserve those exact source dimensions, so replacing
 * the temporary art did not require changing combat or animation code.
 *
 * <p>Textures load in {@link EdenRealm#loadTextures()}, i.e. from
 * {@code initResources()}, which a dedicated server never calls; every draw
 * path here null-checks, so a server that never loaded one cannot trip over it.
 */
public abstract class EdenHostileMob extends HostileMob {

    protected EdenHostileMob(int maxHealth) {
        super(maxHealth);
    }

    /** The borrowed sheet, or null on a server / before resources load. */
    protected abstract GameTexture sheet();

    /** Sprite cell size on that sheet — 128 for the crocodile, 32 for the bee. */
    protected abstract int spriteSize();

    /** X offset the vanilla owner draws with, usually {@code -spriteSize()/2}. */
    protected abstract int drawOffsetX();

    /** Y offset the vanilla owner draws with. */
    protected abstract int drawOffsetY();

    /**
     * Same rule the whole Eden roster uses: the garden is dangerous at noon,
     * and placed light still keeps a camp clear. See {@link EdenSpawnRules}.
     */
    @Override
    public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
        return EdenSpawnRules.gardenSpawn(this, server, client, targetX, targetY);
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList,
            OrderableDrawables topList, Level level, int x, int y, TickManager tickManager,
            GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        GameTexture texture = this.sheet();
        if (texture == null) {
            return;
        }
        int tileX = getTileCoordinate(x);
        int tileY = getTileCoordinate(y);
        GameLight light = level.getLightLevel(tileX, tileY);
        int drawX = camera.getDrawX(x) + this.drawOffsetX();
        int drawY = camera.getDrawY(y) + this.drawOffsetY();
        Point sprite = this.getAnimSprite(x, y, this.getDir());
        drawY += level.getTile(tileX, tileY).getMobSinkingAmount(this);
        final TextureDrawOptionsEnd options = texture
                .initDraw()
                .sprite(sprite.x, sprite.y, this.spriteSize())
                .startGlowOptions(this, (long) this.getID())
                .light(light)
                .applyEnemyTracker(this, perspective)
                .pos(drawX, drawY);
        list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
                options.draw();
            }
        });
        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }
}
