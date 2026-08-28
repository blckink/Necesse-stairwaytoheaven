package stairwaytoheaven.quest;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.LevelData;

/**
 * Server-side world state of "The Warden's Call" quest chain, persisted with
 * the Skyreach level. One shared progression per world (like vanilla world
 * events); rewards go to the delivering player at turn-in time.
 *
 * Stages: 0 = Warden not yet met · 1 = beacon delivery open ·
 * 2+ = beacon lit (shop open; cats & anchor quests both available).
 */
public class SkywatchQuestData extends LevelData {

    public static final String KEY = "skywatchquest";

    /**
     * Save-schema version. v1 (pre-0.5) saves stored the old fetch-chain stage
     * semantics (stage >= 2 meant "beacon lit") and a stairway-anchored spire
     * position. v2 is the portal/recruitment design. A save without this field
     * is v1: its sky-side state is reset ONCE here (idempotent — every v2 save
     * writes the field), so the canonical-origin spire re-stamps cleanly and
     * nobody gets stuck in a farewell loop with a Warden they never recruited.
     * Surface data is never touched by this.
     */
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public int stage = 0;

    // v0.5 recruitment: the Warden joins the player's surface settlement.
    // stage 0 = not met · 1 = met (intro done, recruitment open) ·
    // 2 = recruited (spire warden gone, contract issued)
    public boolean recruited = false;
    /** Auth of the player whose contract it was (multiplayer bookkeeping). */
    public long recruitedAuth = 0L;

    // Warden's Spire (set once when the structure is stamped)
    public boolean spirePlaced = false;
    public int spireX, spireY;          // warden tile
    public int beaconX, beaconY;        // beacon object tile
    public int basketX, basketY;        // cat basket / cat home tile
    /**
     * Whether the basket OBJECT has been placed on that tile. The tile was
     * reserved from v0.2 and stood empty, so SkyLevel fills it in for worlds
     * that already stamped their spire — but exactly once. Healing it the way
     * the beacon is healed would make an uncraftable quest-reward furniture
     * item farmable: break it, wait ten seconds, break it again.
     */
    public boolean basketPlaced = false;

    // v0.5 portal routing: per-player memory of the surface stairway each
    // player ascended from, so the spire's Skywatch Gate sends them home.
    // Keyed by client authentication; values are surface tile X, Y pairs.
    public final java.util.HashMap<Long, long[]> returnStairs = new java.util.HashMap<>();

    // Cats
    public boolean catsSpawned = false;
    public int blackLairX, blackLairY;  // Siggi (Stormveil)
    public int tabbyLairX, tabbyLairY;  // Peanut (Aurora Shoals)
    public boolean blackHome = false;
    public boolean tabbyHome = false;
    public boolean catsIntroShown = false;
    public boolean catsRewardGiven = false;

    // Anchor finale
    /**
     * Locale key ("dirnorth".."dirsouthwest") of the 8-way compass direction
     * from (fromX,fromY) to (toX,toY). Screen convention: north = -y (up),
     * west = -x (left). An axis only counts once it contributes at least a
     * third of the travel, so near-cardinal paths read as plain cardinals.
     */
    public static String directionKey(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        String ns = dy < 0 ? "north" : "south";
        String ew = dx < 0 ? "west" : "east";
        if (dx == 0 && dy == 0) {
            return "dirnorth";
        }
        if (Math.abs(dx) > 2 * Math.abs(dy)) {
            return "dir" + ew;
        }
        if (Math.abs(dy) > 2 * Math.abs(dx)) {
            return "dir" + ns;
        }
        return "dir" + ns + ew;
    }

    public boolean anchorIntroShown = false;
    public boolean anchorDone = false;
    public boolean finaleShown = false;

