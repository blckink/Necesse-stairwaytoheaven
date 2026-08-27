package stairwaytoheaven.mobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchQuestData;

/**
 * The Sky Warden — the last keeper of the Skywatch, resident of the Old
 * Warden Spire and the entry point of the whole Skyreach progression.
 *
 * HOW HE IS RECRUITED (v0.5.2 — this replaces a hand-rolled flow):
 * he is hired through Necesse's OWN world-NPC recruitment, the same one the
 * Miner and the Explorer use. The player talks to him, the shop container
 * opens on its recruit page, the page states the price, and the recruit button
 * takes the coins and moves him in. Vanilla then teleports him to the
 * settlement level itself and registers him as a settler in one step, so he
 * can be given a bed immediately.
 *
 * WHAT WAS WRONG BEFORE, and why it is worth writing down. HumanMob's third
 * constructor argument is the SettlerRegistry key, and this class passed
 * "skywarden" — a key nothing ever registered. So {@code getSettler()} returned
 * null, vanilla's recruit path answered "notsettler", and the recruit button
 * could never work. The old code worked around that instead of fixing it: it
 * counted coins inside {@code interact()} and hand-spawned a SECOND mob on the
 * surface. Three player-visible bugs came out of that one workaround —
 *   · the 100,000 coins were taken by talking, with no dialogue option and no
 *     way to decline (a later patch added a confirm step, still not a real UI);
 *   · the Warden "disappeared" and turned up later in the village as a
 *     stranger who had to be recruited a SECOND time;
 *   · until that second recruitment he was not a settler, so he could not be
 *     assigned a bed and the settlement menu did not know where he was.
 * Passing the registered key and letting vanilla run the transaction removes
 * all three at once, and there is now exactly one Warden mob in a world.
 *
 * Progress lives in the level's {@link SkywatchQuestData}, shared by all
 * players.
 */
public class SkyWardenMob extends HumanShop {

    /**
     * The recruitment price. Intentional design value: it equals the top
     * vanilla settlement expansion tier (100,000 coins), so an endgame player
     * who has finished incursion-tier content pays a meaningful but achievable
     * lump sum — the single largest NPC purchase in the mod, benchmarked
     * against the wiki economy (Elder's priciest stock item is ~6,000).
     */
    public static final int RECRUIT_COST = 100_000;

    public SkyWardenMob() {
        // Third argument is the SettlerRegistry key, NOT a free-form type name.
        // "wardensettler" is the key SkyMobs registers WardenSettler under; any
        // other string makes getSettler() null and breaks recruitment entirely.
        super(500, 500, "wardensettler");
        this.canDespawn = false;
    }

    /**
     * The Miner's AI, which is vanilla's template for a hireable world NPC: he
     * mills about his spire and, once recruited, uses the same human brain
     * every settler does. He used to be pinned with {@code setSpeed(0)}, which
     * would have left him unable to walk to the bed the player assigns him.
     */
    @Override
    public void init() {
        super.init();
        this.ai = new BehaviourTreeAI<>(this, new HumanAI<>(320, true, false, 25000),
                new AIMover(HumanMob.humanPathIterations));
    }

    /**
     * The price, stated by vanilla's own recruit page. This is the whole
     * payment mechanism: {@code ShopContainer.canPayForRecruit} checks it and
     * {@code payForRecruit} takes it, server-side, only when the player presses
     * recruit. No coins can move by talking.
     */
    @Override
    public List<InventoryItem> getRecruitItems(ServerClient client) {
        return Collections.singletonList(new InventoryItem("coin", RECRUIT_COST));
    }

    /** Open on the recruit page until he has actually moved in. */
    @Override
    public boolean startInRecruitForm(ServerClient client) {
        return !this.isSettler();
    }

    /**
     * His own small talk. Without this override HumanMob falls back to
     * {@code mobmsg.humantalk1..5} and the last keeper of the Skywatch greets
     * you with "I often think about the big questions in life" — which is
     * exactly what a playtester screenshotted.
     */
    @Override
    protected ArrayList<GameMessage> getMessages(ServerClient client) {
        return getLocalMessages("misc", "wardentalk", 6);
    }

    /**
     * The line printed at the top of the dialogue window. While he is still a
     * keeper it is his recruitment pitch, so the offer sits directly above the
     * price and the recruit button instead of in a speech bubble that scrolls
     * away. Once he has moved in he makes small talk like any settler.
     */
    @Override
    public GameMessage getDialogueIntroMessage(ServerClient client) {
        return this.isSettler() ? super.getDialogueIntroMessage(client)
                                : new LocalMessage("misc", "wardenrecruit1");
    }

