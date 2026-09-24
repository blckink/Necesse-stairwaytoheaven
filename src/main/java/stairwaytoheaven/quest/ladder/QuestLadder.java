package stairwaytoheaven.quest.ladder;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketAddMapMarker;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;
import necesse.engine.registries.MapIconRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkyQuests;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.settlement.SkySettlers;

/**
 * The quest ladder of the Spire Village — and the read API the Adventurer's
 * Journal ("Abenteurer-Tagebuch") uses.
 *
 * <h2>What it is</h2>
 * The player, 2026-09-24: <i>"... und Stück für Stück Quests von ihnen kriegen,
 * für die man immer weiter in tiefe Gebiete ziehen muss."</i> Twenty steps in
 * six chapters — Skyreach, Eden, Steinfeld, the Ghost Realm, the Crooked
 * Beyond, Hell — each handed out and turned in by one of the village's
 * residents, each sending the player one band further out, each paying
 * something that exists nowhere else. The table, the story of every step and
 * the balance reasoning for every reward are
 * {@code docs/design/chapter-03-spire-village.md} §4-§5.
 *
 * <h2>The rules</h2>
 * <ol>
 *   <li><b>Chapters open in order.</b> A step of chapter N is offered once
 *       every GATING step of the chapters before it is done. Eleanor's choice
 *       is the one non-gating step: releasing her or keeping her is an ending,
 *       and an ending must never be a toll.</li>
 *   <li><b>A giver's own steps come in order.</b> Ossian does not hand out his
 *       Steinfeld step before his Skyreach one.</li>
 *   <li><b>A held quest can always be turned in.</b> Never gated by chapter or
 *       order: an older save that already holds a step, or a quest the Eden
 *       Gate handed out early, must never be stranded — the {@code swh_beacon}
 *       dead end is the thing this rule exists to rule out.</li>
 *   <li><b>One action per conversation, and one bubble.</b> Turn in, or hand
 *       out, or say "not yet" — never two, because a second bubble deletes the
 *       first ({@code ChatBubbleText.java:67-76}, VERIFIED [jar]).</li>
 *   <li><b>Short of the goods? Say nothing.</b> The journal lists what is
 *       missing; the shop opens as usual (the {@code SkyQuests} rule).</li>
 * </ol>
 *
 * <h2>Where completion is recorded</h2>
 * The six steps that are older than the ladder keep their WORLD record in
 * {@link SkywatchWorldData} (their reward waives a shared settler's fee);
 * everything else is per player in {@link LadderWorldData}. {@link Step#isDone}
 * hides the difference from every caller.
 *
 * <h2>The read API (for the journal)</h2>
 * <pre>
 *   QuestLadder.steps()                        every step, in ladder order
 *   QuestLadder.stepsOf(giverMobID)            one resident's steps, in order
 *   QuestLadder.step(stepID)                   one step, or null
 *   QuestLadder.status(client, step)           LOCKED / AVAILABLE / ACTIVE / READY / DONE
 *   QuestLadder.statusOffline(server, auth, s) the same without a connected player
 *                                              (ACTIVE/READY collapse into AVAILABLE)
 *   QuestLadder.chapterOpen(server, auth, n)   whether chapter n (1-6) is open
 *   QuestLadder.currentChapter(server, auth)   the highest open chapter
 *   Step.id / chapter / giver / gating / titleKey() / descKey() / rewardKey()
 *   / targetKey() / giverNameKey()             everything a journal row prints
 * </pre>
 * All of it is server-side and read-only; nothing in it hands out or completes
 * anything except {@link #converse}, which only a resident's own
 * {@code interact} calls.
 */
public final class QuestLadder {

    private QuestLadder() {
    }

    public static final int CHAPTER_SKYREACH = 1;
    public static final int CHAPTER_EDEN = 2;
    public static final int CHAPTER_STEINFELD = 3;
    public static final int CHAPTER_GHOST = 4;
    public static final int CHAPTER_CROOKED = 5;
    public static final int CHAPTER_HELL = 6;
    public static final int CHAPTERS = 6;

    /** Where a step stands for one player. */
    public enum Status {
        /** Its chapter is not open yet, or the giver's earlier step is not done. */
        LOCKED,
        /** The giver will hand it out on the next conversation. */
        AVAILABLE,
        /** In the player's journal, not yet complete. */
        ACTIVE,
        /** In the journal and complete: turn it in to the giver. */
        READY,
        /** Turned in and paid. */
        DONE,
    }

