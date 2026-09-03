package stairwaytoheaven.quest;

import necesse.engine.network.server.Server;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldData.WorldData;
import stairwaytoheaven.worldgen.RealmDepth;

/**
 * The one fact about the Warden that must outlive a dimension.
 *
 * {@link SkywatchQuestData} is a {@code LevelData} and therefore lives and dies
 * with the Skyreach level. That is right for everything about the spire, and
 * wrong for exactly one bit: whether this world has already recruited a Sky
 * Warden. When {@code SkyRegistry.WORLD_GENERATION} is bumped, the mod starts a
 * FRESH Skyreach ("skyreach2"), so its quest data says {@code recruited=false}
 * -- while the Warden the player paid for is still standing in their settlement
 * on the surface. Without a world-scoped record the newly stamped spire would
 * hand out a second Warden, and vanilla's recruit page would happily take the
 * fee a second time (nothing in {@code HumanShop} is per-mob-type).
 *
 * So this is deliberately a {@code WorldData}: it is written next to the
 * settlements in the world entity, not in a level file, and survives every
 * generation bump. Server-side only -- nothing reads it on the client, so it is
 * never packed into a world packet.
 */
public class SkywatchWorldData extends WorldData {

    /** Must match {@code [a-zA-Z0-9]+} -- WorldEntity.addWorldData enforces it. */
    public static final String KEY = "skywatchworld";

    /** True once ANY Sky Warden in this world has become a settler. */
    public boolean wardenRecruited = false;

    /** Auth of the player who paid, for multiplayer bookkeeping. 0 = unknown. */
    public long wardenAuth = 0L;

    /**
     * Mirror of the two cats' "lives at the spire now" flags.
     *
     * The authoritative copy is still {@link SkywatchQuestData}, which is
     * LevelData on the Skyreach. That is right for the lair and basket
     * COORDINATES -- they describe that level -- and wrong for the fact that a
     * player already spent a Cloudpuff Treat, which is progression.
     *
     * The integration test caught a coaxed cat losing its flag across a
     * restart: phase 1 saw both cats at the basket, phase 2 after the restart
     * saw {@code homeFlags black=false tabby=false} and both back at their
     * lairs. It reproduces on some world seeds and not others, and I have NOT
     * root-caused which write is lost -- the plausible candidates all involve
     * the Skyreach level object being replaced between the coax and the save
     * ({@code LevelManager} line 55-61 unloads and overwrites an existing
     * identifier, and {@code Server} unloads-then-saves a quiet level).
     *
     * Rather than guess, this removes the failure class: the flag is written
     * here too, and {@link SkywatchQuestData#get} folds it back in. A level
     * record that lost the write is repaired the next time it is read; a world
     * record cannot be lost to a level unload because it is not on a level.
     */
    public boolean blackHome = false;
    public boolean tabbyHome = false;

    /**
     * The cats' HOME: the tile a player-placed Cat Basket stands on, and the
     * level it stands on.
     *
     * This lives here rather than in {@link SkywatchQuestData} for the same
     * reason this class exists at all. Quest data is {@code LevelData} on the
     * Skyreach: it dies with a generation bump and is not readable while that
     * level is unloaded. A home on the SURFACE cannot be stored in the sky
     * level's data at all -- it is not a fact about the Skyreach.
     *
     * {@code catHomeLevel} is a {@link necesse.engine.util.LevelIdentifier}'s
     * {@code stringID}, which matches {@code [a-z0-9-+]{1,50}}
     * (LevelIdentifier.java:13), so it survives a plain string round trip.
     * Empty means "no player-placed home" and the cats fall back to the spire
     * basket. The tile is meaningless without the level, which is why the two
     * are always written, read and cleared together.
     */
    /**
     * Eleanor's ending, once a player has chosen one.
     *
     * WORLD_DESIGN.md §11 gives the Lost Soul two: PASS ON (she goes, and
     * leaves a trinket) or STAY (she is recruited and becomes a settler). Only
     * the first one needs recording -- staying is written down by the
     * settlement itself, which now holds her -- and it has to be recorded
     * somewhere no dimension owns, because the choice is made in the Veil and
     * the consequence ("never place another Eleanor, never let one travel to a
     * town") has to hold on every level and across a generation bump. That is
     * this class's whole reason for existing, so it lives here rather than in
     * a second WorldData.
     */
    public boolean eleanorPassedOn = false;

