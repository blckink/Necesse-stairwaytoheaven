package stairwaytoheaven.journal;

import java.util.HashMap;
import java.util.Map;

import necesse.engine.GameEventListener;
import necesse.engine.GameEvents;
import necesse.engine.commands.CommandsManager;
import necesse.engine.events.ServerStartEvent;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.PacketRegistry;
import necesse.engine.registries.WorldDataRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.ShopManager;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.veil.VeilWorldData;

/**
 * The Adventurer's Journal, as a feature: registration, who gets one and
 * when, and the one server entry point that builds and sends a player's book.
 *
 * <h2>When the player gets it</h2>
 * The first time they stand in the Skyreach — i.e. on the first ascent, the
 * moment {@code swh_findspire} is handed out and the mod's story begins. That
 * is also how existing saves are covered, once: {@link JournalWorldData}'s
 * two-second watcher asks every connected player "on the sky plane, or holding
 * any of this mod's quests, or already touched by the Veil, or the one who
 * hired the Warden — and never given a journal?" and hands one over. A player
 * who already carries one is only recorded, not given a second.
 *
 * <p>It is a watcher rather than a line in {@code SkywardStairwayObjectEntity}
 * on purpose: the ascent, the teleporting gates and the Warden are all being
 * reworked in parallel, and a hook in each would be three merge conflicts and
 * three ways to miss a player who arrived by a new route.
 *
 * <h2>A lost journal</h2>
 * The Warden sells a replacement for {@link #REPLACEMENT_PRICE} coins
 * ({@link #stockShop}, one line in {@code SkyWardenMob}'s constructor). It is
 * not craftable.
 */
public final class AdventurerJournal {

    public static final String ITEM_ID = "adventurersjournal";
    /** 10x its broker value, the same rule the Warden's chalk is priced by. */
    public static final int REPLACEMENT_PRICE = 100;

    private static volatile JournalStepSource stepSource = new LegacyQuestSource();

    /** Last time each player asked, so a held button cannot flood the server. */
    private static final Map<Long, Long> LAST_SENT = new HashMap<>();
    private static final long RESEND_COOLDOWN_MS = 750L;

    private AdventurerJournal() {
    }

    /**
     * Every registry the journal writes to. Called once from
     * {@code StairwayToHeavenMod.init}, after {@code SkyItems.register} and
     * {@code VeilGate.register} (it reads their IDs only at run time, but the
     * item order in dumps stays stable).
     */
    public static void register() {
        ItemRegistry.registerItem("adventurersjournal", new AdventurersJournalItem(), 10.0F, true);
        PacketRegistry.registerPacket(PacketJournalOpen.class);
        PacketRegistry.registerPacket(PacketJournalRequest.class);
        WorldDataRegistry.registerWorldData(JournalWorldData.KEY, JournalWorldData.class);
        CommandsManager.registerServerCommand(new JournalCommand());
        // A WorldData only ticks once it exists, and it is created lazily —
        // the same start-up nudge VeilGate gives VeilWorldData.
        GameEvents.addListener(ServerStartEvent.class, new GameEventListener<ServerStartEvent>() {
            @Override
            public void onEvent(ServerStartEvent event) {
                JournalWorldData.get(event.server);
            }
        });
    }

    /** The source of the story steps. See {@link JournalStepSource} for the ladder hand-over. */
    public static JournalStepSource stepSource() {
        return stepSource;
    }

    /** Installs another step source (the quest ladder). Null restores the legacy one. */
    public static void setStepSource(JournalStepSource source) {
        stepSource = source == null ? new LegacyQuestSource() : source;
    }

    /** One shelf line for whoever sells the replacement copy. */
    public static void stockShop(ShopManager shop) {
        shop.addSellingItem(ITEM_ID, new SellingShopItem(1, 1))
                .setStaticPrice(REPLACEMENT_PRICE, REPLACEMENT_PRICE);
    }

    // ------------------------------------------------------------------
    // reading

    /** Builds this player's book. Server-side, read-only. */
    public static JournalBook build(Server server, ServerClient client) {
        return JournalBuilder.build(server, client, stepSource);
    }

    /** Builds and sends this player's book, which opens the journal window on their client. */
    public static void sendTo(Server server, ServerClient client, boolean rateLimited) {
        if (server == null || client == null) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (LAST_SENT) {
            Long last = LAST_SENT.get(client.authentication);
            if (rateLimited && last != null && now - last < RESEND_COOLDOWN_MS) {
                return;
            }
            LAST_SENT.put(client.authentication, now);
        }
        try {
            client.sendPacket(new PacketJournalOpen(build(server, client)));
        } catch (RuntimeException e) {
            // Never let reading a book take a player's connection down.
            System.err.println("[swh journal] could not build the journal for "
                    + client.getName() + ": " + e);
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------
    // handing it out

    /** Called by {@link JournalWorldData}'s watcher for every live player. */
    static void maybeGiveJournal(Server server, ServerClient client, JournalWorldData data, boolean onSky) {
        long auth = client.authentication;
        if (data.wasGiven(auth) || !isOnTheJourney(server, client, onSky)) {
            return;
        }
        PlayerMob player = client.playerMob;
        Level level = player == null ? null : player.getLevel();
        if (level == null) {
            return;
        }
        data.markGiven(auth);
        if (player.getInv().main.getAmount(level, player, ItemRegistry.getItem(ITEM_ID), "swhjournal") > 0) {
            return;
        }
        InventoryItem book = new InventoryItem(ITEM_ID, 1);
        boolean added = player.getInv().main.addItem(level, player, book, "swhjournal", null);
        if (!added && book.getAmount() > 0) {
            level.entityManager.pickups.add(new ItemPickupEntity(level, book, player.x, player.y, 0.0F, 0.0F));
        }
        // No chat line: the player's standing rule is "keine Chat-Nachrichten!
        // generell" (see SkySettlerMob). The book simply appears in the bag.
    }

    /** Has this player started the mod's story (now, or in an earlier session of this save)? */
    static boolean isOnTheJourney(Server server, ServerClient client, boolean onSky) {
        if (onSky) {
            return true;
        }
        for (Quest quest : client.getQuests().keySet()) {
            if (quest != null && !quest.isRemoved()
                    && quest.getClass().getName().startsWith("stairwaytoheaven.")) {
                return true;
            }
        }
        VeilWorldData veil = VeilWorldData.get(server);
        if (veil != null && (veil.hasTouchedFog(client.authentication) || veil.hasMark(client.authentication))) {
            return true;
        }
        SkywatchWorldData world = SkywatchWorldData.get(server);
        return world != null && world.wardenRecruited && world.wardenAuth == client.authentication;
    }
}