    /** One rung of the ladder. */
    public static final class Step {
        /** QuestRegistry ID; also the journal key. */
        public final String id;
        public final int chapter;
        /** The giver's mob string ID. */
        public final String giver;
        /** Counts for opening the next chapter. */
        public final boolean gating;
        /** Handed out by the giver's own code, not by {@link #converse} (Eleanor). */
        public final boolean custom;
        public final Class<? extends Quest> questClass;
        private final Supplier<Quest> factory;
        /** World record for a step older than the ladder; null = per player. */
        private final Predicate<Server> worldDone;
        private final Consumer<Server> worldMark;
        private final BiConsumer<Server, ServerClient> reward;
        /** Landmark index ({@code SkyLandmarkPois.KINDS}) to pin on the map, or -1. */
        public final int landmark;
        /** Stem of this step's quest texts ({@code quests.<stem>title} ...). */
        private final String stem;
        /** The giver's own lines for an older chain, which keep their keys. */
        private String askOverride;
        private String doneOverride;

        Step(String id, int chapter, String giver, boolean gating, boolean custom,
             Class<? extends Quest> questClass, Supplier<Quest> factory,
             Predicate<Server> worldDone, Consumer<Server> worldMark,
             BiConsumer<Server, ServerClient> reward, int landmark) {
            this.id = id;
            this.chapter = chapter;
            this.giver = giver;
            this.gating = gating;
            this.custom = custom;
            this.questClass = questClass;
            this.factory = factory;
            this.worldDone = worldDone;
            this.worldMark = worldMark;
            this.reward = reward;
            this.landmark = landmark;
            this.stem = id.replace("_", "");
        }

        public String titleKey() { return this.stem + "title"; }
        public String descKey() { return this.stem + "desc"; }
        public String rewardKey() { return this.stem + "reward"; }
        /** {@code misc.<key>}: the place or boss this step sends the player to. */
        public String targetKey() { return this.stem + "target"; }
        /** {@code misc.<key>}: the bubble the giver says when handing it out. */
        public String askKey() { return this.askOverride != null ? this.askOverride : this.stem + "ask"; }
        /** {@code misc.<key>}: the bubble the giver says when paying. */
        public String doneKey() { return this.doneOverride != null ? this.doneOverride : this.stem + "done"; }

        /** An older chain keeps the lines its giver always said. */
        Step speaks(String ask, String done) {
            this.askOverride = ask;
            this.doneOverride = done;
            return this;
        }
        /** {@code mob.<giver>} — the giver's display name. */
        public String giverNameKey() { return this.giver; }

        /** Whether the step is turned in, for this player (or world, for the old six). */
        public boolean isDone(Server server, long auth) {
            if (this.worldDone != null) {
                return this.worldDone.test(server);
            }
            LadderWorldData data = LadderWorldData.get(server);
            return data != null && data.isDone(auth, this.id);
        }

        /** Recorded for the whole world (the six older chains), not per player. */
        public boolean isWorldScoped() {
            return this.worldDone != null;
        }

        /** A fresh copy of this step's quest. */
        public Quest newQuest() {
            return this.factory.get();
        }
    }

    private static final List<Step> STEPS = build();

