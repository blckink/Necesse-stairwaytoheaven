package stairwaytoheaven.realms.eden;

import java.awt.Color;

import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.item.Item;
import necesse.level.gameObject.LadderDownObject;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;

/**
 * The living-world side of the Eden Gate: the way INTO the Garden.
 *
 * <p>Never crafted and never placed by hand — an {@link EdenSeedBasinObject}
 * seeded with Eden grass seed turns into one, the same way the Soul Basin turns
 * into the Ghost Gate. See that class for why.
 *
 * <p>Archetype: vanilla's {@code LadderDownObject}, exactly as
 * {@code stairwaytoheaven.objects.SkywardStairwayObject} and
 * {@code stairwaytoheaven.realms.ghost.GhostGateObject} both already are — the
 * whole travel path (blocked-exit check, lazy generation of the far level, the
 * counterpart's placement, mob clearing) is the game's own netcode.
 *
 * <p>Its sheet is borrowed a second time: {@code LadderDownObject.loadTextures}
 * reads {@code objects/<textureName>down}, and passing {@code "skystairway"}
 * draws the mod's own existing stairway sheet — fitting, since Eden is climbed
 * to, not walked to (see {@code SkyRegistry.EDEN_DIMENSION}'s own doc: "Eden is
 * the first floor past the Skywatch"). Registered unobtainable, like the Ghost
 * Gate, so no item icon is needed at all: this object is never held, only stood
 * on.
 */
public class EdenGateObject extends LadderDownObject {

    public EdenGateObject() {
        // textureName "skystairway" -> objects/skystairwaydown.png (ours,
        // borrowed a second time); localizationKey "edengate" -> [object]
        // edengate in both locales, shared with EdenSideGateObject.
        super("skystairway", "edengate", SkyRegistry.EDEN_IDENTIFIER,
                new Color(90, 196, 120), Item.Rarity.RARE);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new EdenGateObjectEntity(level, x, y);
    }
}
