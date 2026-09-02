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
 * Eden's enemies wears a VANILLA sprite sheet, and the obvious route — subclass
 * the vanilla mob that owns the sheet — only works when that mob is a hostile
 * with a per-instance damage field. Three of the five fail that test: the
 * crocodile is a {@code FriendlyMob} that turns hostile only when hit, the bee
 * is a summon follower, and the dragon whelp is a pet. So each Eden mob is its
 * own {@code HostileMob} and this class carries the one piece they would
 * otherwise each copy: the draw, which is lifted verbatim from the vanilla mob
 * that owns the sheet (offsets, cell size and all), so a borrowed sheet is
 * drawn exactly the way the game draws it.
 *
 * <p><b>How the sheet is found.</b> {@code GameTexture.fromFile("mobs/crocodile")}
 * from mod code resolves the GAME's own file. VERIFIED [jar]: there is one flat
 * {@code resources.files} map keyed by path ({@code ResourceEncoder.java:75-86})
 * with the mod's resources merged into it, and {@code GameTexture.fromFile}
 * formats the path, checks {@code loadedTextures} and reads through that one map
 * ({@code GameTexture.java:167-199}). It is the same call that resolves
 * {@code mobs/cloudlamb}. Nothing is recoloured, cropped or otherwise derived —
 * see {@code docs/realms/eden.md} for the full borrowed-art table.
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
