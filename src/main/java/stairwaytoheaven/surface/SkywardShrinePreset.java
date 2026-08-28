package stairwaytoheaven.surface;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SignObjectEntity;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyCloudmarbleSet;
import stairwaytoheaven.SkyRegistry;

/**
 * POI 2 — <b>Derelict Skyward Shrine</b>. Somebody tried to build a Stairway
 * here, a very long time ago, and did not finish it.
 *
 * <p>A broken ring of Cloudmarble on Skyway paving with a Marble Chequer
 * plinth at its centre, a Sky Seraph standing on the plinth, two street lamps
 * still alight on the approach, and a weathered sign that says what the place
 * was for. The ring is deliberately incomplete: four axis gaps plus stones
 * missing at random, so it reads as a ruin rather than as a building.
 *
 * <h2>The sign</h2>
 * Vanilla's own {@code AbandonedCampPreset} is the pattern: place a
 * {@code sign}, then reach the {@link SignObjectEntity} the object layer
 * created and hand it a {@link LocalMessage}. A raw string would not translate;
 * a {@code LocalMessage} is resolved per reader, so the German client reads
 * German. The key lives in {@code [misc]} in both locale files.
 *
 * <h2>Multi-tile objects</h2>
 * Same contract as every other preset in this repo: {@code applyToLevel} does
 * not run multi-tile placement, so the Storm Crystal's {@code "r"} half is
 * written explicitly on the tile to its right.
 */
public class SkywardShrinePreset extends Preset {

    public static final int SIZE = 15;
    private static final int C = SIZE / 2;

    /** Counted by the probe to prove the shrine stamped in the real world. */
    public static final String SIGNATURE_OBJECT = "seraphstatue";

    /**
     * The lore key on the shrine's sign. Named in en.lang and de.lang — and
     * written out as a literal at the {@code LocalMessage} call below, so
     * {@code tools/locale_audit.py} can see it (the audit can only follow keys
     * that appear literally at the call site; a constant becomes a "built at
     * runtime" note it cannot check).
     */
    public static final String SIGN_KEY = "swhshrinesign";

    public SkywardShrinePreset(GameRandom random) {
        super(SIZE, SIZE);

        final int paving = SurfaceMaterials.known("tile:skywaytile", SkyCloudmarbleSet.skywayTileID);
        final int plinth = SurfaceMaterials.known("tile:marblecheckertile", SkyRegistry.marbleCheckerID);
        final int wall = SurfaceMaterials.known("object:cloudmarblewall", SkyCloudmarbleSet.cloudmarbleWallID);
        final int rail = SurfaceMaterials.known("object:cloudmarblefence", SkyCloudmarbleSet.cloudmarbleFenceID);
        final int seraph = SurfaceMaterials.known("object:seraphstatue", SkyCloudmarbleSet.seraphStatueID);
        final int lamp = SurfaceMaterials.known("object:wardencandelabra", SkyRegistry.wardenCandelabraID);
        final int rubble = SurfaceMaterials.known("object:skywatchrubble", SkyRegistry.skywatchRubbleID);
        final int shrub = SurfaceMaterials.obj("withershrub");
        final int shards = SurfaceMaterials.known("object:aurorashards", SkyRegistry.auroraShardsID);
        final int crystal = SurfaceMaterials.known("object:stormcrystal", SkyRegistry.stormCrystalID);
        final int crystalR = SurfaceMaterials.known("object:stormcrystalr", SkyRegistry.stormCrystalRID);
        final int sign = SurfaceMaterials.obj("sign");

        // ------------------------------------------------------------ ground
        // A paved disc of radius 5 with a broken edge. Everything it covers is
        // cleared; everything outside is left as vanilla generated it.
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                double dx = x - C;
                double dy = y - C;
                double d = Math.sqrt(dx * dx + dy * dy);
                double wobble = 0.7 * Math.sin(x * 2.1 - y * 1.1) + 0.4 * Math.cos(x * 1.3 + y * 0.7);
                if (d + wobble * 0.6 > 5.4) {
                    continue;
                }
                this.setObject(x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.TILE_LAYER, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, x, y, 0);
                this.setTile(x, y, paving);
            }
        }
        // The plinth: the mod's chequer, used the way DESIGN says it should be
        // used — as a 3x3 monument base, never as a room floor.
        this.fillTile(C - 1, C - 1, 3, 3, plinth);

        // -------------------------------------------------------- the ring --
        // Radius 4, four axis gaps for the ways in, and roughly a third of the
        // remaining stones fallen. Ruins are legible because they are broken in
        // an uneven way, not because they are half as tall.
        for (int a = 0; a < 32; a++) {
            double ang = a * Math.PI / 16.0;
            int x = (int) Math.round(C + Math.cos(ang) * 4.0);
            int y = (int) Math.round(C + Math.sin(ang) * 4.0);
            boolean axisGap = (x == C && Math.abs(y - C) >= 3) || (y == C && Math.abs(x - C) >= 3);
            if (axisGap || random.getChance(0.32F)) {
                // A fallen stone leaves its rubble behind.
                if (!axisGap && random.getChance(0.5F)) {
                    this.setObject(x, y, rubble);
                }
                continue;
            }
            this.setObject(x, y, wall);
        }

        // ------------------------------------------------- what stands in it
        this.setObject(C, C, seraph);
        // Two lamps still burning on the south approach, and a low rail either
        // side of them so the way in reads as a way in.
        this.setObject(C - 2, C + 3, lamp);
        this.setObject(C + 2, C + 3, lamp);
        this.setObject(C - 3, C + 3, rail);
        this.setObject(C + 3, C + 3, rail);
        // A little colour left in the place: aurora shards on the plinth edge,
        // and a storm crystal that has grown up through the cracked paving.
        this.setObject(C + 1, C - 2, shards);
        this.setObject(C - 3, C - 1, crystal);
        this.setObject(C - 2, C - 1, crystalR);
        // Dead planting where the shrine's garden used to be.
        this.setObject(C - 1, C + 2, shrub);
        this.setObject(C + 2, C + 1, shrub);
        this.setObject(C + 3, C - 2, rubble);
        this.setObject(C - 4, C + 1, rubble);

        // ------------------------------------------------------- the sign ---
        // Rotation 2 = facing down, i.e. readable by somebody walking up to the
        // shrine from the south, which is where the lamps are.
        int signX = C;
        int signY = C + 4;
        this.setObject(signX, signY, sign, 2);
        this.addCustomApply(signX, signY, 0, (level, levelX, levelY, dir, blackboard) -> {
            try {
                ObjectEntity objEnt = level.entityManager.getObjectEntity(levelX, levelY);
                if (objEnt instanceof SignObjectEntity) {
                    ((SignObjectEntity) objEnt).setMessage(new LocalMessage("misc", "swhshrinesign"));
                } else if (level.isServer()) {
                    throw new NullPointerException(
                            "Could not find a sign objectEntity for the skyward shrine at " + levelX + ", " + levelY);
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            return null;
        });
    }
}
