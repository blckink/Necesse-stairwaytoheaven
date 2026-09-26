package stairwaytoheaven.journal;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.quest.DeliverItemsQuest;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import stairwaytoheaven.mobs.SkyWardenMob;
import stairwaytoheaven.quest.AnchorDeliveryQuest;
import stairwaytoheaven.quest.CaspernForgeQuest;
import stairwaytoheaven.quest.CrookedDoorQuest;
import stairwaytoheaven.quest.CrookedKeyQuest;
import stairwaytoheaven.quest.EdenArrivalQuest;
import stairwaytoheaven.quest.EdenKeyQuest;
import stairwaytoheaven.quest.EdenPlantsQuest;
import stairwaytoheaven.quest.EleanorQuest;
import stairwaytoheaven.quest.FindSpireQuest;
import stairwaytoheaven.quest.GhostKeyQuest;
import stairwaytoheaven.quest.HellKeyQuest;
import stairwaytoheaven.quest.MortimerRitesQuest;
import stairwaytoheaven.quest.RecruitWardenQuest;
import stairwaytoheaven.quest.SkyreachKeyQuest;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.quest.SpireCatsQuest;
import stairwaytoheaven.quest.SteinfeldKeyQuest;
import stairwaytoheaven.quest.SteinfeldVigilQuest;
import stairwaytoheaven.worldgen.RealmDepth;

/**
 * The journal's steps, read from the quests and flags that exist today.
 *
 * <p>Every status rule below is the code path {@code docs/KOMPLETTUEBERSICHT.md}
 * §1-§2 documents, restated as "done / active / available / locked":
 * <ul>
 * <li><b>DONE</b> is always a world or player RECORD (never "the quest is
 *     gone from the journal", which also happens when another player turned
 *     it in or an admin reset it).</li>
 * <li><b>ACTIVE</b> is "this player holds the vanilla quest" — per player,
 *     exactly what the sidebar shows.</li>
 * <li><b>AVAILABLE</b> is "the giver would hand it out if spoken to now",
 *     using the giver's own guard (e.g. the Warden only offers a region key
 *     once his chapter is DONE and the previous key is earned —
 *     {@code SkyWardenMob.advanceRegionKeys}).</li>
 * <li>Anything else is <b>LOCKED</b>, with a hint naming what unlocks it.</li>
 * </ul>
 *
 * <p>The two dead quests ({@code swh_beacon}, {@code swh_crookedarrival},
 * KOMPLETTUEBERSICHT §8.1) are not listed: nothing can hand them out, and a
 * journal page for a quest nobody can get is the bug, not the fix.
 */
public final class LegacyQuestSource implements JournalStepSource {

    @Override
    public String name() {
        return "legacy";
    }

