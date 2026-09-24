package stairwaytoheaven.journal;

import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.worldgen.RealmDepth;

/**
 * Builds one player's {@link JournalBook}: six realm chapters, each with its
 * story steps (from the installed {@link JournalStepSource}) and then the
 * parts that do not depend on how quests are organised — the realm's key and
 * boss, the people who live there, its landmarks, and the lore the player's
 * progress has unlocked.
 *
 * <p>Server-side, read-only. Nothing here writes a record.
 */
public final class JournalBuilder {

    /** Region-key item per realm, in {@code RealmDepth.REALM_*} order ({@code RegionKeyObject.register}). */
    static final String[] KEY_ITEMS = {"regionkeyskyreach", "regionkeyeden", "regionkeysteinfeld",
            "regionkeyghostrealm", "regionkeycrookedbeyond", "regionkeyhell"};
    /** Boss per realm, in {@code RealmDepth.REALM_*} order ({@code bosses/SkyBossLadder}). */
    static final String[] BOSSES = {"cryoqueen", "moonlightdancer", "ascendedwizard",
            "pestwarden", "crystaldragon", "mutanthydra"};

    private JournalBuilder() {
    }

    public static JournalBook build(Server server, ServerClient client, JournalStepSource source) {
        JournalContext ctx = new JournalContext(server, client);
        JournalBook book = new JournalBook();
        book.sourceName = source.name();
        for (int realm = 0; realm < RealmDepth.REALM_COUNT; realm++) {
            JournalChapter chapter = new JournalChapter(realm);
            chapter.name = realmName(realm);
            chapter.intro = realmIntro(realm);
            book.chapters.add(chapter);
        }
        List<JournalStep> steps = source.buildSteps(ctx);
        for (JournalStep step : steps) {
            JournalChapter chapter = book.chapter(step.realm);
            if (chapter == null) {
                chapter = book.chapters.get(0);
            }
            chapter.steps.add(step);
        }
        for (JournalChapter chapter : book.chapters) {
            facts(ctx, chapter);
            residents(ctx, chapter);
            landmarks(ctx, chapter);
            lore(ctx, chapter);
        }
        return book;
    }

    // ------------------------------------------------------------------

    static GameMessage realmName(int realm) {
        switch (realm) {
            case RealmDepth.REALM_SKYREACH: return new LocalMessage("journal", "realm0");
            case RealmDepth.REALM_EDEN: return new LocalMessage("journal", "realm1");
            case RealmDepth.REALM_STEINFELD: return new LocalMessage("journal", "realm2");
            case RealmDepth.REALM_GHOST: return new LocalMessage("journal", "realm3");
            case RealmDepth.REALM_CROOKED: return new LocalMessage("journal", "realm4");
            default: return new LocalMessage("journal", "realm5");
        }
    }

    private static GameMessage realmIntro(int realm) {
        switch (realm) {
            case RealmDepth.REALM_SKYREACH: return new LocalMessage("journal", "intro0");
            case RealmDepth.REALM_EDEN: return new LocalMessage("journal", "intro1");
            case RealmDepth.REALM_STEINFELD: return new LocalMessage("journal", "intro2");
            case RealmDepth.REALM_GHOST: return new LocalMessage("journal", "intro3");
            case RealmDepth.REALM_CROOKED: return new LocalMessage("journal", "intro4");
            default: return new LocalMessage("journal", "intro5");
        }
    }

    // ------------------------------------------------------------------
    // key, stones, boss

    private static void facts(JournalContext ctx, JournalChapter chapter) {
        int realm = chapter.realm;
        boolean visited = ctx.visited(realm);
        chapter.facts.add(new JournalLine(visited ? JournalStatus.DONE : JournalStatus.LOCKED,
                visited ? new LocalMessage("journal", "factvisited") : new LocalMessage("journal", "factnotvisited"),
                null));

        boolean earned = ctx.keyEarned(realm);
        GameMessage keyName = ItemRegistry.getItemID(KEY_ITEMS[realm]) >= 0
                ? ItemRegistry.getLocalization(KEY_ITEMS[realm]) : new LocalMessage("journal", "unknown");
        chapter.facts.add(new JournalLine(earned ? JournalStatus.DONE : JournalStatus.LOCKED,
                new LocalMessage("journal", "factkey", "key", keyName),
                earned ? new LocalMessage("journal", "factkeyearned") : new LocalMessage("journal", "factkeynot")));

        boolean awake = ctx.portalsAwake(realm);
        chapter.facts.add(new JournalLine(awake ? JournalStatus.DONE : earned ? JournalStatus.AVAILABLE : JournalStatus.LOCKED,
                new LocalMessage("journal", "factstones"),
                awake ? new LocalMessage("journal", "factstonesawake")
                        : earned ? new LocalMessage("journal", "hintplacekey")
                        : new LocalMessage("journal", "factstonesasleep")));

        String boss = BOSSES[realm];
        int kills = ctx.kills(boss);
        GameMessage bossName = MobRegistry.getMobID(boss) >= 0
                ? MobRegistry.getLocalization(boss) : new LocalMessage("journal", "unknown");
        chapter.facts.add(new JournalLine(kills > 0 ? JournalStatus.DONE : awake ? JournalStatus.AVAILABLE : JournalStatus.LOCKED,
                new LocalMessage("journal", "factboss", "boss", bossName),
                kills > 0 ? new LocalMessage("journal", "factbosskills", "count", String.valueOf(kills))
                        : awake ? new LocalMessage("journal", "factbossready")
                        : new LocalMessage("journal", "factbossasleep")));
    }

