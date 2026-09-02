package stairwaytoheaven.veil;

import java.util.HashSet;
import java.util.Set;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldData.WorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.level.maps.Level;

/**
 * The Veil's clock and its ledger of who may cross.
 *
 * <h2>Why a {@code WorldData}</h2>
 *
 * Two things have to live somewhere that is neither a level nor a player:
 *
 * <ol>
 *   <li><b>The region check.</b> {@code WorldEntity.serverTick} ticks every
 *       {@link WorldData} the world holds, every server tick, and it is the one
 *       hook a mod can take that sees every online player on every level
 *       without owning a level or a mob. That is exactly what
 *       {@code docs/WORLD_DESIGN.md} §8 asks for: the effect is checked against
 *       the world REGION, for whoever is standing in it, however they got
 *       there.</li>
 *   <li><b>The Veil Mark.</b> §9 calls it "a permanent character unlock, not a
 *       losable inventory item". A level record dies with the level and an item
 *       can be dropped; a world record is written next to the settlements in
 *       the world file and outlives both, including a
 *       {@code SkyRegistry.WORLD_GENERATION} bump. Same mechanism the mod
 *       already uses for {@code quest/SkywatchWorldData} and
 *       {@code surface/SkyfallWorldData}.</li>
 * </ol>
 *
 * <p>It is a SEPARATE record from {@code SkywatchWorldData} rather than two
 * more fields on it: that one is the Warden's file (his recruitment, his cats,
 * their home), and the Veil is not his story. Two small records also mean the
 * séance questline can be built against this one without touching the
 * recruitment save schema.
 *
 * <h2>The Mark is per character, not per world</h2>
 *
 * §9 makes the Mark something the player earns, so in a shared world one
 * player's séance must not carry another player through the fog. The record is
 * therefore a set of {@code ServerClient.authentication} values — the same key
 * {@code SkywatchWorldData.wardenAuth} and {@code SkywatchQuestData}'s marker
 * sets use — stored world-side so it survives everything, but answered per
 * player.
 *
 * <h2>Server-authoritative</h2>
 *
 * {@code WorldData.tick()} runs on the client too, so everything below is
 * behind {@code isServer()}. A client never decides whether it is in the fog;
 * it is told, by the buff packets the server sends.
 */
public class VeilWorldData extends WorldData {

    /** Must match {@code [a-zA-Z0-9]+} — {@code WorldEntity.addWorldData} enforces it. */
    public static final String KEY = "swhveil";

    /**
     * How often the region is checked, and therefore the exposure clock's tick.
     *
     * <p>One second, because {@code docs/WORLD_DESIGN.md} §8's table is written
     * in whole seconds and {@link SoulExposureBuff} spends one stack per
     * second. Checking faster would buy nothing — the first stack is only
     * "slight vision reduction", so being caught up to a second after
     * teleporting in costs the player nothing — and it would multiply the buff
     * packets by four for a number that cannot change in between.
     */
    private static final int CHECK_INTERVAL_MS = 1000;

    /**
     * Duration handed to each application, i.e. how long after leaving the fog
     * before the stacks start coming back.
     *
     * <p>Three times the refresh interval. Two jobs: while inside, it is
     * comfortably longer than the gap between applications, so the clock only
     * ever climbs; outside, it is the grace period before decay begins, which
     * is what stops a player from shaking the Veil off by stepping over the
     * line and back. {@code ActiveBuff.stack} keeps the LONGER of the old and
     * new times (ActiveBuff.java:103-106), so every application resets it.
     */
    private static final int APPLY_DURATION_MS = 3 * CHECK_INTERVAL_MS;

    /** Don't repeat the same chat warning at a player more often than this. */
    private static final long MESSAGE_COOLDOWN_MS = 30_000L;

    private static final String COOLDOWN_WARNED = "swhveilwarned";
    private static final String COOLDOWN_PARTED = "swhveilparted";

    /**
     * The authentications that hold the Veil Mark.
     *
     * <p>Empty in every world today, because §9's séance questline — Madame
     * Orla, the Séance Table, the Ferryman — is not built. Until it is, the
     * only thing that writes to this set is the {@code /veilmark} admin command
     * (see {@code commands/VeilMarkCommand}). When the questline lands, the
     * Ferryman calls {@link #grantMark} and nothing else in this package
     * changes.
     */
    private final Set<Long> markAuths = new HashSet<>();

    private long nextCheckTime;