    /**
     * Auths of players who already received each world-map marker. Markers
     * persist client-side per world, so each player gets each one exactly
     * once (and re-deleting one on purpose is respected). Two sets because
     * the spire marker can also be delivered by the status command, which
     * does not know the player's stairway position.
     */
    public final java.util.HashSet<Long> spireMarkerAuths = new java.util.HashSet<>();
    public final java.util.HashSet<Long> stairsMarkerAuths = new java.util.HashSet<>();
    /**
     * Who has already been shown the two cat lairs. Siggi and Peanut spawn at
     * fixed lairs a long way from the spire, and until the recruitment handed
     * out SpireCatsQuest there was nothing in game that pointed at them at all:
     * "Siggi und Peanut auch noch nirgends gefunden leider". A quest that says
     * "find two cats" in a world this size needs coordinates, not just a
     * sentence.
     */
    public final java.util.HashSet<Long> catMarkerAuths = new java.util.HashSet<>();

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addInt("schemaVersion", CURRENT_SCHEMA_VERSION);
        save.addInt("stage", this.stage);
        save.addBoolean("recruited", this.recruited);
        save.addLong("recruitedAuth", this.recruitedAuth);
        save.addBoolean("spirePlaced", this.spirePlaced);
        save.addInt("spireX", this.spireX);
        save.addInt("spireY", this.spireY);
        save.addInt("beaconX", this.beaconX);
        save.addInt("beaconY", this.beaconY);
        save.addInt("basketX", this.basketX);
        save.addInt("basketY", this.basketY);
        save.addBoolean("basketPlaced", this.basketPlaced);
        save.addBoolean("catsSpawned", this.catsSpawned);
        save.addInt("blackLairX", this.blackLairX);
        save.addInt("blackLairY", this.blackLairY);
        save.addInt("tabbyLairX", this.tabbyLairX);
        save.addInt("tabbyLairY", this.tabbyLairY);
        save.addBoolean("blackHome", this.blackHome);
        save.addBoolean("tabbyHome", this.tabbyHome);
        save.addBoolean("catsIntroShown", this.catsIntroShown);
        save.addBoolean("catsRewardGiven", this.catsRewardGiven);
        save.addBoolean("anchorIntroShown", this.anchorIntroShown);
        save.addBoolean("anchorDone", this.anchorDone);
        save.addBoolean("finaleShown", this.finaleShown);
        save.addLongArray("spireMarkerAuths", toLongArray(this.spireMarkerAuths));
        save.addLongArray("stairsMarkerAuths", toLongArray(this.stairsMarkerAuths));
        save.addLongArray("catMarkerAuths", toLongArray(this.catMarkerAuths));
        // v0.5 return-stairway bindings: auths, Xs and Ys as parallel arrays
        long[] auths = new long[this.returnStairs.size()];
        long[] xs = new long[this.returnStairs.size()];
        long[] ys = new long[this.returnStairs.size()];
        int i = 0;
        for (java.util.Map.Entry<Long, long[]> entry : this.returnStairs.entrySet()) {
            auths[i] = entry.getKey();
            long[] tile = entry.getValue();
            xs[i] = tile.length > 0 ? tile[0] : 0;
            ys[i] = tile.length > 1 ? tile[1] : 0;
            i++;
        }
        save.addLongArray("returnAuths", auths);
        save.addLongArray("returnXs", xs);
        save.addLongArray("returnYs", ys);
    }

    private static long[] toLongArray(java.util.Set<Long> set) {
        long[] out = new long[set.size()];
        int i = 0;
        for (long value : set) {
            out[i++] = value;
        }
        return out;
    }

    private static void loadLongSet(LoadData save, String name, java.util.Set<Long> into) {
        into.clear();
        if (save.hasLoadDataByName(name)) {
            for (long value : save.getLongArray(name)) {
                into.add(value);
            }
        }
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.stage = save.getInt("stage", 0, false);
        this.recruited = save.getBoolean("recruited", false, false);
        this.recruitedAuth = save.getLong("recruitedAuth", 0L, false);
        this.spirePlaced = save.getBoolean("spirePlaced", false, false);
        this.spireX = save.getInt("spireX", 0, false);
        this.spireY = save.getInt("spireY", 0, false);
        this.beaconX = save.getInt("beaconX", 0, false);
        this.beaconY = save.getInt("beaconY", 0, false);
        this.basketX = save.getInt("basketX", 0, false);
        this.basketY = save.getInt("basketY", 0, false);
        this.basketPlaced = save.getBoolean("basketPlaced", false, false);
        this.catsSpawned = save.getBoolean("catsSpawned", false, false);
        this.blackLairX = save.getInt("blackLairX", 0, false);
        this.blackLairY = save.getInt("blackLairY", 0, false);
        this.tabbyLairX = save.getInt("tabbyLairX", 0, false);
        this.tabbyLairY = save.getInt("tabbyLairY", 0, false);
        this.blackHome = save.getBoolean("blackHome", false, false);
        this.tabbyHome = save.getBoolean("tabbyHome", false, false);
        this.catsIntroShown = save.getBoolean("catsIntroShown", false, false);
        this.catsRewardGiven = save.getBoolean("catsRewardGiven", false, false);
        this.anchorIntroShown = save.getBoolean("anchorIntroShown", false, false);
        this.anchorDone = save.getBoolean("anchorDone", false, false);
        this.finaleShown = save.getBoolean("finaleShown", false, false);
        loadLongSet(save, "spireMarkerAuths", this.spireMarkerAuths);
        loadLongSet(save, "stairsMarkerAuths", this.stairsMarkerAuths);
        loadLongSet(save, "catMarkerAuths", this.catMarkerAuths);
        // v0.5 return-stairway bindings
        this.returnStairs.clear();
        if (save.hasLoadDataByName("returnAuths")) {
            long[] auths = save.getLongArray("returnAuths");
            long[] xs = save.hasLoadDataByName("returnXs") ? save.getLongArray("returnXs") : new long[0];
            long[] ys = save.hasLoadDataByName("returnYs") ? save.getLongArray("returnYs") : new long[0];
            for (int i = 0; i < auths.length; i++) {
                this.returnStairs.put(auths[i], new long[]{
                        i < xs.length ? xs[i] : 0,
                        i < ys.length ? ys[i] : 0});
            }
        }
        this.migrateLegacySave(save);
    }

    /**
     * One-time, idempotent v1 → v2 migration. A v1 save (pre-0.5) has no
     * "schemaVersion" entry; its sky-side progression used different stage
     * semantics and a stairway-anchored spire position, which would leave the
     * new flow hard-stuck (e.g. old stage >= 2 reads as "already recruited").
     * We reset ONLY Skyreach-side quest/landmark state so the canonical-origin
     * spire re-stamps on the next ascent; the Surface level, settlements,
     * inventories and world data are not part of this object and stay intact.
     */
    private void migrateLegacySave(LoadData save) {
        if (save.hasLoadDataByName("schemaVersion")) {
            return;
        }
        this.stage = 0;
        this.recruited = false;
        this.recruitedAuth = 0L;
        this.spirePlaced = false;
        this.spireX = 0;
        this.spireY = 0;
        this.beaconX = 0;
        this.beaconY = 0;
        this.basketX = 0;
        this.basketY = 0;
        this.basketPlaced = false;
        this.catsSpawned = false;
        this.blackLairX = 0;
        this.blackLairY = 0;
        this.tabbyLairX = 0;
        this.tabbyLairY = 0;
        this.blackHome = false;
        this.tabbyHome = false;
        this.catsIntroShown = false;
        this.catsRewardGiven = false;
        this.anchorIntroShown = false;
        this.anchorDone = false;
        this.finaleShown = false;
        this.spireMarkerAuths.clear();
        this.stairsMarkerAuths.clear();
        this.catMarkerAuths.clear();
        this.returnStairs.clear();
    }

    /** Remembers which surface stairway tile this player ascended from. */
    public void setReturnStairway(long clientAuth, int tileX, int tileY) {
        this.returnStairs.put(clientAuth, new long[]{tileX, tileY});
    }

    /**
     * The surface tile this player's Skywatch Gate should open onto: their own
     * last-used stairway if known, otherwise any recorded stairway (the first
     * ascent of a second player should lead somewhere sensible), otherwise
     * null — the gate then refuses politely instead of guessing.
     */
    public long[] getReturnStairway(long clientAuth) {
        long[] own = this.returnStairs.get(clientAuth);
        if (own != null) {
            return own;
        }
        for (long[] tile : this.returnStairs.values()) {
            return tile;
        }
        return null;
    }

    /** Fetches the quest data of a level, creating and attaching it if absent. */
    public static SkywatchQuestData get(Level level) {
        LevelData data = level.getLevelData(KEY);
        SkywatchQuestData quest;
        if (data instanceof SkywatchQuestData) {
            quest = (SkywatchQuestData) data;
        } else {
            quest = new SkywatchQuestData();
            level.addLevelData(KEY, quest);
        }
        reconcileWithWorld(level, quest);
        return quest;
    }

    /**
     * Fold the world-scoped copy of the cat flags back in.
     *
     * These two booleans are progression, not geometry, so
     * {@link SkywatchWorldData} keeps its own copy and it is the one that
     * cannot be lost to a level unload. If the world record says a cat was
     * coaxed home and this level record disagrees, the level record is the one
     * that is stale -- a coax can never be undone -- so it is repaired here,
     * on the read path, where every caller benefits without knowing about it.
     *
     * Server-side only; {@code level.getServer()} is null on a client, and the
     * client's copy arrives by packet anyway.
     */
    private static void reconcileWithWorld(Level level, SkywatchQuestData quest) {
        if (level == null || !level.isServer()) {
            return;
        }
        SkywatchWorldData world = SkywatchWorldData.get(level.getServer());
        if (world == null) {
            return;
        }
        if (world.blackHome) {
            quest.blackHome = true;
        }
        if (world.tabbyHome) {
            quest.tabbyHome = true;
        }
        // ...and the other way, so a world that upgraded from a build without
        // the world record picks it up from the level's own history.
        if (quest.blackHome) {
            world.blackHome = true;
        }
        if (quest.tabbyHome) {
            world.tabbyHome = true;
        }
    }
}