    // ------------------------------------------------------------------
    // residents

    private static void residents(JournalContext ctx, JournalChapter chapter) {
        switch (chapter.realm) {
            case RealmDepth.REALM_SKYREACH:
                resident(ctx, chapter, "skywarden", ctx.stage() >= 1 || ctx.met("skywarden", "wardensettler"),
                        ctx.wardenRecruited(), new LocalMessage("journal", "rolewarden"),
                        new LocalMessage("journal", "wherespire"));
                resident(ctx, chapter, "magpiesettler", ctx.met("magpiesettler"), ctx.seenAsSettler("magpiesettler"),
                        new LocalMessage("journal", "rolemagpie"), new LocalMessage("journal", "lmtollhouse"));
                resident(ctx, chapter, "haldasettler", ctx.met("haldasettler"), ctx.seenAsSettler("haldasettler"),
                        new LocalMessage("journal", "rolehalda"), new LocalMessage("journal", "lmgrange"));
                resident(ctx, chapter, "ossiansettler", ctx.met("ossiansettler"), ctx.seenAsSettler("ossiansettler"),
                        new LocalMessage("journal", "roleossian"), new LocalMessage("journal", "lmtestrange"));
                resident(ctx, chapter, "spirecatblack", ctx.met("spirecatblack") || ctx.catHome(true),
                        ctx.catHome(true), new LocalMessage("journal", "rolesiggi"),
                        new LocalMessage("journal", "wherestormveil"));
                resident(ctx, chapter, "spirecattabby", ctx.met("spirecattabby") || ctx.catHome(false),
                        ctx.catHome(false), new LocalMessage("journal", "rolepeanut"),
                        new LocalMessage("journal", "whereaurora"));
                break;
            case RealmDepth.REALM_EDEN:
                resident(ctx, chapter, "eveleensettler", ctx.met("eveleensettler") || ctx.edenPlantsGiven(),
                        ctx.seenAsSettler("eveleensettler"), new LocalMessage("journal", "roleeveleen"),
                        new LocalMessage("journal", "whereeveleen"));
                break;
            case RealmDepth.REALM_STEINFELD:
                resident(ctx, chapter, "ivessettler",
                        ctx.met("ivessettler") || ctx.chainDone(SkywatchWorldData.CHAIN_STEINFELD_VIGIL),
                        ctx.seenAsSettler("ivessettler"), new LocalMessage("journal", "roleives"),
                        new LocalMessage("journal", "whereives"));
                break;
            case RealmDepth.REALM_GHOST:
                resident(ctx, chapter, "mortimersettler",
                        ctx.met("mortimersettler") || ctx.chainDone(SkywatchWorldData.CHAIN_MORTIMER_RITES),
                        ctx.seenAsSettler("mortimersettler"), new LocalMessage("journal", "rolemortimer"),
                        new LocalMessage("journal", "wheregrave"));
                resident(ctx, chapter, "caspernsettler",
                        ctx.met("caspernsettler") || ctx.chainDone(SkywatchWorldData.CHAIN_CASPERN_FORGE),
                        ctx.seenAsSettler("caspernsettler"), new LocalMessage("journal", "rolecaspern"),
                        new LocalMessage("journal", "wheregrave"));
                if (ctx.eleanorPassedOn()) {
                    chapter.residents.add(new JournalLine(JournalStatus.DONE,
                            MobRegistry.getLocalization("eleanorsettler"),
                            new LocalMessage("journal", "eleanorgone")));
                } else {
                    resident(ctx, chapter, "eleanorsettler", ctx.met("eleanorsettler"),
                            ctx.seenAsSettler("eleanorsettler"), new LocalMessage("journal", "roleeleanor"),
                            new LocalMessage("journal", "wheregrave"));
                }
                resident(ctx, chapter, "ghostguide", ctx.met("ghostguide") || ctx.hasMark(), false,
                        new LocalMessage("journal", "roleghostguide"), new LocalMessage("journal", "whereseance"));
                break;
            case RealmDepth.REALM_CROOKED:
                resident(ctx, chapter, "knottsettler", ctx.met("knottsettler") || ctx.crookedDoorwayOpened(),
                        ctx.seenAsSettler("knottsettler"), new LocalMessage("journal", "roleknott"),
                        new LocalMessage("journal", "whereknott"));
                break;
            default:
                chapter.residents.add(new JournalLine(JournalStatus.LOCKED,
                        new LocalMessage("journal", "noresidents"), null));
                break;
        }
    }

