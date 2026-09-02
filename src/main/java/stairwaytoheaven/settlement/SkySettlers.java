package stairwaytoheaven.settlement;

import java.util.function.Supplier;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import stairwaytoheaven.quest.SkywatchWorldData;

/**
 * The mod's hireable residents, as real settlement settlers.
 *
 * A mob alone is not a settler. {@code HumanMob.getSettler()} resolves the
 * mob's settler key through {@link SettlerRegistry}, and {@code LevelSettler}
 * runs {@code Objects.requireNonNull} on the result — so without a registered
 * {@link Settler} the vanilla recruit path answers "notsettler", the recruit
 * button can never work, and the NPC can never take a bed. That is exactly the
 * bug the Warden shipped with, and it is why every one of these gets its type
 * here in the same breath as its mob.
 *
 * {@code Settler} objects must be constructed while the registry is OPEN, i.e.
 * during {@code init()}. {@code onSettlerRegistryClosed} then validates that
 * each mobStringID resolves to a mob implementing SettlerMob, so a bad
 * registration fails the server boot loudly instead of silently.
 *
 * <h2>The cast</h2>
 *
 * <table>
 * <tr><th>who</th><th>profession</th><th>found</th><th>travels to a town</th></tr>
 * <tr><td>Magpie</td><td>trading missions</td><td>Skyreach workshop</td><td>no</td></tr>
 * <tr><td>Halda</td><td>fishing</td><td>Skyreach workshop</td><td>no</td></tr>
 * <tr><td>Ossian</td><td>crafting only</td><td>Skyreach workshop</td><td>no</td></tr>
 * <tr><td>Eveleen</td><td>fertilising (farmer)</td><td>—</td><td>Eden grass in the settlement</td></tr>
 * <tr><td>Mortimer</td><td>hunting</td><td>Veil bone piles</td><td>a graveyard in the settlement</td></tr>
 * <tr><td>Caspern</td><td>crafting only</td><td>Veil bone piles</td><td>an Aether Forge in the settlement</td></tr>
 * <tr><td>Eleanor</td><td>husbandry</td><td>Veil bone piles</td><td>no — she is an ending, not a visitor</td></tr>
 * </table>
 *
 * "Travels to a town" is {@link SkyArrivals}: the vanilla route by which a
 * settler walks into a settlement and asks to join. The three Skyreach
 * residents deliberately do NOT take it — they are found once per world beside
 * their workshop, and a second route would have to be able to see the first
 * one to avoid standing up two Magpies. {@link SkywatchWorldData#residentsClaimed}
 * is what makes the two routes see each other, and both of them consult it.
 */
public final class SkySettlers {

    private SkySettlers() {
    }

    // --- who is who, so worldgen and the arrival roll agree on the names ---

    public static final String MAGPIE = "magpiesettler";
    public static final String HALDA = "haldasettler";
    public static final String OSSIAN = "ossiansettler";
    public static final String EVELEEN = "eveleensettler";
    public static final String MORTIMER = "mortimersettler";
    public static final String CASPERN = "caspernsettler";
    public static final String ELEANOR = "eleanorsettler";

    /** The Skyreach's own three: placed beside a derelict workshop. */
    public static final String[] SKY_RESIDENTS = {MAGPIE, HALDA, OSSIAN};

    /**
     * The Veil's three. WORLD_DESIGN §11 files them under the Ghost Realm,
     * which is not built; the Veil is the layer that realm grows out of and is
     * the home the mod can give them today.
     */
    public static final String[] VEIL_RESIDENTS = {MORTIMER, CASPERN, ELEANOR};

