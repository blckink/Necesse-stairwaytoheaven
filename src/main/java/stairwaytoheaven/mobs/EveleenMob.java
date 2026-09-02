package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;

/**
 * Eveleen, the Eden Botanist — {@code docs/WORLD_DESIGN.md} §5 and §27.
 *
 * <h2>Profession: FARMING, and the specialism that makes it one</h2>
 *
 * §27 gives her "Farming + Forestry". Both of those job types are
 * {@code defaultDisabledBySettler=false} in vanilla — every settler already
 * does them — so leaving it there would have made her a farmer in name only.
 * What actually distinguishes vanilla's Farmer is one line:
 * {@code getPriority("fertilize").disabledBySettler = false}
 * (FarmerHumanMob.java:23). Fertilising is a job NO settler can do until one of
 * them is a farmer, so a settlement without her cannot fertilise at all.
 * That is her profession, and it is the same one vanilla's own farmer has.
 *
 * <h2>Her shop, and why the mod needed it</h2>
 *
 * §5: "seeds, saplings, rare plants, Bee Hive". Every line is a real vanilla or
 * mod item; the Bee Hive is served by {@code queenbee}, because vanilla's
 * {@code beehive} object is registered NOT obtainable
 * (ObjectRegistry.java:1395) and the queen is how vanilla itself sells one
 * (AnimalKeeperHumanMob.java:38). The one thing on the shelf nobody else in the
 * game sells is {@code overgrownedenseed} — Eden grass, which until now only
 * came out of a sky crate, and which is the only reason a player who has never
 * found one can start an Eden patch at all.
 *
 * <h2>Home region</h2>
 *
 * The Garden of Eden (§5). That realm is not built yet, so she has no worldgen
 * home today and reaches the player the other way: she travels to a settlement
 * that already has Eden grass growing in it. See {@code SkyArrivals.EDEN_PATCH}.
 */
public class EveleenMob extends SkySettlerMob {

    public EveleenMob() {
        super("eveleensettler");

        // --- the profession -------------------------------------------------
        // Fertilising: withheld from every settler until one of them is a
        // farmer. Farming and Forestry (§27) are on for everyone already and
        // are deliberately left alone.
        enableProfession("fertilize");

        // --- seeds ----------------------------------------------------------
        // Her signature line, and the only shop in the game that carries it.
        this.shop.addSellingItem("overgrownedenseed", new SellingShopItem(12, 2))
                .setStaticPriceBasedOnHappiness(180, 380, 40);
        this.shop.addSellingItem("wheatseed", new SellingShopItem(20, 4))
                .setStaticPriceBasedOnHappiness(20, 50, 10);
        this.shop.addSellingItem("carrotseed", new SellingShopItem(20, 4))
                .setStaticPriceBasedOnHappiness(60, 120, 20);
        this.shop.addSellingItem("pumpkinseed", new SellingShopItem(20, 4))
                .setStaticPriceBasedOnHappiness(80, 160, 20);
        this.shop.addSellingItem("strawberryseed", new SellingShopItem(20, 4))
                .setStaticPriceBasedOnHappiness(100, 200, 25);

        // --- saplings -------------------------------------------------------
        this.shop.addSellingItem("cloudberrysapling", new SellingShopItem(6, 1))
                .setStaticPriceBasedOnHappiness(220, 440, 45);
        this.shop.addSellingItem("applesapling", new SellingShopItem(3, 1))
                .setStaticPriceBasedOnHappiness(500, 1000, 100);
        this.shop.addSellingItem("lemonsapling", new SellingShopItem(3, 1))
                .setStaticPriceBasedOnHappiness(1000, 1800, 150)
                .addKilledMobRequirement("sageandgrit");
        this.shop.addSellingItem("bananasapling", new SellingShopItem(3, 1))
                .setStaticPriceBasedOnHappiness(1000, 1800, 150)
                .addKilledMobRequirement("sageandgrit");

        // --- the growing kit ------------------------------------------------
        this.shop.addSellingItem("fertilizer", new SellingShopItem(200, 40))
                .setStaticPriceBasedOnHappiness(12, 30, 6);
        this.shop.addSellingItem("flowerpot", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(40, 90, 12);
        // "Bee Hive" (§5): the queen is what places one.
        this.shop.addSellingItem("queenbee", new SellingShopItem(1, 1))
                .setStaticPriceBasedOnHappiness(1000, 1600, 100)
                .addKilledMobRequirement("piratecaptain");
        // "rare plants" (§5): the one flower vanilla treats as a treasure.
        this.shop.addSellingItem("prettyflower", new SellingShopItem(2, 1))
                .setStaticPriceBasedOnHappiness(600, 1200, 120);

        // --- and she takes the harvest --------------------------------------
        this.shop.addBuyingItem("windwheat", new BuyingShopItem())
                .setPriceBasedOnHappiness(26, 16, 5);
        this.shop.addBuyingItem("cloudberry", new BuyingShopItem())
                .setPriceBasedOnHappiness(22, 13, 4);
        this.shop.addBuyingItem("wheat", new BuyingShopItem())
                .setPriceBasedOnHappiness(10, 2, 2);
        this.shop.addBuyingItem("sunflower", new BuyingShopItem())
                .setPriceBasedOnHappiness(12, 3, 3);
    }

    @Override protected int lookSeed() { return 0xE0E1EE; }
    @Override protected HumanGender gender() { return HumanGender.FEMALE; }
    @Override protected Color shirtColor() { return new Color(58, 104, 62); }
    @Override protected Color shoesColor() { return new Color(74, 58, 40); }
    @Override protected String[] wardrobe() {
        // Vanilla's leaf-and-bark set, worn as clothing the way the Elder wears
        // his hat and shirt. No new art: these are three ItemRegistry IDs.
        return new String[]{"dryadhat", "dryadchestplate", "dryadboots"};
    }
    @Override protected int recruitCost() { return 7000; }
    @Override protected String talkKey() { return "eveleentalk"; }
}
