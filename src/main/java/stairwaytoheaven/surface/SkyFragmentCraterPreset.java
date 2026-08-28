package stairwaytoheaven.surface;

import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;
import stairwaytoheaven.SkyRegistry;

/**
 * POI 1 — <b>Fallen Sky Fragment</b>. A piece of the Skyreach came down here.
 *
 * <p>A ragged crater: a scorched Stormslate core inside a Skystone debris
 * field, Aetherium nodes cracked open by the impact, Skystone boulders thrown
 * onto the rim, and a charred strongbox that came down with it. One lit
 * {@code starfall} accent, so the crater reads at night.
 *
 * <h2>What it writes, and what it deliberately does not</h2>
 * A fresh {@link Preset} is filled with −1 ("leave alone") in every layer
 * ({@code Preset.clearPreset}), so this preset only touches the tiles it names.
 * Outside the crater disc the surface stays exactly as vanilla generated it —
 * that is the whole reason the POI is built as a disc and not as a rectangle.
 *
 * <h2>Multi-tile objects</h2>
 * {@code Preset.applyToLevel} writes IDs with the raw object-layer setter
 * ({@code level.objectLayer.setObject}) and never runs
 * {@code MultiTile.placeObject}, so every half of a multi-tile piece has to be
 * written here. The Storm Crystal is a 2x1 pair — base plus its {@code "r"}
 * counterpart on the tile to the RIGHT, exactly the way
 * {@code SkyTerrainPainter} writes it. See docs/references/presets/README.md.
 */
public class SkyFragmentCraterPreset extends Preset {

    /** Plot size. Odd, so there is a real centre tile. */
    public static final int SIZE = 13;
    private static final int C = SIZE / 2;

    /**
     * The signature object of this POI. The probe counts these in the real
     * world to prove the crater actually stamped rather than merely queued.
     */
    public static final String SIGNATURE_OBJECT = "aetheriumrock";

    /**
     * What the strongbox holds. Sky materials, in amounts that are a taste and
     * not a supply: the Tempest Edge wants 8 Aetherium Bars, i.e. 24 ore, so
     * even a lucky crater cannot skip the trip up the Stairway. Nothing in here
     * is a vanilla progression item, a tool or a weapon.
     */
    public static final LootTable CRATER_LOOT = new LootTable(
            LootItem.between("skystone", 6, 14),
            LootItem.between("aetheriumore", 2, 5),
            new ChanceLootItem(0.5F, "stormshard", r -> r.getIntBetween(1, 3)),
            new ChanceLootItem(0.25F, "aurorapetal", r -> r.getIntBetween(1, 2)),
            new ChanceLootItem(0.35F, "coin", r -> r.getIntBetween(60, 220)));

    public SkyFragmentCraterPreset(GameRandom random) {
        super(SIZE, SIZE);

        final int skystone = SurfaceMaterials.known("tile:skystonetile", SkyRegistry.skystoneTileID);
        final int slate = SurfaceMaterials.known("tile:stormslatetile", SkyRegistry.stormslateID);
        final int gravel = SurfaceMaterials.tile("graveltile");

        final int rock = SurfaceMaterials.known("object:skystonerock", SkyRegistry.skystoneRockID);
        final int ore = SurfaceMaterials.known("object:aetheriumrock", SkyRegistry.aetheriumRockID);
        final int rubble = SurfaceMaterials.known("object:skywatchrubble", SkyRegistry.skywatchRubbleID);
        final int starfall = SurfaceMaterials.known("object:starfall", SkyRegistry.starfallID);
        final int screed = SurfaceMaterials.obj("stormscreed");
        final int chest = SurfaceMaterials.obj("deadwoodchest");
        final int crystal = SurfaceMaterials.known("object:stormcrystal", SkyRegistry.stormCrystalID);
        final int crystalR = SurfaceMaterials.known("object:stormcrystalr", SkyRegistry.stormCrystalRID);

        // ------------------------------------------------------------ ground
        // Three concentric bands with a hashed edge, so the crater has an
        // irregular outline instead of reading as a drawn circle.
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                double dx = x - C;
                double dy = y - C;
                double d = Math.sqrt(dx * dx + dy * dy);
                double wobble = 0.9 * Math.sin(x * 1.7 + y * 2.3) + 0.5 * Math.cos(x * 0.9 - y * 1.3);
                double edge = d + wobble * 0.55;
                if (edge > 5.6) {
                    continue;                       // untouched vanilla surface
                }
                // Everything the crater covers is cleared first: a tree the
                // island generator dropped here would otherwise be standing in
                // the middle of an impact site.
                this.setObject(x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.TILE_LAYER, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.WALL_DECOR, x, y, 0);
                this.setObjectLayer(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, x, y, 0);
                if (edge <= 2.1) {
                    this.setTile(x, y, slate);      // scorched core
                } else if (edge <= 4.5) {
                    this.setTile(x, y, skystone);   // sky-stone debris field
                } else {
                    this.setTile(x, y, gravel);     // thrown-out apron
                }
            }
        }

        // ----------------------------------------------------------- the find
        // The fragment itself: two Aetherium nodes split open on the core, with
        // Skystone boulders where the impact threw the shell.
        this.setObject(C, C, ore);
        this.setObject(C - 2, C + 1, ore);
        this.setObject(C + 2, C - 1, rock);
        this.setObject(C - 1, C - 2, rock);
        this.setObject(C + 1, C + 2, rock);
        this.setObject(C + 3, C + 2, rock);
        this.setObject(C - 3, C - 2, rock);

        // A storm crystal grew straight out of the hot core. 2x1: master here,
        // its "r" half on the tile to the right — both written, or
        // Region.checkGenerationValid deletes the torso.
        this.setObject(C - 1, C + 3, crystal);
        this.setObject(C, C + 3, crystalR);

        // Scorch and debris on the rim.
        this.setObject(C + 4, C, screed);
        this.setObject(C - 4, C + 1, screed);
        this.setObject(C + 1, C - 4, screed);
        this.setObject(C - 2, C + 4, rubble);
        this.setObject(C + 3, C - 3, rubble);
        this.setObject(C - 4, C - 1, rubble);
        // One lit accent, so a crater is something you notice from a distance
        // at night rather than a grey patch you walk past.
        this.setObject(C + 2, C + 3, starfall);

        // The strongbox that came down with it, on the debris field rather than
        // in the core so it is reachable without mining first.
        int chestX = C - 3;
        int chestY = C + 2;
        this.setObject(chestX, chestY, chest, 2);
        this.addInventory(CRATER_LOOT, random, chestX, chestY);
    }
}
