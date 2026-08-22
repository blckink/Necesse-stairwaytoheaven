package stairwaytoheaven.objects;

import java.awt.Color;

import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.item.Item;
import necesse.level.gameObject.LadderDownObject;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;

/**
 * The Stairway to Heaven — the craftable, surface-placed half of the stairway
 * pair. Reuses the vanilla ladder object behavior (surface-only placement,
 * portal interaction, counterpart cleanup on destroy) but ascends to the
 * Skyreach instead of descending, via {@link SkywardStairwayObjectEntity}.
 */
public class SkywardStairwayObject extends LadderDownObject {

    public SkywardStairwayObject() {
        super("skystairway", "stairwaytoheaven", SkyRegistry.SKYREACH_IDENTIFIER,
                new Color(196, 206, 219), Item.Rarity.UNCOMMON);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new SkywardStairwayObjectEntity(level, x, y);
    }
}
