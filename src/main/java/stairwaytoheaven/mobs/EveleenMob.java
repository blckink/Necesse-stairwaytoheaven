package stairwaytoheaven.mobs;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import stairwaytoheaven.quest.EdenArrivalQuest;
import stairwaytoheaven.quest.EdenPlantsQuest;
import stairwaytoheaven.quest.SkyQuests;
import stairwaytoheaven.quest.SkywatchWorldData;

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
 * The Garden of Eden (§5) — found beside a Knowledge Tree, one per world; see
 * {@code EdenLevel.placeResident}. She can also still reach a settlement that
 * already has Eden grass growing in it without the player ever visiting Eden
 * ({@code SkyArrivals.EDEN_PATCH}); both routes share one claim, so the world
 * only ever grows one Eveleen.
 *
 * <h2>The chain she hands out</h2>
 *
 * {@link stairwaytoheaven.quest.EdenArrivalQuest} then
 * {@link stairwaytoheaven.quest.EdenPlantsQuest} — see {@link #interact}.
 * Completing it makes her join for free: her recruit fee is normally
 * {@link #recruitCost()}, and {@link #getRecruitItems} waives it once
 * {@code SkywatchWorldData.edenPlantsGiven} is true.
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

    /**
     * Hands out and turns in {@link EdenArrivalQuest}/{@link EdenPlantsQuest}
     * before opening her shop/recruit page, the same shape
     * {@code EleanorMob.interact} uses for the Ghost chain. Runs only while she
     * is neither a settler nor a visitor — once she has moved in the chain is
     * either already finished or moot.
     */
    @Override
    public void interact(PlayerMob player) {
        if (this.isServer() && player.isServerClient() && !this.isSettler() && !this.isVisitor()) {
            Level level = this.getLevel();
            Server server = level == null ? null : level.getServer();
            if (server != null) {
                advanceEdenChain(server, player.getServerClient());
            }
        }
        super.interact(player);
    }

    private void advanceEdenChain(Server server, ServerClient client) {
        if (SkywatchWorldData.edenPlantsGiven(server)) {
            return;
        }
        EdenPlantsQuest quest = SkyQuests.findHeld(client, EdenPlantsQuest.class);
        if (quest == null) {
            // First meeting: the signpost that led here is done, the ask begins.
            SkyQuests.removeAllOfType(server, EdenArrivalQuest.class);
            SkyQuests.giveOnce(server, client, new EdenPlantsQuest());
            this.bubble("eveleenasksplants");
            return;
        }
        if (!quest.canComplete(client)) {
            return;
        }
        quest.complete(client); // DeliverItemsQuest.complete removes the delivered items itself.
        SkyQuests.removeAllOfType(server, EdenPlantsQuest.class);
        SkywatchWorldData.markEdenPlantsGiven(server);
        // The Knowledge Cutting closes the loop the chain opened with "find
        // the Knowledge Tree"; the bar stack is the endgame payout benchmarked
        // against the Skyreach finale (docs/BALANCE.md), same as every other
        // chain this pass adds.
        give(client, "knowledgecutting", 3);
        give(client, "stormsteelbar", 10);
        this.bubble("eveleenplantsdone");
        client.sendChatMessage(new LocalMessage("misc", "edenplantsdone"));
    }

    /** Reward hand-off; anything that does not fit drops at the player's feet. */
    private void give(ServerClient client, String itemStringID, int amount) {
        PlayerMob player = client.playerMob;
        Level level = player.getLevel();
        InventoryItem item = new InventoryItem(itemStringID, amount);
        boolean added = player.getInv().main.addItem(level, player, item, "eveleen", null);
        if (!added && item.getAmount() > 0) {
            level.entityManager.pickups.add(
                    new necesse.entity.pickup.ItemPickupEntity(level, item, player.x, player.y, 0.0F, 0.0F));
        }
    }

    /**
     * The price, stated by vanilla's own recruit page — waived once she has
     * her three plants. {@code SkySettlerMob}'s own doc is explicit that this
     * IS the payment mechanism and must never be hand-rolled in
     * {@code interact()}; an empty list is exactly what a free recruit is.
     */
    @Override
    public List<InventoryItem> getRecruitItems(ServerClient client) {
        Level level = this.getLevel();
        Server server = level == null ? null : level.getServer();
        if (server != null && SkywatchWorldData.edenPlantsGiven(server)) {
            return Collections.emptyList();
        }
        return super.getRecruitItems(client);
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
