package stairwaytoheaven.quest;

import necesse.engine.network.server.Server;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.worldData.WorldData;

/**
 * The one fact about the Warden that must outlive a dimension.
 *
 * {@link SkywatchQuestData} is a {@code LevelData} and therefore lives and dies
 * with the Skyreach level. That is right for everything about the spire, and
 * wrong for exactly one bit: whether this world has already recruited a Sky
 * Warden. When {@code SkyRegistry.WORLD_GENERATION} is bumped, the mod starts a
 * FRESH Skyreach ("skyreach2"), so its quest data says {@code recruited=false}
 * -- while the Warden the player paid for is still standing in their settlement
 * on the surface. Without a world-scoped record the newly stamped spire would
 * hand out a second Warden, and vanilla's recruit page would happily take the
 * fee a second time (nothing in {@code HumanShop} is per-mob-type).
 *
 * So this is deliberately a {@code WorldData}: it is written next to the
 * settlements in the world entity, not in a level file, and survives every
 * generation bump. Server-side only -- nothing reads it on the client, so it is
 * never packed into a world packet.
 */
public class SkywatchWorldData extends WorldData {

    /** Must match {@code [a-zA-Z0-9]+} -- WorldEntity.addWorldData enforces it. */
    public static final String KEY = "skywatchworld";

    /** True once ANY Sky Warden in this world has become a settler. */
    public boolean wardenRecruited = false;

    /** Auth of the player who paid, for multiplayer bookkeeping. 0 = unknown. */
    public long wardenAuth = 0L;

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addBoolean("wardenRecruited", this.wardenRecruited);
        save.addLong("wardenAuth", this.wardenAuth);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.wardenRecruited = save.getBoolean("wardenRecruited", this.wardenRecruited, false);
        this.wardenAuth = save.getLong("wardenAuth", this.wardenAuth, false);
    }

    /**
     * Records that this world has its Warden. Idempotent, and never downgrades:
     * once true it stays true, and the first auth wins so a second player
     * talking to him cannot rewrite whose contract it was.
     */
    public void markRecruited(long auth) {
        this.wardenRecruited = true;
        if (this.wardenAuth == 0L) {
            this.wardenAuth = auth;
        }
    }

    /**
     * The world record, created on first use. Mirrors
     * {@link SkywatchQuestData#get(necesse.level.maps.Level)}: a world that
     * never had one simply gets an empty record, which reads as "no Warden yet".
     */
    public static SkywatchWorldData get(Server server) {
        if (server == null || server.world == null) {
            return null;
        }
        WorldEntity worldEntity = server.world.worldEntity;
        if (worldEntity == null) {
            return null;
        }
        WorldData data = worldEntity.getWorldData(KEY);
        if (data instanceof SkywatchWorldData) {
            return (SkywatchWorldData) data;
        }
        SkywatchWorldData created = new SkywatchWorldData();
        worldEntity.addWorldData(KEY, created);
        return created;
    }

    /** Convenience: does this world already have a recruited Warden? */
    public static boolean hasWarden(Server server) {
        SkywatchWorldData data = get(server);
        return data != null && data.wardenRecruited;
    }
}
