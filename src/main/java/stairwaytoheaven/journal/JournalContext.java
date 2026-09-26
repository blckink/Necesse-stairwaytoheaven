package stairwaytoheaven.journal;

import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkyQuests;
import stairwaytoheaven.quest.SkywatchQuestData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.veil.VeilWorldData;
import stairwaytoheaven.worldgen.RealmDepth;

/**
 * Everything a journal is built from, for one reader, gathered once.
 *
 * <p>Two scopes, kept apart on purpose because the mod keeps them apart:
 * <ul>
 * <li><b>per world</b> — {@link #sky} (the Skyreach's level record) and
 *     {@link #world} (the world record): the Warden chain, the region keys,
 *     the key pieces, the residents' "paid once" chains. In co-op these are
 *     the same for everyone.</li>
 * <li><b>per player</b> — the quests {@link #client} holds, {@link #veil}'s
 *     fog / chalk / Mark sets, the kill stats, and what {@link #journal} saw
 *     this player meet and visit.</li>
 * </ul>
 *
 * <p>{@link #client} may be null: {@code /swhjournal} builds a world-only book
 * on a server nobody has joined, and every helper answers the "nobody holds
 * anything" way round in that case.
 */
public final class JournalContext {

    public final Server server;
    /** May be null (world-only book). */
    public final ServerClient client;
    /** 0 when {@link #client} is null. */
    public final long auth;
    /** The Skyreach's quest record. Null only if the Skyreach cannot be loaded. */
    public final SkywatchQuestData sky;
    public final SkywatchWorldData world;
    public final VeilWorldData veil;
    public final JournalWorldData journal;

    public JournalContext(Server server, ServerClient client) {
        this.server = server;
        this.client = client;
        this.auth = client == null ? 0L : client.authentication;
        this.sky = skyRecord(server);
        this.world = SkywatchWorldData.get(server);
        this.veil = VeilWorldData.get(server);
        this.journal = JournalWorldData.get(server);
    }

    /**
     * The Skyreach record, read the way every other reader in the mod reads
     * it ({@code SkyWardenMob.skyLevel}): {@code World.getLevel}, which loads
     * the level from disk when it is asleep. A journal is only handed out once
     * a player has stood in the Skyreach, so the level always exists by then.
     */
    private static SkywatchQuestData skyRecord(Server server) {
        if (server == null || server.world == null) {
            return null;
        }
        try {
            Level sky = server.world.getLevel(SkyRegistry.SKYREACH_IDENTIFIER);
            return sky == null ? null : SkywatchQuestData.get(sky);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // per player

    /** Does the reader currently hold a quest of this class? */
    public boolean holds(Class<? extends Quest> type) {
        return this.client != null && SkyQuests.findHeld(this.client, type) != null;
    }

    /** Held, and it could be handed in right now. */
    public boolean ready(Class<? extends Quest> type) {
        Quest held = this.client == null ? null : SkyQuests.findHeld(this.client, type);
        return held != null && held.canComplete(this.client);
    }

    /**
     * Who finished a world-scoped step, when it was somebody other than the
     * reader; null otherwise (or when nobody was recorded - saves from before
     * 2026-09-26).
     */
    public necesse.engine.localization.message.GameMessage doneByOther(String stepID) {
        stairwaytoheaven.quest.SkywatchWorldData world = stairwaytoheaven.quest.SkywatchWorldData.get(this.server);
        String name = world == null ? null : world.doneBy.get(stepID);
        if (name == null || (this.client != null && name.equals(this.client.getName()))) {
            return null;
        }
        return new necesse.engine.localization.message.StaticMessage(name);
    }

    /** How many of an item the reader carries in the main inventory; -1 when unknown. */
    public int have(Item item) {
        PlayerMob player = this.client == null ? null : this.client.playerMob;
        if (player == null || item == null || player.getLevel() == null) {
            return -1;
        }
        return player.getInv().main.getAmount(player.getLevel(), player, item, "swhjournal");
    }

    public int have(String itemStringID) {
        return this.have(ItemRegistry.getItem(itemStringID));
    }

    /** This character's kills of a mob type, from its own stats. */
    public int kills(String mobStringID) {
        if (this.client == null) {
            return 0;
        }
        try {
            return this.client.characterStats().mob_kills.getKills(mobStringID);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Has the reader met ANY of these named mobs (see {@link JournalWorldData#WATCHED})? */
    public boolean met(String... mobStringIDs) {
        if (this.journal == null || this.client == null) {
            return false;
        }
        for (String id : mobStringIDs) {
            if (this.journal.hasSeenMob(this.auth, id)) {
                return true;
            }
        }
        return false;
    }

    public boolean visited(int realm) {
        return this.journal != null && this.client != null && this.journal.hasVisited(this.auth, realm);
    }

    public boolean fogTouched() {
        return this.veil != null && this.veil.hasTouchedFog(this.auth);
    }

    public boolean chalkGiven() {
        return this.veil != null && this.veil.hasBeenGivenChalk(this.auth);
    }

    public boolean hasMark() {
        return this.veil != null && this.veil.hasMark(this.auth);
    }

    // ------------------------------------------------------------------
    // per world

    public int stage() {
        return this.sky == null ? 0 : this.sky.stage;
    }

    public boolean wardenRecruited() {
        return (this.world != null && this.world.wardenRecruited) || (this.sky != null && this.sky.recruited);
    }

    public boolean catHome(boolean black) {
        if (black) {
            return (this.sky != null && this.sky.blackHome) || (this.world != null && this.world.blackHome);
        }
        return (this.sky != null && this.sky.tabbyHome) || (this.world != null && this.world.tabbyHome);
    }

    public boolean catsRewardGiven() {
        return this.sky != null && this.sky.catsRewardGiven;
    }

    public boolean anchorDone() {
        return this.sky != null && this.sky.anchorDone;
    }

    /** {@code SkyWardenMob.chapterFor(...) == DONE}: the Warden hands out region keys. */
    public boolean wardenChainDone() {
        return this.wardenRecruited() && this.catHome(true) && this.catHome(false)
                && this.catsRewardGiven() && this.anchorDone();
    }

    /** Read straight off the set, NOT through {@code SkywatchWorldData.regionKeyEarned}, whose missing-record answer is "paid". */
    public boolean keyEarned(int realm) {
        return this.world != null && this.world.regionKeysEarned.contains(RealmDepth.keyOf(realm));
    }

    public boolean portalsAwake(int realm) {
        return this.world != null && this.world.bossPortalsUnlocked(realm);
    }

    public boolean chainDone(String chainKey) {
        return this.world != null && this.world.residentChainsDone.contains(chainKey);
    }

    public boolean edenPlantsGiven() {
        return this.world != null && this.world.edenPlantsGiven;
    }

    public boolean crookedDoorwayOpened() {
        return this.world != null && this.world.crookedDoorwayOpened;
    }

    public boolean eleanorPassedOn() {
        return this.world != null && this.world.eleanorPassedOn;
    }

    public boolean seenAsSettler(String mobStringID) {
        return this.journal != null && this.journal.seenAsSettler(mobStringID);
    }
}
