package stairwaytoheaven.objects;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
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
 * The War Veteran's Auto Turret ({@code veteranturret}): 1x1, machine-gun
 * rate, hostile-only. Real targeting and damage live in
 * {@link VeteranTurretObjectEntity}; this class is the placed object and its
 * sprite.
 *
 * <p>Sheet {@code objects/veteranturret.png}: 4 rows (rotation: 0 up/north,
 * 1 right/east, 2 down/south, 3 left/west) x 4 columns (0 idle, 1-3 firing),
 * each cell 32x48. Row = {@code level.getObjectRotation(tileX, tileY)}
 * directly — no remap — per the mod's existing bed/clock rotation-column
 * convention (column/row 0 = back = facing away from viewer = north). If the
 * delivered texture is still the old 1-row sheet (height &lt; 4*48), row 0 is
 * used defensively so nothing crashes. Placement rotation itself needs no
 * extra code: Necesse's base {@code GameObject} already rotates on
 * placement via the rotate key for any object that doesn't set
 * {@code replaceRotations = false}, exactly like
 * {@code StormglassKilnObject}/{@code WindsilkLoomObject} in this mod.
 *
 * <p>The firing frames follow the server's real shots: the entity's synced
 * shot counter stamps each shot on the client (see
 * {@link VeteranTurretObjectEntity#firingFrame}).
 */
public class VeteranTurretObject extends GameObject {

    public GameTexture texture;
    /** Cells follow vanilla's makeshiftturret proportions: the gun overhangs its one tile sideways. */
    private static final int FRAME_W = 64;
    private static final int FRAME_H = 64;

    public VeteranTurretObject() {
        // The collision has to go through the constructor: GameObject derives
        // isSolid and regionType from it right there (line 121-123 of the
        // decompiled class), so a later `isSolid = true` leaves the object
        // walk-through and its region OPEN — which is exactly what happened.
        // Box = the mount as drawn on its own tile (sheet rows below: the body
        // covers tile-local y 0..26 and overhangs sideways; the overhang is
        // not blocked, only the tile itself).
        super(new Rectangle(2, 4, 28, 24));
        this.mapColor = new Color(90, 90, 80);
        this.isLightTransparent = true;
        this.objectHealth = 150;
        // A settlement DEFENCE, sold by the War Veteran: vanilla files its
        // own defences under objects.traps. The crafting tree has no traps
        // node, so that side is objects.misc. docs/ITEM_CATEGORIES.md.
        this.setItemCategory("objects", "traps");
        this.setCraftingCategory("objects", "misc");
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/veteranturret");
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new VeteranTurretObjectEntity(level, this.getStringID(), x, y);
    }

    /**
     * Solid to feet, open to shots — vanilla's {@code AscendedPylonObject}
     * makes the same split. A defensive emplacement that blocked projectiles
     * would swallow its own cosmetic tracer (it spawns inside this very tile)
     * and every arrow the settlers fire from behind it.
     */
    @Override
    public List<Rectangle> getProjectileCollisions(Level level, int x, int y, int rotation) {
        return Collections.emptyList();
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, Level level,
            int tileX, int tileY, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        if (this.texture == null) {
            return;
        }
        GameLight light = level.getLightLevel(tileX, tileY);
        // Shots come every 200 ms: three 60 ms frames of muzzle flash, then idle.
        int frame = VeteranTurretObjectEntity.firingFrame(level, tileX, tileY, 60L);
        // Facing follows the last target, not the placement rotation: the
        // turret turns towards what it shoots. Until it has seen anything it
        // keeps the rotation it was built with.
        int rotation = VeteranTurretObjectEntity.aimRow(level, tileX, tileY,
                level.getObjectRotation(tileX, tileY) & 3);
        int row = this.texture.getHeight() >= 4 * FRAME_H ? rotation : 0;
        int drawX = camera.getTileDrawX(tileX) - (FRAME_W - 32) / 2;
        int drawY = camera.getTileDrawY(tileY) - (FRAME_H - 32);
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
        int drawY = camera.getTileDrawY(tileY) - (FRAME_H - 32);
        this.texture.initDraw()
                .section(0, FRAME_W, row * FRAME_H, (row + 1) * FRAME_H)
                .alpha(alpha)
                .draw(drawX, drawY);
    }
}
