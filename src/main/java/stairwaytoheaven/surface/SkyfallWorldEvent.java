package stairwaytoheaven.surface;

import java.awt.Point;
import java.util.ArrayList;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.engine.world.worldEvent.WorldEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;

/**
 * <b>Skyfall</b> — the mod's recurring Surface world event: for a couple of
 * minutes on a rare night, pieces of the Skyreach come down around whoever is
 * outside, and can be broken for sky materials.
 *
 * <h2>Why a {@code WorldEvent}</h2>
 * {@code necesse/engine/world/worldEvent/} is the game's world-scoped event
 * system: {@code WorldEntity.serverTick} ticks every live {@link WorldEvent}
 * and drops it the moment {@code isOver()} answers true,
 * {@code WorldEntity.addWorldEvent} announces it to every client with a
 * {@code PacketWorldEvent}, and {@code shouldSave} puts it in the world file
 * through {@code WorldEventSave}. Vanilla registers exactly one
 * ({@code "ascendedflash"}); this is registered the same way, through
 * {@code WorldEventRegistry.registerEvent(stringID, class)}, which requires a
 * public no-argument constructor because the registry instantiates it
 * reflectively for both the packet and the save.
 *
 * <h2>Server-authoritative</h2>
 * Everything that changes the world happens in {@link #serverTick()}: the
 * chosen tiles, the shards, the chat announcements and the cleanup. The client
 * copy of the event exists only so the countdown ends on both sides; it never
 * places or removes anything. Shards are written with
 * {@code Level.sendObjectChangePacket}, which sets the object on the server and
 * tells the clients that can see the tile.
 *
 * <h2>Time-limited and self-cleaning</h2>
 * The event carries its own remaining time and the list of tiles it wrote. When
 * the time runs out — or when the world is loaded again with the event still
 * running, because both the timer and the list are saved — every tile that
 * still holds one of our shards is cleared. A shard the player already broke,
 * or a tile the player has since built on, is left alone: the cleanup only
 * removes an object it can still identify as its own.
 */
public class SkyfallWorldEvent extends WorldEvent {

    /** Registry string ID. */
    public static final String STRING_ID = "swhskyfall";

    /** How long one shower lasts, in world milliseconds. */
    public static final int DURATION_MS = 120_000;
    /** How often the server tries to drop a shard near each surface player. */
    private static final int DROP_INTERVAL_MS = 3_000;
    /** Hard cap on live shards, so a long night cannot carpet the surface. */
    public static final int MAX_SHARDS = 48;
    /** Shards land in this ring around a player: close enough to see, not on. */
    private static final int MIN_RADIUS = 7;
    private static final int MAX_RADIUS = 18;
    /** Attempts per drop before giving up on a player this interval. */
    private static final int PLACE_ATTEMPTS = 12;

    private final GameRandom random = new GameRandom();

    /** Milliseconds left. Saved, and sent to clients in the spawn packet. */
    protected int remainingMs = DURATION_MS;
    /** {@code getTime()} at which this event is over. Derived in {@link #init}. */
    protected long endTime;
    /** {@code getTime()} of the next drop attempt. */
    protected long nextDropTime;
    /** True once the start has been announced; saved, so a reload is quiet. */
    protected boolean announced;
    /** Surface tiles this event wrote a shard onto and has not cleared yet. */
    protected final ArrayList<Point> shards = new ArrayList<>();
    /** Total shards placed over the event's life, for the probe. */
    protected int totalPlaced;
    /** Whether this event has introduced itself to the schedule. Not saved. */
    private boolean adopted;

    /** Required by {@code WorldEventRegistry}, which instantiates reflectively. */
    public SkyfallWorldEvent() {
        this.shouldSave = true;
    }

    public SkyfallWorldEvent(int durationMs) {
        this();
        this.remainingMs = durationMs;
    }

