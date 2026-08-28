package stairwaytoheaven.objects;

import java.util.ArrayList;

import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Attacker;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.level.gameObject.furniture.FurnitureObject;
import necesse.level.maps.Level;
import stairwaytoheaven.quest.CatHome;

/**
 * The Cat Basket: Siggi and Peanut's bed, and the only thing in the mod that
 * decides WHERE they live.
 *
 * <p>It shipped for four releases as a bare {@code FurnitureObject}. Placing one
 * was pure decoration -- the cats' home was hard-wired to the basket tile inside
 * the Warden's Spire, in the Skyreach -- which is what produced the report this
 * class answers: two baskets placed in a Surface town, and the cats "weg oder
 * irgendwo anders dann erschienen wo ich es nicht weiss".
 *
 * <p>It stays a {@code FurnitureObject} on purpose. {@code SettlementRoom}
 * (jar 1.3.2, SettlementRoom.java:114-115) only counts an object toward a room's
 * furniture score when it is {@code instanceof RoomFurniture}, and
 * {@code FurnitureObject} is what implements that -- so anything that is not one
 * is worth nothing to a settlement. {@code furnitureType} is kept as
 * {@code "petbed"}: that string is a bucket key in
 * {@code SettlementRoom.furnitureTypes}, summed by
 * {@code getFurnitureScore() = sum(count^0.44)} and fed to
 * {@code SettlerThoughtRegistry.getRoomQualityThought}. It is NOT a vanilla type
 * name -- "petbed" appears nowhere in the 1.3.2 jar, and no vanilla code asks
 * {@code getFurnitureTypes(String)} for a particular one -- so it names its own
 * bucket and adds to the room's quality exactly like a distinct piece of
 * furniture should. Renaming it to "bed" would make it collide with real beds
 * for no gain.
 *
 * <p>Both hooks below are the funnels EVERY path goes through:
 * {@code GameObject.placeObject} is called by {@code ObjectItem.onPlaceObject}
 * (the player's own placement), by the settler build path and by our own debug
 * command, and {@code onDestroyed} is called from {@code LevelObject.destroy},
 * {@code DamagedObjectEntity} and {@code PacketTileDestroyed}. Both run on the
 * client too, so both are guarded by {@code level.isServer()}: the record and
 * the cats are server state and nothing here may be client-authoritative.
 */
public class CatBasketObject extends FurnitureObject {

    public CatBasketObject() {
        super();
        this.furnitureType = "petbed";
    }

    /**
     * Placing this basket makes THIS tile, on THIS level, the cats' home.
     *
     * <p>The spire's own basket is written with {@code Level.setObject}
     * ({@code SkyLevel.setQuestObject}) and never reaches here, so healing an
     * old world's spire cannot silently claim a home the player did not choose.
     */
    @Override
    public void placeObject(Level level, int layerID, int x, int y, int rotation, boolean byPlayer) {
        super.placeObject(level, layerID, x, y, rotation, byPlayer);
        // Layer 0 only, the base object layer: that is the one every "what
        // stands on this tile" read in the mod uses, and it is the one the
        // object entity flow itself treats as authoritative (placeObject only
        // creates an ObjectEntity for layerID == 0).
        if (level != null && level.isServer() && layerID == 0) {
            CatHome.claim(level, x, y);
        }
    }

    /**
     * Breaking the active basket sends the cats back to the spire. A basket
     * somewhere else is left alone -- see {@code SkywatchWorldData.clearCatHome}.
     */
    @Override
    public void onDestroyed(Level level, int layerID, int x, int y, Attacker attacker,
            ServerClient client, ArrayList<ItemPickupEntity> itemsDropped) {
        super.onDestroyed(level, layerID, x, y, attacker, client, itemsDropped);
        if (level != null && level.isServer() && layerID == 0) {
            CatHome.release(level, x, y);
        }
    }
}
