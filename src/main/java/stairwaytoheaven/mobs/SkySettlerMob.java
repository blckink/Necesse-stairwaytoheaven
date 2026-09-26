package stairwaytoheaven.mobs;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.ShopContainerData;
import necesse.entity.mobs.friendly.human.humanShop.ShopManager;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobFinder;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.GameHair;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;

/**
 * Shared base for the Skyreach's hireable residents.
 *
 * WHY THIS EXISTS AT ALL. Before this class the mod registered exactly ONE
 * settler — the Warden — and one HumanShop. The player's question was why a
 * settler with one of our professions had never turned up in their town, and
 * the answer was that none existed: the three "professions" are workstations
 * for vanilla settlers, not settler types, and the designed cast lived only in
 * `docs/design/chapter-01-skyreach-cast.md`.
 *
 * WHAT A PROFESSION IS, read out of the game rather than assumed. Necesse ships
 * eighteen of them through {@code SettlerRegistry} — angler, stylist, mage,
 * explorer, miner and the rest — and each one is a {@link HumanShop} subclass
 * whose identity is its SHOP: what it sells, what it buys, and prices that move
 * with its happiness. That is the whole mechanism, and it is what this base
 * gives its three subclasses.
 *
 * APPEARANCE COSTS NO NEW ART. Vanilla dresses a settler by putting real
 * clothing ITEMS on a plain human body — the Elder is a human in `elderhat`,
 * `eldershirt` and `eldershoes` — which `WardenIdentity` already follows. Each
 * resident here picks its wardrobe from vanilla's own, verified present in the
 * ItemRegistry, so three new people cost three 32px icons rather than three
 * human sheets.
 */
public abstract class SkySettlerMob extends HumanShop {

    protected SkySettlerMob(String settlerStringID) {
        // Third argument is the SettlerRegistry key, NOT a free-form name.
        // Getting it wrong makes getSettler() null and silently disables
        // recruitment — the bug that cost the Warden three player-visible
        // faults (see DESIGN_DECISIONS.md).
        super(400, 400, settlerStringID);
        this.canDespawn = false;
    }

    /** The Miner's brain: mills about, and works like any settler once hired. */
    @Override
    public void init() {
        super.init();
        this.installBrain();
    }

    /**
     * Builds the behaviour tree. A resident Dorian has turned
     * ({@link #nightbound}) gets {@link NightboundAI} — the same brain with
     * the night turned around — and everybody else vanilla's {@code HumanAI}.
     * Called again when the flag changes at runtime.
     */
    protected void installBrain() {
        if (this.keepsNightShift()) {
            this.ai = new BehaviourTreeAI<>(this,
                    new NightboundAI<>(320, this.attacksHostiles(), false, 25000),
                    new AIMover(HumanMob.humanPathIterations));
        } else {
            this.ai = new BehaviourTreeAI<>(this, new HumanAI<>(320, this.attacksHostiles(), false, 25000),
                    new AIMover(HumanMob.humanPathIterations));
        }
    }

    // --- the night shift (Dorian, and whoever he has turned) --------------
    //
    // The second night settler of decision "Zuschnitt C" is NOT a mob swap:
    // decisions.json ("Vampir-Siedler: Umfang ...") ruled replacing a settled
    // resident's mob save-risky. A turned resident keeps its class, its
    // uniqueID, its room and its shop; what changes is one saved boolean that
    // flips the three "is it night" decisions vampiresettler already inverts
    // (sleep, work, idling). No speed change: the flag is server-side only and
    // speed is also read on the client.

    /** Turned by Dorian ({@code DorianDialogue}). Saved with the mob. */
    protected boolean nightbound;

    public boolean isNightbound() {
        return this.nightbound;
    }

    /** Whether this settler keeps Dorian's hours. */
    public boolean keepsNightShift() {
        return this.nightbound;
    }

    /** Turn (or give back the daylight to) this resident. Server only. */
    public void setNightbound(boolean value) {
        if (this.nightbound == value) {
            return;
        }
        this.nightbound = value;
        if (this.getLevel() != null) {
            // Out of bed first: the old tree's sleep node holds the bed, and
            // the new tree would never let go of it (findJob refuses anyone
            // with an objectUser).
            if (this.objectUser != null) {
                this.objectUser.stopUsing();
            }
            this.installBrain();
        }
    }