    // ------------------------------------------------------------------ save

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addInt("remainingMs", this.remainingMs);
        save.addBoolean("announced", this.announced);
        save.addInt("totalPlaced", this.totalPlaced);
        int[] xs;
        int[] ys;
        synchronized (this.shards) {
            xs = new int[this.shards.size()];
            ys = new int[this.shards.size()];
            for (int i = 0; i < this.shards.size(); i++) {
                xs[i] = this.shards.get(i).x;
                ys[i] = this.shards.get(i).y;
            }
        }
        save.addIntArray("shardX", xs);
        save.addIntArray("shardY", ys);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.remainingMs = save.getInt("remainingMs", this.remainingMs, false);
        this.announced = save.getBoolean("announced", this.announced, false);
        this.totalPlaced = save.getInt("totalPlaced", this.totalPlaced, false);
        int[] xs = save.getIntArray("shardX", new int[0], false);
        int[] ys = save.getIntArray("shardY", new int[0], false);
        synchronized (this.shards) {
            this.shards.clear();
            for (int i = 0; i < Math.min(xs.length, ys.length); i++) {
                this.shards.add(new Point(xs[i], ys[i]));
            }
        }
    }

    // --------------------------------------------------------------- network

    @Override
    public void setupSpawnPacket(PacketWriter writer) {
        super.setupSpawnPacket(writer);
        writer.putNextInt(this.remainingMs);
    }

    @Override
    public void applySpawnPacket(PacketReader reader) {
        super.applySpawnPacket(reader);
        this.remainingMs = reader.getNextInt();
    }

    // ------------------------------------------------------------------ tick

    /**
     * Called by {@code WorldEntity.addWorldEventHidden} after the world is set
     * and after {@code applyLoadData} / {@code applySpawnPacket}, on both sides.
     */
    @Override
    public void init() {
        super.init();
        this.endTime = this.getTime() + Math.max(0, this.remainingMs);
        this.nextDropTime = this.getTime();
    }

    @Override
    public void clientTick() {
        super.clientTick();
        // The client copy exists only to expire on its own; it touches nothing.
        this.remainingMs = (int) Math.max(0L, this.endTime - this.getTime());
        if (this.remainingMs <= 0) {
            this.over();
        }
    }

    @Override
    public void serverTick() {
        super.serverTick();
        Server server = this.getServer();
        if (server == null) {
            this.over();
            return;
        }
        if (!this.adopted) {
            // A shower that came back out of the save file has to introduce
            // itself: WorldEntity.applyLoadData restores EVENTS *before*
            // WORLDDATA, so the schedule that will be loaded a moment later
            // cannot possibly be holding a reference to this event yet. By the
            // first server tick everything is up, so this is the earliest
            // correct place to do it.
            this.adopted = true;
            SkyfallWorldData data = SkyfallWorldData.get(server);
            if (data != null) {
                data.adopt(this);
            }
        }
        if (!this.announced) {
            this.announced = true;
            this.announce(new LocalMessage("misc", "swhskyfallstart"));
        }

        this.remainingMs = (int) Math.max(0L, this.endTime - this.getTime());
        if (this.remainingMs <= 0) {
            this.finish(server);
            return;
        }

        if (this.getTime() < this.nextDropTime) {
            return;
        }
        this.nextDropTime = this.getTime() + DROP_INTERVAL_MS;
        if (this.liveShards() >= MAX_SHARDS) {
            return;
        }
        for (ServerClient client : server.getClients()) {
            if (this.liveShards() >= MAX_SHARDS) {
                break;
            }
            if (client == null || !client.isSamePlace(LevelIdentifier.SURFACE_IDENTIFIER)) {
                continue;
            }
            Level level = client.getLevel();
            PlayerMob player = client.playerMob;
            if (level == null || player == null || !level.isServer()) {
                continue;
            }
            this.dropShardNear(server, level, player.getTileX(), player.getTileY());
        }
    }

    // ----------------------------------------------------------------- shards

    /**
     * Drops up to {@code count} shards around a fixed tile, through exactly the
     * same placement path a player standing there would trigger. Used by
     * {@code /skysurfacestatus event}: a headless server has no clients to
     * follow, so without this the whole placement half of the event would be
     * unobservable in the integration test.
     *
     * @return how many shards were actually placed
     */
    public int seedShards(Server server, Level level, int centreX, int centreY, int count) {
        int before = this.totalPlaced;
        for (int i = 0; i < count && this.liveShards() < MAX_SHARDS; i++) {
            this.dropShardNear(server, level, centreX, centreY);
        }
        return this.totalPlaced - before;
    }

    /** One attempt at a shard in the ring around a player. */
    private void dropShardNear(Server server, Level level, int centreX, int centreY) {
        for (int attempt = 0; attempt < PLACE_ATTEMPTS; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            int radius = this.random.getIntBetween(MIN_RADIUS, MAX_RADIUS);
            int x = centreX + (int) Math.round(Math.cos(angle) * radius);
            int y = centreY + (int) Math.round(Math.sin(angle) * radius);
            if (!this.isValidShardTile(level, x, y)) {
                continue;
            }
            level.sendObjectChangePacket(server, x, y, SkySurface.skyfallShardID, 0);
            synchronized (this.shards) {
                this.shards.add(new Point(x, y));
            }
            this.totalPlaced++;
            return;
        }
    }

    /**
     * A shard may only land on loaded, empty, natural, dry ground outside any
     * settlement.
     *
     * <p>Four separate guards, and each one is there for a reason:
     * <ul>
     *   <li>a tile that already carries an object is skipped, so nothing a
     *       player built can ever be replaced;</li>
     *   <li>the tile layer must be clear too, so shards do not land on a
     *       carpet or a road;</li>
     *   <li>{@code GameTile.isFloor} is vanilla's own line between terrain and
     *       a floor somebody laid ({@code GameTile(boolean isFloor)}), so a
     *       shard cannot land inside a house that has no settlement flag;</li>
     *   <li>and {@code SettlementsWorldData.hasSettlementAtTile} keeps them out
     *       of a claimed settlement altogether.</li>
     * </ul>
     */
    private boolean isValidShardTile(Level level, int x, int y) {
        if (!level.isTileWithinBounds(x, y) || !level.regionManager.isTileLoaded(x, y)) {
            return false;
        }
        if (level.getObjectID(x, y) != 0) {
            return false;
        }
        if (level.getObjectID(ObjectLayerRegistry.TILE_LAYER, x, y) != 0) {
            return false;
        }
        GameTile tile = level.getTile(x, y);
        if (tile == null || tile.isLiquid || tile.isFloor) {
            return false;
        }
        return !SettlementsWorldData.getSettlementsData(level).hasSettlementAtTile(level, x, y);
    }

    // ------------------------------------------------------------------- end

    /** Clears every shard still standing, announces the end and expires. */
    public void finish(Server server) {
        int removed = this.clearShards(server);
        this.announce(new LocalMessage("misc", "swhskyfallend"));
        this.over();
        if (server != null) {
            System.out.println("[stairwaytoheaven] Skyfall over: placed=" + this.totalPlaced + " cleared=" + removed);
        }
    }

    /**
     * Removes shards this event placed. A tile whose object is no longer our
     * shard was mined by a player or built over, and is left alone.
     * {@code ensureTileIsLoaded} is what makes this exhaustive: a shard whose
     * region has since streamed out is still cleaned up rather than left in the
     * world forever.
     */
    public int clearShards(Server server) {
        ArrayList<Point> toClear;
        synchronized (this.shards) {
            toClear = new ArrayList<>(this.shards);
            this.shards.clear();
        }
        int removed = 0;
        if (server != null && server.world != null) {
            Level level = server.world.levelManager.isLoaded(LevelIdentifier.SURFACE_IDENTIFIER)
                    ? server.world.getLevel(LevelIdentifier.SURFACE_IDENTIFIER)
                    : null;
            if (level != null) {
                for (Point p : toClear) {
                    level.regionManager.ensureTileIsLoaded(p.x, p.y);
                    if (level.getObjectID(p.x, p.y) == SkySurface.skyfallShardID) {
                        level.sendObjectChangePacket(server, p.x, p.y, 0, 0);
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    /**
     * Announces to everyone on the Surface, the way vanilla announces its own
     * events: a server-side {@code PacketChatMessage} carrying a
     * {@link LocalMessage}, sent to the clients at that level, so each player
     * reads it in their own language.
     * {@code IncursionLevelEvent:522} is exactly this call, and
     * {@code ApproachSettlementRaidStage} does the per-client equivalent.
     */
    private void announce(GameMessage message) {
        Server server = this.getServer();
        if (server == null) {
            return;
        }
        server.network.sendToClientsAtEntireLevel(new PacketChatMessage(message), LevelIdentifier.SURFACE_IDENTIFIER);
    }

    /** Live shard count, for the probe. */
    public int liveShards() {
        synchronized (this.shards) {
            return this.shards.size();
        }
    }

    /** Shards placed over this event's whole life, for the probe. */
    public int totalPlaced() {
        return this.totalPlaced;
    }

    /** Milliseconds left, for the probe. */
    public int remainingMs() {
        return this.remainingMs;
    }
}