    /**
     * Every ID below is written as a LITERAL, in both registry calls, and never
     * through the constants above. That is not redundancy: {@code
     * tools/locale_audit.py} follows registration calls in the source to find
     * the IDs a player can see, and an ID handed over as a constant is an ID
     * the audit cannot name-check — which is precisely how objects have shipped
     * from this repository showing their raw string ID. The constants exist for
     * worldgen; {@link #assertWired} makes a drift between the two a loud boot
     * failure instead of a resident who silently never appears.
     */
    public static void register() {
        MobRegistry.registerMob("magpiesettler", stairwaytoheaven.mobs.MagpieMob.class, false);
        MobRegistry.registerMob("haldasettler", stairwaytoheaven.mobs.HaldaMob.class, false);
        MobRegistry.registerMob("ossiansettler", stairwaytoheaven.mobs.OssianMob.class, false);
        MobRegistry.registerMob("eveleensettler", stairwaytoheaven.mobs.EveleenMob.class, false);
        MobRegistry.registerMob("mortimersettler", stairwaytoheaven.mobs.MortimerMob.class, false);
        MobRegistry.registerMob("caspernsettler", stairwaytoheaven.mobs.CaspernMob.class, false);
        MobRegistry.registerMob("eleanorsettler", stairwaytoheaven.mobs.EleanorMob.class, false);

        // The three found in the Skyreach. No arrival ticket: see the class
        // note above.
        SettlerRegistry.registerSettler("magpiesettler",
                new SkyResident("magpiesettler", () -> GameTexture.fromFile("mobs/icons/magpiesettler"),
                        "magpiesettlertip", null, 0));
        SettlerRegistry.registerSettler("haldasettler",
                new SkyResident("haldasettler", () -> GameTexture.fromFile("mobs/icons/haldasettler"),
                        "haldasettlertip", null, 0));
        SettlerRegistry.registerSettler("ossiansettler",
                new SkyResident("ossiansettler", () -> GameTexture.fromFile("mobs/icons/ossiansettler"),
                        "ossiansettlertip", null, 0));

        // The four who TRAVEL. Each icon is a vanilla face borrowed by literal
        // path — GameTexture.fromFile reads one flat resource map with the
        // mod's files merged in, so "mobs/icons/farmerhuman" resolves from mod
        // code exactly as our own icons do (see livestock/SkyPelt). No new art.
        // Every borrowed path has a row in docs/VANILLA_ASSET_MAP.md.
        SettlerRegistry.registerSettler("eveleensettler",
                new SkyResident("eveleensettler", () -> GameTexture.fromFile("mobs/icons/farmerhuman"),
                        "eveleensettlertip", SkyArrivals.EDEN_PATCH, 90));
        SettlerRegistry.registerSettler("mortimersettler",
                new SkyResident("mortimersettler", () -> GameTexture.fromFile("mobs/icons/pawnbrokerhuman"),
                        "mortimersettlertip", SkyArrivals.GRAVEYARD, 90));
        SettlerRegistry.registerSettler("caspernsettler",
                new SkyResident("caspernsettler", () -> GameTexture.fromFile("mobs/icons/blacksmithhuman"),
                        "caspernsettlertip", SkyArrivals.FORGE, 90));
        // Eleanor is an ENDING, not a visitor: WORLD_DESIGN §11 makes the
        // player choose between letting her go and keeping her, and a choice
        // that can be delivered by a visitor timer is not a choice. She is
        // found in the Veil and nowhere else.
        SettlerRegistry.registerSettler("eleanorsettler",
                new SkyResident("eleanorsettler", () -> GameTexture.fromFile("mobs/icons/stylisthuman"),
                        "eleanorsettlertip", null, 0));

        assertWired();
    }

    /**
     * The constants worldgen uses must name mobs that were actually registered.
     *
     * Worldgen asks {@code MobRegistry.getMob(who, level)} and quietly gives up
     * on null, so a one-character drift between a constant and its literal
     * would show up as "that person never spawns" three playtests later. This
     * turns it into a boot failure, which is the same trade
     * {@code SettlerRegistry.onSettlerRegistryClosed} makes for settler types.
     */
    private static void assertWired() {
        for (String[] group : new String[][]{SKY_RESIDENTS, VEIL_RESIDENTS,
                {EVELEEN, MAGPIE, HALDA, OSSIAN, MORTIMER, CASPERN, ELEANOR}}) {
            for (String id : group) {
                if (MobRegistry.getMobID(id) < 0) {
                    throw new IllegalStateException(
                            "SkySettlers constant \"" + id + "\" names no registered mob");
                }
            }
        }
    }

    /**
     * One settler type for all of them: they differ in what they sell, what
     * they will work at, and where the player meets them — not in how they move
     * in.
     */
    public static class SkyResident extends Settler {

        private final Supplier<GameTexture> icon;
        private final String acquireTipKey;
        /** null = never travels to a settlement on its own. */
        private final SkyArrivals.Gate arrivalGate;
        private final int arrivalTickets;

