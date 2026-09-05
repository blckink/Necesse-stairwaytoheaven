package stairwaytoheaven.settlement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import necesse.engine.expeditions.SettlerExpedition;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.registries.ExpeditionMissionRegistry;
import necesse.engine.util.GameLootUtils;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import stairwaytoheaven.quest.SkywatchWorldData;
import stairwaytoheaven.worldgen.RealmDepth;

/**
 * Sky Voyages — the mod's own settler special task, and the first profession
 * in this repository that vanilla does not already have.
 *
 * <h2>Why this is a new profession and the eight before it were not</h2>
 *
 * Every settler the mod has shipped so far borrows a job vanilla already
 * registered: Eveleen calls {@code enableProfession("fertilize")}, Mortimer
 * {@code "hunting"}, Eleanor {@code "husbandry"}, Magpie and Knott
 * {@code "tradingmission"} (see {@code docs/settlers.md}). That is a settler
 * with a vanilla profession, not a new profession. This class registers work
 * that did not exist in the game before the mod was installed: a fourth
 * <b>expedition category</b> beside vanilla's Expedition / Mining trip /
 * Fishing trip, with its own missions, its own gate and its own settler.
 *
 * <h2>The four shapes a special task can take, read out of 1.3.2</h2>
 *
 * Verified against the decompiled server, and written down here because the
 * proposals in {@code docs/design/settler-professions.md} are costed against
 * this list:
 *
 * <ol>
 * <li><b>A new expedition category</b> — what this class does. Three public
 *     hooks and no client code:
 *     {@code ExpeditionMissionRegistry.categoryDisplayNames.put(...)},
 *     {@code registerExpedition(...)} plus {@code setCategory(...)}, and an
 *     override of {@code HumanMob.canDoExpedition} /
 *     {@code getPossibleExpeditions} on the settler
 *     ({@code MinerHumanMob.java:146}).</li>
 * <li><b>A new job type with a new level job</b> —
 *     {@code JobTypeRegistry.registerType} takes mod types (the registry is
 *     open through every mod's {@code init()}), and
 *     {@code SettlementWorkPrioritiesForm.java:95} filters nothing but
 *     {@code canChangePriority}, so a mod job type appears in the settlement
 *     work-priority list by itself. The work still has to be PUBLISHED by
 *     something; see below.</li>
 * <li><b>A new work zone</b> — {@code SettlementWorkZoneRegistry.registerZone}
 *     is public. A zone is what publishes jobs for shape 2: vanilla's own
 *     fertilize and forestry jobs are created inside
 *     {@code SettlementFertilizeZone.java:29} and
 *     {@code SettlementForestryZone.java:94}, not inside {@code tickJobs}. A
 *     tile ({@code LiquidTile.java:157}) and a mob
 *     ({@code CritterMob.java:60}) publish theirs the same way, so a mod has
 *     three separate ways in.</li>
 * <li><b>A workstation</b> — what {@link SkyProfessions} already uses for the
 *     loom, forge and kiln. Not a special task at all: it files under the
 *     vanilla <b>crafting</b> priority every settler already has.</li>
 * </ol>
 *
 * <h2>Where the player meets it</h2>
 *
 * Two places, both vanilla's and both generic:
 *
 * <ul>
 * <li><b>The Mission Board.</b> {@code MissionBoardContainer.java:359}
 *     iterates <em>every</em> registered expedition, keeps the ones whose
 *     {@link #isAvailable} says yes, and
 *     {@code MissionBoardContainerForm.java:459} groups them into buttons by
 *     category name. Our category becomes a fourth button with no client code
 *     written for it.</li>
 * <li><b>Talking to the settler</b>, through {@code getPossibleExpeditions}
 *     on {@code MagpieMob}.</li>
 * </ul>
 *
 * <p><b>A finding worth keeping</b> (this is the one that decides the Kite
 * Rack question left open in {@code docs/design/chapter-01-skyreach-cast.md}):
 * a settler only walks out on a posted mission when the settlement has a
 * mission board, and {@code ServerSettlementData.getMissionBoardTile}
 * (:1499) checks the object's string ID against the literal
 * {@code "missionboard"}. <b>A modded object can therefore never be a mission
 * board.</b> The Kite Rack cannot be the departure station; sky voyages ride
 * on vanilla's own board, and the Rack — if it is ever built — has to be
 * decoration or a processing station instead.
 *
 * <h2>The gate, and why it is the mod's own</h2>
 *
 * Vanilla's expeditions gate on {@code storyProgressSuccessChance}, which only
 * knows vanilla's major story objectives — it cannot see that a player has
 * opened the Ghost Realm. So the gate here is
 * {@link SkywatchWorldData#bossPortalsUnlocked(Server, int)}: a realm is
 * voyage-able once its key piece has been built, which is exactly the record
 * {@code docs/FOGKEY_AND_BOSSPORTALS.md} §B2 already keeps. The shape of the
 * curve is vanilla's, though — a run into the tier you have only just opened
 * is risky, and gets safer as the player climbs past it.
 */
