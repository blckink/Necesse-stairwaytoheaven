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
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.quest.SkywatchWorldData;

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
     * The recruitment price. Still the single largest NPC purchase in the mod
     * — the Elder's priciest stock item is around 6,000 — but deliberately
     * reachable well before the top vanilla settlement-expansion tier, because
     * the Warden is the ENTRY to the Skyreach's content and pricing him at
     * endgame wealth gates the whole layer behind it.
     *
     * <p>This comment used to justify 100,000 while the constant said 30,000:
     * `5ce05ae` dropped the fee and updated neither this nor
     * `DESIGN_DECISIONS.md` nor `CURRENT_STATE.md`. All three now agree with
     * the constant, which is the value the integration test observes.
     */
    public static final int RECRUIT_COST = 30_000;

    /**
     * One-shot guard for {@link #serverTick()}: the world record is stamped the
     * first tick this mob knows it is a settler, not on every tick.
     */
    private boolean stampedWorldRecord = false;

    /**
     * What he has said so far this conversation, waiting for {@link #flushSay}.
     * Never read outside the server thread that ran {@code interact}.
     */
    private final java.util.ArrayList<GameMessage> pendingSay = new java.util.ArrayList<>();

    /** What a replacement Silver Bell costs at the Warden, in coins. */
    public static final int SPARE_BELL_PRICE = 5000;

    /**
     * What a replacement Ghost Chalk costs at the Warden, in coins.
     *
     * <p>{@code ghostchalk} registers at brokerValue 120.0F
     * ({@code SkyItems.register}), so 1200 is a flat <b>10x broker</b>. That
     * multiplier is vanilla's own, not invented: {@code FriendlyWitchHumanMob}
     * prices her whole potion shelf with
     * {@code setStaticBrokerPriceBasedOnHappiness(3.0F, 6.0F, 2.0F)} and her
     * cauldron with {@code (10.0F, 20.0F, 2.0F)}
     * (FriendlyWitchHumanMob.java:94, :113 — <b>VERIFIED [jar]</b>), i.e. a
     * vanilla shop sells between 3x and 20x broker value. The Silver Bell's own
     * line above sits at the top of that range (5000 on a 250.0F bell = 20x)
     * because it is a permanent key; the chalk is spent on use and A1 insists a
     * lost piece must never be a dead end, so it sits at the bottom.
     */
    public static final int SPARE_CHALK_PRICE = 1200;

    public SkyWardenMob() {
        // Third argument is the SettlerRegistry key, NOT a free-form type name.
        // "wardensettler" is the key SkyMobs registers WardenSettler under; any
        // other string makes getSettler() null and breaks recruitment entirely.
        super(500, 500, "wardensettler");
        this.canDespawn = false;
        // The Silver Bell is the Veil's only key, and onRecruited hands exactly
        // one of them to whoever paid the recruitment. In multiplayer that left
        // every other player locked out of a whole dimension with no way in,
        // because the bell is deliberately not craftable. So the keeper keeps
        // spares: a real vanilla shop line, reachable only once he is a settler
        // (before that his container opens on the recruit page instead).
        //
        // Stocked and restocking rather than unlimited, so it reads as a keeper
        // handing out the spares he has rather than a vending machine.
        this.shop.addSellingItem("silverbell", new SellingShopItem(2, 1))
                .setStaticPrice(SPARE_BELL_PRICE, SPARE_BELL_PRICE);
        // ...and the Ghost Chalk, on exactly the same line shape and for
        // exactly the same reason (docs/FOGKEY_AND_BOSSPORTALS.md A1): "The
        // Warden hands it over the first time that player has stood in Soul
        // Exposure fog, and sells replacements from then on... A lost piece is
        // never a dead end because he restocks it."
        //
        // Stocked 3, restocking 1 a day rather than the bell's 2/1: the chalk
        // is consumed by drawing the ring, so a player who moves their base
        // twice in a week needs him to have more than one on the shelf.
        this.shop.addSellingItem("ghostchalk", new SellingShopItem(3, 1))
                .setStaticPrice(SPARE_CHALK_PRICE, SPARE_CHALK_PRICE);
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
        return !this.isSettler() && !worldHasWarden(client.getServer());
    }

    /**
     * One Warden per world, and vanilla enforces it for us.
     *
     * {@code PacketShopContainerUpdate} calls this both to build the settlement
     * dropdown and again server-side before {@code payForRecruit} runs, so
     * returning false here means the fee can never be taken -- not merely that
     * the button is hidden. Without it a world that bumped its generation
     * (fresh Skyreach, fresh {@link SkywatchQuestData}) would stamp a new spire
     * with a new keeper standing in it, and the player who already paid 30,000
     * for the Warden now sitting in their settlement could be charged again for
     * a duplicate.
     */
    @Override
    public boolean isValidRecruitment(necesse.level.maps.levelData.settlementData.CachedSettlementData settlement,
                                      ServerClient client) {
        if (!super.isValidRecruitment(settlement, client)) {
            return false;
        }
        return this.isSettler() || !worldHasWarden(client == null ? null : client.getServer());
    }

    /**
     * The settler stamps the world record himself.
     *
     * The record has to be true BEFORE the player first climbs into a freshly
     * generated Skyreach, otherwise the spire is stamped with a second keeper
     * (and {@code isValidRecruitment} above only prevents the second payment,
     * not the confusing second Warden). Dialogue is too late for that: the
     * surface Warden may never be talked to first. He is on the surface level,
     * which is loaded whenever the player is on it, so his own tick is the
     * earliest reliable moment -- and it costs one boolean read per tick after
     * the first.
     */
    @Override
    public void serverTick() {
        super.serverTick();
        if (this.stampedWorldRecord || !this.isSettler()) {
            return;
        }
        Level level = this.getLevel();
        Server server = level == null ? null : level.getServer();
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world != null) {
            world.markRecruited(0L);
            this.stampedWorldRecord = true;
        }
    }

    /** Whether this world already recruited a Sky Warden. Null-safe. */
    private static boolean worldHasWarden(Server server) {
        return server != null && SkywatchWorldData.hasWarden(server);
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
                // Three short lines. The playtest note was "too much text on
                // first contact: large bubble plus a duplicate-looking chat
                // block, full life story" -- the life story now lives in his
                // small talk and his recruitment pitch instead. The third line
                // stays because it explains the windsilk; an item that appears
                // in the inventory unannounced is worse than one more sentence.
                //
                // These are collected, not sent: flushSay below puts all three
                // in ONE bubble, because a bubble replaces the last one
                // (ChatBubbleText.java:67-76, VERIFIED [jar]) and only the
                // third would otherwise survive.
                say(client, "wardenintro1");
                say(client, "wardenintro2");
                say(client, "wardenintro3");
                give(client, "windsilk", 6);
            }
        }
        if (this.isServer() && player.isServerClient()) {
            offerChalk(player.getServerClient());
            advanceChain(player.getServerClient());
            advanceRegionKeys(player.getServerClient());
        }
        // Everything the three calls above wanted to say, as one bubble.
        this.flushSay();

        // HumanShop.interact turns him to face the player, updates happiness
        // and opens the shop/recruit container.
        super.interact(player);
    }

    /**
     * The Ghost Chalk, once, to whoever has been to the fog.
     *
     * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} A1. Everything about this is
     * per PLAYER and nothing about it is per world, which is the whole point of
     * the section it comes from:
     *
     * <blockquote>"ein NPC der in der nähe spawnt" is a race in multiplayer:
     * two players reach the fog together, one NPC spawns, one chalk. So the
     * grant is per player ... Every player earns their own piece the first time
     * <i>they</i> touch fog.</blockquote>
     *
     * <p>Both halves of the condition therefore live in
     * {@code veil/VeilWorldData}, keyed on {@code ServerClient.authentication}
     * exactly as the Veil Mark already is — {@code hasTouchedFog} is written by
     * the region check that already runs once a second for every online player,
     * and {@code markChalkGiven} is this method's own ledger. Neither goes near
     * {@code quest/SkywatchWorldData}, which is world-scoped and would hand the
     * second player's chalk to whoever asked first.
     *
     * <p>It is checked on EVERY conversation rather than only on recruitment,
     * because the fog is reached long after he moves in: the trigger is the
     * player walking to the wall, and this is the next time they see him.
     */
    private void offerChalk(ServerClient client) {
        Level level = this.getLevel();
        Server server = level == null ? null : level.getServer();
        stairwaytoheaven.veil.VeilWorldData veil = stairwaytoheaven.veil.VeilWorldData.get(server);
        if (veil == null || !veil.hasTouchedFog(client.authentication)) {
            return;
        }
        // markChalkGiven returns true only when the ledger did not already hold
        // this character, so the gift cannot be farmed by talking twice.
        if (!veil.markChalkGiven(client.authentication)) {
            return;
        }
        say(client, "wardengiveschalk");
        give(client, "ghostchalk", 1);
    }

    /**
     * The Skyreach's story record, from wherever this Warden is standing.
     *
     * WHY THIS FORCE-LOADS. The previous version asked
     * {@code levelManager.isLoaded(SKYREACH_IDENTIFIER)} and gave up when the
     * answer was no. But a level with no players on it is unloaded by the
     * server after {@code Settings.unloadLevelsCooldown} (jar 1.3.2,
     * Server.java:365-375) — so the ordinary case, a player who came down from
     * the sky, played on the surface for a minute and then walked over to the
     * Warden in the village, hit {@code sky == null} and skipped the ENTIRE
     * catch-up: no cats quest, no lair markers, no anchor chapter, and the
     * anchor's completion flag never written back. That is a player reporting
     * "warden gibt weiterhin keine quests die ich finden kann" on a build that
     * contains the fix.
     *
     * {@code World.getLevel} loads the level when it is missing (World.java:273)
     * and returns the loaded one otherwise, so this is a small disk read at most
     * once per conversation-after-unload — on a deliberate player action, in the
     * dimension the whole conversation is about.
     */
    private static Level skyLevel(Server server) {
        return server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
    }

    /**
     * Which chapter of "The Warden's Call" a world is owed, as a pure function
     * of the world record. No journal, no client, no level — so it can be
     * enumerated over every reachable save state and asserted
     * (see {@code /skyreachstatus}, "chain check").
     *
     * The point of writing it this way is the class of bug it exists to kill.
     * The old code reacted to quests the player was ALREADY HOLDING, which left
     * three states that could never produce a quest again:
     *
     *  · a world that met the Warden under an older build (stage already 1, not
     *    recruited): {@code RecruitWardenQuest} was only handed out inside the
     *    {@code stage == 0} branch, so talking to him again gave nothing;
     *  · a world where BOTH cats were already home before the cats quest was
     *    ever given — reachable today, because {@code SpireCatMob.interact} sets
     *    blackHome/tabbyHome for anyone holding a treat, quest or not. The old
     *    catch-up was gated on {@code !(blackHome && tabbyHome)}, so it refused
     *    to hand out the quest, and the turn-in branch needed a held quest to
     *    fire: no reward, and the anchor chapter never opened;
     *  · a world past the cats with no anchor quest held: nothing re-issued it.
     *
     * {@code DONE} is the only chapter that hands out nothing, and it is only
     * reachable once the whole chain has actually been finished.
     */
    public enum Chapter {
        /** Hire him — hand out RecruitWardenQuest. */
        RECRUIT,
        /** Coax the cats — hand out SpireCatsQuest and the lair markers. */
        CATS,
        /** Both cats are home and unpaid for — turn the chapter in. */
        CATS_TURNIN,
        /** Anchor the island — hand out AnchorDeliveryQuest. */
        ANCHOR,
        /** The chain is finished. */
        DONE
    }

    /**
     * @param isSettler whether the Warden the player is talking to has already
     *                  moved into a settlement. He can only be a settler if the
     *                  world recruited him, so a settler plus
     *                  {@code recruited == false} is a broken record, not a
     *                  state — it is repaired rather than believed.
     */
    public static Chapter chapterFor(SkywatchQuestData quest, boolean isSettler) {
        if (!quest.recruited && !isSettler) {
            return Chapter.RECRUIT;
        }
        if (!(quest.blackHome && quest.tabbyHome)) {
            return Chapter.CATS;
        }
        if (!quest.catsRewardGiven) {
            return Chapter.CATS_TURNIN;
        }
        return quest.anchorDone ? Chapter.DONE : Chapter.ANCHOR;
    }

    // ------------------------------------------------------------------
    // The region keys (docs/FOGKEY_AND_BOSSPORTALS.md §B1-B2)
    // ------------------------------------------------------------------

    /**
     * One realm's key-piece quest: what it asks, what it pays, what it says.
     *
     * <p>The order of the constants IS the order the Warden offers them, and it
     * is §B4's own boss ladder — 57k Cryo Queen, 127k Moonlight Dancer, 158k
     * Ascended Wizard, 161k Pest Warden, 208k Crystal Dragon — so a player
     * walking this list is walking that table. Hell has no constant because it
     * has no boss portal to unlock.
     *
     * <p>Each constant carries its quest CLASS (for
     * {@code SkyQuests.findHeld}) and a factory for a fresh one, because
     * {@code QuestRegistry} keys quests by class and a shared parameterised
     * quest would collapse five journal entries into one.
     */
    private enum RegionKey {
        SKYREACH(stairwaytoheaven.worldgen.RealmDepth.REALM_SKYREACH,
                stairwaytoheaven.quest.SkyreachKeyQuest.class,
                "regionkeyskyreach", "stormsteelbar", 6,
                "wardenkeyaskskyreach", "wardenkeydoneskyreach") {
            @Override
            stairwaytoheaven.quest.SkyreachKeyQuest newQuest() {
                return new stairwaytoheaven.quest.SkyreachKeyQuest();
            }
        },
        EDEN(stairwaytoheaven.worldgen.RealmDepth.REALM_EDEN,
                stairwaytoheaven.quest.EdenKeyQuest.class,
                "regionkeyeden", "stormsteelbar", 8,
                "wardenkeyaskeden", "wardenkeydoneeden") {
            @Override
            stairwaytoheaven.quest.EdenKeyQuest newQuest() {
                return new stairwaytoheaven.quest.EdenKeyQuest();
            }
        },
        STEINFELD(stairwaytoheaven.worldgen.RealmDepth.REALM_STEINFELD,
                stairwaytoheaven.quest.SteinfeldKeyQuest.class,
                "regionkeysteinfeld", "stormsteelbar", 10,
                "wardenkeyasksteinfeld", "wardenkeydonesteinfeld") {
            @Override
            stairwaytoheaven.quest.SteinfeldKeyQuest newQuest() {
                return new stairwaytoheaven.quest.SteinfeldKeyQuest();
            }
        },
        GHOST(stairwaytoheaven.worldgen.RealmDepth.REALM_GHOST,
                stairwaytoheaven.quest.GhostKeyQuest.class,
                "regionkeyghostrealm", "spiritsteelbar", 10,
                "wardenkeyaskghostrealm", "wardenkeydoneghostrealm") {
            @Override
            stairwaytoheaven.quest.GhostKeyQuest newQuest() {
                return new stairwaytoheaven.quest.GhostKeyQuest();
            }
        },
        CROOKED(stairwaytoheaven.worldgen.RealmDepth.REALM_CROOKED,
                stairwaytoheaven.quest.CrookedKeyQuest.class,
                "regionkeycrookedbeyond", "spiritsteelbar", 12,
                "wardenkeyaskcrookedbeyond", "wardenkeydonecrookedbeyond") {
            @Override
            stairwaytoheaven.quest.CrookedKeyQuest newQuest() {
                return new stairwaytoheaven.quest.CrookedKeyQuest();
            }
        };

        final int realm;
        final Class<? extends necesse.engine.quest.DeliverItemsQuest> questClass;
        /** The key piece itself. Same string ID as its object — ObjectRegistry
         *  .onRegister registers the ObjectItem under the object's own stringID
         *  (ObjectRegistry.java:2114-2124, VERIFIED [jar]). */
        final String keyItemID;
        final String barItemID;
        final int bars;
        final String askKey;
        final String doneKey;

        RegionKey(int realm, Class<? extends necesse.engine.quest.DeliverItemsQuest> questClass,
                String keyItemID, String barItemID, int bars, String askKey, String doneKey) {
            this.realm = realm;
            this.questClass = questClass;
            this.keyItemID = keyItemID;
            this.barItemID = barItemID;
            this.bars = bars;
            this.askKey = askKey;
            this.doneKey = doneKey;
        }

        abstract necesse.engine.quest.DeliverItemsQuest newQuest();
    }

    /**
     * Hands out — and takes in — the five region key pieces.
     *
     * <h2>Why the Warden and not the Elder</h2>
     * §B1 asks for <i>"the reward of an Elder quest"</i>, and the vanilla Elder
     * cannot be given one. {@code ElderHumanMob} overrides the entire
     * quest-giver seam to nothing: {@code getQuests(ServerClient)} returns null
     * (ElderHumanMob.java:400-402), {@code completeQuest} returns false (:405-407),
     * {@code skipQuest} returns false (:410-412) — all VERIFIED [jar] — so the
     * quests tab {@code ShopContainer} would draw for him is never populated,
     * and the mob cannot be swapped for a subclass either
     * ({@code GameRegistry.register} throws on a duplicate stringID,
     * GameRegistry.java:57-58, and {@code GameRegistry.replaceObj} is
     * {@code protected final}, :71). His real "quests with unique rewards" are
     * {@code StoryObjective}s, which a mod CAN register — but
     * {@code StoryObjectiveManager} only ever shows the first unclaimed
     * objective in registration order ({@code getCurrentObjective}, :414;
     * {@code getVisibleObjectives}, :486-494), and a mod's objectives sort after
     * all 24 of vanilla's, so the ASK would stay invisible until a player had
     * finished and claimed the whole vanilla story line. A quest nobody can see
     * is the {@code swh_beacon} failure again, so this pass did not ship it.
     * The Warden is the fallback the brief names, and he is already this mod's
     * quest-giver.
     *
     * <h2>The shape</h2>
     * Gated on his own chain being {@link Chapter#DONE}: the keys are what comes
     * AFTER "The Warden's Call", and a player who has not anchored the island
     * yet does not need five more journal entries. One key is live at a time,
     * in {@link RegionKey} order.
     *
     * <p>Turn-ins are checked across ALL five before a new one is offered, and
     * deliberately not only for the quest the player is "supposed" to be on:
     * the world record is what decides, so a state where somebody else's
     * turn-in advanced the world cannot strand a held quest. Every branch is
     * idempotent — {@code regionKeyEarned} guards the payout,
     * {@code SkyQuests.giveOnce} guards the hand-out — so this runs on every
     * single conversation, settler or not, exactly like {@code offerChalk}.
     */
    private void advanceRegionKeys(ServerClient client) {
        Server server = client.getServer();
        Level sky = skyLevel(server);
        if (sky == null) {
            return;
        }
        if (chapterFor(SkywatchQuestData.get(sky), this.isSettler()) != Chapter.DONE) {
            return;
        }

        // 0. Clear anything the world has already moved past. In co-op the
        //    record is shared, so a second player can be left holding a quest
        //    for a realm somebody else finished -- and its only turn-in path
        //    (step 1 below) is guarded on that same record, so it would sit in
        //    their journal forever with no way to complete it. That is exactly
        //    the swh_beacon failure docs/quests.md is named after, and it is
        //    reachable here in a way it is not in a single-player world.
        for (RegionKey step : RegionKey.values()) {
            if (SkywatchWorldData.regionKeyEarned(server, step.realm)
                    && stairwaytoheaven.quest.SkyQuests.findHeld(client, step.questClass) != null) {
                stairwaytoheaven.quest.SkyQuests.removeAllOfType(server, step.questClass);
            }
        }

        // 1. Anything the player is carrying the goods for, in any order.
        for (RegionKey step : RegionKey.values()) {
            if (SkywatchWorldData.regionKeyEarned(server, step.realm)) {
                continue;
            }
            necesse.engine.quest.DeliverItemsQuest held =
                    stairwaytoheaven.quest.SkyQuests.findHeld(client, step.questClass);
            if (held == null || !held.canComplete(client)) {
                continue;
            }
            // DeliverItemsQuest.complete removes the delivered items itself.
            held.complete(client);
            stairwaytoheaven.quest.SkyQuests.removeAllOfType(server, step.questClass);
            SkywatchWorldData.markRegionKeyEarned(server, step.realm);
            give(client, step.keyItemID, 1);
            give(client, step.barItemID, step.bars);
            // His doneKey line already tells the player where the piece goes
            // ("Set it down at home, where the walls are yours"), and
            // RegionKeyObject.onPlaceFail answers the same question again, in
            // a bubble, at the moment a player tries to stand one up outside a
            // settlement ("misc.regionkeyneedsettlement"). The extra chat line
            // "misc.regionkeyearned" said it a third time and is gone.
            say(client, step.doneKey);
            return; // one hand-over per conversation; the next ask waits for the next hello.
        }

        // 2. Otherwise the next key nobody has earned yet.
        for (RegionKey step : RegionKey.values()) {
            if (SkywatchWorldData.regionKeyEarned(server, step.realm)) {
                continue;
            }
            if (stairwaytoheaven.quest.SkyQuests.findHeld(client, step.questClass) != null) {
                return; // already asked, and still owed.
            }
            stairwaytoheaven.quest.SkyQuests.giveOnce(server, client, step.newQuest());
            say(client, step.askKey);
            return;
        }
    }

    /**
     * Hands out (or turns in) whatever {@link #chapterFor} says this world is
     * owed. Every branch is keyed off that one function, so the chapter the
     * probe reports and the chapter the player is given cannot drift apart.
     */
    private void advanceChain(ServerClient client) {
        Server server = client.getServer();
        Level sky = skyLevel(server);
        if (sky == null) {
            return;
        }
        SkywatchQuestData quest = SkywatchQuestData.get(sky);

        // Repair first. He is standing here as a SETTLER, so this world
        // recruited him; if the record disagrees it was written by a build
        // whose bookkeeping did not survive an unloaded Skyreach (or by the
        // v0.5.0 hand-spawned second mob, which never touched this record at
        // all). Without this the chain dead-ends on a false `recruited`.
        if (this.isSettler()) {
            SkywatchWorldData world = SkywatchWorldData.get(server);
            if (world != null) {
                world.markRecruited(client.authentication);
                this.stampedWorldRecord = true;
            }
        }
        if (this.isSettler() && !quest.recruited) {
            quest.recruited = true;
            quest.stage = Math.max(quest.stage, 2);
            if (quest.recruitedAuth == 0L) {
                quest.recruitedAuth = client.authentication;
            }
            igniteBeacon(sky, quest);
            stairwaytoheaven.quest.SkyQuests.removeAllOfType(
                    server, stairwaytoheaven.quest.RecruitWardenQuest.class);
        }

        Chapter chapter = chapterFor(quest, this.isSettler());
        if (chapter == Chapter.RECRUIT) {
            stairwaytoheaven.quest.SkyQuests.removeAllOfType(
                    server, stairwaytoheaven.quest.FindSpireQuest.class);
            stairwaytoheaven.quest.SkyQuests.giveOnce(server, client,
                    new stairwaytoheaven.quest.RecruitWardenQuest());
            return;
        }

        // Push world truth into the journal copy before anything else, so a
        // player who coaxed a cat home while holding the quest sees the tick
        // even if the sync at coax time reached a different level object.
        stairwaytoheaven.quest.SkyQuests.syncCatQuests(server, quest);

        if (chapter == Chapter.CATS) {
            stairwaytoheaven.quest.SkyQuests.giveOnce(server, client,
                    new stairwaytoheaven.quest.SpireCatsQuest());
            stairwaytoheaven.quest.SkyMapMarkers.sendCatLairs(client, quest);
            return;
        }

        if (chapter == Chapter.CATS_TURNIN) {
            stairwaytoheaven.quest.SpireCatsQuest cats = stairwaytoheaven.quest.SkyQuests
                    .findHeld(client, stairwaytoheaven.quest.SpireCatsQuest.class);
            if (cats != null) {
                cats.complete(client);
            }
            // Clear every player's copy, not just this one's: the chapter is
            // world progression and the others' journals are now stale.
            stairwaytoheaven.quest.SkyQuests.removeAllOfType(
                    server, stairwaytoheaven.quest.SpireCatsQuest.class);
            quest.catsRewardGiven = true;
            // Cat Basket is the ONLY source of the object in the whole mod
            // (docs/OVERVIEW.md §9) -- it stays, and stays mandatory, but per
            // the endgame rescale it is no longer the payout by itself.
            // Flickerlight Garland is the same story (no second source
            // either) and stays an extra at its original count.
            give(client, "catbasket", 1);
            give(client, "flickerlightgarland", 2);
            // The real payout: a serious stack of the mod's own endgame bar.
            // Benchmarked against StormsteelArmor.Helmet's own recipe cost
            // (SkyItems.registerGearRecipes: stormsteelbar 8) plus a 2-bar
            // margin -- enough on its own to clear the bar cost of any single
            // Stormsteel recipe (helmet 8, boots 6, vambrace 6) once the
            // player has that recipe's other ingredients in hand.
            give(client, "stormsteelbar", 10);
            say(client, "wardencatsdone");
            chapter = chapterFor(quest, this.isSettler());
        }

        if (chapter == Chapter.ANCHOR) {
            stairwaytoheaven.quest.AnchorDeliveryQuest anchor = stairwaytoheaven.quest.SkyQuests
                    .giveOnce(server, client, new stairwaytoheaven.quest.AnchorDeliveryQuest());
            if (anchor != null && anchor.canComplete(client)) {
                // DeliverItemsQuest.complete removes the delivered items itself.
                anchor.complete(client);
                stairwaytoheaven.quest.SkyQuests.removeAllOfType(
                        server, stairwaytoheaven.quest.AnchorDeliveryQuest.class);
                quest.anchorDone = true;
                // Skywatch Banner and Aurora Petal are the old reward kept
                // whole, as decoration and a small material top-up -- extras
                // now, not the payout. The player's own verdict on these two
                // being the WHOLE finale reward was "das sind keine endgame
                // belohnungen tbh", and this is the fix.
                give(client, "skywatchbanner", 1);
                give(client, "aurorapetal", 5);
                // The actual endgame payout for finishing the whole chain:
                // one of the mod's three EPIC trinkets (SkyItems.registerGear),
                // handed over outright rather than through its own recipe
                // (stormsteelbar 6 + skyweave 4 + aetheriumbar 2). Stormsteel
                // is the name on both the ask this quest just took and the
                // set the mod's endgame gear is built around, so the vambrace
                // is the one of the three that closes the loop the chain
                // opened.
                give(client, "stormsteelvambrace", 1);
                say(client, "wardenanchordone");
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
        // World-scoped first: this is the record that survives a generation
        // bump, and everything below it lives in the Skyreach level file.
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world != null) {
            world.markRecruited(client.authentication);
            this.stampedWorldRecord = true;
        }
        // Force-loaded, not "only if it happens to be loaded". The legacy
        // WardenSettlerMob is recruited FOR FREE ON THE SURFACE, and by then
        // the Skyreach has usually been unloaded again (Server.java:365-375) --
        // so the old isLoaded() guard silently skipped the whole payoff:
        // quest.recruited never became true, the beacon never lit, and the cat
        // lair markers never arrived. World.getLevel loads it (World.java:273).
        Level sky = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
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
        if (sky != null) {
            // ...with the lairs on the map. The quest is otherwise "find two
            // cats" in an endless dimension.
            stairwaytoheaven.quest.SkyMapMarkers.sendCatLairs(client, SkywatchQuestData.get(sky));
        }

        // The keeper's Silver Bell changes hands here. It is the key the Seance
        // Circle checks for, and since the old cat quest that used to award it
        // is gone this is its only source — without it the Veil is unreachable.
        give(client, "silverbell", 1);
        say(client, "wardengivesbell");
        this.flushSay();
        // The old "misc.wardenmovedin" chat line named the settlement he had
        // just joined. It is gone, and its own comment said why it could go:
        // vanilla's recruit packet already sends the joined-settlement line,
        // and vanilla's settlement UI is what tells a player which of their
        // settlers still needs a bed. A bubble was not an option either -- it
        // would have replaced the Silver Bell line above (ChatBubbleText.init,
        // ChatBubbleText.java:67-76, VERIFIED [jar]), and the bell is the item
        // that unlocks the Veil.
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

    /**
     * One line of what he says this conversation. Buffered, not sent —
     * {@link #flushSay} puts the whole conversation in ONE speech bubble.
     *
     * <p><b>Why it is buffered.</b> It used to send its bubble immediately and
     * ALSO post the same sentence into the interacting player's chat log,
     * formatted {@code "The Sky Warden: <line>"} through
     * {@code misc.wardenchatformat}. Both that key and {@code misc.wardenname}
     * are gone with the chat log itself — but the chat half was quietly load
     * bearing, because {@code ChatBubbleText.init} (ChatBubbleText.java:67-76,
     * VERIFIED [jar]) removes any bubble the same mob already has. His first
     * contact says three lines in a row and his conversations can add the
     * chalk line and a region-key line on top of those; with immediate sends
     * only the LAST of them would ever be seen, and the other four would be
     * deleted a frame after they appeared. The chat log was the only place
     * they survived, so deleting it without this buffer would have silently
     * deleted them too.
     *
     * <p>{@code client} is kept in the signature: every caller has one, they
     * all read as "tell THIS player", and dropping it would make a per-player
     * line impossible to add back without touching nine call sites.
     */
    protected void say(ServerClient client, String miscKey) {
        this.pendingSay.add(new LocalMessage("misc", miscKey));
    }

    /**
     * Everything {@link #say} collected, as one bubble over his head, seen by
     * everyone nearby.
     *
     * <p>The lines are joined with {@code \n} inside a
     * {@code GameMessageBuilder} rather than translated here, so every player
     * reads them in their own language — the same reason
     * {@code AbstractBeeHiveObjectEntity} builds its inspect text that way
     * (AbstractBeeHiveObjectEntity.java:590-611, VERIFIED [jar]).
     * {@code FairType} treats {@code \n} as a line break and wraps the rest at
     * {@code ChatBubbleText.maxWidth} (FairType.java:262, VERIFIED [jar]).
     */
    protected void flushSay() {
        if (this.pendingSay.isEmpty()) {
            return;
        }
        Level level = this.getLevel();
        Server server = level == null ? null : level.getServer();
        if (server == null) {
            this.pendingSay.clear();
            return;
        }
        necesse.engine.localization.message.GameMessageBuilder speech =
                new necesse.engine.localization.message.GameMessageBuilder();
        for (int i = 0; i < this.pendingSay.size(); i++) {
            if (i > 0) {
                speech.append("\n");
            }
            speech.append(this.pendingSay.get(i));
        }
        this.pendingSay.clear();
        server.network.sendToClientsWithEntity(
                new necesse.engine.network.packet.PacketMobChat(this.getUniqueID(), speech), this);
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
