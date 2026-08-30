package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;

/**
 * Magpie — the courier who kept the cargo.
 *
 * Vanilla archetype: the Explorer's role (she goes away and comes back with
 * goods) with the Pawnbroker's shop behaviour (she buys what nobody else will).
 *
 * HER ADVANTAGE, which is the point of hiring her: she is the only vendor in
 * the mod who BUYS in quantity, and she pays over broker for salvage. That
 * turns the crates and wreck caches scattered across the Skyreach from clutter
 * into an income — the loop the sky did not have. She also stocks goods from
 * biomes the player is currently nowhere near, so a sky settlement is not cut
 * off from the rest of the world.
 *
 * She was never a member of the Skywatch. She was the courier they used.
 */
public class MagpieMob extends SkySettlerMob {

    public MagpieMob() {
        super("magpiesettler");

        // --- what she sells: goods from elsewhere, at a courier's markup ---
        this.shop.addSellingItem("wormbait", new SellingShopItem(120, 12))
                .setStaticPriceBasedOnHappiness(9, 22, 5);
        this.shop.addSellingItem("sandstone", new SellingShopItem(80, 8))
                .setStaticPriceBasedOnHappiness(14, 30, 6);
        this.shop.addSellingItem("coconut", new SellingShopItem(30, 3))
                .setStaticPriceBasedOnHappiness(18, 38, 7);
        this.shop.addSellingItem("snowball", new SellingShopItem(60, 6))
                .setStaticPriceBasedOnHappiness(10, 24, 5);
        this.shop.addSellingItem("glass", new SellingShopItem(40, 4))
                .setStaticPriceBasedOnHappiness(20, 44, 8);
        // the rotating rare line: a spare bell is the Veil's only key, and she
        // is the only person besides the Warden who has one to sell.
        this.shop.addSellingItem("silverbell", new SellingShopItem(1, 1))
                .setStaticPriceBasedOnHappiness(9000, 16000, 1200);

        // --- what she buys: the salvage loop. This is her real function. ---
        this.shop.addBuyingItem("skystone", new BuyingShopItem())
                .setPriceBasedOnHappiness(30, 18, 5);
        this.shop.addBuyingItem("windsilk", new BuyingShopItem())
                .setPriceBasedOnHappiness(40, 26, 7);
        this.shop.addBuyingItem("aetheriumore", new BuyingShopItem())
                .setPriceBasedOnHappiness(70, 45, 12);
        this.shop.addBuyingItem("stormshard", new BuyingShopItem())
                .setPriceBasedOnHappiness(85, 55, 14);
        this.shop.addBuyingItem("aurorapetal", new BuyingShopItem())
                .setPriceBasedOnHappiness(85, 55, 14);
        this.shop.addBuyingItem("fulgurite", new BuyingShopItem())
                .setPriceBasedOnHappiness(95, 62, 16);
        this.shop.addBuyingItem("prismshard", new BuyingShopItem())
                .setPriceBasedOnHappiness(95, 62, 16);
    }

    @Override protected int lookSeed() { return 0x4A6913; }
    @Override protected HumanGender gender() { return HumanGender.FEMALE; }
    @Override protected Color shirtColor() { return new Color(64, 62, 78); }
    @Override protected Color shoesColor() { return new Color(52, 40, 32); }
    @Override protected String[] wardrobe() {
        return new String[]{"trapperhat", "sharpshootercoat", "leatherboots"};
    }
    @Override protected int recruitCost() { return 12000; }
    @Override protected String talkKey() { return "magpietalk"; }
}
