package stairwaytoheaven.surface;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldData.WorldData;

/**
 * The clock behind {@link SkyfallWorldEvent}: decides which nights get a
 * Skyfall, and starts one.
 *
 * <h2>Why a {@code WorldData}</h2>
 * {@code WorldEntity.serverTick} ticks every {@link WorldData} the world holds,
 * every server tick, right beside the world events themselves — which makes it
 * the one place in the engine where a mod can hold a world-scoped recurring
 * schedule that also persists. The record is written into the world file
 * ({@code WorldEntity.addSaveData} → {@code WORLDDATA}), so the cadence
 * survives a restart instead of re-rolling from scratch every boot. The mod
 * already uses the same mechanism for {@code quest/SkywatchWorldData}.
 *
 * <h2>Server-authoritative</h2>
 * {@code WorldData.tick()} runs on the client too, so everything below is
 * behind an {@code isServer()} guard. Clients learn about a Skyfall only from
 * the {@code PacketWorldEvent} that {@code addWorldEvent} sends and from the
 * chat announcement — they never decide anything.
 */
public class SkyfallWorldData extends WorldData {

    /** Must match {@code [a-zA-Z0-9]+} — {@code WorldEntity.addWorldData} enforces it. */
    public static final String KEY = "swhskyfall";

    /** Nights between showers, drawn per event. Rare, and never twice running. */
    public static final int MIN_DAYS_BETWEEN = 4;
    public static final int MAX_DAYS_BETWEEN = 9;

    /** Don't evaluate the schedule on every single tick. */
    private static final int CHECK_INTERVAL_MS = 2_000;

    private final GameRandom random = new GameRandom();

    /** The world day on or after which the next Skyfall may happen. −1 = unset. */
    protected int nextDay = -1;
    /** The world day the last Skyfall started on. −1 = never. */
    protected int lastDay = -1;
    /**
     * World {@code getTime()} until which a Skyfall counts as still running.
     * Saved, so a restart in the middle of a shower cannot start a second one.
     */
    protected long busyUntil = 0L;

    private long nextCheckTime;

    /** The event this world data started, while it is still alive. Never saved. */
    private transient SkyfallWorldEvent active;

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addInt("nextDay", this.nextDay);
        save.addInt("lastDay", this.lastDay);
        save.addLong("busyUntil", this.busyUntil);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.nextDay = save.getInt("nextDay", this.nextDay, false);
        this.lastDay = save.getInt("lastDay", this.lastDay, false);
        this.busyUntil = save.getLong("busyUntil", this.busyUntil, false);
    }

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

        if (this.active != null && this.active.isOver()) {
            this.active = null;
        }
        if (this.nextDay < 0) {
            // First boot of a world: the first shower is a few nights out, so a
            // brand new save is not greeted by one on night one.
            this.nextDay = world.getDay() + this.random.getIntBetween(MIN_DAYS_BETWEEN, MAX_DAYS_BETWEEN);
            return;
        }
        if (this.active != null || now < this.busyUntil) {
            return;                                  // one shower at a time
        }
        if (world.getDay() < this.nextDay || world.getDay() == this.lastDay || !world.isNight()) {
            return;
        }
        if (!anyPlayerOnSurface(server)) {
            // A shower nobody can see is a shower wasted; wait for the next
            // night that somebody actually spends outside.
            return;
        }
        this.start(world, SkyfallWorldEvent.DURATION_MS);
    }

    /**
     * Starts a shower now. Also used by the debug command, which is how the
     * integration test observes the whole event without waiting for a night.
     */
    public SkyfallWorldEvent start(WorldEntity world, int durationMs) {
        SkyfallWorldEvent event = new SkyfallWorldEvent(durationMs);
        this.lastDay = world.getDay();
        this.nextDay = this.lastDay + this.random.getIntBetween(MIN_DAYS_BETWEEN, MAX_DAYS_BETWEEN);
        this.busyUntil = world.getTime() + durationMs + 5_000L;
        this.active = event;
        // addWorldEvent, not addWorldEventHidden: it runs init() AND sends the
        // PacketWorldEvent, so every connected client gets its own copy of the
        // event and expires it on the same clock.
        world.addWorldEvent(event);
        return event;
    }

    /**
     * Takes over a shower that came back out of the save file. Called from
     * {@link SkyfallWorldEvent#serverTick()} on its first tick, because the
     * world file restores its events before its world data — so on a load the
     * event exists before this record does, and only the event can make the
     * introduction.
     */
    void adopt(SkyfallWorldEvent event) {
        if (this.active == null || this.active.isOver()) {
            this.active = event;
        }
    }

    /** The running shower, or null. */
    public SkyfallWorldEvent active() {
        if (this.active != null && this.active.isOver()) {
            this.active = null;
        }
        return this.active;
    }

    public int nextDay() {
        return this.nextDay;
    }

    public int lastDay() {
        return this.lastDay;
    }

    private static boolean anyPlayerOnSurface(Server server) {
        for (ServerClient client : server.getClients()) {
            if (client != null && client.isSamePlace(LevelIdentifier.SURFACE_IDENTIFIER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The world record, created on first use — the same idiom
     * {@code SkywatchWorldData.get} uses. {@link SkySurface} calls this once per
     * server start so the schedule ticks even before anything else touches it.
     */
    public static SkyfallWorldData get(Server server) {
        if (server == null || server.world == null) {
            return null;
        }
        WorldEntity worldEntity = server.world.worldEntity;
        if (worldEntity == null) {
            return null;
        }
        WorldData data = worldEntity.getWorldData(KEY);
        if (data instanceof SkyfallWorldData) {
            return (SkyfallWorldData) data;
        }
        SkyfallWorldData created = new SkyfallWorldData();
        worldEntity.addWorldData(KEY, created);
        return created;
    }
}
