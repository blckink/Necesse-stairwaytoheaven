package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import stairwaytoheaven.quest.SkywatchWorldData;

/**
 * Eleanor, the Lost Soul — {@code docs/WORLD_DESIGN.md} §11.
 *
 * <h2>Her two endings, and how both are actually built</h2>
 *
 * §11: "Two endings: <b>Pass on</b> (reward: strong trinket) or <b>Stay</b>
 * (Eleanor becomes a recruitable settler)." Both ship:
 *
 * <ul>
 * <li><b>STAY</b> is the ordinary vanilla path and needs no new machinery.
 *     Talking to her opens the recruit page ({@code startInRecruitForm} is true
 *     while she is not a settler), the coins are taken server-side by
 *     {@code ShopContainer.payForRecruit}, and she moves in as the settlement's
 *     husbandry settler. She asks less than anyone else in the mod, because
 *     what she wants is to be somewhere rather than to be paid.</li>
 * <li><b>PASS ON</b> is a deliberate act with an item in hand, the same shape
 *     the mod already uses to coax a spire cat home with a Cloudpuff Treat
 *     ({@code SpireCatMob.interact}): hold {@link #PASS_ON_COUNT}
 *     {@code veilessence} — what a Gloom Shade is made of, and what she is made
 *     of — in the SELECTED slot and talk to her. Holding it in the hand rather
 *     than merely owning it is what makes it a choice: a player who is carrying
 *     essence for crafting and wants to recruit her simply is not holding it,
 *     and gets the recruit page as usual.</li>
 * </ul>
 *
 * The pass-on branch is refused once she is a settler or a visitor, so nobody
 * can delete a settler they already hired. It is recorded in
 * {@link SkywatchWorldData#eleanorPassedOn} — a WorldData, so no dimension owns
 * it — and that record stops both worldgen and the travel-to-settlement roll
 * from ever producing a second Eleanor.
 *
 * <h2>Profession: HUSBANDRY</h2>
 *
 * §27 does not give the Lost Soul a job, because §11 files her as a quest NPC.
 * The ending that keeps her needs one, and §27's own Ghost-realm row for the
 * work is "Spirit Shepherd — Husbandry"; §12's Spirit Sheep and Grave Chicken
 * are the animals it is for. Husbandry is one of the five jobs vanilla withholds
 * from settlers by default ({@code AnimalKeeperHumanMob.java:26} is the only
 * place vanilla grants it), so a settlement with Eleanor in it can collect wool,
 * milk and eggs and one without her cannot.
 *
 * <h2>Home region</h2>
 *
 * Ghost Realm / Aftergarden (§10-§11) when it exists; the VEIL today. She never
 * travels to a settlement on her own — that is what {@code Settler
 * .addNewRecruitSettler} adding no ticket means, and it is deliberate: an
 * ending you can be handed by a visitor timer is not an ending.
 */
public class EleanorMob extends SkySettlerMob {

    /** Veil essence she needs before she can let go. */
    public static final int PASS_ON_COUNT = 12;

    /**
     * §11's "strong trinket".
     *
     * A vanilla trinket stands in, because this pass adds no new art:
     * {@code willowisplantern} is a wisp-lantern accessory
     * (ItemRegistry.java:946), which is the shape a soul that has let go would
     * leave behind. It is written down in {@code docs/VANILLA_ASSET_MAP.md} and
     * swaps for a mod trinket when the Ghost Realm ships one.
     */
    public static final String PASS_ON_REWARD = "willowisplantern";

