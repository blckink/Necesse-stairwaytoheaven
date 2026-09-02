package stairwaytoheaven.realms.ghost;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;

/** An open grave-field whose raised centre keeps its cache above the marsh. */
public class SunkenGraveyardPreset extends Preset {
    public static final int WIDTH = 15;
    public static final int HEIGHT = 15;

    public SunkenGraveyardPreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        int fence = ObjectRegistry.getObjectID("cryptfence");
        int gate = ObjectRegistry.getObjectID("cryptfencegate");
        int chest = ObjectRegistry.getObjectID("bonechest");
        int grave1 = ObjectRegistry.getObjectID("cryptgravestone1");
        int grave2 = ObjectRegistry.getObjectID("cryptgravestone2");
        for (int y = 1; y < HEIGHT - 1; y++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                this.setTile(x, y, GhostRealm.graveyardSoilID);
                if (x == 1 || x == WIDTH - 2 || y == 1 || y == HEIGHT - 2) {
                    this.setObject(x, y, fence);
                }
            }
        }
        this.setObject(WIDTH / 2, HEIGHT - 2, gate);
        for (int y = 4; y <= 10; y += 3) {
            for (int x = 3; x <= 11; x += 4) {
                this.setObject(x, y, ((x + y) & 1) == 0 ? grave1 : grave2);
            }
        }
        this.setTile(7, 7, GhostRealm.spiritStoneID);
        this.setObject(7, 7, chest);
        this.addInventory(new LootTable(
                LootItem.between("ectoplasm", 8, 16),
                ChanceLootItem.between(0.75F, "bonewood", 5, 12),
                ChanceLootItem.between(0.55F, "soulthread", 4, 8),
                ChanceLootItem.between(0.25F, "spiritsteelbar", 1, 2)),
                random, 7, 7, new Object[0]);
    }
}
