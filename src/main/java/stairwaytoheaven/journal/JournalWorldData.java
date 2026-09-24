package stairwaytoheaven.journal;

import java.util.HashSet;
import java.util.Set;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldData.WorldData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;
import stairwaytoheaven.level.SkyLevel;
import stairwaytoheaven.worldgen.RealmDepth;

/**
 * What the journal knows that no other record in the mod keeps: which players
 * already got their journal, and what each player has SEEN.
 *
 * <p>Every other fact the journal shows is read from the records the quests
 * already write ({@code SkywatchQuestData}, {@code SkywatchWorldData},
 * {@code VeilWorldData}, the player's own quests and kill stats). Two things
 * are nobody's: "has this character met Ives yet" and "has this character been
 * to Steinfeld yet" — worldgen places people and realms whether or not anyone
 * ever walks past them. So this record watches, once every two seconds, which
 * of the mod's named people stand within {@link #SIGHT_TILES} of each player,
 * and which realm band each player stands in, and writes it down.
 *
 * <p>Per player (by authentication), like {@code VeilWorldData}'s Mark: two
 * people on one world explore separately. One flag is per world: which named
 * residents have been seen living in a settlement ({@link #settledSeen}),
 * because a settler is shared property.
 *
 * <p>Stored as flat {@code "auth:key"} strings rather than a nested save
 * structure, the same "set of names survives renumbering" reasoning
 * {@code SkywatchWorldData.bossPortalsUnlocked} gives.
 */
public class JournalWorldData extends WorldData {

    /** Must match {@code [a-zA-Z0-9]+} — {@code WorldEntity.addWorldData} enforces it. */
    public static final String KEY = "swhjournal";

    /** How far a player "sees" a named person, in tiles. About one screen. */
    public static final int SIGHT_TILES = 14;

    private static final long CHECK_INTERVAL_MS = 2000L;

    /** Named mobs worth remembering having met. Anything else is ignored. */
    public static final Set<String> WATCHED = new HashSet<>(java.util.Arrays.asList(
            "skywarden", "wardensettler", "magpiesettler", "haldasettler", "ossiansettler",
            "spirecatblack", "spirecattabby", "eveleensettler", "ivessettler",
            "mortimersettler", "caspernsettler", "eleanorsettler", "ghostguide", "knottsettler",
            "vampiresettler", "tollwright", "sourvatbloom", "vatling", "prototypenine"));

    private final Set<Long> givenAuths = new HashSet<>();
    /** {@code auth + ":" + key}; keys are {@code mob:<id>} and {@code realm:<key>}. */
    private final Set<String> seen = new HashSet<>();
    /** Mob IDs of named residents that have been seen working as settlers. */
    private final Set<String> settledSeen = new HashSet<>();

    private long nextCheckTime;

