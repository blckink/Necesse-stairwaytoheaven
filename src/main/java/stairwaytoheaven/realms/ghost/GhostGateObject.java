package stairwaytoheaven.realms.ghost;

import java.awt.Color;

import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.item.Item;
import necesse.level.gameObject.LadderDownObject;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;

/**
 * The living-world side of the Ghost Gate: the way INTO the Aftergarden.
 *
 * <p>Never crafted and never placed by hand — a {@link SoulBasinObject} filled
 * with ectoplasm turns into one, exactly the way the Seance Circle turns into
 * the Veil Rift. That is deliberate: a door to the land of the dead should cost
 * something and happen somewhere, not sit in a hotbar.
 *
 * <p>Archetype: vanilla's {@link LadderDownObject}, so the whole travel path is
 * the game's own netcode — the blocked-exit check, the lazy generation of the
 * far level, the placement of the counterpart and the mob clearing all come for
 * free and behave the way every ladder in the game behaves.
 *
 * <p>Its sheet is borrowed: {@code LadderDownObject.loadTextures} reads
 * {@code objects/<textureName>down}, and {@code textureName} is a constructor
 * argument, so passing {@code "veilrift"} draws the mod's existing rift sheet.
 * Registered unobtainable, like the Veil's own rift, so no item icon is
 * involved at all.
 */
public class GhostGateObject extends LadderDownObject {

    public GhostGateObject() {
        // textureName "veilrift" -> objects/veilriftdown.png (ours, borrowed);
        // localizationKey "ghostgate" -> [object] ghostgate in both locales.
        super("veilrift", "ghostgate", SkyRegistry.GHOST_IDENTIFIER,
                new Color(84, 190, 176), Item.Rarity.EPIC);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new GhostGateObjectEntity(level, x, y);
    }
}