public final class SkyVoyages {

    /**
     * The category string ID. It is the key of the fourth button on the
     * mission board, and its display name resolves through
     * {@code ui.skyvoyagemission} — every category name does
     * ({@code SettlerExpedition.getFullDisplayName}).
     */
    public static final String CATEGORY = "skyvoyage";

    /**
     * Sentinel realm for the capstone: the voyage that is not into one realm
     * but around all of them (WORLD_DESIGN §32's "Cross-Realm Expedition").
     */
    private static final int REALM_ALL = -1;

    /**
     * How the success chance falls off, indexed by how many realms the player
     * has opened BEYOND the one they are sending the courier into.
     *
     * <p>Vanilla's own shape ({@code SettlerExpedition.storyProgressSuccessChances}
     * is {@code {0.6, 0.8, 1.0}}), lifted one notch because our entry rung has
     * no cheaper rung under it: vanilla's 0.6 sits above a whole tree of safe
     * cave expeditions, ours would be the first mission a player ever posts.
     */
    private static final float[] CHANCES = {0.7F, 0.85F, 1.0F};

    /** Registration order, which is the order the board lists them in. */
    private static List<SettlerExpedition> voyages = Collections.emptyList();

    /** IDs, so {@code canDoExpedition} is a set lookup and not a cast. */
    private static Set<Integer> voyageIDs = Collections.emptySet();

    private SkyVoyages() {
    }

