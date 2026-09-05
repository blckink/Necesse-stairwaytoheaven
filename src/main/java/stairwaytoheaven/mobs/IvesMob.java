package stairwaytoheaven.mobs;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import stairwaytoheaven.quest.SkyQuests;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.quest.SteinfeldVigilQuest;

/**
 * Ives, the Verger of the Quiet Reach — Steinfeld's first inhabitant.
 *
 * <h2>Why Steinfeld gets a person, and why this one</h2>
 * {@code docs/WORLD_DESIGN.md} Part B, in its own list of what the concept does
 * not answer: <i>"Steinfeld has no NPC, no boss and no station."</i>
 * {@code docs/AREA_OVERVIEW.md} measured the cost of that: a band 2280 tiles
 * deep with four hostiles, no critters, no animals and nobody to talk to, in
 * the middle of the climb. It is the emptiest realm the mod claims to have
 * finished.
 *
 * <p>The character comes from §A3.4, which is the strongest line the design
 * gives this realm:
 *
 * <blockquote><i>"Es ist der Ort, an dem der Himmel aufhört richtig zu
 * funktionieren. Hier landen Dinge, die nicht mehr richtig zum Himmel
 * gehören."</i></blockquote>
 *
 * A verger is the person who keeps a churchyard — who sweeps up after
 * everything that has stopped belonging anywhere. §A3.4 also says the ghosts
 * here are mostly not enemies: <i>"Some simply stand. Some walk without
 * purpose. One might walk the same path between two gravestones forever."</i>
 * Ives is the one person out here who has decided that is somebody's job to see
 * to, and {@link SteinfeldVigilQuest} is him asking for the salt and the moss to
 * do it with.
 *
 * <h2>His shop: the realm's own materials, finally sold to somebody</h2>
 * {@code docs/OVERVIEW.md} §8.9: Steinfeld's four materials — Pale Stone, Grave
 * Salt, Spirit Moss and Echo Shard — are <i>"droppable and sellable, and no
 * recipe anywhere names any of them"</i>. Sellable to whom, though: the realm
 * had no vendor at all, so a player mining it carried the ore back to a Skyreach
 * broker at broker rates. He buys all four, above broker, which is what makes
 * the ground worth digging before the key quest asks for two of them.
 *
 * <p>What he SELLS is a churchyard: vanilla's own gravestones, urn, candles and
 * the stone fence and gate that go around them, plus the realm's own Pale Stone
 * to build with. Every ID is one the mod or the game already registers, so the
 * shelf costs no new art — the same working method {@code MortimerMob}'s shop
 * uses. Steinfeld's own deco objects (the mourner statue, the broken angel, the
 * chapel column, the heaven slab) are deliberately NOT on it: all four register
 * {@code isObtainable = false}, so there is no item behind any of them to sell.
 *
 * <h2>Profession: none withheld, and that is deliberate</h2>
 * Vanilla withholds exactly five job types from ordinary settlers
 * ({@code JobTypeRegistry}) and the mod already spends all five — fertilise on
 * Eveleen, husbandry on Eleanor, fishing on Halda, hunting on Mortimer, trading
 * missions on Magpie and Knott. Ives takes none of them. He is crafting and
 * hauling like Caspern and Ossian, refuses farming and forestry the way
 * {@code MortimerMob} does and for the same reason, and his character is his
 * shop and his quest. Inventing a sixth "profession" by giving him a job vanilla
 * hands out freely would be a label, not a mechanic;
 * {@code docs/OVERVIEW.md} §5 records the four real mechanisms still open and
 * an expedition is the one that fits a verger, which is a content family of its
 * own rather than a line in this constructor.
 *
 * <h2>No new art</h2>
 * Wardrobe and settler icon are vanilla items and a vanilla face, borrowed by
 * literal path exactly as Mortimer, Caspern and Eleanor are, and recorded in
 * {@code docs/VANILLA_ASSET_MAP.md}.
 */
public class IvesMob extends SkySettlerMob {

    /**
     * Between Mortimer's 8 000 and Caspern's 14 000, because Steinfeld sits
     * between Eden and the Ghost band on the climb and a settler's price is
     * the mod's plainest statement of how far out you had to go to find them.
     */
    private static final int RECRUIT_COST = 11000;

    /** Bars for the vigil. Ten, matching Eveleen's — see the quest's own doc. */
    public static final int VIGIL_BARS = 10;

