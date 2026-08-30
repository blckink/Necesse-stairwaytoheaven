package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;

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

        // --- the bridge into incursion tier, rationed and expensive ---
        this.shop.addSellingItem("crystalessence", new SellingShopItem(8, 1))
                .setStaticPriceBasedOnHappiness(900, 1800, 160);
        this.shop.addSellingItem("ascendedshard", new SellingShopItem(3, 1))
                .setStaticPriceBasedOnHappiness(2600, 5000, 400);
        // reading light and glassware: the archive's own stock
        this.shop.addSellingItem("stormglass", new SellingShopItem(16, 2))
                .setStaticPriceBasedOnHappiness(150, 300, 30);
        this.shop.addSellingItem("prismshard", new SellingShopItem(24, 3))
                .setStaticPriceBasedOnHappiness(90, 190, 20);

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
}
