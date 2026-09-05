package stairwaytoheaven.mobs;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import necesse.engine.expeditions.SettlerExpedition;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.ExpeditionList;
import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import stairwaytoheaven.settlement.SkyVoyages;

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

        // --- her PROFESSION: trading missions ------------------------------
        // The one job in the game that means "goes away and comes back with
        // goods", and vanilla grants it to exactly one settler type
        // (TraderHumanMob.java:29). It is withheld from every other settler by
        // default, so a settlement with Magpie in it can run trading missions
        // and one without her cannot. For a courier this is not decoration: it
        // is the same verb her whole character is written around.
        enableProfession("tradingmission");

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

    // --- her SECOND profession: sky voyages ----------------------------
    //
    // The mod's own special task, registered in
    // stairwaytoheaven.settlement.SkyVoyages — see that class for what makes
    // an expedition category a new profession rather than a borrowed one, and
    // for the finding that a modded object can never be a mission board.
    //
    // The two overrides below are the whole settler side of it, and they are
    // the same two vanilla's Miner writes (MinerHumanMob.java:146 and :153).
    // No job type is involved: vanilla's `expeditions` type is enabled for
    // every settler by default (JobTypeRegistry.java:23 passes
    // defaultDisabledBySettler = false), so what decides who may be sent is
    // canDoExpedition alone — which is why no vanilla Explorer, Miner or
    // Angler can take a sky voyage, and why Magpie can.
    //
    // Her trading missions above are NOT replaced. They are a different
    // mechanism (a shipping chest, LevelJobRegistry's `starttrading`), they
    // sell the settlement's surplus rather than fetch materials, and taking
    // one away from a settler a player has already paid 12,000 coins for is
    // a decision for the user, not a side effect of adding a second job.
    // Splitting them — Magpie voyages, Knott trades — is written up as an
    // open question in docs/design/settler-professions.md.

    @Override
    public boolean canDoExpedition(SettlerExpedition expedition) {
        return SkyVoyages.isVoyage(expedition);
    }

    /**
     * The "I want to send you somewhere" page on her own dialogue, beside the
     * mission board's fourth category button. Unavailable voyages stay in the
     * list and grey out — {@code ExpeditionList} keeps them and shows
     * {@code getUnavailableMessage} — so a player can see the whole ladder and
     * what opening the next realm would buy them.
     */
    @Override
    public List<ExpeditionList> getPossibleExpeditions() {
        if (this.isSettlerWithinSettlement()) {
            ServerSettlementData data = this.getSettlerSettlementServerData();
            if (data != null) {
                return Collections.singletonList(new ExpeditionList(
                        new LocalMessage("ui", "skyvoyageask"),
                        new LocalMessage("ui", "skyvoyageselect"),
                        new LocalMessage("ui", "skyvoyagecost"),
                        new LocalMessage("ui", "skyvoyagemore"),
                        data, this, SkyVoyages.all()));
            }
        }
        return super.getPossibleExpeditions();
    }

    /** What the icon over her head says when she is back with a full pack. */
    @Override
    public GameMessage getWorkInvNotificationMessage() {
        return this.completedMission
                ? new LocalMessage("ui", "skyvoyagecomplete")
                : super.getWorkInvNotificationMessage();
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