    /**
     * Has this world already paid Eveleen's price and had her join?
     *
     * WORLD_DESIGN.md §5: she is "unlocked by discovering an Eden island +
     * collecting three Eden plants". {@link stairwaytoheaven.quest.EdenPlantsQuest}
     * is that ask made concrete, and this is its one-time payout record: once a
     * player has handed her the three plants, she never asks again and — see
     * {@code EveleenMob.getRecruitItems} — she stops asking for the settlement
     * fee too. A WorldData rather than a flag on the mob itself for the same
     * reason {@link #eleanorPassedOn} is: the choice is made wherever Eveleen is
     * standing, and the consequence has to hold everywhere she might ever stand,
     * including a settlement on the surface a dimension away.
     */
    public boolean edenPlantsGiven = false;

    /**
     * Has this world's Doorman already proven a Crooked door can lead
     * somewhere? {@link stairwaytoheaven.quest.CrookedDoorQuest}'s one-time
     * payout record, for the same reason {@link #edenPlantsGiven} is one: the
     * delivery happens wherever Mr. Knott is standing, and "already paid" has
     * to be a fact about the world, not about one Crooked region.
     */
    public boolean crookedDoorwayOpened = false;

    /**
     * Mob string IDs of the mod's NAMED residents this world has already
     * produced — placed by worldgen, or moved into a settlement.
     *
     * WHY IT EXISTS. Each of these people is an individual, not a settler type:
     * one Magpie, one Halda, one Eveleen per world. They can now be produced by
     * two independent routes — worldgen stands them beside a workshop or a bone
     * pile, and {@code SkyArrivals} lets them travel to a settlement and ask to
     * join — and neither route can see the other. Worldgen runs on a region of
     * the Skyreach or the Veil that a settlement's visitor roll knows nothing
     * about, and the visitor roll runs on a level that may be a thousand tiles
     * and one dimension away from any generated region. Without a shared record
     * a world can hold two Magpies.
     *
     * So both routes claim the name here first and both refuse a name already
     * claimed. It is a WorldData for the same reason {@code wardenRecruited} is:
     * it is a fact about the WORLD, not about any level, and it has to survive a
     * generation bump that starts a fresh Skyreach.
     */
    public final java.util.HashSet<String> residentsClaimed = new java.util.HashSet<>();

    /**
     * Which realms' boss portals a key piece has already unlocked.
     *
     * <p>{@code docs/FOGKEY_AND_BOSSPORTALS.md} §B2: <i>"Stand the key piece in
     * your base and that region's boss portals unlock. Before that they are
     * inert."</i> Two facts follow from that one sentence and both point here.
     *
     * <p>The key piece stands in the player's BASE and the portals stand in the
     * realm, so the unlock cannot live on either of them — it is a fact about
     * the WORLD, exactly like {@link #wardenRecruited} and
     * {@link #crookedDoorwayOpened}, and for exactly the same reason: a
     * {@code LevelData} record dies with a generation bump and cannot be read
     * while its level is unloaded, and the two places involved are frequently
     * different levels.
     *
     * <p>And it is world-scoped rather than per player, unlike the Ghost
     * chalk's grant: a key piece is a BUILDING. One player builds it, it stands
     * in a shared settlement, and everyone in that world can then use the
     * portals. That is what §B1-B2 describe, and it is the only reading that
     * does not make a co-op world build five statues.
     *
     * <p>Stored as realm KEYS ({@code RealmDepth.keyOf}) rather than as a fixed
     * array of booleans, mirroring {@link #residentsClaimed}: a save written
     * before Hell exists must still load once {@code REALM_COUNT} grows, and a
     * set of names cannot be silently shifted by a renumbering the way an array
     * of flags can.
     */
    public final java.util.HashSet<String> bossPortalsUnlocked = new java.util.HashSet<>();