    private static List<Step> build() {
        List<Step> s = new ArrayList<>();
        // ------------------------------------------------ I. Himmelsweite
        s.add(new Step("swh_ladder_post", CHAPTER_SKYREACH, SkySettlers.MAGPIE, true, false,
                LadderQuests.Post.class, LadderQuests.Post::new, null, null,
                (server, client) -> give(client, "magpiesatchel", 1), 0));
        s.add(new Step("swh_ladder_round", CHAPTER_SKYREACH, SkySettlers.HALDA, true, false,
                LadderQuests.Round.class, LadderQuests.Round::new, null, null,
                (server, client) -> give(client, "stormbrew", 3), 1));
        s.add(new Step("swh_ladder_prototype", CHAPTER_SKYREACH, SkySettlers.OSSIAN, true, false,
                LadderQuests.Prototype.class, LadderQuests.Prototype::new, null, null,
                (server, client) -> give(client, "readerslens", 1), 2));
        // ------------------------------------------------ II. Garten Eden
        s.add(new Step("swh_edenreach", CHAPTER_EDEN, SkySettlers.EVELEEN, true, false,
                stairwaytoheaven.quest.EdenArrivalQuest.class,
                stairwaytoheaven.quest.EdenArrivalQuest::new, null, null,
                (server, client) -> give(client, "overgrownedenseed", 6), -1));
        s.add(new Step("swh_edenplants", CHAPTER_EDEN, SkySettlers.EVELEEN, true, false,
                stairwaytoheaven.quest.EdenPlantsQuest.class,
                stairwaytoheaven.quest.EdenPlantsQuest::new,
                SkywatchWorldData::edenPlantsGiven, SkywatchWorldData::markEdenPlantsGiven,
                (server, client) -> {
                    give(client, "knowledgecutting", 3);
                    give(client, "stormsteelbar", 10);
                }, -1).speaks("eveleenasksplants", "eveleenplantsdone"));
        s.add(new Step("swh_ladder_cider", CHAPTER_EDEN, SkySettlers.HALDA, true, false,
                LadderQuests.Cider.class, LadderQuests.Cider::new, null, null,
                (server, client) -> give(client, "paradisecider", 3), -1));
        s.add(new Step("swh_ladder_contraband", CHAPTER_EDEN, SkySettlers.MAGPIE, true, false,
                LadderQuests.Contraband.class, LadderQuests.Contraband::new, null, null,
                (server, client) -> {
                    LadderWorldData data = LadderWorldData.get(server);
                    if (data != null) {
                        data.flags.add(LadderWorldData.FLAG_WRIT_HONOURED);
                    }
                }, -1));
        // ------------------------------------------------ III. Steinfeld
        s.add(new Step("swh_steinfeldvigil", CHAPTER_STEINFELD, SkySettlers.IVES, true, false,
                stairwaytoheaven.quest.SteinfeldVigilQuest.class,
                stairwaytoheaven.quest.SteinfeldVigilQuest::new,
                server -> SkywatchWorldData.residentChainDone(server, SkywatchWorldData.CHAIN_STEINFELD_VIGIL),
                server -> SkywatchWorldData.markResidentChainDone(server, SkywatchWorldData.CHAIN_STEINFELD_VIGIL),
                (server, client) -> give(client, "stormsteelbar",
                        stairwaytoheaven.mobs.IvesMob.VIGIL_BARS), -1)
                .speaks("ivesasksvigil", "ivesvigildone"));
        s.add(new Step("swh_ladder_echo", CHAPTER_STEINFELD, SkySettlers.OSSIAN, true, false,
                LadderQuests.Echo.class, LadderQuests.Echo::new, null, null,
                (server, client) -> give(client, "echoconch", 1), -1));
        s.add(new Step("swh_ladder_steps", CHAPTER_STEINFELD, SkySettlers.IVES, true, false,
                LadderQuests.Steps.class, LadderQuests.Steps::new, null, null,
                (server, client) -> give(client, "vergerlantern", 1), -1));
        // ------------------------------------------------ IV. Geisterreich
        s.add(new Step("swh_mortimerrites", CHAPTER_GHOST, SkySettlers.MORTIMER, true, false,
                stairwaytoheaven.quest.MortimerRitesQuest.class,
                stairwaytoheaven.quest.MortimerRitesQuest::new,
                server -> SkywatchWorldData.residentChainDone(server, SkywatchWorldData.CHAIN_MORTIMER_RITES),
                server -> SkywatchWorldData.markResidentChainDone(server, SkywatchWorldData.CHAIN_MORTIMER_RITES),
                (server, client) -> give(client, "spiritsteelbar", 6), -1)
                .speaks("mortimerasksrites", "mortimerritesdone"));
        s.add(new Step("swh_caspernforge", CHAPTER_GHOST, SkySettlers.CASPERN, true, false,
                stairwaytoheaven.quest.CaspernForgeQuest.class,
                stairwaytoheaven.quest.CaspernForgeQuest::new,
                server -> SkywatchWorldData.residentChainDone(server, SkywatchWorldData.CHAIN_CASPERN_FORGE),
                server -> SkywatchWorldData.markResidentChainDone(server, SkywatchWorldData.CHAIN_CASPERN_FORGE),
                (server, client) -> give(client, "spiritsteelbar", 6), -1)
                .speaks("caspernasksforge", "caspernforgedone"));
        s.add(new Step("swh_eleanor", CHAPTER_GHOST, SkySettlers.ELEANOR, false, true,
                stairwaytoheaven.quest.EleanorQuest.class,
                stairwaytoheaven.quest.EleanorQuest::new,
                QuestLadder::eleanorDecided, null, null, -1));
        s.add(new Step("swh_ladder_memory", CHAPTER_GHOST, SkySettlers.CASPERN, true, false,
                LadderQuests.Memory.class, LadderQuests.Memory::new, null, null,
                (server, client) -> give(client, "memoryblade", 1), -1));
        s.add(new Step("swh_ladder_shrouds", CHAPTER_GHOST, SkySettlers.MORTIMER, true, false,
                LadderQuests.Shrouds.class, LadderQuests.Shrouds::new, null, null,
                (server, client) -> give(client, "mourningbrooch", 1), -1));
        // ------------------------------------------------ V. Krummes Jenseits
        s.add(new Step("swh_crookedarrival", CHAPTER_CROOKED, SkySettlers.KNOTT, true, false,
                stairwaytoheaven.quest.CrookedArrivalQuest.class,
                stairwaytoheaven.quest.CrookedArrivalQuest::new, null, null,
                (server, client) -> give(client, "realityshard", 4), -1));
        s.add(new Step("swh_crookeddoor", CHAPTER_CROOKED, SkySettlers.KNOTT, true, false,
                stairwaytoheaven.quest.CrookedDoorQuest.class,
                stairwaytoheaven.quest.CrookedDoorQuest::new,
                SkywatchWorldData::crookedDoorwayOpened, SkywatchWorldData::markCrookedDoorwayOpened,
                (server, client) -> {
                    give(client, "zephyrharness", 1);
                    give(client, "spiritsteelbar", 12);
                    give(client, "realityshard", 6);
                }, -1).speaks("knottasksdoor", "knottdoordone"));
        s.add(new Step("swh_ladder_seeds", CHAPTER_CROOKED, SkySettlers.EVELEEN, true, false,
                LadderQuests.Seeds.class, LadderQuests.Seeds::new, null, null,
                (server, client) -> give(client, "rootcrown", 1), -1));
        s.add(new Step("swh_ladder_shells", CHAPTER_CROOKED, SkySettlers.KNOTT, true, false,
                LadderQuests.Shells.class, LadderQuests.Shells::new, null, null,
                (server, client) -> give(client, "knottkeyring", 1), -1));
        // ------------------------------------------------ VI. Hölle
        s.add(new Step("swh_ladder_form", CHAPTER_HELL, SkySettlers.OSSIAN, true, false,
                LadderQuests.Form.class, LadderQuests.Form::new, null, null,
                (server, client) -> give(client, "auditorsseal", 1), -1));
        return Collections.unmodifiableList(s);
    }

