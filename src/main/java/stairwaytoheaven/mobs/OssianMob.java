package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;

/**
 * Ossian Vane — the Skywatch's last reader.
 *
 * Vanilla archetype: the Mage — the settler who deals in the things you cannot
 * make yet.
 *
 * HIS ADVANTAGE: he is the only vendor in the game who will part with
 * INCURSION-tier salvage without an incursion. `crystalessence` and
 * `ascendedshard` otherwise come only from content far past the sky's own tier,
 * and the Skyreach's endgame had no bridge to it. He buys the sky's own
 * high-tier bars in exchange, so the Aether Forge finally has a customer.
 *
 * The three items he sells were each checked against the game's ItemRegistry
 * before being named here — `arcanicbar` and `voidcrystal`, the obvious
 * guesses, do not exist.
 *
 * He stayed for the archive, not for the order.
 */
public class OssianMob extends SkySettlerMob {

    public OssianMob() {
        super("ossiansettler");

        // --- his PROFESSION: crafting, and nothing outdoors ------------------
        // He has no specialism to grant: the archivist's work IS crafting, the
        // job every settler already has. What makes him a scholar rather than a
        // farmhand is the other half of the mechanism, the one vanilla uses on
        // its Guard (GuardHumanMob.java:31-34): the field jobs come OFF. He
        // hauls, he works a station, and he is never found in a field.
        refuseJob("farming");
        refuseJob("forestry");

        // --- THE EXCLUSIVE STOCK, and it rotates every day ------------------
        // The player's ask: "Ossian soll exklusiv items aus den incursionen
        // anbieten da die sonst nirgends kaufbar sind. Jeden Tag wechselnd."
        //
        // Everything in this block comes out of an incursion and has no other
        // vendor in the game. Each line is gated by a REQUIREMENT rather than
        // by stock, because `SellingShopItem.requirement` is tested every time
        // the shop page is built -- so a line is simply not on the page on a
        // day it is not offered, instead of being visibly sold out.
        //
        // Three of the eight show on any given day, chosen by the world day
        // itself, so every player in a world sees the same window and it moves
        // at midnight without anything having to tick.
        offerOnRotation("crystalessence", 0, 6, 1, 900, 1800, 160);
        offerOnRotation("ascendedshard",  1, 3, 1, 2600, 5000, 400);
        offerOnRotation("voidbullet",     2, 200, 40, 26, 52, 6);
        offerOnRotation("arcanichelmet",  3, 1, 1, 5200, 9000, 700);
        offerOnRotation("arcanicchestplate", 4, 1, 1, 6400, 11000, 850);
        offerOnRotation("arcanicboots",   5, 1, 1, 4800, 8400, 650);
        offerOnRotation("voidbag",        6, 1, 1, 7500, 13000, 1000);
        offerOnRotation("eyeofthevoid",   7, 1, 1, 12000, 20000, 1600);

        // --- and he takes the sky's own top tier off the player's hands ---
        this.shop.addBuyingItem("aetheriumbar", new BuyingShopItem())
                .setPriceBasedOnHappiness(150, 95, 22);
        this.shop.addBuyingItem("stormsteelbar", new BuyingShopItem())
                .setPriceBasedOnHappiness(220, 140, 30);
        this.shop.addBuyingItem("skyweave", new BuyingShopItem())
                .setPriceBasedOnHappiness(90, 58, 15);
    }

    @Override protected int lookSeed() { return 0x0551A0; }
    @Override protected HumanGender gender() { return HumanGender.MALE; }
    @Override protected Color shirtColor() { return new Color(70, 62, 104); }
    @Override protected Color shoesColor() { return new Color(44, 40, 58); }
    @Override protected String[] wardrobe() {
        // Incursion-tier robe and boots: the player invited vanilla's own
        // incursion art, and a scholar in arcanic gear reads as "he has been
        // somewhere you have not" without a single new pixel.
        return new String[]{"runichat", "voidrobe", "arcanicboots"};
    }
    @Override protected int recruitCost() { return 18000; }
    @Override protected String talkKey() { return "ossiantalk"; }

    /**
     * One rotating line.
     *
     * `slot` is this item's fixed place in the rotation. The window is three
     * wide and steps once per world day, so on day D the shop shows slots
     * D, D+1 and D+2 modulo the roster -- every item comes round in under
     * three days, nothing is ever gone for a week, and the sequence is a pure
     * function of the day, so it needs no state, no tick and no save data, and
     * two players in one world always see the same shelf.
     */
    private void offerOnRotation(String itemID, int slot, int maxStock, int restockPerDay,
                                 int minPrice, int maxPrice, int priceStep) {
        this.shop.addSellingItem(itemID, new SellingShopItem(maxStock, restockPerDay))
                .setStaticPriceBasedOnHappiness(minPrice, maxPrice, priceStep)
                .setRequirement((random, client, mob, blackboard) -> {
                    if (client == null || client.getServer() == null
                            || client.getServer().world == null) {
                        return true;      // no world to ask: show everything
                    }
                    int day = client.getServer().world.worldEntity.getDay();
                    int offset = Math.floorMod(slot - day, ROTATION_SIZE);
                    return offset < ROTATION_WINDOW;
                });
    }

    /** How many incursion lines exist, and how many show at once. */
    private static final int ROTATION_SIZE = 8;
    private static final int ROTATION_WINDOW = 3;
}