    /** Night, as the world sees it. False on a client with no world entity. */
    public boolean isNightTime() {
        return this.getWorldEntity() != null && this.getWorldEntity().isNight();
    }

    /**
     * Whether a night-shift settler is on its feet: after dark, or because the
     * player is keeping it up (party member / standing orders).
     */
    public boolean isOnDuty() {
        return this.isNightTime()
                || this.adventureParty.isInAdventureParty()
                || this.hasCommandOrders();
    }

    /**
     * The night lock, turned around — for night-shift settlers only; everyone
     * else gets vanilla's untouched.
     *
     * <p>Off duty: no jobs. On duty by day: {@code super} is safe, vanilla's
     * own lock is inactive during the day. On duty at night: {@code super}
     * would refuse before doing anything else, so it is rebuilt in
     * {@link #findJobIgnoringNight}.
     */
    @Override
    public JobSequence findJob(boolean ignoreRecreationJobs, Consumer<JobFinder> finderMod) {
        if (!this.keepsNightShift()) {
            return super.findJob(ignoreRecreationJobs, finderMod);
        }
        if (!this.isOnDuty()) {
            return null;
        }
        if (!this.isNightTime()) {
            return super.findJob(ignoreRecreationJobs, finderMod);
        }
        return this.findJobIgnoringNight(ignoreRecreationJobs, finderMod);
    }

