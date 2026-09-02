package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;

/**
 * Caspern, the Spirit Smith — {@code docs/WORLD_DESIGN.md} §11 and §27.
 * Own name, own design; §11 is explicit that he is not a copy of anybody.
 *
 * <h2>Profession: CRAFTING, made real by what he will NOT do</h2>
 *
 * §27 gives him Crafting, and crafting is the job type every settler already
 * has — so on its own it says nothing. What makes him the settlement's smith
 * rather than its handyman is the shape vanilla gives its Guard: he keeps
 * crafting and hauling and refuses {@code farming} and {@code forestry}
 * (GuardHumanMob.java:31-34 does the same thing in the other direction). Put
 * him on the Aether Forge and he stays on the Aether Forge.
 *
 * That matters because the three mod workstations — Windsilk Loom, Aether
 * Forge, Stormglass Kiln — are all {@code SettlementWorkstationObject}s whose
 * jobs are filed under vanilla's <b>crafting</b> priority
 * ({@code SkyProfessions}), so "crafting" is precisely the profession that runs
 * them.
 *
 * <h2>His shop</h2>
 *
 * §11: "Spiritsteel, spectral weapon recipes, Soul Thread". None of those three
 * materials exist yet, and inventing them would collide with the Ghost Realm
 * agent's own work, so each is served by the closest thing vanilla already has
 * and the row is written down in {@code docs/VANILLA_ASSET_MAP.md}:
 * {@code nightsteelbar}/{@code nightsteelore} for Spiritsteel and
 * {@code phantomdust} for Soul Thread. He is the only vendor in the game for
 * any of them, and he buys the sky's own bars, so the Aether Forge gets a
 * second customer after Ossian.
 *
 * <h2>Home region</h2>
 *
 * Ghost Realm / Aftergarden (§10-§11) when it exists; the VEIL today. He
 * travels to a settlement once it has a forge — §11's "build the Spirit Forge",
 * answered with the forge the mod actually has. See {@code SkyArrivals.FORGE}.
 */
public class CaspernMob extends SkySettlerMob {

    public CaspernMob() {
        super("caspernsettler");

        // --- the profession -------------------------------------------------
        refuseJob("farming");
        refuseJob("forestry");

        // --- Spiritsteel (stand-in: nightsteel) ------------------------------
        this.shop.addSellingItem("nightsteelore", new SellingShopItem(40, 8))
                .setStaticPriceBasedOnHappiness(45, 100, 12);
        this.shop.addSellingItem("nightsteelbar", new SellingShopItem(20, 4))
                .setStaticPriceBasedOnHappiness(160, 340, 32);
        // --- Soul Thread (stand-in: phantom dust) and the cloth it goes with -
        this.shop.addSellingItem("phantomdust", new SellingShopItem(30, 6))
                .setStaticPriceBasedOnHappiness(70, 160, 18);
        this.shop.addSellingItem("silk", new SellingShopItem(40, 8))
                .setStaticPriceBasedOnHappiness(30, 70, 10);
        // --- the spectral line ------------------------------------------------
        this.shop.addSellingItem("bonearrow", new SellingShopItem(300, 60))
                .setStaticPriceBasedOnHappiness(4, 10, 2);
        this.shop.addSellingItem("bonehilt", new SellingShopItem(1, 1))
                .setStaticPriceBasedOnHappiness(900, 1700, 150)
                .addKilledMobRequirement("swampguardian");
        this.shop.addSellingItem("nightsteelveil", new SellingShopItem(1, 1))
                .setStaticPriceBasedOnHappiness(2600, 4800, 400)
                .addKilledMobRequirement("reaper");

        // --- and he buys metal ------------------------------------------------
        this.shop.addBuyingItem("aetheriumbar", new BuyingShopItem())
                .setPriceBasedOnHappiness(150, 95, 22);
        this.shop.addBuyingItem("stormsteelbar", new BuyingShopItem())
                .setPriceBasedOnHappiness(220, 140, 30);
        this.shop.addBuyingItem("bone", new BuyingShopItem())
                .setPriceBasedOnHappiness(16, 9, 3);
        this.shop.addBuyingItem("ectoplasm", new BuyingShopItem())
                .setPriceBasedOnHappiness(45, 29, 8);
    }

    @Override protected int lookSeed() { return 0xCA5DE1; }
    @Override protected HumanGender gender() { return HumanGender.MALE; }
    @Override protected Color shirtColor() { return new Color(48, 52, 66); }
    @Override protected Color shoesColor() { return new Color(38, 34, 34); }
    @Override protected String[] wardrobe() {
        // A smith's apron and shoes under a nightsteel veil: three vanilla item
        // IDs, no new art, and the veil is what makes him read as the SPIRIT
        // smith rather than the village one.
        return new String[]{"nightsteelveil", "smithingapron", "smithingshoes"};
    }
    @Override protected int recruitCost() { return 14000; }
    @Override protected String talkKey() { return "casperntalk"; }
}
