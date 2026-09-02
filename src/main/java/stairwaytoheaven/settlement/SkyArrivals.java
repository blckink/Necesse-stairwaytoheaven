package stairwaytoheaven.settlement;

import java.awt.Rectangle;

import necesse.engine.network.server.Server;
import necesse.engine.registries.ObjectRegistry;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import stairwaytoheaven.SkyRegistry;
import stairwaytoheaven.quest.SkywatchWorldData;

/**
 * How a mod settler TRAVELS TO A SETTLEMENT and asks to join.
 *
 * <h2>What vanilla actually does — read out of 1.3.2, not assumed</h2>
 *
 * A settlement spawns a visitor on a timer: {@code ServerSettlementData
 * .tickNextVisitor} counts {@code nextVisitorTimer} down and calls
 * {@code spawnNextVisitor} (ServerSettlementData.java:1591-1602). That method
 * picks a spawner, and every other firing it asks
 * {@code visitorRecruitsOdds.getNewVisitorSpawner} — which builds a
 * {@code TicketSystemList} by walking EVERY registered {@link
 * necesse.level.maps.levelData.settlementData.settler.Settler} and calling
 * {@code settler.addNewRecruitSettler(data, isRandomEvent, ticketSystem)}
 * (ServerSettlementData.java:122-133 and the "recruit" entry in
 * {@code visitorOdds}, :2165-2190). One ticket is drawn, the mob is spawned
 * inside the settlement through {@code spawnVisitor}, {@code startVisitor}
 * gives it a stay timer and a move-in point, and everyone on the team gets the
 * chat line {@code settlement.travelingarrive} — "X has arrived at Y".
 *
 * <p><b>{@code Settler.addNewRecruitSettler} is empty in the base class.</b>
 * That single fact is why no settler of this mod had ever walked into a town:
 * they were registered, they were recruitable, and they added no ticket, so the
 * ticket system never had one to draw. Nothing else was wrong. The whole
 * arrival path is one override.
 *
 * <h2>What this class adds on top</h2>
 *
 * Vanilla's own settlers arrive on story progress
 * ({@code AnimalKeeperSettler} waits for {@code defeatvoidwizard}). A Skyreach
 * settler has no vanilla story objective to hang on, so the gates live here:
 * one shared precondition — the world has a Sky Warden, i.e. the player has a
 * Skywatch house for word to reach — plus one per-character condition that the
 * player can see and build towards, taken from {@code docs/WORLD_DESIGN.md}
 * where it names one (§11: Mortimer "after building a graveyard", Caspern
 * "build the Spirit Forge").
 *
 * <p>All of it is server-side: {@code addNewRecruitSettler} only ever runs
 * inside {@code ServerSettlementData}.
 */
public final class SkyArrivals {

    private SkyArrivals() {
    }

    /**
     * One character's condition for travelling to a settlement.
     *
     * Evaluated on the server every time that settlement rolls a recruit
     * visitor — a few minutes apart at best, so a gate may read the level.
     */
    public interface Gate {
        boolean isOpen(ServerSettlementData data);
    }

    /** No condition beyond the shared one. */
    public static final Gate ALWAYS = data -> true;

    /**
     * The shared precondition: this world has recruited its Sky Warden.
     *
     * {@link SkywatchWorldData} is a {@code WorldData}, not level data, so it
     * survives a generation bump and answers the same on the surface as it does
     * in the sky — which is what a settlement on the surface needs to ask.
     */
    public static boolean wardenSettled(ServerSettlementData data) {
        Level level = data.getLevel();
        if (level == null) {
            return false;
        }
        Server server = level.getServer();
        return server != null && SkywatchWorldData.hasWarden(server);
    }

    // --- gates that read the settlement itself -------------------------

    /**
     * §11: Mortimer arrives "after building a graveyard in the settlement".
     *
     * There is no vanilla "graveyard" room type — {@code SettlementRoomsManager}
     * knows bedrooms, kitchens and the like, not burial grounds — so the test is
     * the thing a player would actually build: gravestones standing inside the
     * settlement bounds. Three of them, so a single decorative headstone in a
     * garden is not a cemetery.
     */
    public static final Gate GRAVEYARD = data -> countObjects(data, graveObjects(), 3) >= 3;

