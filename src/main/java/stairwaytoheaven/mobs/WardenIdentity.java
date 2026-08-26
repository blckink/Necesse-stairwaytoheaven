package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.engine.util.GameRandom;
import necesse.gfx.GameHair;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;

/**
 * The Warden's one face and one set of clothes, shared by both of his forms.
 *
 * He exists twice: {@link SkyWardenMob} in the Old Warden Spire and
 * {@link WardenSettlerMob} once recruited to the surface settlement. Before
 * this class they were built completely differently — the sky one was a
 * FriendlyMob with a single hand-drawn 64px sheet, the settler was a HumanShop
 * whose look was randomly rolled — so recruiting him replaced a hooded keeper
 * with a randomly generated stranger. Both now run through here, so it is the
 * same man who walks down.
 *
 * The look is rolled from a FIXED seed rather than written out field by field:
 * that keeps us on vanilla's own randomizeLook path (so any look field we do
 * not care about still gets a sane value) while producing an identical result
 * on every call, on every client, in every world.
 */
public final class WardenIdentity {

    /** Fixed so both Wardens roll the same man. Changing it changes his face. */
    private static final int LOOK_SEED = 0x5C0FFEE;

    /**
     * Vanilla's hair-colour weights run light-to-dark; the Elder uses 140 for
     * his grey, and the Warden is older still.
     */
    private static final int HAIR_WEIGHT = 140;

    /** Vanilla beard facial features — the Elder picks from 1, 3 and 4. */
    private static final int BEARD = 3;

    /** Skywatch storm-blue, the colour his mantle and boots are cut from. */
    public static final Color CLOTH = new Color(86, 96, 122);
    public static final Color LEATHER = new Color(46, 44, 60);

    private WardenIdentity() {
    }

    /** Applies his fixed look. Call from a mob's randomizeLook override. */
    public static HumanGender apply(HumanLook look) {
        GameRandom random = new GameRandom(LOOK_SEED);
        look.randomizeLook(random, true, HumanGender.MALE, true, true, true, true);
        look.setFacialFeature(BEARD);
        look.setHairColor(GameHair.getRandomHairColorAtSpecificWeight(random, HAIR_WEIGHT));
        look.setShirtColor(CLOTH);
        look.setShoesColor(LEATHER);
        return HumanGender.MALE;
    }

    /**
     * Puts the Skywatch clothes on the human body. This is how vanilla gives a
     * settler a distinctive silhouette — the Elder is a plain human wearing
     * elderhat, eldershirt and eldershoes — rather than by replacing the body
     * with a bespoke sprite sheet.
     */
    public static void dress(HumanDrawOptions drawOptions) {
        drawOptions.helmet(new InventoryItem("skywatchhood"));
        drawOptions.chestplate(new InventoryItem("wardenmantle"));
        drawOptions.boots(new InventoryItem("wardenboots"));
    }
}