    /**
     * He cannot be killed. He is a one-of-a-kind story NPC with no random
     * replacement ({@code WardenSettler.getArriveAsRecruitAfterDeathChance} is
     * 0), and after a 100,000-coin purchase losing him to a stray mob would be
     * unrecoverable.
     */
    @Override
    public boolean canTakeDamage() {
        return false;
    }

    /**
     * First contact: the introduction, the journal hand-over, then vanilla's
     * own dialogue window. Everything transactional happens in that window —
     * this method only advances the story and never touches the player's
     * inventory.
     */
    @Override
    public void interact(PlayerMob player) {
        Level level = this.getLevel();
        // The story state lives on the Skyreach level. SkywatchQuestData.get
        // CREATES a blank record for whatever level it is handed, so calling it
        // on the surface would hand back a fresh stage-0 record and replay the
        // introduction every time a settled Warden is spoken to.
        boolean inTheSky = level instanceof stairwaytoheaven.level.SkyLevel;
        if (inTheSky && this.isServer() && player.isServerClient() && !this.isSettler()) {
            ServerClient client = player.getServerClient();
            SkywatchQuestData quest = SkywatchQuestData.get(level);
            if (quest.stage == 0) {
                Server server = level.getServer();
                quest.stage = 1;
                stairwaytoheaven.quest.SkyQuests.removeAllOfType(
                        server, stairwaytoheaven.quest.FindSpireQuest.class);
                stairwaytoheaven.quest.SkyQuests.giveOnce(server, client,
                        new stairwaytoheaven.quest.RecruitWardenQuest());
                // Three short lines. The playtest note was "too much text on
                // first contact: large bubble plus a duplicate-looking chat
                // block, full life story" -- the life story now lives in his
                // small talk and his recruitment pitch instead. The third line
                // stays because it explains the windsilk; an item that appears
                // in the inventory unannounced is worse than one more sentence.
                say(client, "wardenintro1");
                say(client, "wardenintro2");
                say(client, "wardenintro3");
                give(client, "windsilk", 6);
            }
        }
        if (this.isServer() && player.isServerClient()) {
            handleTurnIns(player.getServerClient());
        }

        // HumanShop.interact turns him to face the player, updates happiness
        // and opens the shop/recruit container.
        super.interact(player);
    }

