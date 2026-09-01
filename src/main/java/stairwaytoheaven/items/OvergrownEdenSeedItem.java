package stairwaytoheaven.items;

import java.awt.geom.Line2D;

import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.tileItem.GrassSeedItem;
import necesse.level.maps.Level;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import stairwaytoheaven.SkyRegistry;

/**
 * The Eden grass seed, on vanilla's own {@code GrassSeedItem} — which is the
 * class vanilla registers {@code overgrowngrassseed} with, the asset the
 * supplied icon (kk-sprites/overgrowngrassseed-overgrownedenseed.png) names as
 * its source. Everything is inherited: the compostable/seed ingredient tags,
 * the seed tooltip, the place handler, the death penalty.
 *
 * <p>One override, and it is the one that makes the item usable at all where
 * this mod lives: <b>VERIFIED [jar]</b> {@code GrassSeedItem.canPlace} accepts
 * ONLY {@code TileRegistry.dirtID} (line 72, returning "notdirt" for anything
 * else), and the Skyreach has no dirt — its soil is Cloudturf. So Cloudturf is
 * accepted alongside dirt: on the surface this plants exactly like vanilla's
 * seed, and in the sky it plants on the ground the sky actually has. Not on
 * stormslate, shoal or the wrong grounds — Eden takes root in soil, not in
 * slate, which also keeps a planted patch from overwriting a biome's signature
 * terrain wholesale.
 */
public class OvergrownEdenSeedItem extends GrassSeedItem {

    public OvergrownEdenSeedItem() {
        super("overgrownedentile");
    }

    @Override
    public String canPlace(Level level, int x, int y, PlayerMob player,
            Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
        int tileX = GameMath.getTileCoordinate(x);
        int tileY = GameMath.getTileCoordinate(y);
        if (level.isTileWithinBounds(tileX, tileY)
                && !level.isProtected(tileX, tileY)
                && level.getTileID(tileX, tileY) == SkyRegistry.cloudturfID) {
            return !this.isInPlaceRange(level, tileX * 32 + 16, tileY * 32 + 16,
                    player, playerPositionLine, item) ? "outofrange" : null;
        }
        // Dirt (and every error message) stays vanilla's.
        return super.canPlace(level, x, y, player, playerPositionLine, item, mapContent);
    }

    /** The tile it plants, for anything that wants to ask rather than assume. */
    public int plantsTileID() {
        return TileRegistry.getTileID(this.grassStringID);
    }
}
