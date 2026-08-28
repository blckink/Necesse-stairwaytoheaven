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

    /**
     * Mirror of the two cats' "lives at the spire now" flags.
     *
     * The authoritative copy is still {@link SkywatchQuestData}, which is
     * LevelData on the Skyreach. That is right for the lair and basket
     * COORDINATES -- they describe that level -- and wrong for the fact that a
     * player already spent a Cloudpuff Treat, which is progression.
     *
     * The integration test caught a coaxed cat losing its flag across a
     * restart: phase 1 saw both cats at the basket, phase 2 after the restart
     * saw {@code homeFlags black=false tabby=false} and both back at their
     * lairs. It reproduces on some world seeds and not others, and I have NOT
     * root-caused which write is lost -- the plausible candidates all involve
     * the Skyreach level object being replaced between the coax and the save
     * ({@code LevelManager} line 55-61 unloads and overwrites an existing
     * identifier, and {@code Server} unloads-then-saves a quiet level).
     *
     * Rather than guess, this removes the failure class: the flag is written
     * here too, and {@link SkywatchQuestData#get} folds it back in. A level
     * record that lost the write is repaired the next time it is read; a world
     * record cannot be lost to a level unload because it is not on a level.
     */
    public boolean blackHome = false;
    public boolean tabbyHome = false;

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addBoolean("wardenRecruited", this.wardenRecruited);
        save.addLong("wardenAuth", this.wardenAuth);
        save.addBoolean("blackHome", this.blackHome);
        save.addBoolean("tabbyHome", this.tabbyHome);
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.wardenRecruited = save.getBoolean("wardenRecruited", this.wardenRecruited, false);
        this.wardenAuth = save.getLong("wardenAuth", this.wardenAuth, false);
        this.blackHome = save.getBoolean("blackHome", this.blackHome, false);
        this.tabbyHome = save.getBoolean("tabbyHome", this.tabbyHome, false);
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

    /** Records a cat as living at the spire. Never un-records one. */
    public void markCatHome(boolean isBlackCat) {
        if (isBlackCat) {
            this.blackHome = true;
        } else {
            this.tabbyHome = true;
        }
    }

    /** Convenience: does this world already have a recruited Warden? */
    public static boolean hasWarden(Server server) {
        SkywatchWorldData data = get(server);
        return data != null && data.wardenRecruited;
    }
}