    /** A person the player has met reads in full; one not yet met is "???" and where to look. */
    private static void resident(JournalContext ctx, JournalChapter chapter, String mobID, boolean met,
            boolean settled, GameMessage role, GameMessage where) {
        if (!met) {
            chapter.residents.add(new JournalLine(JournalStatus.LOCKED,
                    new LocalMessage("journal", "unknownresident"),
                    new LocalMessage("journal", "residenthint", "where", where)));
            return;
        }
        GameMessage name = MobRegistry.getMobID(mobID) >= 0
                ? MobRegistry.getLocalization(mobID) : new LocalMessage("journal", "unknown");
        GameMessage detail = settled
                ? new LocalMessage("journal", "residentsettled", "role", role)
                : new LocalMessage("journal", "residentat", "role", role).addReplacement("where", where);
        chapter.residents.add(new JournalLine(settled ? JournalStatus.DONE : JournalStatus.AVAILABLE, name, detail));
    }

    // ------------------------------------------------------------------
    // landmarks

    private static void landmarks(JournalContext ctx, JournalChapter chapter) {
        switch (chapter.realm) {
            case RealmDepth.REALM_SKYREACH:
                landmark(chapter, ctx.stage() >= 1 || ctx.met("skywarden", "wardensettler"),
                        new LocalMessage("journal", "lmspire"), new LocalMessage("journal", "lmspiredesc"),
                        new LocalMessage("journal", "lmspirehint"));
                landmark(chapter, ctx.met("magpiesettler", "tollwright") || ctx.kills("tollwright") > 0,
                        new LocalMessage("journal", "lmtollhouse"), new LocalMessage("journal", "lmtollhousedesc"),
                        new LocalMessage("journal", "lmtollhousehint"));
                landmark(chapter, ctx.met("haldasettler", "sourvatbloom", "vatling") || ctx.kills("sourvatbloom") > 0,
                        new LocalMessage("journal", "lmgrange"), new LocalMessage("journal", "lmgrangedesc"),
                        new LocalMessage("journal", "lmgrangehint"));
                landmark(chapter, ctx.met("ossiansettler", "prototypenine") || ctx.kills("prototypenine") > 0,
                        new LocalMessage("journal", "lmtestrange"), new LocalMessage("journal", "lmtestrangedesc"),
                        new LocalMessage("journal", "lmtestrangehint"));
                break;
            case RealmDepth.REALM_EDEN:
                landmark(chapter, ctx.met("eveleensettler"),
                        new LocalMessage("journal", "lmknowledgetree"), new LocalMessage("journal", "lmknowledgetreedesc"),
                        new LocalMessage("journal", "lmknowledgetreehint"));
                break;
            case RealmDepth.REALM_STEINFELD:
                landmark(chapter, ctx.met("ivessettler"),
                        new LocalMessage("journal", "lmbrokenangel"), new LocalMessage("journal", "lmbrokenangeldesc"),
                        new LocalMessage("journal", "lmbrokenangelhint"));
                break;
            case RealmDepth.REALM_GHOST:
                landmark(chapter, ctx.fogTouched() || ctx.hasMark(),
                        new LocalMessage("journal", "lmfogwall"), new LocalMessage("journal", "lmfogwalldesc"),
                        new LocalMessage("journal", "wherefog"));
                landmark(chapter, ctx.met("mortimersettler", "caspernsettler", "eleanorsettler"),
                        new LocalMessage("journal", "lmgraves"), new LocalMessage("journal", "lmgravesdesc"),
                        new LocalMessage("journal", "wheregrave"));
                landmark(chapter, ctx.met("ghostguide") || ctx.hasMark(),
                        new LocalMessage("journal", "lmseance"), new LocalMessage("journal", "lmseancedesc"),
                        new LocalMessage("journal", "whereseance"));
                break;
            case RealmDepth.REALM_CROOKED:
                landmark(chapter, ctx.met("knottsettler"),
                        new LocalMessage("journal", "lmdooryard"), new LocalMessage("journal", "lmdooryarddesc"),
                        new LocalMessage("journal", "whereknott"));
                break;
            default:
                landmark(chapter, ctx.visited(RealmDepth.REALM_HELL),
                        new LocalMessage("journal", "lmhellrim"), new LocalMessage("journal", "lmhellrimdesc"),
                        new LocalMessage("journal", "lmhellrimhint"));
                break;
        }
    }

