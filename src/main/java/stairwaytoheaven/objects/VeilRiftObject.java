package stairwaytoheaven.objects;

import java.awt.Color;

import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.item.Item;
import necesse.level.gameObject.LadderDownObject;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;

/**
 * The surface-side Rift into the Veil. Never crafted or placed by hand — a
 * Seance Circle transforms into it. Rides the vanilla ladder netcode.
 */
public class VeilRiftObject extends LadderDownObject {

    public VeilRiftObject() {
        super("veilrift", "veilrift", SkyRegistry.VEIL_IDENTIFIER,
                new Color(96, 140, 110), Item.Rarity.EPIC);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new VeilRiftObjectEntity(level, x, y);
    }
}
