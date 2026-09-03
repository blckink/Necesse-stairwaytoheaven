package stairwaytoheaven.quest;

import java.util.ArrayList;
import java.util.List;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.mobs.SpireCatMob;

/**
 * Where Siggi and Peanut live, and everything that has to happen when that
 * changes.
 *
 * <p>The player report this exists for: <em>"Katzenbetten sollen in normalem
 * Haus platziert werden koennen etc in der Stadt damit die Katzen dort wohnen.
 * ich habe beide gerade platziert und die sind weg oder irgendwo anders dann
 * erschienen wo ich es nicht weiss"</em>. Two Cat Baskets went down in a town
 * on the Surface and nothing happened, because the basket was a plain
 * {@code FurnitureObject} with no connection to the cats at all, and their home
 * was hard-wired to {@code SkywatchQuestData.basketX/basketY} -- the tile inside
 * the Warden's Spire, in the Skyreach. The cats were never lost; they were in
 * another dimension, and nothing said so.
 *
 * <p>Now a placed basket IS the home, wherever it stands, and the cats move to
 * it. Two rules keep that comprehensible:
 * <ul>
 *   <li><b>The newest basket wins.</b> Placing a second one anywhere moves them
 *       to it, and the chat line says so.</li>
 *   <li><b>Only coaxed cats move.</b> A cat still wild in its lair stays wild --
 *       the Cloudpuff Treat is the quest step, and a basket must not skip it.
 *       {@link SkywatchWorldData#blackHome}/{@code tabbyHome} is that record.</li>
 * </ul>
 *
 * <p>Everything here is server-side. The record lives in
 * {@link SkywatchWorldData} (a {@code WorldData}) rather than in
 * {@link SkywatchQuestData} (a {@code LevelData} on the Skyreach) because a home
 * on the Surface is not a fact about the Skyreach and must survive that level
 * being unloaded or regenerated.
 */
public final class CatHome {

    private CatHome() {
    }

    /** A place a cat can be tethered to: one tile on one level. */
    public static final class Spot {
        public final LevelIdentifier level;
        public final int tileX;
        public final int tileY;
        /** True when this is a basket the player put down, false for the spire/lair. */
        public final boolean playerPlaced;

        Spot(LevelIdentifier level, int tileX, int tileY, boolean playerPlaced) {
            this.level = level;
            this.tileX = tileX;
            this.tileY = tileY;
            this.playerPlaced = playerPlaced;
        }

        /** Is this spot on the level given? */
        public boolean isOn(Level level) {
            return level != null && level.getIdentifier() != null
                    && level.getIdentifier().equals(this.level);
        }

        @Override
        public String toString() {
            return this.level + ":" + this.tileX + "," + this.tileY;
        }
    }

    // ------------------------------------------------------------------
    // reading the record