    @Override
    public List<JournalStep> buildSteps(JournalContext ctx) {
        List<JournalStep> out = new ArrayList<>();
        skyreach(ctx, out);
        eden(ctx, out);
        steinfeld(ctx, out);
        ghost(ctx, out);
        crooked(ctx, out);
        hell(ctx, out);
        // A world-scoped step somebody else finished: say who, instead of
        // "reward received" to a reader who received nothing.
        for (JournalStep step : out) {
            if (step.worldScoped && step.status == JournalStatus.DONE) {
                step.doneBy = ctx.doneByOther(step.id);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Skyreach — the Warden's chain

    private static void skyreach(JournalContext ctx, List<JournalStep> out) {
        int realm = RealmDepth.REALM_SKYREACH;
        GameMessage warden = MobRegistry.getLocalization("skywarden");

        JournalStep find = new JournalStep("findspire", realm);
        find.title = new LocalMessage("quests", "swhfindspiretitle");
        find.description = new LocalMessage("quests", "swhfindspiredesc");
        find.giver = new LocalMessage("journal", "giverstairway");
        find.where = new LocalMessage("journal", "wherestairway");
        find.worldScoped = true;
        boolean found = ctx.stage() >= 1 || ctx.wardenRecruited() || ctx.met("skywarden", "wardensettler");
        find.status = found ? JournalStatus.DONE
                : ctx.holds(FindSpireQuest.class) ? JournalStatus.ACTIVE : JournalStatus.AVAILABLE;
        if (!found) {
            find.objectives.add(new LocalMessage("quests", "swhfindspireobj"));
        }
        find.reward = new LocalMessage("journal", "rewardfindspire");
        explain(find);
        out.add(find);

        JournalStep recruit = new JournalStep("recruitwarden", realm);
        recruit.title = new LocalMessage("quests", "swhrecruitwardentitle");
        recruit.description = new LocalMessage("quests", "swhrecruitwardendesc");
        recruit.giver = warden;
        recruit.where = new LocalMessage("journal", "wherespire");
        recruit.worldScoped = true;
        recruit.status = ctx.wardenRecruited() ? JournalStatus.DONE
                : ctx.holds(RecruitWardenQuest.class) ? JournalStatus.ACTIVE
                : found ? JournalStatus.AVAILABLE : JournalStatus.LOCKED;
        if (recruit.status == JournalStatus.LOCKED) {
            recruit.hint = after(new LocalMessage("quests", "swhfindspiretitle"));
        } else if (recruit.status != JournalStatus.DONE) {
            recruit.objectives.add(new LocalMessage("quests", "swhrecruitwardenobj",
                    "cost", String.valueOf(SkyWardenMob.RECRUIT_COST)));
            recruit.objectives.add(new LocalMessage("journal", "objneedsettlement"));
        }
        recruit.reward = new LocalMessage("journal", "rewardrecruitwarden");
        explain(recruit);
        out.add(recruit);

        JournalStep cats = new JournalStep("cats", realm);
        cats.title = new LocalMessage("quests", "swhcatstitle");
        cats.description = new LocalMessage("quests", "swhcatsdesc");
        cats.giver = warden;
        cats.where = new LocalMessage("journal", "wherecats");
        cats.worldScoped = true;
        boolean catsDone = ctx.catsRewardGiven();
        boolean black = ctx.catHome(true);
        boolean tabby = ctx.catHome(false);
        cats.status = catsDone ? JournalStatus.DONE
                : ctx.wardenRecruited() && black && tabby ? JournalStatus.READY
                : ctx.holds(SpireCatsQuest.class) || (ctx.wardenRecruited() && (black || tabby)) ? JournalStatus.ACTIVE
                : ctx.wardenRecruited() ? JournalStatus.AVAILABLE : JournalStatus.LOCKED;
        if (cats.status == JournalStatus.LOCKED) {
            cats.hint = after(new LocalMessage("quests", "swhrecruitwardentitle"));
        } else if (!catsDone) {
            cats.objectives.add(check(black, black ? new LocalMessage("quests", "swhcatblackhome")
                    : new LocalMessage("quests", "swhcatblack")));
            cats.objectives.add(check(tabby, tabby ? new LocalMessage("quests", "swhcattabbyhome")
                    : new LocalMessage("quests", "swhcattabby")));
            if (black && tabby) {
                cats.objectives.add(new LocalMessage("quests", "swhreturnwarden"));
            } else {
                cats.objectives.add(new LocalMessage("journal", "objcattreat"));
            }
        }
        cats.reward = new LocalMessage("quests", "swhcatsreward");
        explain(cats);
        out.add(cats);

        JournalStep anchor = new JournalStep("anchor", realm);
        anchor.title = new LocalMessage("quests", "swhanchortitle");
        anchor.description = new LocalMessage("quests", "swhanchordesc");
        anchor.giver = warden;
        anchor.where = new LocalMessage("journal", "wherewarden");
        anchor.worldScoped = true;
        anchor.status = ctx.anchorDone() ? JournalStatus.DONE
                : ctx.ready(AnchorDeliveryQuest.class) ? JournalStatus.READY
                : ctx.holds(AnchorDeliveryQuest.class) ? JournalStatus.ACTIVE
                : catsDone ? JournalStatus.AVAILABLE : JournalStatus.LOCKED;
        if (anchor.status == JournalStatus.LOCKED) {
            anchor.hint = after(new LocalMessage("quests", "swhcatstitle"));
        } else if (anchor.status != JournalStatus.DONE) {
            deliver(ctx, anchor, AnchorDeliveryQuest.class, new LocalMessage("quests", "swhreturnwarden"));
        }
        anchor.reward = new LocalMessage("quests", "swhanchorreward");
        explain(anchor);
        out.add(anchor);

        out.add(regionKey(ctx, realm, SkyreachKeyQuest.class,
                new LocalMessage("quests", "swhkeyskyreachtitle"),
                new LocalMessage("quests", "swhkeyskyreachdesc"),
                new LocalMessage("quests", "swhkeyskyreachreward"),
                new LocalMessage("quests", "swhanchortitle")));
    }

    // ------------------------------------------------------------------
    // Eden — Eveleen

    private static void eden(JournalContext ctx, List<JournalStep> out) {
        int realm = RealmDepth.REALM_EDEN;
        GameMessage eveleen = MobRegistry.getLocalization("eveleensettler");

        JournalStep reach = new JournalStep("edenreach", realm);
        reach.title = new LocalMessage("quests", "swhedenreachtitle");
        reach.description = new LocalMessage("quests", "swhedenreachdesc");
        reach.where = new LocalMessage("journal", "whereeden");
        boolean metEveleen = ctx.met("eveleensettler") || ctx.edenPlantsGiven() || ctx.holds(EdenPlantsQuest.class);
        reach.status = metEveleen ? JournalStatus.DONE
                : ctx.holds(EdenArrivalQuest.class) ? JournalStatus.ACTIVE : JournalStatus.AVAILABLE;
        if (!metEveleen) {
            reach.objectives.add(new LocalMessage("quests", "swhedenreachobj"));
            reach.hint = new LocalMessage("journal", "hintedengate");
        }
        out.add(reach);

        JournalStep plants = new JournalStep("edenplants", realm);
        plants.title = new LocalMessage("quests", "swhedenplantstitle");
        plants.description = new LocalMessage("quests", "swhedenplantsdesc");
        plants.giver = eveleen;
        plants.where = new LocalMessage("journal", "whereeveleen");
        plants.worldScoped = true;
        plants.status = ctx.edenPlantsGiven() ? JournalStatus.DONE
                : ctx.ready(EdenPlantsQuest.class) ? JournalStatus.READY
                : ctx.holds(EdenPlantsQuest.class) ? JournalStatus.ACTIVE : JournalStatus.AVAILABLE;
        if (plants.status != JournalStatus.DONE) {
            deliver(ctx, plants, EdenPlantsQuest.class, new LocalMessage("quests", "swhspeaktoeveleen"));
        }
        plants.reward = new LocalMessage("quests", "swhedenplantsreward");
        out.add(plants);

        out.add(regionKey(ctx, realm, EdenKeyQuest.class,
                new LocalMessage("quests", "swhkeyedentitle"),
                new LocalMessage("quests", "swhkeyedendesc"),
                new LocalMessage("quests", "swhkeyedenreward"),
                new LocalMessage("quests", "swhkeyskyreachtitle")));
    }

    // ------------------------------------------------------------------
    // Steinfeld — Ives

    private static void steinfeld(JournalContext ctx, List<JournalStep> out) {
        int realm = RealmDepth.REALM_STEINFELD;
        out.add(residentChain(ctx, "steinfeldvigil", realm, SteinfeldVigilQuest.class,
                SkywatchWorldData.CHAIN_STEINFELD_VIGIL, "ivessettler",
                new LocalMessage("quests", "swhsteinfeldvigiltitle"),
                new LocalMessage("quests", "swhsteinfeldvigildesc"),
                new LocalMessage("quests", "swhsteinfeldvigilreward"),
                new LocalMessage("journal", "whereives"),
                new LocalMessage("quests", "swhspeaktoives"), true));
        out.add(regionKey(ctx, realm, SteinfeldKeyQuest.class,
                new LocalMessage("quests", "swhkeysteinfeldtitle"),
                new LocalMessage("quests", "swhkeysteinfelddesc"),
                new LocalMessage("quests", "swhkeysteinfeldreward"),
                new LocalMessage("quests", "swhkeyedentitle")));
    }

    // ------------------------------------------------------------------
    // Ghost Realm — the fog, the chalk, the Mark, and the Aftergarden's dead

    private static void ghost(JournalContext ctx, List<JournalStep> out) {
        int realm = RealmDepth.REALM_GHOST;
        boolean mark = ctx.hasMark();
        boolean fog = ctx.fogTouched() || mark;
        boolean chalk = ctx.chalkGiven() || mark;

        JournalStep wall = new JournalStep("fogwall", realm);
        wall.title = new LocalMessage("journal", "stepfogtitle");
        wall.description = new LocalMessage("journal", "stepfogdesc");
        wall.where = new LocalMessage("journal", "wherefog");
        wall.status = fog ? JournalStatus.DONE : JournalStatus.AVAILABLE;
        out.add(wall);

        JournalStep chalkStep = new JournalStep("ghostchalk", realm);
        chalkStep.title = new LocalMessage("journal", "stepchalktitle");
        chalkStep.description = new LocalMessage("journal", "stepchalkdesc");
        chalkStep.giver = MobRegistry.getLocalization("skywarden");
        chalkStep.where = new LocalMessage("journal", "wherewarden");
        chalkStep.status = chalk ? JournalStatus.DONE : fog ? JournalStatus.AVAILABLE : JournalStatus.LOCKED;
        if (chalkStep.status == JournalStatus.LOCKED) {
            chalkStep.hint = after(new LocalMessage("journal", "stepfogtitle"));
        } else if (!chalk) {
            chalkStep.objectives.add(new LocalMessage("quests", "swhreturnwarden"));
        }
        chalkStep.reward = new LocalMessage("journal", "rewardchalk");
        explain(chalkStep);
        out.add(chalkStep);

        JournalStep markStep = new JournalStep("veilmark", realm);
        markStep.title = new LocalMessage("journal", "stepmarktitle");
        markStep.description = new LocalMessage("journal", "stepmarkdesc");
        markStep.giver = MobRegistry.getLocalization("ghostguide");
        markStep.where = new LocalMessage("journal", "whereseance");
        markStep.status = mark ? JournalStatus.DONE : chalk ? JournalStatus.AVAILABLE : JournalStatus.LOCKED;
        if (markStep.status == JournalStatus.LOCKED) {
            markStep.hint = after(new LocalMessage("journal", "stepchalktitle"));
        } else if (!mark) {
            markStep.objectives.add(new LocalMessage("journal", "objseance"));
        }
        markStep.reward = new LocalMessage("journal", "rewardmark");
        out.add(markStep);

        JournalStep eleanor = new JournalStep("eleanor", realm);
        eleanor.title = new LocalMessage("quests", "swheleanortitle");
        eleanor.description = new LocalMessage("quests", "swheleanordesc");
        eleanor.giver = MobRegistry.getLocalization("eleanorsettler");
        eleanor.where = new LocalMessage("journal", "wheregrave");
        eleanor.worldScoped = true;
        boolean eleanorDone = ctx.eleanorPassedOn() || ctx.seenAsSettler("eleanorsettler");
        eleanor.status = eleanorDone ? JournalStatus.DONE
                : ctx.holds(EleanorQuest.class) ? JournalStatus.ACTIVE : JournalStatus.AVAILABLE;
        if (!eleanorDone) {
            eleanor.objectives.add(new LocalMessage("quests", "swheleanorobj"));
        } else if (ctx.eleanorPassedOn()) {
            eleanor.hint = new LocalMessage("journal", "eleanorgone");
        }
        eleanor.reward = new LocalMessage("quests", "swheleanorreward");
        out.add(eleanor);

        out.add(residentChain(ctx, "mortimerrites", realm, MortimerRitesQuest.class,
                SkywatchWorldData.CHAIN_MORTIMER_RITES, "mortimersettler",
                new LocalMessage("quests", "swhmortimerritestitle"),
                new LocalMessage("quests", "swhmortimerritesdesc"),
                new LocalMessage("quests", "swhmortimerritesreward"),
                new LocalMessage("journal", "wheregrave"),
                new LocalMessage("quests", "swhspeaktomortimer"), true));
        out.add(residentChain(ctx, "caspernforge", realm, CaspernForgeQuest.class,
                SkywatchWorldData.CHAIN_CASPERN_FORGE, "caspernsettler",
                new LocalMessage("quests", "swhcaspernforgetitle"),
                new LocalMessage("quests", "swhcaspernforgedesc"),
                new LocalMessage("quests", "swhcaspernforgereward"),
                new LocalMessage("journal", "wheregrave"),
                new LocalMessage("quests", "swhspeaktocaspern"), true));
        out.add(regionKey(ctx, realm, GhostKeyQuest.class,
                new LocalMessage("quests", "swhkeyghostrealmtitle"),
                new LocalMessage("quests", "swhkeyghostrealmdesc"),
                new LocalMessage("quests", "swhkeyghostrealmreward"),
                new LocalMessage("quests", "swhkeysteinfeldtitle")));
    }

    // ------------------------------------------------------------------
    // Crooked Beyond — Mr. Knott

    private static void crooked(JournalContext ctx, List<JournalStep> out) {
        int realm = RealmDepth.REALM_CROOKED;
        JournalStep door = new JournalStep("crookeddoor", realm);
        door.title = new LocalMessage("quests", "swhcrookeddoortitle");
        door.description = new LocalMessage("quests", "swhcrookeddoordesc");
        door.giver = MobRegistry.getLocalization("knottsettler");
        door.where = new LocalMessage("journal", "whereknott");
        door.worldScoped = true;
        boolean reachable = ctx.hasMark() || ctx.met("knottsettler");
        door.status = ctx.crookedDoorwayOpened() ? JournalStatus.DONE
                : ctx.ready(CrookedDoorQuest.class) ? JournalStatus.READY
                : ctx.holds(CrookedDoorQuest.class) ? JournalStatus.ACTIVE
                : reachable ? JournalStatus.AVAILABLE : JournalStatus.LOCKED;
        if (door.status == JournalStatus.LOCKED) {
            door.hint = new LocalMessage("journal", "hintneedmark");
        } else if (door.status != JournalStatus.DONE) {
            deliver(ctx, door, CrookedDoorQuest.class, new LocalMessage("quests", "swhspeaktoknott"));
        }
        door.reward = new LocalMessage("quests", "swhcrookeddoorreward");
        out.add(door);

        out.add(regionKey(ctx, realm, CrookedKeyQuest.class,
                new LocalMessage("quests", "swhkeycrookedbeyondtitle"),
                new LocalMessage("quests", "swhkeycrookedbeyonddesc"),
                new LocalMessage("quests", "swhkeycrookedbeyondreward"),
                new LocalMessage("quests", "swhkeyghostrealmtitle")));
    }

    // ------------------------------------------------------------------
    // Hell

    private static void hell(JournalContext ctx, List<JournalStep> out) {
        out.add(regionKey(ctx, RealmDepth.REALM_HELL, HellKeyQuest.class,
                new LocalMessage("quests", "swhkeyhelltitle"),
                new LocalMessage("quests", "swhkeyhelldesc"),
                new LocalMessage("quests", "swhkeyhellreward"),
                new LocalMessage("quests", "swhkeycrookedbeyondtitle")));
    }

    // ------------------------------------------------------------------
    // shared shapes

    /**
     * A region-key quest. The Warden offers them strictly in realm order, and
     * only once "The Warden's Call" is DONE ({@code SkyWardenMob.advanceRegionKeys}).
     */
    private static JournalStep regionKey(JournalContext ctx, int realm, Class<? extends DeliverItemsQuest> type,
            GameMessage title, GameMessage desc, GameMessage reward, GameMessage previousTitle) {
        JournalStep step = new JournalStep("key" + RealmDepth.keyOf(realm), realm);
        step.title = title;
        step.description = desc;
        step.giver = MobRegistry.getLocalization("skywarden");
        step.where = new LocalMessage("journal", "wherewarden");
        step.worldScoped = true;
        boolean previousEarned = true;
        for (int r = 0; r < realm; r++) {
            previousEarned &= ctx.keyEarned(r);
        }
        boolean offered = ctx.wardenChainDone() && previousEarned;
        step.status = ctx.keyEarned(realm) ? JournalStatus.DONE
                : ctx.ready(type) ? JournalStatus.READY
                : ctx.holds(type) ? JournalStatus.ACTIVE
                : offered ? JournalStatus.AVAILABLE : JournalStatus.LOCKED;
        if (step.status == JournalStatus.LOCKED) {
            step.hint = after(previousTitle);
        } else if (step.status != JournalStatus.DONE) {
            deliver(ctx, step, type, new LocalMessage("quests", "swhreturnwarden"));
        } else if (!ctx.portalsAwake(realm)) {
            step.hint = new LocalMessage("journal", "hintplacekey");
        }
        step.reward = reward;
        explain(step);
        return step;
    }

    /** Ives, Mortimer, Caspern: {@code SkyQuests.advanceResidentChain}'s three states. */
    private static JournalStep residentChain(JournalContext ctx, String id, int realm,
            Class<? extends DeliverItemsQuest> type, String chainKey, String giverMob,
            GameMessage title, GameMessage desc, GameMessage reward, GameMessage where,
            GameMessage turnIn, boolean worldScoped) {
        JournalStep step = new JournalStep(id, realm);
        step.title = title;
        step.description = desc;
        step.giver = MobRegistry.getLocalization(giverMob);
        step.where = where;
        step.worldScoped = worldScoped;
        step.status = ctx.chainDone(chainKey) ? JournalStatus.DONE
                : ctx.ready(type) ? JournalStatus.READY
                : ctx.holds(type) ? JournalStatus.ACTIVE : JournalStatus.AVAILABLE;
        if (step.status != JournalStatus.DONE) {
            deliver(ctx, step, type, turnIn);
        }
        step.reward = reward;
        return step;
    }

    /** One objective line per asked item, with the reader's own count, then the turn-in line. */
    private static void deliver(JournalContext ctx, JournalStep step, Class<? extends DeliverItemsQuest> type,
            GameMessage turnIn) {
        for (QuestAsks.Ask ask : QuestAsks.of(type)) {
            GameMessage itemName = ItemRegistry.getLocalization(ask.item.getID());
            int have = ctx.have(ask.item);
            if (have < 0) {
                step.objectives.add(new LocalMessage("journal", "objdeliver")
                        .addReplacement("amount", String.valueOf(ask.amount))
                        .addReplacement("item", itemName));
            } else {
                LocalMessage line = new LocalMessage("journal", "objdeliverhave")
                        .addReplacement("amount", String.valueOf(ask.amount))
                        .addReplacement("item", itemName)
                        .addReplacement("have", String.valueOf(have));
                step.objectives.add(check(have >= ask.amount, line));
            }
        }
        if (turnIn != null) {
            step.objectives.add(turnIn);
        }
    }

    /**
     * Every step the Warden gives, by journal ID: his own chain, the chalk
     * and the six region keys. Each carries a {@code journal.why<id>} and a
     * {@code journal.opens<id>} line, and {@code /swhjournal} counts that they
     * do, in both languages.
     */
    public static final java.util.Set<String> WARDEN_STEPS = new java.util.LinkedHashSet<>(java.util.Arrays.asList(
            "findspire", "recruitwarden", "cats", "anchor", "ghostchalk",
            "keyskyreach", "keyeden", "keysteinfeld", "keyghostrealm", "keycrookedbeyond", "keyhell"));

    /** Why the Warden asks for it and what it opens, from the step's own ID. */
    private static void explain(JournalStep step) {
        // Built keys, one per WARDEN_STEPS entry; tools/locale_audit.py notes
        // them as runtime-built, and /swhjournal checks each in en and de.
        String whyKey = "why" + step.id;
        String opensKey = "opens" + step.id;
        step.why = new LocalMessage("journal", whyKey);
        step.opens = new LocalMessage("journal", opensKey);
    }

    private static GameMessage after(GameMessage title) {
        return new LocalMessage("journal", "hintafter", "step", title);
    }

    /** Prefixes a tick or an open box, so a finished line reads finished in any language. */
    private static GameMessage check(boolean done, GameMessage line) {
        return done ? new LocalMessage("journal", "objdone", "line", line)
                : new LocalMessage("journal", "objopen", "line", line);
    }

    /** For {@code /swhjournal}: every delivery quest this source reads asks from. */
    public static List<Class<? extends DeliverItemsQuest>> deliveryQuests() {
        List<Class<? extends DeliverItemsQuest>> out = new ArrayList<>();
        out.add(AnchorDeliveryQuest.class);
        out.add(SkyreachKeyQuest.class);
        out.add(EdenPlantsQuest.class);
        out.add(EdenKeyQuest.class);
        out.add(SteinfeldVigilQuest.class);
        out.add(SteinfeldKeyQuest.class);
        out.add(MortimerRitesQuest.class);
        out.add(CaspernForgeQuest.class);
        out.add(GhostKeyQuest.class);
        out.add(CrookedDoorQuest.class);
        out.add(CrookedKeyQuest.class);
        out.add(HellKeyQuest.class);
        return out;
    }
}