    private static void landmark(JournalChapter chapter, boolean found, GameMessage name, GameMessage desc,
            GameMessage hint) {
        chapter.landmarks.add(new JournalLine(found ? JournalStatus.DONE : JournalStatus.LOCKED, name,
                found ? desc : new LocalMessage("journal", "notfoundyet", "where", hint)));
    }

    // ------------------------------------------------------------------
    // lore

    private static void lore(JournalContext ctx, JournalChapter chapter) {
        switch (chapter.realm) {
            case RealmDepth.REALM_SKYREACH:
                lore(chapter, ctx.stage() >= 1 || ctx.met("skywarden", "wardensettler"),
                        new LocalMessage("journal", "lore0atitle"), new LocalMessage("journal", "lore0a"),
                        new LocalMessage("journal", "lore0ahint"));
                lore(chapter, ctx.catHome(true) || ctx.catHome(false),
                        new LocalMessage("journal", "lore0btitle"), new LocalMessage("journal", "lore0b"),
                        new LocalMessage("journal", "lore0bhint"));
                lore(chapter, ctx.anchorDone(),
                        new LocalMessage("journal", "lore0ctitle"), new LocalMessage("journal", "lore0c"),
                        new LocalMessage("journal", "lore0chint"));
                break;
            case RealmDepth.REALM_EDEN:
                lore(chapter, ctx.visited(RealmDepth.REALM_EDEN),
                        new LocalMessage("journal", "lore1atitle"), new LocalMessage("journal", "lore1a"),
                        new LocalMessage("journal", "lore1ahint"));
                lore(chapter, ctx.edenPlantsGiven(),
                        new LocalMessage("journal", "lore1btitle"), new LocalMessage("journal", "lore1b"),
                        new LocalMessage("journal", "lore1bhint"));
                break;
            case RealmDepth.REALM_STEINFELD:
                lore(chapter, ctx.visited(RealmDepth.REALM_STEINFELD),
                        new LocalMessage("journal", "lore2atitle"), new LocalMessage("journal", "lore2a"),
                        new LocalMessage("journal", "lore2ahint"));
                lore(chapter, ctx.chainDone(SkywatchWorldData.CHAIN_STEINFELD_VIGIL),
                        new LocalMessage("journal", "lore2btitle"), new LocalMessage("journal", "lore2b"),
                        new LocalMessage("journal", "lore2bhint"));
                break;
            case RealmDepth.REALM_GHOST:
                lore(chapter, ctx.fogTouched() || ctx.hasMark(),
                        new LocalMessage("journal", "lore3atitle"), new LocalMessage("journal", "lore3a"),
                        new LocalMessage("journal", "lore3ahint"));
                lore(chapter, ctx.hasMark(),
                        new LocalMessage("journal", "lore3btitle"), new LocalMessage("journal", "lore3b"),
                        new LocalMessage("journal", "lore3bhint"));
                lore(chapter, ctx.chainDone(SkywatchWorldData.CHAIN_MORTIMER_RITES)
                                || ctx.chainDone(SkywatchWorldData.CHAIN_CASPERN_FORGE)
                                || ctx.eleanorPassedOn(),
                        new LocalMessage("journal", "lore3ctitle"), new LocalMessage("journal", "lore3c"),
                        new LocalMessage("journal", "lore3chint"));
                break;
            case RealmDepth.REALM_CROOKED:
                lore(chapter, ctx.visited(RealmDepth.REALM_CROOKED),
                        new LocalMessage("journal", "lore4atitle"), new LocalMessage("journal", "lore4a"),
                        new LocalMessage("journal", "lore4ahint"));
                lore(chapter, ctx.crookedDoorwayOpened(),
                        new LocalMessage("journal", "lore4btitle"), new LocalMessage("journal", "lore4b"),
                        new LocalMessage("journal", "lore4bhint"));
                break;
            default:
                lore(chapter, ctx.visited(RealmDepth.REALM_HELL),
                        new LocalMessage("journal", "lore5atitle"), new LocalMessage("journal", "lore5a"),
                        new LocalMessage("journal", "lore5ahint"));
                lore(chapter, ctx.keyEarned(RealmDepth.REALM_HELL),
                        new LocalMessage("journal", "lore5btitle"), new LocalMessage("journal", "lore5b"),
                        new LocalMessage("journal", "lore5bhint"));
                break;
        }
    }

    /** An unlocked entry shows its title and text; a locked one only says what unlocks it. */
    private static void lore(JournalChapter chapter, boolean unlocked, GameMessage title, GameMessage text,
            GameMessage hint) {
        chapter.lore.add(unlocked
                ? new JournalLine(JournalStatus.DONE, title, text)
                : new JournalLine(JournalStatus.LOCKED, new LocalMessage("journal", "lorelocked"), hint));
    }
}