    /** The player-placed home, or null when no basket has been placed. */
    public static Spot placed(Server server) {
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world == null || !world.catHomeSet
                || world.catHomeLevel == null || world.catHomeLevel.isEmpty()) {
            return null;
        }
        try {
            return new Spot(new LevelIdentifier(world.catHomeLevel),
                    world.catHomeX, world.catHomeY, true);
        } catch (RuntimeException e) {
            // LevelIdentifier's constructor throws on anything that does not
            // match its pattern. A record we cannot turn back into a level is
            // worse than no record: it would make every read throw.
            world.catHomeSet = false;
            world.catHomeLevel = "";
            return null;
        }
    }

    /**
     * Where this cat's tether belongs right now.
     *
     * <p>Order: the player-placed basket if there is one and this cat has been
     * coaxed home; otherwise the spire basket; otherwise the lair it was born
     * in. Returns null when nothing is known yet -- the caller then leaves the
     * cat where it is rather than teleporting it to (0,0), which is what the
     * old {@code spirePlaced} guard was for.
     *
     * @param catLevel      the level the cat is standing on
     * @param mayLoadSkyreach whether it is safe here to pull the Skyreach into
     *        memory to read its quest data. False on the tether-rebuild path,
     *        which runs inside mob load; true for a deliberate action such as
     *        {@code sendHome}.
     */
    public static Spot resolve(Server server, Level catLevel, boolean isBlackCat,
                               boolean mayLoadSkyreach) {
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world == null) {
            return null;
        }
        boolean coaxed = world.isCatCoaxed(isBlackCat);
        Spot placed = placed(server);
        if (coaxed && placed != null) {
            return placed;
        }
        SkywatchQuestData quest = skyQuest(server, catLevel, mayLoadSkyreach);
        if (quest == null) {
            return null;
        }
        if (coaxed) {
            // Fall back to the spire basket. Without a stamped spire there is
            // no basket tile at all and (0,0) is not a home.
            return quest.spirePlaced
                    ? new Spot(SkyRegistry.SKYREACH_IDENTIFIER, quest.basketX, quest.basketY, false)
                    : null;
        }
        if (!quest.catsSpawned) {
            return null;
        }
        return new Spot(SkyRegistry.SKYREACH_IDENTIFIER,
                isBlackCat ? quest.blackLairX : quest.tabbyLairX,
                isBlackCat ? quest.blackLairY : quest.tabbyLairY, false);
    }

    /**
     * The Skyreach's quest data, without ever attaching an empty copy to a
     * level it does not describe.
     *
     * <p>{@code SkywatchQuestData.get(level)} CREATES the level data when it is
     * missing, so calling it on the Surface would silently mint a record with
     * basket and lair at 0,0 -- and a cat tethered to 0,0 is the same class of
     * bug as a cat teleported into the void.
     */
    private static SkywatchQuestData skyQuest(Server server, Level catLevel, boolean mayLoad) {
        if (catLevel != null && catLevel.isServer()
                && SkyRegistry.SKYREACH_IDENTIFIER.equals(catLevel.getIdentifier())) {
            return SkywatchQuestData.get(catLevel);
        }
        if (!mayLoad || server == null || server.world == null) {
            return null;
        }
        // World.getLevel, not levelManager.isLoaded: "is it in memory" is never
        // "does it exist" (docs/TECHNICAL_LEARNINGS.md), and a cross-dimension
        // read on a deliberate action must not give up because the sky happens
        // to be asleep.
        Level sky = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        return sky == null ? null : SkywatchQuestData.get(sky);
    }

    // ------------------------------------------------------------------
    // changing the record

    /**
     * A Cat Basket was placed at this tile: it becomes the cats' home and every
     * coaxed cat moves in. Server-side; safe to call from any placement path.
     */
    public static void claim(Level level, int tileX, int tileY) {
        if (level == null || !level.isServer()) {
            return;
        }
        Server server = level.getServer();
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world == null || level.getIdentifier() == null) {
            return;
        }
        Spot previous = placed(server);
        boolean hadOther = previous != null
                && !(previous.isOn(level) && previous.tileX == tileX && previous.tileY == tileY);
        world.setCatHome(level.getIdentifier().stringID, tileX, tileY);

        int moved = sendCoaxedCatsHome(server, previous);
        announce(server, level, tileX, tileY, moved, hadOther, world.anyCatCoaxed());
    }

    /**
     * A Cat Basket was removed. If it was the ACTIVE home the record is cleared
     * and the coaxed cats travel back to the spire basket -- a cat left standing
     * on the Surface tethered to furniture that no longer exists is how "the
     * cats are gone again" would come back.
     */
    public static void release(Level level, int tileX, int tileY) {
        if (level == null || !level.isServer()) {
            return;
        }
        Server server = level.getServer();
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world == null || level.getIdentifier() == null) {
            return;
        }
        if (!world.clearCatHome(level.getIdentifier().stringID, tileX, tileY)) {
            return;  // some other basket; the cats keep the one they live in
        }
        int moved = sendCoaxedCatsHome(server, new Spot(level.getIdentifier(), tileX, tileY, true));
        if (moved > 0) {
            broadcast(server, level, tileX, tileY, new LocalMessage("misc", "catbasketremoved"));
        }
    }

    // ------------------------------------------------------------------
    // moving the cats

    /**
     * Sends every coaxed cat to wherever the record now says home is.
     *
     * <p>Only cats that are actually IN MEMORY can be moved, so the regions the
     * cats are anchored to are forced in first -- the spire basket, both lairs
     * and the basket they are leaving. Without that this measures which regions
     * happen to be streamed in, which is the same mistake the NPC census made
     * once and reported a missing cat for.
     *
     * <p>KNOWN LIMIT: a coaxed cat sitting in some OTHER unloaded region would
     * be missed, and would keep its old tether until something moves it again.
     * That is not reachable today -- a coaxed cat is teleported onto its home
     * tile and {@code HomesickCritterAI} pulls it back past three tiles, so it
     * never leaves that tile's region -- and the fix for it would be a
     * per-tick self-heal in {@code SpireCatMob.serverTick}, which would call
     * {@code changeMobLevel} while the old level's entity lock is held and take
     * the new level's on top of it. Not worth a lock-order deadlock for a case
     * the AI already prevents.
     *
     * @param leaving the home they are moving away from, or null
     * @return how many cats were actually sent
     */
    private static int sendCoaxedCatsHome(Server server, Spot leaving) {
        SkywatchWorldData world = SkywatchWorldData.get(server);
        if (world == null || !world.anyCatCoaxed()) {
            // Nobody has been coaxed home yet, so nothing may move -- and the
            // Skyreach must not be generated just because a basket went down.
            return 0;
        }
        int sent = 0;
        for (Level level : catLevels(server, leaving)) {
            List<SpireCatMob> cats = new ArrayList<>();
            for (Mob mob : level.entityManager.mobs) {
                if (mob instanceof SpireCatMob && ((SpireCatMob) mob).isCoaxedHome()) {
                    cats.add((SpireCatMob) mob);
                }
            }
            // Collected first: sendHome can move a cat off this level, and
            // mutating the list being iterated is not worth the risk.
            for (SpireCatMob cat : cats) {
                cat.sendHome(level);
                sent++;
            }
        }
        return sent;
    }

    /** The levels a coaxed cat could be standing on, loaded and ready to scan. */
    private static List<Level> catLevels(Server server, Spot leaving) {
        List<Level> levels = new ArrayList<>();
        Level sky = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
        if (sky != null) {
            SkywatchQuestData quest = SkywatchQuestData.get(sky);
            if (quest.spirePlaced) {
                sky.regionManager.ensureTileIsLoaded(quest.basketX, quest.basketY);
            }
            if (quest.catsSpawned) {
                sky.regionManager.ensureTileIsLoaded(quest.blackLairX, quest.blackLairY);
                sky.regionManager.ensureTileIsLoaded(quest.tabbyLairX, quest.tabbyLairY);
            }
            levels.add(sky);
        }
        if (leaving != null) {
            Level old = server.world.getLevel(leaving.level);
            if (old != null) {
                old.regionManager.ensureTileIsLoaded(leaving.tileX, leaving.tileY);
                if (!containsLevel(levels, old)) {
                    levels.add(old);
                }
            }
        }
        return levels;
    }

    private static boolean containsLevel(List<Level> levels, Level level) {
        for (Level existing : levels) {
            if (existing == level || existing.getIdentifier().equals(level.getIdentifier())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // telling the player

    /**
     * Says what just happened, in chat, to everyone on the level the basket was
     * placed on.
     *
     * <p>This IS the bug report. "die sind weg oder irgendwo anders dann
     * erschienen wo ich es nicht weiss" is a player left guessing by silence,
     * so placing a basket now always answers: they moved in, they moved here
     * from the old one, or they are still out there and need a treat first.
     */
    private static void announce(Server server, Level level, int tileX, int tileY,
                                 int moved, boolean replacedAnother, boolean anyCoaxed) {
        GameMessage message;
        if (!anyCoaxed) {
            message = new LocalMessage("misc", "catbasketready");
        } else if (replacedAnother) {
            message = new LocalMessage("misc", "catbasketmoved",
                    "count", String.valueOf(moved),
                    "x", String.valueOf(tileX), "y", String.valueOf(tileY));
        } else {
            message = new LocalMessage("misc", "catbasketmovedin",
                    "count", String.valueOf(moved),
                    "x", String.valueOf(tileX), "y", String.valueOf(tileY));
        }
        broadcast(server, level, tileX, tileY, message);
    }

    /**
     * Over the basket, for everyone who can see it.
     *
     * <p>It used to be a level-wide {@code PacketChatMessage}. The chat log is
     * gone (the player: <i>"und keine chat nachrichten! generell.. die sind
     * total kacke lesbar"</i>), and a basket announcing itself to a player two
     * thousand tiles away never earned its place anyway — whoever put it down
     * is standing on it. The message is still sent unresolved, so each player
     * reads it in their own language.
     */
    private static void broadcast(Server server, Level level, int tileX, int tileY, GameMessage message) {
        if (level == null || level.getIdentifier() == null) {
            return;
        }
        stairwaytoheaven.util.TileText.atAll(server, level, tileX, tileY, message);
    }
}