    /** Eleanor's step is "done" once she is released or has moved into a town. */
    private static boolean eleanorDecided(Server server) {
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world == null) {
            return false;
        }
        if (world.eleanorPassedOn) {
            return true;
        }
        return world.residentsClaimed.contains(SkySettlers.ELEANOR)
                && !world.villageResidents.contains(SkySettlers.ELEANOR);
    }

    // ------------------------------------------------------------------
    // Read API
    // ------------------------------------------------------------------

    public static List<Step> steps() {
        return STEPS;
    }

    public static Step step(String id) {
        for (Step s : STEPS) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    public static List<Step> stepsOf(String giver) {
        List<Step> out = new ArrayList<>();
        for (Step s : STEPS) {
            if (s.giver.equals(giver)) {
                out.add(s);
            }
        }
        return out;
    }

    /** Every gating step of every chapter below {@code chapter} is done. */
    public static boolean chapterOpen(Server server, long auth, int chapter) {
        for (Step s : STEPS) {
            if (s.chapter < chapter && s.gating && !s.isDone(server, auth)) {
                return false;
            }
        }
        return true;
    }

    /** The highest chapter this player may take steps from. */
    public static int currentChapter(Server server, long auth) {
        int open = CHAPTER_SKYREACH;
        for (int c = CHAPTER_SKYREACH + 1; c <= CHAPTERS; c++) {
            if (chapterOpen(server, auth, c)) {
                open = c;
            }
        }
        return open;
    }

    /** Rules 1 and 2: may this step be handed out now (ignoring whether it is held)? */
    public static boolean isOffered(Server server, long auth, Step step) {
        if (step.isDone(server, auth) || !chapterOpen(server, auth, step.chapter)) {
            return false;
        }
        for (Step before : stepsOf(step.giver)) {
            if (before == step) {
                return true;
            }
            if (!before.custom && !before.isDone(server, auth)) {
                return false;
            }
        }
        return true;
    }

    /** Whether this player has turned in the step with this ID. */
    public static boolean isDone(ServerClient client, String stepID) {
        Step step = step(stepID);
        return client != null && step != null && step.isDone(client.getServer(), client.authentication);
    }

    /** Whether this player holds the step's quest right now. */
    public static boolean holds(ServerClient client, String stepID) {
        Step step = step(stepID);
        return client != null && step != null && SkyQuests.findHeld(client, step.questClass) != null;
    }

    /** Status for a connected player — the journal's question. */
    public static Status status(ServerClient client, Step step) {
        Server server = client.getServer();
        long auth = client.authentication;
        if (step.isDone(server, auth)) {
            return Status.DONE;
        }
        Quest held = SkyQuests.findHeld(client, step.questClass);
        if (held != null) {
            return held.canComplete(client) ? Status.READY : Status.ACTIVE;
        }
        return isOffered(server, auth, step) ? Status.AVAILABLE : Status.LOCKED;
    }

    /** Status without a connected player: ACTIVE and READY read as AVAILABLE. */
    public static Status statusOffline(Server server, long auth, Step step) {
        if (step.isDone(server, auth)) {
            return Status.DONE;
        }
        return isOffered(server, auth, step) ? Status.AVAILABLE : Status.LOCKED;
    }

    /**
     * The step {@link #converse} would HAND OUT to a player whose finished
     * steps are exactly {@code done} — a pure function of the table, for the
     * census to prove every step has a giver who reaches it. Treats every step
     * in {@code done} as done and every other as not, ignoring world records.
     */
    public static Step wouldOffer(String giver, java.util.Set<String> done) {
        for (Step step : stepsOf(giver)) {
            if (done.contains(step.id)) {
                continue;
            }
            if (step.custom) {
                continue;
            }
            for (Step s : STEPS) {
                if (s.chapter < step.chapter && s.gating && !done.contains(s.id)) {
                    return null;
                }
            }
            return step;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // The conversation
    // ------------------------------------------------------------------

    /** What the giver says after {@link #converse}; null = say nothing. */
    public static final class Reply {
        public final GameMessage bubble;
        public final Step step;
        public final boolean paid;

        Reply(GameMessage bubble, Step step, boolean paid) {
            this.bubble = bubble;
            this.step = step;
            this.paid = paid;
        }
    }

    /**
     * One conversation with a ladder giver: turn in, or hand out, or say "not
     * yet" — whichever comes first in their own order. Server side only;
     * every branch is idempotent, and it is deliberately NOT gated on the
     * giver's settler state (see {@code SkyQuests.advanceResidentChain}).
     */
    public static Reply converse(String giver, ServerClient client) {
        if (client == null || client.playerMob == null) {
            return null;
        }
        Server server = client.getServer();
        long auth = client.authentication;
        // Rule 3 first: anything held and complete is turned in, whatever order.
        for (Step step : stepsOf(giver)) {
            if (step.custom || step.isDone(server, auth)) {
                continue;
            }
            Quest held = SkyQuests.findHeld(client, step.questClass);
            if (held != null && held.canComplete(client)) {
                held.complete(client);
                // A world-scoped step is finished for everybody: clear every
                // copy. A per-player step only clears this player's.
                if (step.worldMark != null) {
                    SkyQuests.removeAllOfType(server, step.questClass);
                    step.worldMark.accept(server);
                } else {
                    server.world.getQuests().removeQuest(held);
                    LadderWorldData data = LadderWorldData.get(server);
                    if (data != null) {
                        data.markDone(auth, step.id);
                    }
                }
                if (step.reward != null) {
                    step.reward.accept(server, client);
                }
                return new Reply(new LocalMessage("misc", step.doneKey()), step, true);
            }
        }
        for (Step step : stepsOf(giver)) {
            if (step.custom || step.isDone(server, auth)) {
                continue;
            }
            if (SkyQuests.findHeld(client, step.questClass) != null) {
                return null;                        // rule 5: short, say nothing
            }
            if (isOffered(server, auth, step)) {
                SkyQuests.giveOnce(server, client, step.newQuest());
                pinTarget(client, step);
                return new Reply(new LocalMessage("misc", step.askKey()), step, false);
            }
            // The first step of theirs this player cannot have yet.
            return new Reply(new LocalMessage("misc", waitKey(giver),
                    "chapter", new LocalMessage("misc", chapterKey(step.chapter))),
                    step, false);
        }
        return null;
    }

    /**
     * {@code misc.<key>} naming chapter 1-6. Literal strings on purpose:
     * {@code tools/locale_audit.py} name-checks what it can read in the source.
     */
    private static final String[] CHAPTER_KEYS = {
            "swhladderchapter1", "swhladderchapter2", "swhladderchapter3",
            "swhladderchapter4", "swhladderchapter5", "swhladderchapter6",
    };

    /** {@code misc.<key>}: a giver's "not yet" line. Literal, for the same audit. */
    public static String waitKey(String giver) {
        switch (giver) {
            case SkySettlers.MAGPIE: return "swhladderwaitmagpie";
            case SkySettlers.HALDA: return "swhladderwaithalda";
            case SkySettlers.OSSIAN: return "swhladderwaitossian";
            case SkySettlers.EVELEEN: return "swhladderwaiteveleen";
            case SkySettlers.IVES: return "swhladderwaitives";
            case SkySettlers.MORTIMER: return "swhladderwaitmortimer";
            case SkySettlers.CASPERN: return "swhladderwaitcaspern";
            case SkySettlers.ELEANOR: return "swhladderwaiteleanor";
            default: return "swhladderwaitknott";
        }
    }

    public static String chapterKey(int chapter) {
        return CHAPTER_KEYS[Math.max(1, Math.min(CHAPTERS, chapter)) - 1];
    }

    /** "magpiesettler" → "magpie": the stem of a giver's own locale keys. */
    public static String giverShort(String giver) {
        return giver.endsWith("settler") ? giver.substring(0, giver.length() - "settler".length()) : giver;
    }

    /**
     * A map pin at the landmark a Skyreach step sends the player to — the
     * same packet {@code SkyMapMarkers} sends for the spire and the cats. Sent
     * on hand-out, so a player who is told "the Toll-House" is also shown it.
     */
    private static void pinTarget(ServerClient client, Step step) {
        if (step.landmark < 0 || client == null) {
            return;
        }
        int seed = stairwaytoheaven.worldgen.SkyOrigin.worldGenSeed(
                client.getServer().world.worldEntity);
        stairwaytoheaven.worldgen.pois.SkyLandmarkPois.Site site =
                stairwaytoheaven.worldgen.pois.SkyLandmarkPois.site(seed, step.landmark);
        int kind = stairwaytoheaven.worldgen.pois.SkyLandmarkPois.KINDS[step.landmark];
        int x = site.x + stairwaytoheaven.worldgen.pois.RealmPoiPresets.width(kind) / 2;
        int y = site.y + stairwaytoheaven.worldgen.pois.RealmPoiPresets.height(kind) / 2;
        client.sendPacket(new PacketAddMapMarker(MapIconRegistry.getIcon("poi"),
                new LocalMessage("misc", step.targetKey()), SkyRegistry.SKYREACH_IDENTIFIER, x, y));
    }

    /** Reward hand-off; anything that does not fit drops at the player's feet. */
    public static void give(ServerClient client, String itemStringID, int amount) {
        if (client == null || client.playerMob == null) {
            return;
        }
        PlayerMob player = client.playerMob;
        Level level = player.getLevel();
        InventoryItem item = new InventoryItem(itemStringID, amount);
        boolean added = player.getInv().main.addItem(level, player, item, "questladder", null);
        if (!added && item.getAmount() > 0) {
            level.entityManager.pickups.add(new necesse.entity.pickup.ItemPickupEntity(
                    level, item, player.x, player.y, 0.0F, 0.0F));
        }
    }

    /** Unused helper kept for the journal: the seat a giver lives at, or null. */
    public static Point homeOf(Level skyLevel, String giver) {
        return stairwaytoheaven.village.SpireVillage.seatOf(skyLevel,
                stairwaytoheaven.village.SpireVillage.houseOf(giver));
    }
}