    /**
     * §11: Caspern arrives once the Spirit Forge is built. The mod has no
     * Spirit Forge yet; the Aether Forge is its forge, it is already a
     * {@code SettlementWorkstationObject}, and it is the station Caspern would
     * be put to work at, so that is the one he waits for.
     */
    public static final Gate FORGE = data ->
            countObjects(data, new int[]{SkyProfessions.aetherForgeID}, 1) >= 1;

    /**
     * §5: Eveleen is unlocked by "discovering an Eden island + collecting three
     * Eden plants". The Garden of Eden is not built yet, and this deliberately
     * does not pretend it is: what exists today is Eden grass
     * ({@code overgrownedentile}), planted from seed found in sky crates. A
     * patch of it in the settlement is the nearest true statement of "this
     * player has Eden growing", and it is a thing they did on purpose.
     */
    public static final Gate EDEN_PATCH = data ->
            countTiles(data, SkyRegistry.overgrownEdenID, 9) >= 9;

    // --- the survey ----------------------------------------------------

    /**
     * Vanilla gravestones, resolved once the object registry is closed.
     *
     * Object IDs are assigned at registration, so they cannot be constants;
     * they also never change inside a run, so one resolution is enough.
     */
    private static int[] graveObjectIDs;

    private static final String[] GRAVE_OBJECT_IDS = {
            "gravestone1", "gravestone2", "cryptgravestone1", "cryptgravestone2", "sarcophagus",
    };

    private static int[] graveObjects() {
        int[] cached = graveObjectIDs;
        if (cached != null) {
            return cached;
        }
        int[] ids = new int[GRAVE_OBJECT_IDS.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ObjectRegistry.getObjectID(GRAVE_OBJECT_IDS[i]);
        }
        graveObjectIDs = ids;
        return ids;
    }

    /**
     * How many tiles inside the settlement carry one of these objects, stopping
     * at {@code enough}.
     *
     * The rectangle is the settlement's own
     * ({@code SettlementBoundsManager.getTileRectangle}), which is 5x5 regions
     * at flag tier 0 and 17x17 at the top tier. That is a lot of array reads
     * for a big settlement, which is why every caller passes a small
     * {@code enough} and the loop returns the moment it is reached, and why
     * this only ever runs on a visitor roll — minutes apart, never per tick.
     */
    private static int countObjects(ServerSettlementData data, int[] objectIDs, int enough) {
        Level level = data.getLevel();
        if (level == null || objectIDs == null || objectIDs.length == 0) {
            return 0;
        }
        Rectangle bounds = data.boundsManager.getTileRectangle();
        int found = 0;
        for (int tileY = bounds.y; tileY < bounds.y + bounds.height; tileY++) {
            for (int tileX = bounds.x; tileX < bounds.x + bounds.width; tileX++) {
                if (!level.isTileWithinBounds(tileX, tileY)) {
                    continue;
                }
                int id = level.getObjectID(tileX, tileY);
                if (id == 0) {
                    continue;
                }
                for (int wanted : objectIDs) {
                    if (wanted >= 0 && id == wanted) {
                        if (++found >= enough) {
                            return found;
                        }
                        break;
                    }
                }
            }
        }
        return found;
    }

    /** The same survey, for a ground tile rather than an object. */
    private static int countTiles(ServerSettlementData data, int tileID, int enough) {
        Level level = data.getLevel();
        if (level == null || tileID < 0) {
            return 0;
        }
        Rectangle bounds = data.boundsManager.getTileRectangle();
        int found = 0;
        for (int tileY = bounds.y; tileY < bounds.y + bounds.height; tileY++) {
            for (int tileX = bounds.x; tileX < bounds.x + bounds.width; tileX++) {
                if (!level.isTileWithinBounds(tileX, tileY)) {
                    continue;
                }
                if (level.getTileID(tileX, tileY) == tileID && ++found >= enough) {
                    return found;
                }
            }
        }
        return found;
    }

}
