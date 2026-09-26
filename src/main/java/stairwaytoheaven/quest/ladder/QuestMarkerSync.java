package stairwaytoheaven.quest.ladder;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.QuestMarkerOptions;
import necesse.entity.mobs.ability.CustomMobAbility;

/**
 * The vanilla quest marker over a head — the "!" and "?" the Settlement Elder
 * wears while he has a quest to give or to take back
 * ({@code QuestGiver.QuestGiverObject.getMarkerOptions}, VERIFIED [jar]) — for
 * our own people. Kevin, 2026-09-26: a player walking past should SEE that
 * someone has something new, something to turn in, or something wrong,
 * instead of a bubble that fades or nothing at all.
 *
 * <p>The state lives on the server (the ladder, the journal), the marker is
 * drawn on the client ({@code HumanMob.getMarkerDrawOptions}, called from its
 * draw with the local player as perspective). So the server works out one
 * code per connected player every second and, when anything changed (or every
 * ten seconds, for whoever just came into range), sends the whole table as one
 * mob ability packet. A code under {@link #EVERYONE} is shown to every player.
 */
public final class QuestMarkerSync {

    public static final int NONE = 0;
    /** Yellow "!": has something new for you — talk to them. */
    public static final int NEW = 1;
    /** Yellow "?": your task for them is complete — turn it in. */
    public static final int READY = 2;
    /** Grey "?": still running. Not used — Kevin wants no mark for that. */
    public static final int ACTIVE = 3;
    /** Red "!": something is wrong with them (the vampire's thirst). */
    public static final int WARNING = 4;

    /** Key for a code every player sees. */
    public static final long EVERYONE = Long.MIN_VALUE;

    private static final Color RED = new Color(220, 60, 60);

    private final Mob mob;
    private final CustomMobAbility ability;
    private Map<Long, Integer> codes = new HashMap<>();
    private int ticks;
    private int sinceSend;

    /** Construct in the mob's constructor (field initializer): it registers an ability. */
    public QuestMarkerSync(Mob mob) {
        this.mob = mob;
        this.ability = mob.registerAbility(new CustomMobAbility() {
            @Override
            protected void run(Packet content) {
                QuestMarkerSync.this.read(content);
            }
        });
    }

    /**
     * Server side, every tick. {@code perPlayer} gives the code for one player
     * (NONE for none), {@code everyone} the code everyone sees (NONE for none).
     */
    public void serverTick(ToIntFunction<ServerClient> perPlayer, java.util.function.IntSupplier everyone) {
        if (!this.mob.isServer() || ++this.ticks < 20) {
            return;
        }
        this.ticks = 0;
        Map<Long, Integer> next = new HashMap<>();
        int all = everyone == null ? NONE : everyone.getAsInt();
        if (all != NONE) {
            next.put(EVERYONE, all);
        }
        Server server = this.mob.getLevel() == null ? null : this.mob.getLevel().getServer();
        if (server != null && perPlayer != null) {
            for (ServerClient client : server.getClients()) {
                if (client == null || client.playerMob == null) {
                    continue;
                }
                int code;
                try {
                    code = perPlayer.applyAsInt(client);
                } catch (RuntimeException e) {
                    code = NONE; // a marker is never worth a crashed tick
                }
                if (code != NONE) {
                    next.put(client.authentication, code);
                }
            }
        }
        if (!next.equals(this.codes) || ++this.sinceSend >= 10) {
            this.sinceSend = 0;
            this.codes = next;
            Packet content = new Packet();
            PacketWriter writer = new PacketWriter(content);
            writer.putNextShortUnsigned(next.size());
            for (Map.Entry<Long, Integer> e : next.entrySet()) {
                writer.putNextLong(e.getKey());
                writer.putNextByteUnsigned(e.getValue());
            }
            this.ability.runAndSend(content);
        }
    }

    private void read(Packet content) {
        PacketReader reader = new PacketReader(content);
        int n = reader.getNextShortUnsigned();
        Map<Long, Integer> read = new HashMap<>();
        for (int i = 0; i < n; i++) {
            long key = reader.getNextLong();
            read.put(key, reader.getNextByteUnsigned());
        }
        this.codes = read;
    }

    /** Client side: the marker for this perspective, or null. */
    public QuestMarkerOptions options(PlayerMob perspective) {
        int code = NONE;
        if (perspective != null) {
            NetworkClient client = perspective.getNetworkClient();
            if (client != null) {
                code = this.codes.getOrDefault(client.authentication, NONE);
            }
        }
        if (code == NONE) {
            code = this.codes.getOrDefault(EVERYONE, NONE);
        }
        switch (code) {
            case NEW:
                return new QuestMarkerOptions('!', QuestMarkerOptions.orangeColor);
            case READY:
                return new QuestMarkerOptions('?', QuestMarkerOptions.orangeColor);
            case ACTIVE:
                return new QuestMarkerOptions('?', new Color(100, 100, 100));
            case WARNING:
                return new QuestMarkerOptions('!', RED);
            default:
                return null;
        }
    }

    /**
     * The code for one quest-ladder giver and one player, in the Elder's
     * order: something to turn in beats something new.
     */
    public static int ladderCode(String giver, ServerClient client) {
        boolean offered = false;
        for (QuestLadder.Step step : QuestLadder.stepsOf(giver)) {
            QuestLadder.Status status = QuestLadder.status(client, step);
            if (status == QuestLadder.Status.READY) {
                return READY;
            }
            if (status == QuestLadder.Status.AVAILABLE && !step.custom) {
                // A custom step is handed out by its own trigger, not by
                // talking, so a "!" for it would send the player to talk
                // for nothing; held and complete it still shows "?".
                offered = true;
            }
        }
        // Nothing for a task still running: the Elder shows a mark only
        // when there is something to take or to hand in (Kevin, 2026-09-26).
        return offered ? NEW : NONE;
    }
}