    // ------------------------------------------------------------------
    // persistence
    // ------------------------------------------------------------------

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        long[] auths = new long[this.markAuths.size()];
        int i = 0;
        for (long auth : this.markAuths) {
            auths[i++] = auth;
        }
        save.addLongArray("markAuths", auths);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.markAuths.clear();
        if (save.hasLoadDataByName("markAuths")) {
            for (long auth : save.getLongArray("markAuths")) {
                this.markAuths.add(auth);
            }
        }
    }

    // ------------------------------------------------------------------
    // the Veil Mark
    // ------------------------------------------------------------------

    /** Does this character carry the Veil Mark? */
    public boolean hasMark(long auth) {
        return this.markAuths.contains(auth);
    }

    /** Records the Mark. Idempotent; returns true when it was actually new. */
    public boolean grantMark(long auth) {
        return this.markAuths.add(auth);
    }

    /**
     * Takes the Mark back. Nothing in the design does this — §9 calls the Mark
     * permanent — and nothing but the admin command calls it. It exists so the
     * gate can be tested from both sides in one session without a new world.
     */
    public boolean revokeMark(long auth) {
        return this.markAuths.remove(auth);
    }

    /** How many characters in this world have crossed. For the status command. */
    public int markCount() {
        return this.markAuths.size();
    }

    // ------------------------------------------------------------------
    // the region check
    // ------------------------------------------------------------------

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
            this.tickClient(client);
        }
    }

    /**
     * One player, one question: are you in the fog right now?
     *
     * <p>This is the whole of §8's "teleport / movement abuse must be handled".
     * Nothing here knows or cares how the player reached the tile they are
     * standing on — there is no entry hook to dodge, no boundary to be on the
     * far side of, and no blocked tile to rope over. A player who teleports
     * past the edge is asked the same question one second later and gets the
     * same answer as one who walked.
     *
     * <p>Players only. Settlers, pets and hostiles are not gated by §8, and the
     * Veil has no reason to kill the Gloom Shades that live in it.
     */
    private void tickClient(ServerClient client) {
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
        if (!VeilRegion.isInside(level, player.getTileX(), player.getTileY())) {
            // Outside: nothing to do. The stacks already on the player drain
            // themselves through SoulExposureBuff.getRemainingStacksDuration,
            // and the fog buff expires on its own within APPLY_DURATION_MS.
            return;
        }

        // The fog is scenery and applies to everyone inside, Mark or no Mark:
        // WORLD_DESIGN §9 keeps it visible after the unlock so the border stays
        // legible.
        refresh(player, VeilGate.fog());

        if (this.hasMark(client.authentication)) {
            if (!player.isOnGenericCooldown(COOLDOWN_PARTED)) {
                player.startGenericCooldown(COOLDOWN_PARTED, MESSAGE_COOLDOWN_MS);
                client.sendChatMessage(new LocalMessage("misc", "veilmarkcrossing"));
            }
            return;
        }

        boolean firstStack = player.buffManager.getBuff(VeilGate.exposure()) == null;
        refresh(player, VeilGate.exposure());
        if (firstStack && !player.isOnGenericCooldown(COOLDOWN_WARNED)) {
            // An unexplained health drain reads as a bug. The player is told
            // what is happening the second it starts, before the first stack
            // does anything worse than dim the view.
            player.startGenericCooldown(COOLDOWN_WARNED, MESSAGE_COOLDOWN_MS);
            client.sendChatMessage(new LocalMessage("misc", "veilexposurewarning"));
        }
    }

    /**
     * Adds one stack, or starts the buff.
     *
     * <p>{@code sendUpdatePacket} is true: the stack count is the severity, the
     * HUD readout and the blindness the client renders, so the client has to be
     * told about every one of them.
     */
    private static void refresh(PlayerMob player, Buff buff) {
        if (buff == null) {
            return;
        }
        player.buffManager.addBuff(new ActiveBuff(buff, player, APPLY_DURATION_MS, VeilGate.ATTACKER), true);
    }

    // ------------------------------------------------------------------
    // access
    // ------------------------------------------------------------------

    /**
     * The world record, created on first use — the same shape as
     * {@code SkywatchWorldData.get}: a world that never had one simply gets an
     * empty record, which reads as "nobody has crossed yet".
     */
    public static VeilWorldData get(Server server) {
        if (server == null || server.world == null) {
            return null;
        }
        WorldEntity worldEntity = server.world.worldEntity;
        if (worldEntity == null) {
            return null;
        }
        WorldData data = worldEntity.getWorldData(KEY);
        if (data instanceof VeilWorldData) {
            return (VeilWorldData) data;
        }
        VeilWorldData created = new VeilWorldData();
        worldEntity.addWorldData(KEY, created);
        return created;
    }

    /** Convenience: does this character carry the Mark in this world? */
    public static boolean hasMark(Server server, long auth) {
        VeilWorldData data = get(server);
        return data != null && data.hasMark(auth);
    }
}
