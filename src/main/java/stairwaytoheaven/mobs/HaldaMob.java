package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;

/**
 * Halda, the Cellarer — the last of the Skywatch's household.
 *
 * Vanilla archetype: the Farmer — the settler who works a station and sells
 * what it makes.
 *
 * HER ADVANTAGE: she is the supply line for the three settlement stations. The
 * loom, the forge and the kiln each turn raw sky materials into something, and
 * until now the player had to gather every gram of input themselves. Halda
 * SELLS the worked goods — skyweave, stormglass, stormsteel — so a settlement
 * can build with them before it can produce them, and she BUYS the raw inputs,
 * which gives the sky's gathering loops somewhere to end.
 *
 * She kept the stores after everyone else left, and stopped counting the years
 * in years.
 */
public class HaldaMob extends SkySettlerMob {

    public HaldaMob() {
        super("haldasettler");

        // --- the worked goods, so a settlement can build before it produces ---
        this.shop.addSellingItem("skyweave", new SellingShopItem(20, 2))
                .setStaticPriceBasedOnHappiness(150, 300, 30);
        this.shop.addSellingItem("stormglass", new SellingShopItem(20, 2))
                .setStaticPriceBasedOnHappiness(140, 280, 28);
        this.shop.addSellingItem("stormsteelbar", new SellingShopItem(10, 1))
                .setStaticPriceBasedOnHappiness(420, 800, 70);
        // household stores: the treat is the only way to coax the cats, and
        // running out of it used to mean a trip back to a workstation.
        this.shop.addSellingItem("cloudpufftreat", new SellingShopItem(30, 3))
                .setStaticPriceBasedOnHappiness(60, 130, 14);
        this.shop.addSellingItem("cloudberry", new SellingShopItem(60, 6))
                .setStaticPriceBasedOnHappiness(12, 26, 5);

        // --- and she takes the raw end of every sky gathering loop ---
        this.shop.addBuyingItem("windwheat", new BuyingShopItem())
                .setPriceBasedOnHappiness(24, 15, 4);
        this.shop.addBuyingItem("cloudberry", new BuyingShopItem())
                .setPriceBasedOnHappiness(20, 12, 4);
        this.shop.addBuyingItem("nimbuswood", new BuyingShopItem())
                .setPriceBasedOnHappiness(26, 16, 5);
        this.shop.addBuyingItem("charwood", new BuyingShopItem())
                .setPriceBasedOnHappiness(30, 19, 5);
    }

    @Override protected int lookSeed() { return 0xAA1DA0; }
    @Override protected HumanGender gender() { return HumanGender.FEMALE; }
    @Override protected Color shirtColor() { return new Color(122, 96, 72); }
    @Override protected Color shoesColor() { return new Color(60, 52, 46); }
    @Override protected String[] wardrobe() {
        return new String[]{"battlechefhat", "farmershirt", "clothboots"};
    }
    @Override protected int recruitCost() { return 9000; }
    @Override protected String talkKey() { return "haldatalk"; }
}
