package stairwaytoheaven.objects;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.Mob;
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

/**
 * The War Veteran's Catapult ({@code veterancatapult}).
 *
 * <p>Sheet {@code objects/veterancatapult.png}: 4 rows (rotation: 0 up/north,
 * 1 right/east, 2 down/south, 3 left/west) x 4 columns (0 idle, 1-3 firing),
 * each cell 96x128. Row = {@code level.getObjectRotation(tileX, tileY)}
 * directly, same convention as {@link VeteranTurretObject}. If the delivered
 * texture is still the old 1-row sheet (height &lt; 4*128), row 0 is used
 * defensively.
 *
 * <p>CUT under the session's time limit: the true 3x3 {@code MultiTile}
 * footprint. Necesse's multi-tile registration (master/slave objects tied
 * together per rotation) is its own API surface this pass had no time left
 * to read and verify after the turret and the splash logic. This registers
 * as a SINGLE solid tile with an oversized, bottom-anchored sprite (the same
 * overhang idiom {@code SkyDecoObject} already uses) — it reads as a large
 * object and fires correctly, but a player can walk into the two tiles the
 * art implies rather than being blocked by them. A later pass should give it
 * a real {@code MultiTile} before calling this shippable as a true 3x3.
 */
public class VeteranCatapultObject extends GameObject {

    public GameTexture texture;
    private static final int FRAME_W = 96;
    private static final int FRAME_H = 128;

    public VeteranCatapultObject() {
        this.mapColor = new Color(110, 95, 70);
        this.isLightTransparent = true;
        this.isSolid = true;
        this.objectHealth = 250;
        this.collision = new Rectangle(-32, 0, 64, 32);
        this.setItemCategory("objects", "decorations");
        this.setCraftingCategory("objects", "decorations");
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/veterancatapult");
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new VeteranCatapultObjectEntity(level, this.getStringID(), x, y);
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, Level level,
            int tileX, int tileY, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (this.texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(tileX, tileY);
        int frame = 0;
        Mob hostile = stairwaytoheaven.objects.VeteranTurretObjectEntity.findNearestHostile(
                level, tileX, tileY, VeteranCatapultObjectEntity.CATAPULT_RANGE_PX);
        if (hostile != null) {
            frame = 1 + (int) ((System.currentTimeMillis() / 250L) % 3L);
        }
        int rotation = level.getObjectRotation(tileX, tileY) & 3;
        int row = this.texture.getHeight() >= 4 * FRAME_H ? rotation : 0;
        int drawX = camera.getTileDrawX(tileX) - (FRAME_W - 32) / 2;
        int drawY = camera.getTileDrawY(tileY) - FRAME_H + 32;
        final TextureDrawOptionsEnd options = this.texture.initDraw()
                .section(frame * FRAME_W, (frame + 1) * FRAME_W, row * FRAME_H, (row + 1) * FRAME_H)
                .light(light)
                .pos(drawX, drawY);
        list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
                return 16;
            }

            @Override
            public void draw(TickManager tickManager) {
                options.draw();
            }
        });
    }

    @Override
    public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
        if (this.texture == null) {
            return;
        }
        int row = this.texture.getHeight() >= 4 * FRAME_H ? (rotation & 3) : 0;
        int drawX = camera.getTileDrawX(tileX) - (FRAME_W - 32) / 2;
        int drawY = camera.getTileDrawY(tileY) - FRAME_H + 32;
        this.texture.initDraw()
                .section(0, FRAME_W, row * FRAME_H, (row + 1) * FRAME_H)
                .alpha(alpha)
                .draw(drawX, drawY);
    }
}