    /**
     * Category and missions. Runs in {@code init()} — the expedition registry
     * closes with every other registry once every mod's {@code init()} has
     * run ({@code GlobalData.java:347}).
     *
     * <p>Note what is deliberately NOT called: {@code registerExplorerExpedition},
     * {@code registerMiningTrip} and {@code registerFishingTrip} would each add
     * the ID to one of vanilla's three sets, and vanilla's Explorer, Miner and
     * Angler test exactly those sets in their own {@code canDoExpedition}. Going
     * through the plain {@code registerExpedition} is what keeps a sky voyage
     * something only our own courier can be sent on.
     */
    public static void register() {
        ExpeditionMissionRegistry.categoryDisplayNames.put(CATEGORY,
                new LocalMessage("ui", "skyvoyagemission"));

        List<SettlerExpedition> registered = new ArrayList<>();
        Set<Integer> ids = new LinkedHashSet<>();

        // --- the ladder -------------------------------------------------
        // One voyage per realm the mod actually ships, in progression order,
        // each paying that realm's OWN materials. Hell has no items yet, so it
        // has no voyage yet: an expedition that pays nothing is worse than one
        // that does not exist.
        //
        // Costs climb roughly with the tier the haul belongs to and stay under
        // Magpie's own 12,000 recruit fee, so the courier is never a worse deal
        // than hiring her was. Haul values are coin-equivalents fed to
        // GameLootUtils.getItemsValuedAt, not item counts.

        add(registered, ids, "skyreachvoyage", new RealmVoyage(
                RealmDepth.REALM_SKYREACH, 900, 700, 1100, false,
                new String[]{"skystone", "cloudwood", "windsilk", "aurorapetal",
                        "fulgurite", "stormshard"}));

        add(registered, ids, "edenvoyage", new RealmVoyage(
                RealmDepth.REALM_EDEN, 1800, 1300, 2000, false,
                new String[]{"edenwood", "edensap", "edenberry", "paradiseapple",
                        "goldenpollen", "edencopperore", "sungrape"}));

        add(registered, ids, "steinfeldvoyage", new RealmVoyage(
                RealmDepth.REALM_STEINFELD, 2600, 1900, 2900, false,
                new String[]{"palestone", "gravesalt", "spiritmoss", "echoshard",
                        "charwood"}));

        add(registered, ids, "ghostvoyage", new RealmVoyage(
                RealmDepth.REALM_GHOST, 3600, 2600, 3900, true,
                new String[]{"bonewood", "soulthread", "spectralore", "veilessence"}));

        add(registered, ids, "crookedvoyage", new RealmVoyage(
                RealmDepth.REALM_CROOKED, 4800, 3400, 5200, true,
                new String[]{"oddwood", "warpresin", "strangefabric", "eyeseed",
                        "stripedshell", "realityshard"}));

        // The capstone. Not a sixth rung on the same ladder: it needs the whole
        // road open, it pays out of every realm at once, and its haul is the
        // only one in the mod that can return a legendary happiness object —
        // vanilla reserves those for its deepest content
        // (SettlerExpedition's static block), which is exactly what this is.
        add(registered, ids, "longroundvoyage", new RealmVoyage(
                REALM_ALL, 9000, 6000, 9000, true,
                new String[]{"skyweave", "stormsteelbar", "edenbronzebar", "echoshard",
                        "spiritsteelbar", "realityshard", "prismshard"}));

        voyages = Collections.unmodifiableList(registered);
        voyageIDs = Collections.unmodifiableSet(ids);
    }

    private static void add(List<SettlerExpedition> into, Set<Integer> ids,
                            String stringID, SettlerExpedition voyage) {
        voyage.setCategory(CATEGORY);
        ids.add(ExpeditionMissionRegistry.registerExpedition(stringID, voyage));
        into.add(voyage);
    }

    /**
     * Is this one of ours? The whole of {@code MagpieMob.canDoExpedition}, and
     * the reason no vanilla settler can be assigned a sky voyage on the mission
     * board ({@code MissionBoardContainer.java:167} asks every settler this).
     */
    public static boolean isVoyage(SettlerExpedition expedition) {
        return expedition != null && voyageIDs.contains(expedition.getID());
    }

    /** Every voyage, in ladder order, for the settler's own talk page. */
    public static List<SettlerExpedition> all() {
        return voyages;
    }

    /**
     * One voyage. The realm decides both the gate and the haul, so a new realm
     * is one more {@code add(...)} line above and two locale keys — not a class.
     */
    public static class RealmVoyage extends SettlerExpedition {

        /** {@link RealmDepth}'s realm constant, or {@link #REALM_ALL}. */
        public final int realm;
        public final int baseCost;
        public final int valueMin;
        public final int valueMax;
        /** true = the haul can carry an epic happiness object rather than a rare. */
        public final boolean isDeepRealm;
        public final String[] haul;

        public RealmVoyage(int realm, int baseCost, int valueMin, int valueMax,
                           boolean isDeepRealm, String[] haul) {
            this.realm = realm;
            this.baseCost = baseCost;
            this.valueMin = valueMin;
            this.valueMax = valueMax;
            this.isDeepRealm = isDeepRealm;
            this.haul = haul;
        }