    public IvesMob() {
        super("ivessettler");

        // --- the profession -------------------------------------------------
        // Crafting and hauling, which every settler has. A verger does not farm
        // and does not fell trees; the same call MortimerMob makes, and vanilla
        // makes for its Guard (GuardHumanMob.java:31-34).
        refuseJob("farming");
        refuseJob("forestry");

        // --- what a churchyard keeper sells ---------------------------------
        this.shop.addSellingItem("gravestone1", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(90, 200, 20);
        this.shop.addSellingItem("gravestone2", new SellingShopItem(10, 2))
                .setStaticPriceBasedOnHappiness(90, 200, 20);
        this.shop.addSellingItem("candle", new SellingShopItem(40, 8))
                .setStaticPriceBasedOnHappiness(14, 34, 6);
        this.shop.addSellingItem("vase", new SellingShopItem(8, 2))
                .setStaticPriceBasedOnHappiness(70, 150, 15);
        // The churchyard wall. Mortimer sells what goes IN a grave; Ives sells
        // what goes around one, so the two shelves do not read as one shop
        // split in half.
        this.shop.addSellingItem("stonefence", new SellingShopItem(30, 8))
                .setStaticPriceBasedOnHappiness(20, 44, 6);
        this.shop.addSellingItem("stonefencegate", new SellingShopItem(6, 2))
                .setStaticPriceBasedOnHappiness(40, 90, 10);

        // --- and the realm's own stone, which nobody else sells --------------
        this.shop.addSellingItem("palestone", new SellingShopItem(40, 10))
                .setStaticPriceBasedOnHappiness(18, 40, 6);

        // --- what he takes off your hands -----------------------------------
        // All four Steinfeld materials, above broker. Before this the realm had
        // no vendor at all and its ground paid broker rates in the Skyreach.
        this.shop.addBuyingItem("palestone", new BuyingShopItem())
                .setPriceBasedOnHappiness(12, 7, 3);
        this.shop.addBuyingItem("gravesalt", new BuyingShopItem())
                .setPriceBasedOnHappiness(34, 22, 6);
        this.shop.addBuyingItem("spiritmoss", new BuyingShopItem())
                .setPriceBasedOnHappiness(40, 26, 7);
        this.shop.addBuyingItem("echoshard", new BuyingShopItem())
                .setPriceBasedOnHappiness(75, 48, 12);
    }

    /**
     * The vigil, run before vanilla's dialogue window opens. Same shape as
     * {@code MortimerMob.interact} and {@code EveleenMob.interact}: idempotent
     * in every branch, and deliberately not gated on {@code !isSettler()} so a
     * player who paid his fee on the first meeting can still finish the chain.
     */
    @Override
    public void interact(PlayerMob player) {
        if (this.isServer() && player.isServerClient()) {
            Level level = this.getLevel();
            Server server = level == null ? null : level.getServer();
            advanceVigil(server, player.getServerClient());
        }
        super.interact(player);
    }

    private void advanceVigil(Server server, ServerClient client) {
        SkyQuests.Step step = SkyQuests.advanceResidentChain(server, client,
                SkywatchWorldData.CHAIN_STEINFELD_VIGIL, new SteinfeldVigilQuest());
        if (step == SkyQuests.Step.ASKED) {
            this.bubble("ivesasksvigil");
        } else if (step == SkyQuests.Step.PAID) {
            giveReward(client, "stormsteelbar", VIGIL_BARS, "ives");
            this.bubble("ivesvigildone");
        }
    }

    /** The price, waived once the graves are laid. See {@code EveleenMob}. */
    @Override
    public List<InventoryItem> getRecruitItems(ServerClient client) {
        Level level = this.getLevel();
        Server server = level == null ? null : level.getServer();
        if (server != null && SkywatchWorldData.residentChainDone(
                server, SkywatchWorldData.CHAIN_STEINFELD_VIGIL)) {
            return Collections.emptyList();
        }
        return super.getRecruitItems(client);
    }

    @Override protected int lookSeed() { return 0x17E5C0; }
    @Override protected HumanGender gender() { return HumanGender.MALE; }
    /** Pale grey-green: §A3.4's realm, where the gold oxidises and darkens. */
    @Override protected Color shirtColor() { return new Color(104, 112, 106); }
    @Override protected Color shoesColor() { return new Color(66, 62, 58); }
    @Override protected String[] wardrobe() {
        // A hood, a plain robe and cloth boots — three vanilla item IDs, no new
        // art, and the only wardrobe in the mod's cast that reads as somebody
        // who works outdoors in fog rather than as a merchant.
        return new String[]{"leatherhood", "clothrobe", "clothboots"};
    }
    @Override protected int recruitCost() { return RECRUIT_COST; }
    @Override protected String talkKey() { return "ivestalk"; }
}