    public EleanorMob() {
        super("eleanorsettler");

        // --- the profession -------------------------------------------------
        enableProfession("husbandry");

        // --- her shelf: what people leave at a grave -------------------------
        this.shop.addSellingItem("prettyflower", new SellingShopItem(3, 1))
                .setStaticPriceBasedOnHappiness(500, 1000, 100);
        this.shop.addSellingItem("prettybouquet", new SellingShopItem(1, 1))
                .setStaticPriceBasedOnHappiness(2200, 4000, 300);
        this.shop.addSellingItem("pottedflower1", new SellingShopItem(6, 2))
                .setStaticPriceBasedOnHappiness(60, 140, 15);
        this.shop.addSellingItem("pottedflower2", new SellingShopItem(6, 2))
                .setStaticPriceBasedOnHappiness(60, 140, 15);
        this.shop.addSellingItem("pottedflower3", new SellingShopItem(6, 2))
                .setStaticPriceBasedOnHappiness(60, 140, 15);
        this.shop.addSellingItem("lantern", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(50, 120, 14);
        this.shop.addSellingItem("waterlantern", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(60, 140, 15);

        // --- and what she will take off your hands ---------------------------
        this.shop.addBuyingItem("veilessence", new BuyingShopItem())
                .setPriceBasedOnHappiness(60, 38, 10);
        this.shop.addBuyingItem("ectoplasm", new BuyingShopItem())
                .setPriceBasedOnHappiness(38, 24, 7);
    }

    /**
     * The fork. Everything transactional that is NOT the pass-on happens in
     * vanilla's own dialogue window, which {@code super.interact} opens.
     */
    @Override
    public void interact(PlayerMob player) {
        if (!this.isServer() || !player.isServerClient()
                || this.isSettler() || this.isVisitor()) {
            super.interact(player);
            return;
        }
        ServerClient client = player.getServerClient();
        Level level = this.getLevel();

        InventoryItem held = player.getSelectedItem();
        boolean offering = held != null && held.item != null
                && "veilessence".equals(held.item.getStringID());
        if (!offering) {
            super.interact(player);
            return;
        }
        int have = player.getInv().main.getAmount(level, player,
                ItemRegistry.getItem("veilessence"), "eleanor");
        if (have < PASS_ON_COUNT) {
            // She says what she is short of. Without this the offering branch
            // would be invisible to a player holding eleven.
            this.bubble("eleanorneedessence");
            client.sendChatMessage(new LocalMessage("misc", "eleanorneedessencecount",
                    "have", String.valueOf(have), "need", String.valueOf(PASS_ON_COUNT)));
            super.interact(player);
            return;
        }

        // --- PASS ON --------------------------------------------------------
        player.getInv().main.removeItems(level, player,
                ItemRegistry.getItem("veilessence"), PASS_ON_COUNT, "eleanor");
        this.bubble("eleanorfarewell");
        client.sendChatMessage(new LocalMessage("misc", "eleanorpassedon"));
        give(client, PASS_ON_REWARD, 1);
        if (level.getServer() != null) {
            SkywatchWorldData.markEleanorPassedOn(level.getServer());
        }
        this.remove();
    }

    /** Reward hand-off; anything that does not fit drops at the player's feet. */
    private void give(ServerClient client, String itemStringID, int amount) {
        PlayerMob player = client.playerMob;
        Level level = player.getLevel();
        InventoryItem item = new InventoryItem(itemStringID, amount);
        boolean added = player.getInv().main.addItem(level, player, item, "eleanor", null);
        if (!added && item.getAmount() > 0) {
            level.entityManager.pickups.add(
                    new ItemPickupEntity(level, item, player.x, player.y, 0.0F, 0.0F));
        }
    }

    @Override protected int lookSeed() { return 0xE1EA07; }
    @Override protected HumanGender gender() { return HumanGender.FEMALE; }
    @Override protected Color shirtColor() { return new Color(196, 200, 214); }
    @Override protected Color shoesColor() { return new Color(120, 124, 138); }
    @Override protected String[] wardrobe() {
        // Pale and cold, out of vanilla's own wardrobe. No new art.
        return new String[]{"snowhood", "snowcloak", "clothboots"};
    }
    @Override protected int recruitCost() { return 5000; }
    @Override protected String talkKey() { return "eleanortalk"; }
}
