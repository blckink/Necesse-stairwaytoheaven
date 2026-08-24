package stairwaytoheaven.objects;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.PortalObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import stairwaytoheaven.SkyRegistry;

/**
 * The Skywatch Gate: the Skyreach-side return portal standing at the Old
 * Warden Spire. 
 *
 * v0.5 DESIGN: the old auto-placed coordinate ladder is gone. This is now a
 * permanent, unbreakable fixture of the spire preset (placed by
 * {@link WardenSpirePreset}) and the ONLY way home — it routes each player
 * back to the surface stairway they ascended from (per-player server-side
 * binding, see {@link SkywatchGateObjectEntity}). Unbreakable for the same
 * reason the quest beacon is: mining your way home must not be possible.
 */
public class SkySideStairwayObject extends GameObject {

    public GameTexture texture;

    public SkySideStairwayObject() {
        this.mapColor = new Color(196, 206, 219);
        this.toolType = ToolType.UNBREAKABLE;
        this.isLightTransparent = true;
        this.lightLevel = 75;
        this.hoverHitbox = new Rectangle(0, -20, 32, 52);
        this.setItemCategory("objects", "misc");
        this.setCraftingCategory("objects", "misc");
    }

    @Override
    public GameMessage getNewLocalization() {
        return new LocalMessage("object", "skywatchgate");
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/skystairwayup");
    }

    @Override
    public void addDrawables(List<LevelSortedDrawable> list, OrderableDrawables tileList, Level level,
            int tileX, int tileY, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        GameLight light = level.getLightLevel(tileX, tileY);
        int drawX = camera.getTileDrawX(tileX) - this.texture.getWidth() / 2 + 16;
        int drawY = camera.getTileDrawY(tileY) - (this.texture.getHeight() - 32) + 32;
        final TextureDrawOptions options = this.texture
                .initDraw()
                .section(0, this.texture.getWidth(), 32, this.texture.getHeight())
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
        GameLight light = level.getLightLevel(tileX, tileY);
        int drawX = camera.getTileDrawX(tileX) - this.texture.getWidth() / 2 + 16;
        int drawY = camera.getTileDrawY(tileY) - (this.texture.getHeight() - 32) + 32;
        this.texture.initDraw().section(0, this.texture.getWidth(), 32, this.texture.getHeight()).light(light).alpha(alpha).draw(drawX, drawY);
    }

    @Override
    public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
        return !level.getIdentifier().equals(SkyRegistry.SKYREACH_IDENTIFIER)
                ? "invalidlevel"
                : super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
    }

    @Override
    public boolean canInteract(Level level, int x, int y, PlayerMob player) {
        return true;
    }

    @Override
    public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
        return Localization.translate("controls", "usetip");
    }

    @Override
    public void interact(Level level, int x, int y, PlayerMob player) {
        if (level.isServer() && player.isServerClient()) {
            ObjectEntity objectEntity = level.entityManager.getObjectEntity(x, y);
            if (objectEntity instanceof PortalObjectEntity) {
                ((PortalObjectEntity) objectEntity).use(level.getServer(), player.getServerClient());
            }
        }

        super.interact(level, x, y, player);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new SkywatchGateObjectEntity(level, x, y);
    }
}