    /**
     * The chapters after recruitment, turned in wherever he is standing now.
     *
     * Every one of these quests already existed, registered and fully
     * implemented, and none of them had ever been handed to a player or taken
     * back — so the whole chain after first contact was invisible in game.
     * Cats: the treat, the coax interaction, the travel-home puff and the
     * journal sync all shipped in v0.2. Anchor: DeliverItemsQuest tracks the
     * item counts and removes them in {@code complete} by itself.
     *
     * Order matters — one chapter opens the next — so this runs before
     * HumanShop opens its window, and each turn-in is idempotent because the
     * quest is removed as it is handed in.
     */
    private void handleTurnIns(ServerClient client) {
        Server server = client.getServer();
        stairwaytoheaven.quest.SpireCatsQuest cats = stairwaytoheaven.quest.SkyQuests
                .findHeld(client, stairwaytoheaven.quest.SpireCatsQuest.class);
        if (cats != null && cats.blackHome && cats.tabbyHome) {
            cats.complete(client);
            cats.remove();
            give(client, "catbasket", 1);
            give(client, "flickerlightgarland", 2);
            say(client, "wardencatsdone");
            stairwaytoheaven.quest.SkyQuests.giveOnce(server, client,
                    new stairwaytoheaven.quest.AnchorDeliveryQuest());
            return;
        }

        stairwaytoheaven.quest.AnchorDeliveryQuest anchor = stairwaytoheaven.quest.SkyQuests
                .findHeld(client, stairwaytoheaven.quest.AnchorDeliveryQuest.class);
        if (anchor != null && anchor.canComplete(client)) {
            // DeliverItemsQuest.complete removes the delivered items itself.
            anchor.complete(client);
            anchor.remove();
            give(client, "skywatchbanner", 1);
            give(client, "aurorapetal", 5);
            say(client, "wardenanchordone");
            Level sky = server.world.levelManager.isLoaded(SkyRegistry.SKYREACH_IDENTIFIER)
                    ? server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER)
                    : null;
            if (sky != null) {
                SkywatchQuestData.get(sky).anchorDone = true;
            }
        }
    }

    /**
     * The Skywatch wakes up. Vanilla has already changed this mob's level and
     * moved him into the settlement by the time this runs, so the beacon is
     * reached through the Skyreach level explicitly rather than through
     * {@code getLevel()}.
     */
    @Override
    public void onRecruited(ServerClient client, ServerSettlementData data, LevelSettler settler) {
        super.onRecruited(client, data, settler);
        if (client == null) {
            return;
        }
        Server server = client.getServer();
        Level sky = server.world.levelManager.isLoaded(SkyRegistry.SKYREACH_IDENTIFIER)
                ? server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER)
                : null;
        if (sky != null) {
            SkywatchQuestData quest = SkywatchQuestData.get(sky);
            quest.stage = 2;
            quest.recruited = true;
            quest.recruitedAuth = client.authentication;
            igniteBeacon(sky, quest);
        }

        stairwaytoheaven.quest.SkyQuests.removeAllOfType(
                server, stairwaytoheaven.quest.RecruitWardenQuest.class);
        // The next chapter. SpireCatsQuest, its two lair positions, the treat
        // item, the coax interaction and the travel-home puff have all shipped
        // since v0.2 and were fully working -- the quest was simply never given
        // to anyone, so no player could see it. This is the hand-out.
        stairwaytoheaven.quest.SkyQuests.giveOnce(server, client,
                new stairwaytoheaven.quest.SpireCatsQuest());

        // The keeper's Silver Bell changes hands here. It is the key the Seance
        // Circle checks for, and since the old cat quest that used to award it
        // is gone this is its only source — without it the Veil is unreachable.
        give(client, "silverbell", 1);
        say(client, "wardengivesbell");
        // Say where he went. Vanilla's own "joined settlement" line is sent by
        // the recruit packet, but a player who bought him in another dimension
        // deserves to be told he is already home and waiting for a bed.
        client.sendChatMessage(new LocalMessage("misc", "wardenmovedin",
                "settlement", data.networkData.getSettlementName()));
    }

    private void igniteBeacon(Level level, SkywatchQuestData quest) {
        swapObject(level, quest.beaconX, quest.beaconY,
                SkyRegistry.wardenBeaconOffID, SkyRegistry.wardenBeaconOnID);
    }

    private void swapObject(Level level, int tileX, int tileY, int expectedID, int newID) {
        level.regionManager.ensureTileIsLoaded(tileX, tileY);
        if (expectedID == 0 && level.getObjectID(tileX, tileY) != 0) {
            level.setObject(tileX, tileY, 0);
        }
        if (expectedID == 0 || level.getObjectID(tileX, tileY) == expectedID) {
            level.setObject(tileX, tileY, newID);
            level.getServer().network.sendToClientsWithTile(
                    new necesse.engine.network.packet.PacketChangeObject(level, 0, tileX, tileY, newID),
                    level, tileX, tileY);
        }
    }

    /**
     * His fixed face, shared with every form of him so recruiting the hooded
     * keeper does not produce a different, randomly generated man. See
     * {@link WardenIdentity}.
     */
    @Override
    public void randomizeLook(necesse.gfx.HumanLook look, necesse.gfx.HumanGender gender,
                              necesse.engine.util.GameRandom random) {
        this.gender = WardenIdentity.apply(look);
    }

    /**
     * HumanMob.setDefaultArmor delegates to the registered Settler, which
     * already dresses him — this override keeps the Skywatch clothes on him
     * before he is a settler at all.
     */
    @Override
    public void setDefaultArmor(necesse.gfx.drawOptions.human.HumanDrawOptions drawOptions) {
        WardenIdentity.dress(drawOptions);
    }

    /** Speech bubble for everyone nearby + chat line for the interacting player. */
    protected void say(ServerClient client, String miscKey) {
        GameMessage message = new LocalMessage("misc", miscKey);
        this.getLevel().getServer().network.sendToClientsWithEntity(
                new necesse.engine.network.packet.PacketMobChat(this.getUniqueID(), message), this);
        client.sendChatMessage(new LocalMessage("misc", "wardenchatformat", "name",
                new LocalMessage("misc", "wardenname").translate(), "line", message.translate()));
    }

    /** Give items to the player; anything that does not fit drops at their feet. */
    protected void give(ServerClient client, String itemStringID, int amount) {
        PlayerMob player = client.playerMob;
        Level level = player.getLevel();
        InventoryItem item = new InventoryItem(itemStringID, amount);
        boolean added = player.getInv().main.addItem(level, player, item, "skywatchreward", null);
        if (!added && item.getAmount() > 0) {
            level.entityManager.pickups.add(new ItemPickupEntity(level, item, player.x, player.y, 0.0F, 0.0F));
        }
    }
}
