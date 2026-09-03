package stairwaytoheaven.objects;

import java.awt.Color;

import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.item.Item;
import necesse.level.gameObject.LadderDownObject;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;

/**
 * The Rift the seance opens — and it opens onto the GHOST BAND of the one
 * plane, not into a world of its own.
 *
 * <p>{@code docs/PLAN_ONE_PLANE.md}: <i>"The seance is fast travel to the Ghost
 * BAND of the one plane. It is not a door to another world, because there is no
 * other world."</i> The {@code veil2} dimension this used to lead to is retired;
 * the ground it led to (murkmoss, blackpeat, ashsand, the whisperreeds and the
 * Gloom Shade) is now the fen inside the Ghost Realm's band
 * ({@code WORLD_DESIGN} §41.5), so the rift still lands the player in exactly
 * the place it always did — that place is simply somewhere they could also have
 * walked to.
 *
 * <p>Never crafted or placed by hand: a Seance Circle transforms into it. Rides
 * the vanilla ladder netcode.
 */
public class VeilRiftObject extends LadderDownObject {

    public VeilRiftObject() {
        super("veilrift", "veilrift", SkyRegistry.SKYREACH_IDENTIFIER,
                new Color(96, 140, 110), Item.Rarity.EPIC);
    }

    @Override
    public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
        return new VeilRiftObjectEntity(level, x, y);
    }
}
