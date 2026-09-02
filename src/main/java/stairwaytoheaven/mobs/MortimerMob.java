package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;

/**
 * Mortimer, the Undertaker — {@code docs/WORLD_DESIGN.md} §11 and §27.
 *
 * <h2>Profession: HAULING + CRAFTING (§27), and HUNTING on top</h2>
 *
 * §27 gives him Hauling and Crafting; both are on for every settler already, so
 * they are left exactly as vanilla has them and he will fetch, stock and work a
 * station like anyone else. The specialism that makes him worth a bed is
 * {@code hunting} — the job vanilla withholds from every settler except its own
 * Hunter (HunterHumanMob.java:38), where a settler goes out, takes down animals
 * and brings the remains home. That is the undertaker's trade told straight, and
 * it is an ADDITION to §27's row rather than a replacement: nothing §27 grants
 * him is taken away.
 *
 * He will not farm and he will not chop. Vanilla does the same thing to its
 * Guard (GuardHumanMob.java:31-34) for the same reason: a settler who does
 * everything has no character.
 *
 * <h2>His shop</h2>
 *
 * §11, item for item: "coffin, grave decorations, black candles, urns, Bonewood
 * furniture". Vanilla already owns all of it — the eleven-piece bone furniture
 * family, four gravestones, the sarcophagus, the spirit basin — so the whole
 * shelf costs zero new art and swaps out cleanly when the Ghost Realm ships its
 * own. He is also the only vendor who buys {@code veilessence}, which gives the
 * Veil's Gloom Shades a reason to be fought by somebody who is not going to
 * craft with the drop.
 *
 * <h2>Home region</h2>
 *
 * Ghost Realm / Aftergarden (§10-§11), beside a gravestone; see
 * {@code settlement/VeilResidents} for the placement rules and for why that
 * class still carries the Veil's name.
 */
public class MortimerMob extends SkySettlerMob {

    public MortimerMob() {
        super("mortimersettler");

        // --- the profession -------------------------------------------------
        enableProfession("hunting");
        refuseJob("farming");
        refuseJob("forestry");

        // --- graves ---------------------------------------------------------
        this.shop.addSellingItem("gravestone1", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(90, 200, 20);
        this.shop.addSellingItem("gravestone2", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(90, 200, 20);
        this.shop.addSellingItem("cryptgravestone1", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(120, 260, 25);
        this.shop.addSellingItem("cryptgravestone2", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(120, 260, 25);
        // The coffin of §11: vanilla's is a sarcophagus, and it is a real
        // container object rather than decoration.
        this.shop.addSellingItem("sarcophagus", new SellingShopItem(2, 1))
                .setStaticPriceBasedOnHappiness(1800, 3400, 250)
                .addKilledMobRequirement("swampguardian");

        // --- candles and urns -----------------------------------------------
        this.shop.addSellingItem("candle", new SellingShopItem(40, 8))
                .setStaticPriceBasedOnHappiness(14, 34, 6);
        this.shop.addSellingItem("bonecandelabra", new SellingShopItem(8, 2))
                .setStaticPriceBasedOnHappiness(100, 220, 22);
        this.shop.addSellingItem("deadwoodcandles", new SellingShopItem(8, 2))
                .setStaticPriceBasedOnHappiness(80, 180, 18);
        this.shop.addSellingItem("spiritbasin", new SellingShopItem(1, 1))
                .setStaticPriceBasedOnHappiness(2400, 4200, 300)
                .addKilledMobRequirement("reaper");

        // --- the Bonewood family --------------------------------------------
        this.shop.addSellingItem("bonechair", new SellingShopItem(8, 2))
                .setStaticPriceBasedOnHappiness(70, 150, 15);
        this.shop.addSellingItem("bonemodulartable", new SellingShopItem(8, 2))
                .setStaticPriceBasedOnHappiness(90, 190, 18);
        this.shop.addSellingItem("bonebookshelf", new SellingShopItem(4, 1))
                .setStaticPriceBasedOnHappiness(150, 320, 30);
        this.shop.addSellingItem("bonedresser", new SellingShopItem(4, 1))
                .setStaticPriceBasedOnHappiness(150, 320, 30);
        this.shop.addSellingItem("boneclock", new SellingShopItem(3, 1))
                .setStaticPriceBasedOnHappiness(200, 420, 40);
        this.shop.addSellingItem("bonechest", new SellingShopItem(4, 1))
                .setStaticPriceBasedOnHappiness(160, 340, 32);
        this.shop.addSellingItem("skull", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(60, 130, 14);

        // --- and he takes the remains ---------------------------------------
        this.shop.addBuyingItem("bone", new BuyingShopItem())
                .setPriceBasedOnHappiness(14, 8, 3);
        this.shop.addBuyingItem("ectoplasm", new BuyingShopItem())
                .setPriceBasedOnHappiness(40, 26, 7);
        this.shop.addBuyingItem("veilessence", new BuyingShopItem())
                .setPriceBasedOnHappiness(70, 45, 12);
    }

    @Override protected int lookSeed() { return 0x30E713; }
    @Override protected HumanGender gender() { return HumanGender.MALE; }
    @Override protected Color shirtColor() { return new Color(34, 32, 38); }
    @Override protected Color shoesColor() { return new Color(26, 24, 28); }
    @Override protected String[] wardrobe() {
        // Top hat, black cloak, dress shoes — three vanilla item IDs, no new art.
        return new String[]{"tophat", "thiefscloak", "dressshoes"};
    }
    @Override protected int recruitCost() { return 8000; }
    @Override protected String talkKey() { return "mortimertalk"; }
}
