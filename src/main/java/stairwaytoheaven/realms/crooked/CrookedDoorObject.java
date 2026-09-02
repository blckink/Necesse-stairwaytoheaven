package stairwaytoheaven.realms.crooked;

import java.awt.Color;

import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.item.Item;
import necesse.level.gameObject.LadderDownObject;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;

/**
 * The Crooked Door — the Skyreach-side half of the way into Crooked Beyond.
 *
 * <p>Never crafted and never placed by hand. A Seance Circle standing inside the
 * Beetle Outlands turns into one when it is used
 * ({@link stairwaytoheaven.objects.SeanceCircleObject}), which is how a rim
 * finally becomes a door to the place it was the rim of.
 *
 * <p>It rides the vanilla ladder netcode, exactly as the Veil rift does, so the
 * blocked-exit check, the lazy generation of the far level, the counterpart
 * placement and the mob clearing are all vanilla's rather than ours.
 *
 * <p><b>Art:</b> the mod's own {@code objects/veilriftdown.png} —
 * {@code LadderDownObject} resolves its sheet as
 * {@code objects/&lt;textureName&gt;down}, so passing {@code "veilrift"} points
 * it at a sprite this repo already owns: a ring of green light standing open on
 * a low pedestal. It is a stand-in for §15's free-standing red door and is
 * recorded as one in {@code docs/realms/crooked.md}; the red door proper arrives
 * with Mr. Knott, who is a separate job.
 */
public class CrookedDoorObject extends LadderDownObject {

    public CrookedDoorObject() {
        // (textureName, localizationKey, destination, mapColor, rarity)
        // The localization key is shared by both halves -- vanilla's own ladder
        // pair does the same -- so [object] needs crookeddoor as well as the two
        // registered IDs crookeddoordown / crookeddoorup.
        super("veilrift", "crookeddoor", SkyRegistry.CROOKED_IDENTIFIER,
                new Color(150, 60, 180), Item.Rarity.EPIC);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new CrookedDoorObjectEntity(level, x, y);
    }
}