    // ------------------------------------------------------------------
    // persistence

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        long[] given = new long[this.givenAuths.size()];
        int i = 0;
        for (long auth : this.givenAuths) {
            given[i++] = auth;
        }
        save.addLongArray("givenAuths", given);
        save.addStringArray("seen", this.seen.toArray(new String[0]));
        save.addStringArray("settledSeen", this.settledSeen.toArray(new String[0]));
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.givenAuths.clear();
        if (save.hasLoadDataByName("givenAuths")) {
            for (long auth : save.getLongArray("givenAuths")) {
                this.givenAuths.add(auth);
            }
        }
        this.seen.clear();
        for (String entry : save.getStringArray("seen", new String[0], false)) {
            if (entry != null && !entry.isEmpty()) {
                this.seen.add(entry);
            }
        }
        this.settledSeen.clear();
        for (String entry : save.getStringArray("settledSeen", new String[0], false)) {
            if (entry != null && !entry.isEmpty()) {
                this.settledSeen.add(entry);
            }
        }
    }

    // ------------------------------------------------------------------
    // reads

    public boolean wasGiven(long auth) {
        return this.givenAuths.contains(auth);
    }

    public void markGiven(long auth) {
        this.givenAuths.add(auth);
    }

    public int givenCount() {
        return this.givenAuths.size();
    }

    public boolean hasSeenMob(long auth, String mobStringID) {
        return this.seen.contains(auth + ":mob:" + mobStringID);
    }

    public boolean hasVisited(long auth, int realm) {
        return this.seen.contains(auth + ":realm:" + RealmDepth.keyOf(realm));
    }

    public boolean seenAsSettler(String mobStringID) {
        return this.settledSeen.contains(mobStringID);
    }

    public int seenCount() {
        return this.seen.size();
    }

    /** For {@code /swhjournal}: record a sighting by hand, as the tick would. */
    public void recordSeen(long auth, String key) {
        this.seen.add(auth + ":" + key);
    }

    /**
     * Forgets everything this record holds except who already has a journal.
     * Called by nothing in play; exists so a test (or a future
     * {@code /swhreset} hook) can put a world back to "met nobody".
     */
    public void resetSightings() {
        this.seen.clear();
        this.settledSeen.clear();
    }

    // ------------------------------------------------------------------
    // the watcher

    @Override
    public void tick() {
        super.tick();
        if (!this.isServer()) {
            return;
        }
        WorldEntity world = this.getWorldEntity();
        Server server = this.getServer();
        if (world == null || server == null) {
            return;
        }
        long now = world.getTime();
        if (now < this.nextCheckTime) {
            return;
        }
        this.nextCheckTime = now + CHECK_INTERVAL_MS;
        for (ServerClient client : server.getClients()) {
            try {
                this.tickClient(server, client);
            } catch (RuntimeException e) {
                // A journal must never be the reason a server tick dies.
                System.err.println("[swh journal] watcher skipped " + e);
            }
        }
    }

    private void tickClient(Server server, ServerClient client) {
        if (client == null || !client.hasSpawned() || client.isDead()) {
            return;
        }
        PlayerMob player = client.playerMob;
        if (player == null || player.removed()) {
            return;
        }
        Level level = player.getLevel();
        if (level == null || !level.isServer()) {
            return;
        }
        long auth = client.authentication;
        boolean onSky = level instanceof SkyLevel;
        if (onSky) {
            int realm = ((SkyLevel) level).realmAt(player.getTileX(), player.getTileY());
            this.seen.add(auth + ":realm:" + RealmDepth.keyOf(realm));
        }
        int px = player.getTileX();
        int py = player.getTileY();
        for (Mob mob : level.entityManager.mobs.getInRegionByTileRange(px, py, SIGHT_TILES)) {
            if (mob == null || mob.removed()) {
                continue;
            }
            String id = mob.getStringID();
            if (!WATCHED.contains(id)) {
                continue;
            }
            int dx = mob.getTileX() - px;
            int dy = mob.getTileY() - py;
            if (dx * dx + dy * dy > SIGHT_TILES * SIGHT_TILES) {
                continue;
            }
            this.seen.add(auth + ":mob:" + id);
            if (mob instanceof HumanMob && ((HumanMob) mob).isSettler()) {
                this.settledSeen.add(id);
            }
        }
        AdventurerJournal.maybeGiveJournal(server, client, this, onSky);
    }

    // ------------------------------------------------------------------
    // access

    /** The world record, created on first use — the {@code VeilWorldData.get} shape. */
    public static JournalWorldData get(Server server) {
        if (server == null || server.world == null) {
            return null;
        }
        WorldEntity worldEntity = server.world.worldEntity;
        if (worldEntity == null) {
            return null;
        }
        WorldData data = worldEntity.getWorldData(KEY);
        if (data instanceof JournalWorldData) {
            return (JournalWorldData) data;
        }
        JournalWorldData created = new JournalWorldData();
        worldEntity.addWorldData(KEY, created);
        return created;
    }
}
