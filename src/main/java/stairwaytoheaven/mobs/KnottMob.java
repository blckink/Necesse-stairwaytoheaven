package stairwaytoheaven.mobs;

import java.awt.Color;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.humanShop.BuyingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.HumanGender;
import necesse.level.maps.Level;
import stairwaytoheaven.quest.CrookedDoorQuest;
import stairwaytoheaven.quest.SkyQuests;
import stairwaytoheaven.quest.SkywatchWorldData;

/**
 * Mr. Knott, the Doorman — {@code docs/WORLD_DESIGN.md} §15.
 *
 * <p>{@code CrookedRealm}'s own class doc named him and the Architect as
 * "deferred... an NPC pass and a boss chapter, both out of scope here by
 * instruction" — that instruction was scoping THAT pass, not retiring him. He
 * is the settler job this one exists to finish.
 *
 * <h2>Profession: TRADING, and what that reads as here</h2>
 *
 * §27 has no row for the Doorman, so this borrows the closest fit rather than
 * inventing a mechanic: {@code tradingmission}, one of the five jobs vanilla
 * withholds from every settler by default (the same shape §27's speculative
 * "Vex" row uses for the same archetype — the settler who deals in access
 * rather than goods). He refuses {@code farming}/{@code forestry} the way
 * Caspern does: a shopkeeper standing at a door yard, not a field hand.
 *
 * <h2>His shop</h2>
 *
 * §15: "keys, doors, portals, weird furniture, cosmetic masks." The Reality
 * Stitcher — the station that would make Crooked doors and furniture real
 * inventory — is itself deferred ({@code CrookedRealm}'s own doc), so "doors"
 * and "keys" are not yet anything the game can hand a player. What the realm
 * already owns and can actually sell: two vanilla oddities that read as "weird
 * furniture" without a word of persuasion ({@code voidcube}, a violet
 * table ornament; {@code smallrunestone}, standing stone that does nothing
 * useful) and a shelf of vanilla cosmetic masks, which is §15's line made
 * literal. He is also the buyer for the realm's own stranger half — Warp Resin,
 * Eye Seed and Reality Shard — because a doorman is the one person here who
 * would actually want to know what reality is made of.
 *
 * <h2>Home region</h2>
 *
 * Crooked Beyond, at the Door Yard — §15's own "standing at a free-standing red
 * door" made literal by {@code DoorYardPreset}, the realm's own free-standing-door
 * POI. See {@code settlement.CrookedResidents}.
 *
 * <h2>The chain he hands out</h2>
 *
 * {@link CrookedDoorQuest} — see that class. Not a recruitment gate: he is
 * recruitable the moment he is found, like Mortimer and Caspern once found;
 * the chain is a second, separate reward track layered on top, the way
 * Eveleen's is.
 */
public class KnottMob extends SkySettlerMob {

    public KnottMob() {
        super("knottsettler");

        // --- the profession -------------------------------------------------
        enableProfession("tradingmission");
        refuseJob("farming");
        refuseJob("forestry");

        // --- "weird furniture" (§15) -----------------------------------------
        this.shop.addSellingItem("voidcube", new SellingShopItem(6, 1))
                .setStaticPriceBasedOnHappiness(220, 440, 45);
        this.shop.addSellingItem("smallrunestone", new SellingShopItem(6, 1))
                .setStaticPriceBasedOnHappiness(180, 360, 38);

        // --- "cosmetic masks" (§15) -------------------------------------------
        this.shop.addSellingItem("voidmask", new SellingShopItem(2, 1))
                .setStaticPriceBasedOnHappiness(1400, 2600, 220);
        this.shop.addSellingItem("alienmask", new SellingShopItem(3, 1))
                .setStaticPriceBasedOnHappiness(700, 1300, 120);
        this.shop.addSellingItem("sharkmask", new SellingShopItem(3, 1))
                .setStaticPriceBasedOnHappiness(700, 1300, 120);

        // --- and he buys the stranger half of the realm's own economy --------
        this.shop.addBuyingItem("warpresin", new BuyingShopItem())
                .setPriceBasedOnHappiness(26, 17, 5);
        this.shop.addBuyingItem("eyeseed", new BuyingShopItem())
                .setPriceBasedOnHappiness(34, 22, 6);
        this.shop.addBuyingItem("realityshard", new BuyingShopItem())
                .setPriceBasedOnHappiness(65, 42, 11);
    }