    /**
     * Which realms' key-piece quests have already been turned in.
     *
     * <p>The bookkeeping half of §B1. It is NOT the same fact as
     * {@link #bossPortalsUnlocked}: this one says the Warden has handed the key
     * piece over, that one says somebody has stood it up in a settlement. The
     * two are deliberately separable — a player can carry a key piece around
     * for a week, drop it in a chest, or build it somewhere that is not a
     * settlement yet — and collapsing them would either pay the reward twice or
     * unlock a realm the moment the quest was turned in, which is precisely the
     * <i>"stand the key piece in your base"</i> beat §B2 exists to create.
     *
     * <p>World-scoped, like {@link #bossPortalsUnlocked} and for the same
     * reason: a key piece is a BUILDING, one per world, and a co-op world
     * should not be asked to farm five realms once per player. Stored as realm
     * KEYS ({@code RealmDepth.keyOf}) so a save written before Hell exists still
     * loads once {@code REALM_COUNT} grows.
     */
    public final java.util.HashSet<String> regionKeysEarned = new java.util.HashSet<>();

    public boolean catHomeSet = false;
    public int catHomeX = 0;
    public int catHomeY = 0;
    public String catHomeLevel = "";

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addBoolean("wardenRecruited", this.wardenRecruited);
        save.addLong("wardenAuth", this.wardenAuth);
        save.addBoolean("blackHome", this.blackHome);
        save.addBoolean("tabbyHome", this.tabbyHome);
        save.addBoolean("eleanorPassedOn", this.eleanorPassedOn);
        save.addBoolean("edenPlantsGiven", this.edenPlantsGiven);
        save.addBoolean("crookedDoorwayOpened", this.crookedDoorwayOpened);
        save.addStringArray("residentsClaimed",
                this.residentsClaimed.toArray(new String[0]));
        save.addStringArray("bossPortalsUnlocked",
                this.bossPortalsUnlocked.toArray(new String[0]));
        save.addStringArray("regionKeysEarned",
                this.regionKeysEarned.toArray(new String[0]));
        save.addBoolean("catHomeSet", this.catHomeSet);
        save.addInt("catHomeX", this.catHomeX);
        save.addInt("catHomeY", this.catHomeY);
        save.addSafeString("catHomeLevel", this.catHomeLevel == null ? "" : this.catHomeLevel);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.wardenRecruited = save.getBoolean("wardenRecruited", this.wardenRecruited, false);
        this.wardenAuth = save.getLong("wardenAuth", this.wardenAuth, false);
        this.blackHome = save.getBoolean("blackHome", this.blackHome, false);
        this.tabbyHome = save.getBoolean("tabbyHome", this.tabbyHome, false);
        this.eleanorPassedOn = save.getBoolean("eleanorPassedOn", this.eleanorPassedOn, false);
        this.edenPlantsGiven = save.getBoolean("edenPlantsGiven", this.edenPlantsGiven, false);
        this.crookedDoorwayOpened = save.getBoolean("crookedDoorwayOpened", this.crookedDoorwayOpened, false);
        this.residentsClaimed.clear();
        for (String claimed : save.getStringArray("residentsClaimed", new String[0], false)) {
            if (claimed != null && !claimed.isEmpty()) {
                this.residentsClaimed.add(claimed);
            }
        }
        this.bossPortalsUnlocked.clear();
        for (String unlocked : save.getStringArray("bossPortalsUnlocked", new String[0], false)) {
            if (unlocked != null && !unlocked.isEmpty()) {
                this.bossPortalsUnlocked.add(unlocked);
            }
        }
        this.regionKeysEarned.clear();
        for (String earned : save.getStringArray("regionKeysEarned", new String[0], false)) {
            if (earned != null && !earned.isEmpty()) {
                this.regionKeysEarned.add(earned);
            }
        }
        this.catHomeSet = save.getBoolean("catHomeSet", this.catHomeSet, false);
        this.catHomeX = save.getInt("catHomeX", this.catHomeX, false);
        this.catHomeY = save.getInt("catHomeY", this.catHomeY, false);
        this.catHomeLevel = save.getSafeString("catHomeLevel", this.catHomeLevel, false);
        // A tile without a level names no place, and sending a cat to one is
        // exactly the failure this pair of fields exists to prevent. A save
        // written before this field existed lands here.
        if (this.catHomeLevel == null || this.catHomeLevel.isEmpty()) {
            this.catHomeSet = false;
            this.catHomeLevel = "";
        }
    }

    /**
     * Records that this world has its Warden. Idempotent, and never downgrades:
     * once true it stays true, and the first auth wins so a second player
     * talking to him cannot rewrite whose contract it was.
     */
    public void markRecruited(long auth) {
        this.wardenRecruited = true;
        if (this.wardenAuth == 0L) {
            this.wardenAuth = auth;
        }
    }

    /**
     * The world record, created on first use. Mirrors
     * {@link SkywatchQuestData#get(necesse.level.maps.Level)}: a world that
     * never had one simply gets an empty record, which reads as "no Warden yet".
     */
    public static SkywatchWorldData get(Server server) {
        if (server == null || server.world == null) {
            return null;
        }
        WorldEntity worldEntity = server.world.worldEntity;
        if (worldEntity == null) {
            return null;
        }
        WorldData data = worldEntity.getWorldData(KEY);
        if (data instanceof SkywatchWorldData) {
            return (SkywatchWorldData) data;
        }
        SkywatchWorldData created = new SkywatchWorldData();
        worldEntity.addWorldData(KEY, created);
        return created;
    }

    /**
     * Records a player-placed Cat Basket as the cats' home. The NEWEST basket
     * always wins -- placing a second one anywhere moves them -- because that
     * is the only rule a player can hold in their head while decorating.
     */
    public void setCatHome(String levelStringID, int tileX, int tileY) {
        this.catHomeSet = true;
        this.catHomeLevel = levelStringID;
        this.catHomeX = tileX;
        this.catHomeY = tileY;
    }

    /** True when the recorded home is exactly this tile on this level. */
    public boolean isCatHome(String levelStringID, int tileX, int tileY) {
        return this.catHomeSet && this.catHomeX == tileX && this.catHomeY == tileY
                && this.catHomeLevel != null && this.catHomeLevel.equals(levelStringID);
    }

    /**
     * Forgets the recorded home, but ONLY when it is the tile given: a basket
     * broken somewhere else must never evict the cats from the one they live
     * in.
     *
     * @return true when a record was actually cleared.
     */
    public boolean clearCatHome(String levelStringID, int tileX, int tileY) {
        if (!this.isCatHome(levelStringID, tileX, tileY)) {
            return false;
        }
        this.catHomeSet = false;
        this.catHomeLevel = "";
        this.catHomeX = 0;
        this.catHomeY = 0;
        return true;
    }

    /** True once EITHER cat has been coaxed home with a Cloudpuff Treat. */
    public boolean anyCatCoaxed() {
        return this.blackHome || this.tabbyHome;
    }

    /** Has this cat been coaxed home? Progression, not position. */
    public boolean isCatCoaxed(boolean isBlackCat) {
        return isBlackCat ? this.blackHome : this.tabbyHome;
    }

    /** Records a cat as living at the spire. Never un-records one. */
    public void markCatHome(boolean isBlackCat) {
        if (isBlackCat) {
            this.blackHome = true;
        } else {
            this.tabbyHome = true;
        }
    }

    /** Convenience: does this world already have a recruited Warden? */
    public static boolean hasWarden(Server server) {
        SkywatchWorldData data = get(server);
        return data != null && data.wardenRecruited;
    }

    /**
     * Has Eleanor already been let go in this world? Never un-records: an
     * ending is an ending, and a second Eleanor would make the first one mean
     * nothing.
     */
    public static boolean eleanorGone(Server server) {
        SkywatchWorldData data = get(server);
        return data != null && data.eleanorPassedOn;
    }

    /** Records Eleanor's PASS ON ending. Idempotent. */
    public static void markEleanorPassedOn(Server server) {
        SkywatchWorldData data = get(server);
        if (data != null) {
            data.eleanorPassedOn = true;
        }
    }

    /** Has this world already given Eveleen her three Eden plants? */
    public static boolean edenPlantsGiven(Server server) {
        SkywatchWorldData data = get(server);
        return data != null && data.edenPlantsGiven;
    }

    /** Records the Eden Plants delivery. Idempotent. */
    public static void markEdenPlantsGiven(Server server) {
        SkywatchWorldData data = get(server);
        if (data != null) {
            data.edenPlantsGiven = true;
        }
    }

    /** Has this world already proven a Crooked doorway open for Mr. Knott? */
    public static boolean crookedDoorwayOpened(Server server) {
        SkywatchWorldData data = get(server);
        return data != null && data.crookedDoorwayOpened;
    }

    /** Records the Crooked Door delivery. Idempotent. */
    public static void markCrookedDoorwayOpened(Server server) {
        SkywatchWorldData data = get(server);
        if (data != null) {
            data.crookedDoorwayOpened = true;
        }
    }

    /**
     * Has this world already produced this named resident, by either route?
     *
     * Answers TRUE when the record cannot be read at all, because the safe
     * failure is "do not make another one": a missing person is a bug the
     * player can work around, two of the same person is one they cannot.
     */
    public static boolean residentClaimed(Server server, String mobStringID) {
        SkywatchWorldData data = get(server);
        return data == null || data.residentsClaimed.contains(mobStringID);
    }

    /** Claims a named resident for whichever route produced them. Idempotent. */
    public static void claimResident(Server server, String mobStringID) {
        SkywatchWorldData data = get(server);
        if (data != null) {
            data.residentsClaimed.add(mobStringID);
        }
    }

    // ------------------------------------------------------------------
    // The boss portals (docs/FOGKEY_AND_BOSSPORTALS.md §B2-B3)
    // ------------------------------------------------------------------

    /**
     * Are this realm's boss portals live?
     *
     * <p>The READ side of §B2. {@code bosses/BossPortalObjectEntity.use} asks
     * this before it wakes anything; a portal whose realm answers false says so
     * and does nothing, which is what §B3's <i>"before that they are inert"</i>
     * means in play.
     *
     * <p>Answers FALSE when the record cannot be read at all, the opposite way
     * round from {@link #residentClaimed}: the safe failure for an unlock is
     * "still locked". A portal that refuses when the world record is missing
     * costs a player one confused click; one that summons a tier-10 boss
     * because it could not find the record costs them the run.
     */
    public boolean bossPortalsUnlocked(int realm) {
        return this.bossPortalsUnlocked.contains(RealmDepth.keyOf(realm));
    }

    /**
     * Records that this realm's key piece has been built, and its portals are
     * live from now on.
     *
     * <p>The WRITE side of §B2, and the entry point the Elder-quest / key-piece
     * work calls. Idempotent, and it never un-records: a key piece the player
     * later mines up does not re-lock a realm they have already earned, in the
     * same spirit as {@link #eleanorPassedOn} and {@link #wardenRecruited} —
     * progression in this mod only ever goes forwards.
     */
    public void unlockBossPortals(int realm) {
        this.bossPortalsUnlocked.add(RealmDepth.keyOf(realm));
    }

    /** {@link #bossPortalsUnlocked(int)} for a caller that only has a server. */
    public static boolean bossPortalsUnlocked(Server server, int realm) {
        SkywatchWorldData data = get(server);
        return data != null && data.bossPortalsUnlocked(realm);
    }

    /** {@link #unlockBossPortals(int)} for a caller that only has a server. */
    public static void unlockBossPortals(Server server, int realm) {
        SkywatchWorldData data = get(server);
        if (data != null) {
            data.unlockBossPortals(realm);
        }
    }

    // ------------------------------------------------------------------
    // The region key quests (docs/FOGKEY_AND_BOSSPORTALS.md §B1)
    // ------------------------------------------------------------------

    /**
     * Has this realm's key piece already been handed over?
     *
     * <p>Answers TRUE when the record cannot be read at all, the opposite way
     * round from {@link #bossPortalsUnlocked(int)} and for the same kind of
     * reason: the safe failure for a REWARD is "already paid". A world whose
     * record is missing should not be able to farm the Warden for an unlimited
     * supply of key pieces, and a player who is told "you already have it" once
     * has lost nothing they cannot recover by breaking and re-placing the one
     * they built.
     */
    public static boolean regionKeyEarned(Server server, int realm) {
        SkywatchWorldData data = get(server);
        return data == null || data.regionKeysEarned.contains(RealmDepth.keyOf(realm));
    }

    /**
     * Records that a realm's key-piece quest has been turned in and paid.
     * Idempotent, and it never un-records — progression in this mod only ever
     * goes forwards, the same as {@link #unlockBossPortals(int)}.
     */
    public static void markRegionKeyEarned(Server server, int realm) {
        SkywatchWorldData data = get(server);
        if (data != null) {
            data.regionKeysEarned.add(RealmDepth.keyOf(realm));
        }
    }
}