        public SkyResident(String mobStringID, Supplier<GameTexture> icon, String acquireTipKey,
                           SkyArrivals.Gate arrivalGate, int arrivalTickets) {
            super(mobStringID);
            this.icon = icon;
            this.acquireTipKey = acquireTipKey;
            this.arrivalGate = arrivalGate;
            this.arrivalTickets = arrivalTickets;
            // Vanilla's COMPLETE_HOST achievement wants one of every settler
            // type in a settlement. A modded settler must stay out of that set
            // or installing this mod makes the achievement unreachable.
            this.isPartOfCompleteHost = false;
        }

        @Override
        public void loadTextures() {
            this.texture = this.icon.get();
        }

        /**
         * The settlement screen's "where do I get this settler" line. Vanilla
         * uses it for "found in a village" and the miner's cave hint; ours
         * names the region the character comes from, which is the only place
         * in game that says so.
         */
        @Override
        public GameMessage getAcquireTip() {
            return new LocalMessage("misc", this.acquireTipKey);
        }

        /**
         * THE ARRIVAL. This is the override that was missing, and its absence
         * is the whole reason no settler of this mod had ever walked into a
         * town — {@code Settler.addNewRecruitSettler} is empty in the base
         * class, so our settlers put no ticket into the recruit-visitor draw
         * and could never be drawn.
         *
         * {@code ServerSettlementData}'s recruit odds call this on every
         * registered settler, take one ticket, spawn that mob inside the
         * settlement as a visitor and announce it in chat. A settler that adds
         * a ticket travels; one that does not, does not. See {@link SkyArrivals}.
         *
         * <p>Three conditions, all of which have to hold:
         * <ol>
         * <li>this settlement does not already have them — {@code isRandomEvent}
         *     does NOT bypass it, unlike vanilla's Farmer, because ours are
         *     named individuals rather than a role;</li>
         * <li>the world has not already produced them by the other route
         *     (worldgen), which is what {@code residentsClaimed} records;</li>
         * <li>the world has a Sky Warden, plus this character's own condition.</li>
         * </ol>
         */
        @Override
        public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent,
                                         TicketSystemList<Supplier<HumanMob>> ticketSystem) {
            if (this.arrivalGate == null || this.arrivalTickets <= 0) {
                return;
            }
            if (this.doesSettlementHaveThisSettler(data)) {
                return;
            }
            Level level = data.getLevel();
            Server server = level == null ? null : level.getServer();
            if (server == null || SkywatchWorldData.residentClaimed(server, this.mobStringID)) {
                return;
            }
            if (!SkyArrivals.wardenSettled(data) || !this.arrivalGate.isOpen(data)) {
                return;
            }
            ticketSystem.addObject(this.arrivalTickets, this.getNewRecruitMob(data));
        }

        /**
         * The moment they take a bed, the world stops being able to produce a
         * second one of them anywhere. This is the settlement half of the
         * shared claim; worldgen makes the same call when it stands one up.
         */
        @Override
        public void onMoveIn(LevelSettler settler) {
            super.onMoveIn(settler);
            ServerSettlementData data = settler.data;
            Level level = data == null ? null : data.getLevel();
            Server server = level == null ? null : level.getServer();
            if (server != null) {
                SkywatchWorldData.claimResident(server, this.mobStringID);
            }
        }

        /**
         * Never rolled in as a free settler. This is the OTHER spawn path —
         * {@code tickSpawnInSettlers} moves one in for nothing — and it is not
         * the same thing as arriving as a recruit, which is what
         * {@code addNewRecruitSettler} above does and which the player pays for.
         */
        @Override
        public boolean canSpawnInSettlement(ServerSettlementData settlement, PlayerStats stats) {
            return false;
        }

        /** Paid for, and a named character: they do not wander off again. */
        @Override
        public boolean canMoveOut(LevelSettler settler, ServerSettlementData settlement) {
            return false;
        }

        @Override
        public boolean canBanish(LevelSettler settler, ServerSettlementData settlement) {
            return false;
        }

        /** No random stand-in arrives if one dies. They are individuals. */
        @Override
        public float getArriveAsRecruitAfterDeathChance(ServerSettlementData settlement) {
            return 0.0F;
        }
    }
}