    /**
     * {@link #findJob}'s night branch: vanilla's method without its clock.
     * Everything below the time check is vanilla's, in vanilla's order
     * (HumanMob.java:3353-3388 and {@code EntityJobWorker.findJob}), so a
     * night settler on strike, in a raid, in a disbanding settlement or hiding
     * behaves exactly like anybody else.
     */
    private JobSequence findJobIgnoringNight(boolean ignoreRecreationJobs,
            Consumer<JobFinder> finderMod) {
        if (this.objectUser != null) {
            return null;
        }
        NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
        if (settlement != null
                && (settlement.isRaidActive() || settlement.isDisbanding() || !settlement.hasOwner())) {
            return null;
        }
        if (this.isHiding || this.isVisitor()) {
            return null;
        }
        if (!this.adventureParty.isInAdventureParty() && !this.hasCommandOrders()
                && this.attemptStartStrike(true)) {
            return null;
        }

        // EntityJobWorker.findJob's own body, which `super` can no longer be
        // asked for: `EntityJobWorker.super` is illegal once HumanMob has
        // overridden the default (JLS 15.12.3).
        JobTypeHandler handler = this.getJobTypeHandler();
        long currentTime = this.getMobWorker().getTime();
        if (handler.isOnGlobalCooldown(currentTime)) {
            return null;
        }
        JobFinder jobFinder = new JobFinder(this);
        if (finderMod != null) {
            finderMod.accept(jobFinder);
        }
        FoundJob<?> first = jobFinder.findJob(ignoreRecreationJobs);
        if (handler.resetPrioritizeNextJobIfFound) {
            handler.prioritizeNextJobID = -1;
        }
        JobSequence foundJob = null;
        if (first != null) {
            first.startCooldown(currentTime);
            handler.lastPerformedJobID = first.job.prioritizeForSameJobAgain() ? first.job.getID() : -1;
            foundJob = first.getSequence();
        } else {
            handler.lastPerformedJobID = -1;
            handler.prioritizeNextJobID = -1;
        }

        if (foundJob == null && this.getWorkInventory().isFull()) {
            this.submitFullInventoryNotification();
        } else {
            this.removeFullInventoryNotification();
        }
        return foundJob;
    }

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        if (this.nightbound) {
            save.addBoolean("swhnightbound", true);
        }
    }

    /**
     * The one name each story resident goes by.
     *
     * <p>Vanilla gives every settler a random first name
     * ({@code HumanMob.setSettlerSeed} → {@code getRandomName}) and then shows
     * it three ways: alone in the settlement list, as {@code <id>name} with the
     * title in the dialogue header, and not at all in the quest texts, which
     * say "Halda". So one woman was "Bertha", "Bertha die Kellermeisterin" and
     * "Halda" at once. The player: <i>"du hast irgendwie drei namen für manche
     * npcs dass muss einheitlich sein"</i>. A story resident now IS her name:
     * the raw {@code settlerName} is this string, and {@code <id>name} is
     * vanilla's {@code "<name>, Beruf"} around it.
     *
     * <p>{@code settlerName} is a saved raw string, not a locale key, so every
     * name here is spelled the same in English and German — which is why
     * Magpie is "Magpie" in the German text too.
     */
    private static final java.util.Map<String, String> OWN_NAMES = new java.util.HashMap<>();
    static {
        OWN_NAMES.put("magpiesettler", "Magpie");
        OWN_NAMES.put("haldasettler", "Halda");
        OWN_NAMES.put("ossiansettler", "Ossian");
        OWN_NAMES.put("eveleensettler", "Eveleen");
        OWN_NAMES.put("ivessettler", "Ives");
        OWN_NAMES.put("mortimersettler", "Mortimer");
        OWN_NAMES.put("caspernsettler", "Caspern");
        OWN_NAMES.put("eleanorsettler", "Eleanor");
        OWN_NAMES.put("knottsettler", "Mr. Knott");
        OWN_NAMES.put("vampiresettler", "Dorian");
    }

    /** This resident's own name, or null for one that takes a random vanilla name. */
    public static String ownName(String mobStringID) {
        return OWN_NAMES.get(mobStringID);
    }

    @Override
    protected String getRandomName(GameRandom random) {
        String own = OWN_NAMES.get(this.getStringID());
        return own != null ? own : super.getRandomName(random);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        // Saves from before 2026-09-26 carry the random name; the story name
        // replaces it on load.
        String own = OWN_NAMES.get(this.getStringID());
        if (own != null) {
            this.settlerName = own;
        }
        boolean turned = save.getBoolean("swhnightbound", false, false);
        if (turned != this.nightbound) {
            this.nightbound = turned;
            if (this.getLevel() != null) {
                this.installBrain();
            }
        }
    }

    /**
     * Whether this resident goes looking for a fight.
     *
     * <p>{@code HumanAI}'s second argument, and vanilla treats it as a
     * character trait rather than a default: the Miner, the Explorer and the
     * Hunter pass {@code true}, and the two NPCs you FIND standing in the world
     * waiting to be recruited — {@code FriendlyJonasHumanMob} and
     * {@code FriendlyWitchHumanMob} — pass {@code false} (VERIFIED [jar]).
     *
     * <p>That distinction became load-bearing on 2026-09-10, when the three
     * recruit sites got the enemies §2 asks for. A settler with
     * {@code attackHostiles} true charges anything hostile within 320px, and
     * measured on seed 1443… Halda (§2.13 seat (12,10)) is 172px from the
     * Sourvat Bloom (7,8) and Magpie (§2.12 seat (18,5)) is 233px from the
     * Tollwright (16,12). Both walk out of the room they are supposed to be
     * found in — and because {@link #canTakeDamage} is false they cannot lose,
     * so they eventually kill the boss and the recruit key drops on an empty
     * floor before the player has arrived. §2.12 says it plainly: Magpie
     * <i>"does not come out until the Tollwright is down"</i>.
     *
     * <p>Default true, so the four realm residents keep the behaviour they
     * shipped with; the three Skyreach residents override it.
     */
    protected boolean attacksHostiles() {
        return true;
    }

    // --- professions ----------------------------------------------------
    //
    // WHAT A PROFESSION IS IN THE JOB SYSTEM, read out of 1.3.2.
    //
    // JobTypeRegistry.registerCore (JobTypeRegistry.java:20-32) registers each
    // job type as `new JobType(canChangePriority, defaultDisabledBySettler,
    // name, tip)`. The SECOND flag is the whole profession mechanism:
    //
    //   hauling, crafting, forestry, farming   -> false: EVERY settler does them
    //   fertilize, husbandry, fishing, hunting,
    //   tradingmission                         -> true: nobody does them...
    //
    // ...unless their mob turns the flag off for itself. JobTypeHandler's
    // constructor copies `type.defaultDisabledBySettler` into a per-mob
    // TypePriority (JobTypeHandler.java:132-135), and JobTypeHandler.streamJobs
    // drops every job whose TypePriority is `disabledBySettler`
    // (JobTypeHandler.java:88-91). So vanilla's professions are literally one
    // line each in the mob's constructor:
    //
    //   FarmerHumanMob:       getPriority("fertilize").disabledBySettler = false
    //   AnimalKeeperHumanMob: getPriority("husbandry").disabledBySettler = false
    //   AnglerHumanMob:       getPriority("fishing").disabledBySettler   = false
    //   HunterHumanMob:       getPriority("hunting").disabledBySettler   = false
    //   TraderHumanMob:       getPriority("tradingmission")...           = false
    //
    // and the opposite direction is a profession too: GuardHumanMob switches
    // crafting, forestry and farming OFF (GuardHumanMob.java:31-34) because a
    // guard guards. That is the pair of verbs below.
    //
    // The job handlers these touch are created inside the HumanMob constructor
    // (`LevelJobRegistry.addHandlers`, HumanMob.java:514), so a subclass
    // constructor is the correct and only place to call them - which is exactly
    // where vanilla calls them. TypePriority.loadSaveData restores `priority`
    // and `disabledByPlayer` and never `disabledBySettler`
    // (JobTypeHandler.java:137-140), so what is set here survives every load.

    /**
     * Grant this settler a job vanilla withholds from settlers by default -
     * i.e. give them a specialist profession.
     *
     * @param jobType one of {@code fertilize}, {@code husbandry},
     *                {@code fishing}, {@code hunting}, {@code tradingmission}
     */
    protected final void enableProfession(String jobType) {
        JobTypeHandler.TypePriority priority = this.jobTypeHandler.getPriority(jobType);
        if (priority != null) {
            priority.disabledBySettler = false;
        }
    }

    /**
     * Take a job away that vanilla grants to every settler, because this
     * character would not do it. The Guard's own move.
     *
     * @param jobType one of {@code hauling}, {@code crafting}, {@code forestry},
     *                {@code farming}
     */
    protected final void refuseJob(String jobType) {
        JobTypeHandler.TypePriority priority = this.jobTypeHandler.getPriority(jobType);
        if (priority != null) {
            priority.disabledBySettler = true;
        }
    }

    /**
     * Speech bubble over their head, seen by everyone nearby.
     *
     * <p>This is the mod's ONLY way for a person to say something — the chat
     * log is gone (the player: <i>"und keine chat nachrichten! generell.. die
     * sind total kacke lesbar"</i>). Two rules come with it, both from
     * {@code ChatBubbleText} (VERIFIED [jar]):
     *
     * <ul>
     *   <li>{@code init} (ChatBubbleText.java:67-76) removes any bubble the
     *       same mob already has, so <b>only the last call in a method is
     *       ever seen</b>. Say one thing, or build one message.</li>
     *   <li>the text wraps at {@code maxWidth} 200px and honours {@code \n}
     *       (FairType.java:262), so a line that needs a break can have one —
     *       but a paragraph in a bubble is unreadable and belongs nowhere.</li>
     * </ul>
     */
    protected void bubble(String miscKey) {
        this.bubble(new LocalMessage("misc", miscKey));
    }

    /**
     * The same bubble, for a line that carries replacements — a count, a name,
     * a position. The message is sent unresolved so every player reads it in
     * their own language.
     */
    protected void bubble(necesse.engine.localization.message.GameMessage message) {
        if (message == null || this.getLevel() == null || this.getLevel().getServer() == null) {
            return;
        }
        this.getLevel().getServer().network.sendToClientsWithEntity(
                new necesse.engine.network.packet.PacketMobChat(
                        this.getUniqueID(), message), this);
    }

    /**
     * Hands a quest reward over; anything that does not fit drops at the
     * player's feet.
     *
     * <p>{@code EveleenMob} and {@code EleanorMob} each carry a private,
     * character-for-character identical copy of this, written before there was
     * a third caller. There are five now, so it lives here — a reward that is
     * silently swallowed by a full inventory is the kind of bug that only shows
     * up as "the quest paid me nothing", and it should be impossible to
     * reintroduce by copying the method badly. The two originals are left alone
     * deliberately: they work, and rewriting working reward code to save eight
     * lines is a change with no upside and a real downside.
     *
     * @param source the inventory-event source string vanilla logs the transfer
     *               under; use the giver's own name
     */
    protected final void giveReward(ServerClient client, String itemStringID, int amount,
            String source) {
        if (client == null || client.playerMob == null) {
            return;
        }
        necesse.entity.mobs.PlayerMob player = client.playerMob;
        necesse.level.maps.Level level = player.getLevel();
        InventoryItem item = new InventoryItem(itemStringID, amount);
        boolean added = player.getInv().main.addItem(level, player, item, source, null);
        if (!added && item.getAmount() > 0) {
            level.entityManager.pickups.add(new necesse.entity.pickup.ItemPickupEntity(
                    level, item, player.x, player.y, 0.0F, 0.0F));
        }
    }

    /** A fixed face, so the same person is the same person in every world. */
    protected abstract int lookSeed();

    protected abstract HumanGender gender();

    protected abstract Color shirtColor();

    protected abstract Color shoesColor();

    /** helmet / chestplate / boots item string IDs, or null to leave bare. */
    protected abstract String[] wardrobe();

    /** Coins asked at vanilla's own recruit page. */
    protected abstract int recruitCost();

    /**
     * The one item, besides coins, this resident asks for — or null.
     *
     * <p>{@code chapter-01-skyreach-cast.md} §1: <i>"Each recruit price is
     * coins + one key item that only exists in that POI. That is the design
     * answer to 'POIs never have special loot': at three of them, the loot is
     * a person."</i> The three Skyreach residents each override this with the
     * reward their own once-per-world place carries; every other settler on
     * this base — the four realm residents — leaves it null and keeps the
     * plain coin price it shipped with.
     *
     * <p>Nothing bespoke is needed to make vanilla take it. VERIFIED [jar]:
     * {@code ShopContainer.canPayForRecruit} (:592) walks the WHOLE
     * {@code recruitItems} list and asks the inventory for each item's amount,
     * and {@code payForRecruit} (:607) removes each of them by item and
     * amount. Coins are not a special case in either loop.
     */
    protected String recruitKey() {
        return null;
    }

    /** The `misc.<key>N` prefix for this resident's small talk. */
    protected abstract String talkKey();

    @Override
    public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
        GameRandom fixed = new GameRandom(this.lookSeed());
        HumanGender g = this.gender();
        look.randomizeLook(fixed, true, g, true, true, true, true);
        look.setHairColor(GameHair.getRandomHairColorAtSpecificWeight(fixed, 140));
        look.setShirtColor(this.shirtColor());
        look.setShoesColor(this.shoesColor());
        this.gender = g;
    }

    @Override
    public void setDefaultArmor(HumanDrawOptions drawOptions) {
        String[] w = this.wardrobe();
        if (w == null) {
            return;
        }
        if (w.length > 0 && w[0] != null) {
            drawOptions.helmet(new InventoryItem(w[0]));
        }
        if (w.length > 1 && w[1] != null) {
            drawOptions.chestplate(new InventoryItem(w[1]));
        }
        if (w.length > 2 && w[2] != null) {
            drawOptions.boots(new InventoryItem(w[2]));
        }
    }

    /**
     * The price, stated by vanilla's own recruit page.
     *
     * This IS the payment mechanism: {@code ShopContainer.payForRecruit} takes
     * the coins server-side on the button press. Never hand-roll a payment in
     * {@code interact()} — that is exactly what produced the Warden's
     * "coins taken by talking" bug.
     */
    @Override
    public List<InventoryItem> getRecruitItems(ServerClient client) {
        InventoryItem coins = new InventoryItem("coin", this.recruitCost());
        String key = this.recruitKey();
        if (key == null) {
            return Collections.singletonList(coins);
        }
        // Two entries, never a merged one: the container pays each element
        // separately. `client` is deliberately not read — SkyreachStatusCommand
        // probes this with null to print the price from a running server, and a
        // price that needs a player is a price nothing can check.
        ArrayList<InventoryItem> price = new ArrayList<>(2);
        price.add(coins);
        price.add(new InventoryItem(key, 1));
        return price;
    }

    /** Open on the recruit page until they have actually moved in. */
    @Override
    public boolean startInRecruitForm(ServerClient client) {
        // A resident with a task for this player opens on the dialogue page,
        // where the task is written; Recruit is a button there.
        return !this.isSettler() && this.questBrief(client) == null;
    }

    /** True only while {@link #getShopContainerData} builds a locked window. */
    private boolean shopClosedForThisWindow;

    /**
     * No trade before this resident's chapter is open for this player
     * (Kevin, 2026-09-25: "Handel erst freischalten, wenn das Kapitel ihres
     * ersten Auftrags offen ist"). The recruit page's Back — Circle on a
     * controller — leads to the plain dialogue, and that is where vanilla puts
     * Trade ({@code ShopContainerForm}:153); a wait bubble alone never closed it.
     *
     * <p>Vanilla's own "no shop" path does the closing: with {@link #getShop}
     * null, {@code HumanShop.getShopContainerData} sends no wares and the
     * server-side container gets no {@code ShopManager}, so neither buying nor
     * selling works. Keyed on {@code QuestLadder.chapterOpen}, which only ever
     * opens — never on the wait bubble, which comes back in later chapters.
     * Recruiting early does not open it: the ladder still tells a recruited
     * resident's player "come back when chapter X opens" ({@link #talkLadder}),
     * and a Trade button beside that line is the contradiction reported
     * (Kevin, 2026-09-26: "wie man es als Spieler logisch findet").
     */
    @Override
    public ShopContainerData getShopContainerData(ServerClient client) {
        this.shopClosedForThisWindow = this.tradeLocked(client);
        try {
            return super.getShopContainerData(client);
        } finally {
            this.shopClosedForThisWindow = false;
        }
    }

    @Override
    public ShopManager getShop() {
        return this.shopClosedForThisWindow ? null : super.getShop();
    }

    private boolean tradeLocked(ServerClient client) {
        if (client == null || client.getServer() == null) {
            return false;
        }
        return chapterLocksTrade(client.getServer(), client.authentication, this.getStringID());
    }

    /** The chapter half of {@link #tradeLocked}; {@code skyreachstatus} probes it headless. */
    public static boolean chapterLocksTrade(necesse.engine.network.server.Server server, long auth, String residentID) {
        List<stairwaytoheaven.quest.ladder.QuestLadder.Step> steps =
                stairwaytoheaven.quest.ladder.QuestLadder.stepsOf(residentID);
        return !steps.isEmpty() && !stairwaytoheaven.quest.ladder.QuestLadder.chapterOpen(
                server, auth, steps.get(0).chapter);
    }

    /**
     * Their own small talk. Without this override HumanMob falls back to
     * {@code mobmsg.humantalk1..5} and a Skywatch courier greets the player
     * with "I often think about the big questions in life" — which is what a
     * playtester screenshotted on the Warden.
     */
    @Override
    protected ArrayList<GameMessage> getMessages(ServerClient client) {
        return getLocalMessages("misc", this.talkKey(), 4);
    }

    @Override
    public GameMessage getDialogueIntroMessage(ServerClient client) {
        GameMessage brief = this.questBrief(client);
        if (brief != null) {
            return brief;
        }
        return this.isSettler() ? super.getDialogueIntroMessage(client)
                : new LocalMessage("misc", this.talkKey() + "pitch");
    }

    /**
     * This resident's task for this player, in the same shape the Sky Warden
     * uses ({@code journal.WardenBrief#task}): their ask, the task with what
     * is still missing, and the Chronicle pointer - or, when their next step
     * waits for a chapter, their "not yet" line. Null when they have nothing
     * to say about a task. Until 2026-09-26 the residents only spoke in
     * fading bubbles and said nothing at all to a player who was short of
     * items, while the Warden wrote the whole task into his window.
     */
    protected GameMessage questBrief(ServerClient client) {
        if (client == null || client.getServer() == null) {
            return null;
        }
        List<stairwaytoheaven.quest.ladder.QuestLadder.Step> mine =
                stairwaytoheaven.quest.ladder.QuestLadder.stepsOf(this.getStringID());
        if (mine.isEmpty()) {
            return null;
        }
        List<stairwaytoheaven.journal.JournalStep> steps = stairwaytoheaven.journal.AdventurerJournal.stepSource()
                .buildSteps(new stairwaytoheaven.journal.JournalContext(client.getServer(), client));
        for (stairwaytoheaven.quest.ladder.QuestLadder.Step step : mine) {
            if (step.custom) {
                continue;
            }
            stairwaytoheaven.journal.JournalStep shown = null;
            for (stairwaytoheaven.journal.JournalStep js : steps) {
                if (js.id.equals(step.id)) {
                    shown = js;
                    break;
                }
            }
            if (shown == null || shown.status == stairwaytoheaven.journal.JournalStatus.DONE) {
                continue;
            }
            if (shown.status == stairwaytoheaven.journal.JournalStatus.ACTIVE
                    || shown.status == stairwaytoheaven.journal.JournalStatus.READY) {
                return stairwaytoheaven.journal.WardenBrief.task(new LocalMessage("misc", step.askKey()), shown);
            }
            if (!stairwaytoheaven.quest.ladder.QuestLadder.chapterOpen(client.getServer(),
                    client.authentication, step.chapter)) {
                return new LocalMessage("misc", stairwaytoheaven.quest.ladder.QuestLadder.waitKey(this.getStringID()),
                        "chapter", new LocalMessage("misc",
                                stairwaytoheaven.quest.ladder.QuestLadder.chapterKey(step.chapter)));
            }
            return null;
        }
        return null;
    }

    /**
     * One conversation's worth of the Spire Village's quest ladder
     * ({@code quest.ladder.QuestLadder}): turn a step in, hand the next one
     * out, or say "not yet" — and say it in this resident's own bubble.
     *
     * <p>Call it from {@code interact} BEFORE {@code super.interact}, the way
     * every resident chain here always has; server side only, idempotent in
     * every branch, and deliberately not gated on {@link #isSettler()}: a
     * resident who was recruited early can still take and pay every step.
     *
     * @return true if the ladder said something (so the caller says nothing)
     */
    protected boolean talkLadder(necesse.entity.mobs.PlayerMob player) {
        if (!this.isServer() || player == null || !player.isServerClient()) {
            return false;
        }
        stairwaytoheaven.quest.ladder.QuestLadder.Reply reply =
                stairwaytoheaven.quest.ladder.QuestLadder.converse(this.getStringID(),
                        player.getServerClient());
        if (reply == null || reply.bubble == null) {
            return false;
        }
        // Bubbles are for reactions only (decision 10n): the thanks when a
        // step is paid. A new task or a "not yet" is written into the
        // dialogue window (questBrief), which stays open to be read.
        if (reply.paid) {
            this.bubble(reply.bubble);
        }
        return true;
    }

    private int homeCheckTicks;

    /**
     * Home-bound. A resident an older build stood somewhere in the sky — a
     * workshop, a landmark, a gravestone in the Aftergarden — walks into their
     * house in the Spire Village the first time their region is loaded, and
     * from then on strolls around their own door by day and sleeps inside by
     * night ({@code village.SpireVillage#bringHome}). Every half second is
     * plenty: it is one null check until the day it is not.
     */
    @Override
    public void serverTick() {
        super.serverTick();
        if (++this.homeCheckTicks >= 10) {
            this.homeCheckTicks = 0;
            if (this.home == null) {
                stairwaytoheaven.village.SpireVillage.bringHome(this);
            }
        }
    }

    /** Unique story residents: no stray mob may kill one. */
    @Override
    public boolean canTakeDamage() {
        return false;
    }
}
