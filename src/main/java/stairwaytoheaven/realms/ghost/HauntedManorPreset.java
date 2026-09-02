package stairwaytoheaven.realms.ghost;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.Preset;

/** A compact deadwood manor with a guarded reward room. */
public class HauntedManorPreset extends Preset {
    public static final int WIDTH = 15;
    public static final int HEIGHT = 13;

    public HauntedManorPreset(GameRandom random) {
        super(WIDTH, HEIGHT);
        int wall = ObjectRegistry.getObjectID("deadwoodwall");
        int door = ObjectRegistry.getObjectID("deadwooddoor");
        int chest = ObjectRegistry.getObjectID("bonechest");
        int chair = ObjectRegistry.getObjectID("deadwoodchair");
        int table = ObjectRegistry.getObjectID("deadwoodmodulartable");
        int light = ObjectRegistry.getObjectID("deadwoodcandelabra");
        for (int y = 1; y < HEIGHT - 1; y++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                this.setTile(x, y, GhostRealm.blackCobbleID);
                if (x == 1 || x == WIDTH - 2 || y == 1 || y == HEIGHT - 2) {
                    this.setObject(x, y, wall);
                }
            }
        }
        this.setObject(WIDTH / 2, HEIGHT - 2, door);
        this.setObject(4, 5, table);
        this.setObject(3, 5, chair, (byte) 1);
        this.setObject(5, 5, chair, (byte) 3);
        this.setObject(10, 5, table);
        this.setObject(9, 5, chair, (byte) 1);
        this.setObject(11, 5, chair, (byte) 3);
        this.setObject(3, 3, light);
        this.setObject(11, 3, light);
        this.setObject(7, 3, chest);
        this.addInventory(new LootTable(
                LootItem.between("ectoplasm", 10, 18),
                LootItem.between("soulthread", 5, 10),
                ChanceLootItem.between(0.65F, "spectralore", 4, 8),
                ChanceLootItem.between(0.30F, "spiritsteelbar", 1, 3)),
                random, 7, 3, new Object[0]);
    }
}
