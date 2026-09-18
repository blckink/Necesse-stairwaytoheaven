package stairwaytoheaven.objects;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.StaticMultiTile;

/**
 * The War Veteran's Catapult ({@code veterancatapult}), a real 3x3 multi-tile
 * object built the way vanilla's {@code StaticMultiObject} /
 * {@code BlacksmithStatueObject} are: nine registered pieces sharing one ID
 * array, the top-left piece is the master. Only the master carries the
 * firing entity and draws the sprite; the other eight only block.
 *
 * <p>Sheet {@code objects/veterancatapult.png}: 4 rows (rotation: 0 up/north,
 * 1 right/east, 2 down/south, 3 left/west) x 4 columns (0 idle, 1-3 firing),
 * each cell 96x128 — three tiles wide, the top 32 px overhang the footprint.
 * The footprint is square, so {@link StaticMultiTile} keeps it unrotated and
 * only the drawn row follows the placement rotation.
 */
public class VeteranCatapultObject extends GameObject {

    public static final int SIZE = 3;
    private static final int FRAME_W = 96;
    private static final int FRAME_H = 128;
    /** Each firing frame lasts this long after a shot (3 frames, then idle). */
    private static final long FRAME_MS = 250L;

    public GameTexture texture;
    private final int multiX;
    private final int multiY;
    private final int[] multiIDs;

    /**
     * The machine as drawn, in footprint pixels (0,0 = top-left tile of the
     * 3x3). Measured off the idle frame: the body covers x 15..79, y 27..94 of
     * the 96x96 footprint, i.e. the bottom two tile rows. The top row is bare
     * art, so the box starts at y=32 and leaves it open — blocking all nine
     * tiles would put a wall where the player sees floor. Vanilla insets the
     * same way ({@code BlacksmithStatueObject} blocks 80x54 of its 96x64).
     */
    private static final Rectangle FULL_COLLISION = new Rectangle(12, 32, 72, 64);

    /**
     * This piece's share of {@link #FULL_COLLISION}, in its own tile's
     * coordinates — the split {@code StaticMultiObject} does. The three
     * top-row pieces intersect the box at zero height, so they keep a
     * degenerate rectangle, {@code GameObject} leaves their {@code isSolid}
     * false and the bare top row stays walkable; the six pieces below block.
     */
    private static Rectangle pieceCollision(int multiX, int multiY) {
        Rectangle piece = FULL_COLLISION.intersection(new Rectangle(multiX * 32, multiY * 32, 32, 32));
        if (piece.width < 0) {
            piece.width = 0;
        }
        if (piece.height < 0) {
            piece.height = 0;
        }
        piece.x -= multiX * 32;
        piece.y -= multiY * 32;
        return piece;
    }

    private VeteranCatapultObject(int multiX, int multiY, int[] multiIDs) {
        // Through the constructor, not after it: GameObject reads the
        // rectangle to set isSolid and regionType and never looks again, so
        // assigning isSolid later left every piece walk-through.
        super(pieceCollision(multiX, multiY));
        this.multiX = multiX;
        this.multiY = multiY;
        this.multiIDs = multiIDs;
        this.mapColor = new Color(110, 95, 70);
        this.isLightTransparent = true;
        this.objectHealth = 250;
        this.setItemCategory("objects", "decorations");
        this.setCraftingCategory("objects", "decorations");
    }

    /** Registers the nine pieces; the master keeps the ID {@code veterancatapult}. */
    public static int[] register(float brokerValue) {
        int[] ids = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int i = y * SIZE + x;
                String id = i == 0 ? "veterancatapult" : "veterancatapult" + (i + 1);
                ids[i] = ObjectRegistry.registerObject(id, new VeteranCatapultObject(x, y, ids),
                        i == 0 ? brokerValue : 0.0F, i == 0);
            }
        }
        return ids;
    }

    private boolean isMaster() {
        return this.multiX == 0 && this.multiY == 0;
    }

    @Override
    public MultiTile getMultiTile(int rotation) {
        return new StaticMultiTile(this.multiX, this.multiY, SIZE, SIZE, this.isMaster(), this.multiIDs);
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/veterancatapult");
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return this.isMaster() ? new VeteranCatapultObjectEntity(level, this.getStringID(), x, y) : null;
    }

    /**
     * Solid to feet, open to shots — as in {@link VeteranTurretObject}. The
     * stone spawns at the footprint's own centre-top ({@code tileX * 32 + 48},
     * {@code tileY * 32 + 16}), so a projectile-blocking hull would drop every
     * shot the moment it left the arm.
     */
    @Override
    public List<Rectangle> getProjectileCollisions(Level level, int x, int y, int rotation) {
        return Collections.emptyList();
    }

    private int row(int rotation) {
        return this.texture.getHeight() >= 4 * FRAME_H ? (rotation & 3) : 0;
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, Level level,
            int tileX, int tileY, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (!this.isMaster() || this.texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(tileX + 1, tileY + 1);
        int frame = VeteranTurretObjectEntity.firingFrame(level, tileX, tileY, FRAME_MS);
        // The arm swings towards the target it last threw at; the placement
        // rotation is only the starting pose.
        int row = this.row(VeteranTurretObjectEntity.aimRow(level, tileX, tileY,
                level.getObjectRotation(tileX, tileY) & 3));
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY) - (FRAME_H - SIZE * 32);
        final TextureDrawOptionsEnd options = this.texture.initDraw()
                .section(frame * FRAME_W, (frame + 1) * FRAME_W, row * FRAME_H, (row + 1) * FRAME_H)
                .light(light)
                .pos(drawX, drawY);
        list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
                // Sort with the footprint's bottom row, not the master's top row.
                return (SIZE - 1) * 32 + 16;
            }

            @Override
            public void draw(TickManager tickManager) {
                options.draw();
            }
        });
    }

    @Override
    public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
        if (!this.isMaster() || this.texture == null) {
            return;
        }
        int row = this.row(rotation);
        int drawX = camera.getTileDrawX(tileX);
        int drawY = camera.getTileDrawY(tileY) - (FRAME_H - SIZE * 32);
        this.texture.initDraw()
                .section(0, FRAME_W, row * FRAME_H, (row + 1) * FRAME_H)
                .alpha(alpha)
                .draw(drawX, drawY);
    }
}
