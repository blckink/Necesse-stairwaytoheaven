package stairwaytoheaven.realms.ghost;

import necesse.engine.registries.ObjectRegistry;
import stairwaytoheaven.SkyRegistry;
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
        // Vanilla has NO deadwood wall or door: its deadwood set covers
        // furniture only, and a wall's door is cut from the wall's own 352x128
        // sheet rather than shipped as a separate object, so `deadwoodwall` and
        // `deadwooddoor` were both ObjectRegistry misses returning -1. The
        // manor was stamping id -1 around its whole perimeter and for its door,
        // which is a house with no walls. Nightfell is the mod's own dark
        // building set and is exactly what a haunted manor wants.
        int wall = SkyRegistry.nightfellWallID;
        int door = SkyRegistry.nightfellDoorID;
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
        // The realm's two weapons live here as well as on the elite drop table
        // (GhostLoot.elite) -- docs/FOGKEY_AND_BOSSPORTALS.md A3.3: "the same
        // ghost weapons also drop randomly in the Ghost region, so a player who
        // never trades still finds them." A manor chest is the one place in the
        // Aftergarden where finding a weapon reads as finding a weapon.
        //
        // 20% each, against the 30% this same chest already gives a single
        // Spiritsteel bar: a finished weapon is worth about eight of those bars
        // (see GhostGuideMob's price list), but there is only one manor and one
        // chest in it, so the chance is a rung below the bar rather than an
        // order of magnitude below it the way the repeatable mob table is.
        this.addInventory(new LootTable(
                LootItem.between("ectoplasm", 10, 18),
                LootItem.between("soulthread", 5, 10),
                ChanceLootItem.between(0.65F, "spectralore", 4, 8),
                ChanceLootItem.between(0.30F, "spiritsteelbar", 1, 3),
                ChanceLootItem.between(0.20F, "spiritsteelreaver", 1, 1),
                ChanceLootItem.between(0.20F, "gravewindbow", 1, 1)),
                random, 7, 3, new Object[0]);
    }
}