        /**
         * How deep the world is open, as a realm ordinal, or -1 when the road
         * has not started.
         *
         * <p>The Skyreach counts as open the moment the world has a Warden —
         * it is the realm the player is standing in, and it has no key piece of
         * its own to build. Everything past it asks
         * {@code bossPortalsUnlocked}, which answers FALSE when the world
         * record cannot be read at all; a locked realm is the safe failure for
         * a gate, and {@link SkywatchWorldData} documents that choice.
         */
        private static int deepestOpen(Server server) {
            if (server == null || !SkywatchWorldData.hasWarden(server)) {
                return -1;
            }
            int deepest = RealmDepth.REALM_SKYREACH;
            for (int r = RealmDepth.REALM_EDEN; r < RealmDepth.REALM_COUNT; r++) {
                if (SkywatchWorldData.bossPortalsUnlocked(server, r)) {
                    deepest = r;
                }
            }
            return deepest;
        }

        private static Server serverOf(ServerSettlementData settlement) {
            Level level = settlement == null ? null : settlement.getLevel();
            return level == null ? null : level.getServer();
        }

        /**
         * Zero means the board never lists it and the talk page greys it out —
         * {@code SettlerExpedition.isAvailable} is literally "chance > 0".
         */
        @Override
        public float getSuccessChance(ServerSettlementData settlement) {
            int deepest = deepestOpen(serverOf(settlement));
            if (deepest < 0) {
                return 0.0F;
            }
            if (this.realm == REALM_ALL) {
                // The capstone wants the whole road, and stays a gamble even
                // then: there is nothing deeper to grow safe against.
                return deepest >= RealmDepth.REALM_CROOKED ? 0.75F : 0.0F;
            }
            if (this.realm > deepest) {
                return 0.0F;
            }
            return CHANCES[Math.min(deepest - this.realm, CHANCES.length - 1)];
        }

        /** Shown on the talk page in place of a price. */
        @Override
        public GameMessage getUnavailableMessage() {
            return new LocalMessage("expedition", "skyvoyagelocked");
        }

        @Override
        public int getBaseCost(ServerSettlementData settlement) {
            return this.baseCost;
        }

        /**
         * What she comes back with. Same construction as vanilla's mining trip
         * ({@code MiningTripExpedition.java:100}): a ticket list of the realm's
         * own materials, drawn until a coin-equivalent budget is spent, then one
         * happiness object on top so a voyage can also furnish the settlement.
         */
        @Override
        public List<InventoryItem> getRewardItems(ServerSettlementData settlement, HumanMob mob) {
            TicketSystemList<InventoryItem> pool = new TicketSystemList<>();
            for (String itemStringID : this.haul) {
                pool.addObject(100, new InventoryItem(itemStringID, Integer.MAX_VALUE));
            }
            int value = GameRandom.globalRandom.getIntBetween(this.valueMin, this.valueMax);
            ArrayList<InventoryItem> out =
                    GameLootUtils.getItemsValuedAt(GameRandom.globalRandom, value, 0.8F, pool);
            out.sort(java.util.Comparator.comparing(InventoryItem::getBrokerValue).reversed());

            if (this.realm == REALM_ALL) {
                legendaryHappinessObjects.addItems(out, GameRandom.globalRandom, 1.0F);
            } else if (this.isDeepRealm) {
                epicHappinessObjects.addItems(out, GameRandom.globalRandom, 1.0F);
            } else {
                rareHappinessObjects.addItems(out, GameRandom.globalRandom, 1.0F);
            }
            return out;
        }

        /** The icons the board draws beside the mission name. */
        @Override
        public List<InventoryItem> getItemIcons() {
            ArrayList<InventoryItem> icons = new ArrayList<>();
            if (this.haul.length > 0) {
                icons.add(new InventoryItem(this.haul[this.haul.length - 1]));
            }
            return icons;
        }
    }
}