    /**
     * Handing out and turning in {@link CrookedDoorQuest} — the same shape
     * {@code EveleenMob}/{@code EleanorMob} use, kept out of {@code interact}
     * itself so the shop-opening flow below stays readable.
     *
     * <p>Not a recruitment gate: unlike Eveleen's chain, finding him already
     * makes him recruitable (he is a {@code SkySettlerMob}, so
     * {@code startInRecruitForm} needs nothing else), so this only ever adds a
     * reward on top — never withholds one. That is also why this is
     * deliberately NOT gated on {@code !isSettler() && !isVisitor()}: a player
     * who recruits him on the very first meeting — the one that already handed
     * out {@code CrookedDoorQuest} — before ever delivering the three
     * materials must still be able to turn it in later. Gating this on settler
     * state would make {@code advanceDoorChain} permanently unreachable the
     * moment he moves in, which is exactly the dead end
     * {@code docs/CONTENT_LEDGER.md}'s own {@code swh_beacon} already is:
     * a quest registered, handed out, and then unfinishable forever.
     * {@code advanceDoorChain} is idempotent by its own two guards
     * ({@code crookedDoorwayOpened} and {@code findHeld}), so calling it on
     * every interaction — settler or not — costs nothing.
     */
    @Override
    public void interact(PlayerMob player) {
        if (this.isServer() && player.isServerClient()) {
            Level level = this.getLevel();
            Server server = level == null ? null : level.getServer();
            if (server != null) {
                advanceDoorChain(server, player.getServerClient());
            }
        }
        super.interact(player);
    }

    private void advanceDoorChain(Server server, ServerClient client) {
        if (SkywatchWorldData.crookedDoorwayOpened(server)) {
            return;
        }
        CrookedDoorQuest quest = SkyQuests.findHeld(client, CrookedDoorQuest.class);
        if (quest == null) {
            SkyQuests.removeAllOfType(server, stairwaytoheaven.quest.CrookedArrivalQuest.class);
            SkyQuests.giveOnce(server, client, new CrookedDoorQuest());
            this.bubble("knottasksdoor");
            return;
        }
        if (!quest.canComplete(client)) {
            return;
        }
        quest.complete(client);
        SkyQuests.removeAllOfType(server, CrookedDoorQuest.class);
        SkywatchWorldData.markCrookedDoorwayOpened(server);
        give(client, "zephyrharness", 1);
        give(client, "stormsteelbar", 12);
        give(client, "realityshard", 6);
        client.sendChatMessage(new LocalMessage("misc", "knottdoordone"));
    }

    /** Reward hand-off; anything that does not fit drops at the player's feet. */
    private void give(ServerClient client, String itemStringID, int amount) {
        PlayerMob player = client.playerMob;
        Level level = player.getLevel();
        necesse.inventory.InventoryItem item = new necesse.inventory.InventoryItem(itemStringID, amount);
        boolean added = player.getInv().main.addItem(level, player, item, "knott", null);
        if (!added && item.getAmount() > 0) {
            level.entityManager.pickups.add(
                    new necesse.entity.pickup.ItemPickupEntity(level, item, player.x, player.y, 0.0F, 0.0F));
        }
    }

    @Override protected int lookSeed() { return 0x6707701; }
    @Override protected HumanGender gender() { return HumanGender.MALE; }
    @Override protected Color shirtColor() { return new Color(140, 40, 46); }
    @Override protected Color shoesColor() { return new Color(30, 30, 34); }
    @Override protected String[] wardrobe() {
        // A jester's hat over a lab coat: three vanilla item IDs, no new art,
        // and the combination is the joke -- a showman testing whether a door
        // actually leads anywhere.
        return new String[]{"jesterhat", "labcoat", "jesterboots"};
    }
    @Override protected int recruitCost() { return 22000; }
    @Override protected String talkKey() { return "knotttalk"; }
}
