package stairwaytoheaven.objects;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.LadderUpObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.PortalObjectEntity;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import stairwaytoheaven.SkyRegistry;

/**
 * The Veil-side rift leading back up to the living world. Functional twin of
 * the sky-side return stairway: auto-placed on first entry, restricted to the
 * Veil, cleans up its surface counterpart like vanilla ladder pairs.
 */
public class VeilSideRiftObject extends GameObject {

    public GameTexture texture;

    public VeilSideRiftObject() {
        this.mapColor = new Color(96, 140, 110);
        this.toolType = ToolType.ALL;
        this.isLightTransparent = true;
        this.lightLevel = 75;
        this.hoverHitbox = new Rectangle(0, -20, 32, 52);
        this.setItemCategory("objects", "misc");
        this.setCraftingCategory("objects", "misc");
    }

    @Override
    public GameMessage getNewLocalization() {
        return new LocalMessage("object", "veilrift");
    }

    @Override
    public void loadTextures() {
        super.loadTextures();
        this.texture = GameTexture.fromFile("objects/veilriftup");
    }

    @Override
    public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
        // Breaking either side yields the craftable stairway item
        return ObjectRegistry.getObject(SkyRegistry.veilRiftDownID).getLootTable(level, layerID, tileX, tileY);
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
        return !level.getIdentifier().equals(SkyRegistry.VEIL_IDENTIFIER)
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
    public void onDestroyed(Level level, int layerID, int x, int y, Attacker attacker, ServerClient client, ArrayList<ItemPickupEntity> itemsDropped) {
        if (level.isServer()) {
            ObjectEntity objectEntity = level.entityManager.getObjectEntity(x, y);
            if (objectEntity instanceof PortalObjectEntity) {
                PortalObjectEntity portal = (PortalObjectEntity) objectEntity;
                if (level.getServer().world.levelExists(portal.getDestinationIdentifier())) {
                    Level nextLevel = level.getServer().world.getLevel(portal.getDestinationIdentifier());
                    nextLevel.regionManager.ensureTileIsLoaded(portal.destinationTileX, portal.destinationTileY);
                    if (nextLevel.getObjectID(portal.destinationTileX, portal.destinationTileY) == SkyRegistry.veilRiftDownID) {
                        nextLevel.setObject(portal.destinationTileX, portal.destinationTileY, 0);
                        level.getServer().network.sendToClientsWithTile(
                                new PacketChangeObject(nextLevel, 0, portal.destinationTileX, portal.destinationTileY, 0),
                                nextLevel, portal.destinationTileX, portal.destinationTileY);
                    }
                }
            }
        }

        super.onDestroyed(level, layerID, x, y, attacker, client, itemsDropped);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new LadderUpObjectEntity(
                "veilriftup",
                level,
                x,
                y,
                LevelIdentifier.SURFACE_IDENTIFIER,
                SkyRegistry.veilRiftDownID,
                this.texture == null ? null : new GameSprite(this.texture, 0, 0, 32));
    }
}
