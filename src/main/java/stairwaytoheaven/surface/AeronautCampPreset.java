package stairwaytoheaven.surface;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SignObjectEntity;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;

/**
 * POI 3 — <b>Stranded Aeronaut Camp</b>. Somebody came DOWN out of the sky
 * here, made camp beside the wreck, and eventually walked away from it.
 *
 * <p>Built out of the three sky oddities the mod already registers —
 * {@code aeronautwreck}, {@code skyballoon}, {@code skyparcel} — plus a vanilla
 * camp kit: a big tent, a campfire, a barrel and a chest. A short Skyiron fence
 * on the windward side, an oil lantern still burning, and a sign the aeronaut
 * left behind.
 *
 * <h2>Multi-tile objects</h2>
 * {@code bigtent} is vanilla's only 2x2 static multi-object:
 * {@code BigTentObject.registerTent} registers the four parts at multi
 * coordinates (0,0), (1,0), (0,1) and (1,1) — i.e. {@code bigtent},
 * {@code bigtent2}, {@code bigtent3}, {@code bigtent4} laid out as
 * <pre>
 *     bigtent   bigtent2
 *     bigtent3  bigtent4
 * </pre>
 * A preset does no multi-tile placement, so all four are written here. Vanilla's
 * own {@code TravellersCampsitePreset} writes exactly that quad.
 */
public class AeronautCampPreset extends Preset {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 13;

    /** Counted by the probe to prove the camp stamped in the real world. */
    public static final String SIGNATURE_OBJECT = "aeronautwreck";

    /**
     * The note the aeronaut left. Named in en.lang and de.lang — and written
     * out as a literal at the {@code LocalMessage} call below so
     * {@code tools/locale_audit.py} can check it.
     */
    public static final String SIGN_KEY = "swhaeronautsign";

    /**
     * Camp supplies. Deliberately mundane: rations, rope-grade materials and a
     * little sky salvage. Nothing here unlocks anything — the aeronaut was
     * stranded, not rich.
     */
    public static final LootTable CAMP_LOOT = new LootTable(
            LootItem.between("skystone", 3, 8),
            LootItem.between("windsilk", 2, 5),
            new ChanceLootItem(0.6F, "cloudberry", r -> r.getIntBetween(2, 6)),
            new ChanceLootItem(0.4F, "stormshard", r -> r.getIntBetween(1, 2)),
            new ChanceLootItem(0.3F, "aetheriumore", r -> r.getIntBetween(1, 3)),
            new ChanceLootItem(0.45F, "coin", r -> r.getIntBetween(40, 160)));

    public AeronautCampPreset(GameRandom random) {
        super(WIDTH, HEIGHT);

        final int trodden = SurfaceMaterials.tile("graveltile");
        final int path = SurfaceMaterials.tile("woodpathtile");

        final int wreck = SurfaceMaterials.obj("aeronautwreck");
        final int balloon = SurfaceMaterials.obj("skyballoon");
        final int parcel = SurfaceMaterials.obj("skyparcel");
        final int tent = SurfaceMaterials.obj("bigtent");
        final int tent2 = SurfaceMaterials.obj("bigtent2");
        final int tent3 = SurfaceMaterials.obj("bigtent3");
        final int tent4 = SurfaceMaterials.obj("bigtent4");
        final int campfire = SurfaceMaterials.obj("campfire");
        final int chest = SurfaceMaterials.obj("oakchest");
        final int barrel = SurfaceMaterials.obj("barrel");
        final int lantern = SurfaceMaterials.obj("oillantern");
        final int sign = SurfaceMaterials.obj("sign");
        final int fence = SurfaceMaterials.known("object:skyironfence", SkyRegistry.skyironFenceID);
        final int rubble = SurfaceMaterials.known("object:skywatchrubble", SkyRegistry.skywatchRubbleID);

        // ------------------------------------------------------------ ground
        // A trodden oval, so the camp sits in a clearing rather than inside a
        // hedge. Outside it, vanilla's own terrain is untouched.
        double cx = (WIDTH - 1) / 2.0;
        double cy = (HEIGHT - 1) / 2.0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                double dx = (x - cx) / 6.4;
                double dy = (y - cy) / 5.4;
                double wobble = 0.10 * Math.sin(x * 1.9 + y * 1.4);
                if (dx * dx + dy * dy + wobble > 1.0) {
                    continue;
                }
                this.setObject(x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.TILE_LAYER, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, x, y, 0);
                this.setTile(x, y, trodden);
            }
        }
        // The bit of decking they laid between the tent and the fire.
        this.fillTile(5, 5, 4, 3, path);

        // -------------------------------------------------------- the wreck --
        // The reason the camp is here at all, on the windward edge with the
        // deflated envelope still tangled beside it.
        this.setObject(10, 4, wreck);
        this.setObject(11, 5, balloon);
        this.setObject(9, 3, rubble);
        this.setObject(11, 3, rubble);

        // ----------------------------------------------------------- the camp
        // Tent quad. Master top-left; the other three parts are written
        // explicitly because a preset does no multi-tile placement.
        this.setObject(3, 4, tent);
        this.setObject(4, 4, tent2);
        this.setObject(3, 5, tent3);
        this.setObject(4, 5, tent4);

        this.setObject(6, 6, campfire);
        this.setObject(4, 8, chest, 2);
        this.addInventory(CAMP_LOOT, random, 4, 8);
        this.setObject(6, 8, barrel);
        this.setObject(8, 8, parcel);
        this.setObject(9, 7, parcel);
        this.setObject(3, 7, parcel);
        this.setObject(8, 5, lantern);

        // A short windbreak on the north side: four posts in a straight run, so
        // the fence's four-orthogonal-neighbour connection actually forms a
        // line instead of a row of lone posts.
        for (int x = 5; x <= 9; x++) {
            this.setObject(x, 2, fence);
        }

        // -------------------------------------------------------- the note ---
        // Rotation 2 = facing down, readable from the south approach.
        int signX = 7;
        int signY = 10;
        this.setObject(signX, signY, sign, 2);
        this.addCustomApply(signX, signY, 0, (level, levelX, levelY, dir, blackboard) -> {
            try {
                ObjectEntity objEnt = level.entityManager.getObjectEntity(levelX, levelY);
                if (objEnt instanceof SignObjectEntity) {
                    ((SignObjectEntity) objEnt).setMessage(new LocalMessage("misc", "swhaeronautsign"));
                } else if (level.isServer()) {
                    throw new NullPointerException(
                            "Could not find a sign objectEntity for the aeronaut camp at " + levelX + ", " + levelY);
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            return null;
        });
    }
}
